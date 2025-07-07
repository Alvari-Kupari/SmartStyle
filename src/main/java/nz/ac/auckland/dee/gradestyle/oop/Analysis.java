package nz.ac.auckland.dee.gradestyle.oop;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import io.vavr.control.Either;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.config.Config;
import nz.ac.auckland.dee.gradestyle.oop.Declaration.Nesting;
import nz.ac.auckland.dee.gradestyle.util.FileUtils;
import nz.ac.auckland.dee.gradestyle.util.JavaParser;

public class Analysis {
  private Config config;

  private List<String> templateNames;

  public Analysis(Config config) {
    this.config = config;
  }

  public List<AnalysisResult> analyse(List<Repo> repos) throws IOException {
    analyseTemplate();

    List<AnalysisResult> results = new ArrayList<>();

    for (Repo repo : repos) {
      System.out.print("Analysing " + repo.getName() + "... ");
      results.add(analyseRepo(repo));
      System.out.println("done.");
    }

    return results;
  }

  private void analyseTemplate() throws IOException {
    if (templateNames != null) {
      return;
    }

    Path templatePath = config.getTemplateRepo();

    templateNames = new ArrayList<>();

    if (!Files.isDirectory(templatePath)) {
      return;
    }

    Repo templateRepo =
        new Repo(
            templatePath, config.getGithubClassroom(), templatePath.getFileName().toString(), null);

    templateNames =
        analyseRepo(templateRepo).getDeclarations().stream()
            .map(Declaration::getType)
            .map(Type::getLocalResolvedName)
            .toList();
  }

  private AnalysisResult analyseRepo(Repo repo) throws IOException {
    List<CompilationUnit> compilationUnits = new ArrayList<>();
    List<Declaration> declarations = new ArrayList<>();

    for (Path file : FileUtils.getJavaSrcFiles(repo).toList()) {
      ParseResult<CompilationUnit> result = JavaParser.get(repo).parse(file);

      if (!result.isSuccessful()) {
        System.err.println("OOP analysis of \"" + repo.getName() + "\" failed @ \"" + file + "\".");
        return new AnalysisResult(repo, file);
      }

      CompilationUnit cu = result.getResult().get();

      compilationUnits.add(cu);

      declarationVisitor(file).visit(cu, declarations);
    }

    for (CompilationUnit cu : compilationUnits) {
      relationshipVisitor().visit(cu, declarations);
    }

    for (CompilationUnit cu : compilationUnits) {
      Stream<ClassOrInterfaceType> instantiations =
          cu.findAll(ObjectCreationExpr.class).stream().map(NodeWithType::getType);

      Stream<ClassOrInterfaceType> referenceInstantiations =
          cu.findAll(MethodReferenceExpr.class).stream()
              .filter(m -> m.getIdentifier().equals("new"))
              .map(MethodReferenceExpr::getScope)
              .filter(type -> type instanceof TypeExpr)
              .map(type -> (TypeExpr) type)
              .map(TypeExpr::getType)
              .filter(type -> type instanceof ClassOrInterfaceType)
              .map(type -> (ClassOrInterfaceType) type);

      Stream<ClassOrInterfaceType> extended =
          cu.findAll(ClassOrInterfaceDeclaration.class).stream()
              .map(NodeWithExtends::getExtendedTypes)
              .flatMap(Collection::stream);

      Stream<Declaration> instantiatedOrExtended =
          Stream.concat(instantiations, Stream.concat(referenceInstantiations, extended))
              .map(type -> getDeclaration(declarations, type, ClassDeclaration.class));

      Stream<Declaration> implemented =
          cu.findAll(ClassOrInterfaceDeclaration.class).stream()
              .map(NodeWithImplements::getImplementedTypes)
              .flatMap(Collection::stream)
              .map(type -> getDeclaration(declarations, type, InterfaceDeclaration.class));

      Stream.concat(instantiatedOrExtended, implemented)
          .filter(Objects::nonNull)
          .forEach(declaration -> declaration.setReferenced(true));
    }

    declarations.stream()
        .filter(declaration -> declaration instanceof ClassDeclaration)
        .map(declaration -> (ClassDeclaration) declaration)
        .filter(Predicate.not(ClassDeclaration::isReferenced))
        .toList()
        .stream()
        .forEach(this::unreferenceDeclaration);

    return new AnalysisResult(repo, declarations);
  }

  private VoidVisitorAdapter<List<Declaration>> declarationVisitor(Path file) {
    return new VoidVisitorAdapter<List<Declaration>>() {
      @Override
      public void visit(ClassOrInterfaceDeclaration decl, List<Declaration> declarations) {
        super.visit(decl, declarations);

        String declName = decl.getFullyQualifiedName().get();
        Type type = new Type(getDisplayName(declName), declName);

        if (isTemplateName(declName)) {
          return;
        }

        int line = decl.getRange().get().begin.line;

        if (decl.isInterface()) {
          declarations.add(new InterfaceDeclaration(type, file, line));
          return;
        }

        declarations.add(new ClassDeclaration(type, file, line, decl.isAbstract()));
      }
    };
  }

  private VoidVisitorAdapter<List<Declaration>> relationshipVisitor() {
    return new VoidVisitorAdapter<List<Declaration>>() {
      @Override
      public void visit(ClassOrInterfaceDeclaration decl, List<Declaration> declarations) {
        super.visit(decl, declarations);

        String declName = decl.getFullyQualifiedName().get();

        if (isTemplateName(declName)) {
          return;
        }

        if (decl.isInterface()) {
          InterfaceDeclaration declaration =
              findDeclaration(declName, declarations, InterfaceDeclaration.class).getLeft();
          resolveInterface(decl, declaration, declarations);
        } else {
          ClassDeclaration declaration =
              findDeclaration(declName, declarations, ClassDeclaration.class).getLeft();
          resolveClass(decl, declaration, declarations);
        }
      }

      private void resolveInterface(
          ClassOrInterfaceDeclaration decl,
          InterfaceDeclaration declaration,
          List<Declaration> declarations) {
        if (decl.getExtendedTypes().size() == 0) {
          return;
        }

        String extendsInterface = getResolvedTypeName(decl.getExtendedTypes().get(0));
        Either<InterfaceDeclaration, Type> superInterface =
            findDeclaration(extendsInterface, declarations, InterfaceDeclaration.class);

        declaration.setSuperInterface(superInterface);

        if (superInterface.isLeft()) {
          superInterface.getLeft().getSubInterfaces().add(declaration);
        }

        resolveNesting(decl, declaration, declarations);
      }

      private void resolveClass(
          ClassOrInterfaceDeclaration decl,
          ClassDeclaration declaration,
          List<Declaration> declarations) {
        if (decl.getExtendedTypes().size() != 0) {
          String extendsClass = getResolvedTypeName(decl.getExtendedTypes().get(0));
          Either<ClassDeclaration, Type> superClass =
              findDeclaration(extendsClass, declarations, ClassDeclaration.class);

          declaration.setSuperClass(superClass);

          if (superClass.isLeft()) {
            superClass.getLeft().getSubClasses().add(declaration);
          }
        }

        List<Either<InterfaceDeclaration, Type>> interfaceDeclarations =
            decl.getImplementedTypes().stream()
                .map(Analysis.this::getResolvedTypeName)
                .map(
                    resolvedName ->
                        findDeclaration(resolvedName, declarations, InterfaceDeclaration.class))
                .toList();

        for (Either<InterfaceDeclaration, Type> implementsInterface : interfaceDeclarations) {
          declaration.getImplementsInterfaces().add(implementsInterface);

          if (implementsInterface.isLeft()) {
            implementsInterface.getLeft().getImplementedClasses().add(declaration);
          }
        }

        resolveNesting(decl, declaration, declarations);
      }

      private <T extends TypeDeclaration<T>> void resolveNesting(
          TypeDeclaration<T> decl, Declaration declaration, List<Declaration> declarations) {
        if (!decl.isNestedType()) {
          return;
        }

        @SuppressWarnings("unchecked")
        TypeDeclaration<ClassOrInterfaceDeclaration> outerType =
            (TypeDeclaration<ClassOrInterfaceDeclaration>) decl.getParentNode().get();
        String outerClassName = outerType.getFullyQualifiedName().get();
        Either<Declaration, Type> outerClass =
            findDeclaration(outerClassName, declarations, Declaration.class);

        if (decl instanceof ClassOrInterfaceDeclaration classDecl) {
          if (classDecl.isInnerClass()) {
            declaration.setOuterDeclaration(outerClass, Nesting.Inner);

            if (outerClass.isLeft()) {
              outerClass.getLeft().getInnerDeclarations().add(declaration);
            }
          } else {
            declaration.setOuterDeclaration(outerClass, Nesting.Nested);

            if (outerClass.isLeft()) {
              outerClass.getLeft().getNestedDeclarations().add(declaration);
            }
          }
        }
      }

      private <T extends Declaration> Either<T, Type> findDeclaration(
          String name, List<Declaration> declarations, Class<T> t) {
        Either<T, Type> type = Either.right(new Type(getDisplayName(name), name));

        if (isTemplateName(name)) {
          return type;
        }

        for (Declaration declaration : declarations) {
          if (declaration.getType().getLocalResolvedName().equals(name)) {
            return Either.left(t.cast(declaration));
          }
        }

        return type;
      }
    };
  }

  private <T extends Declaration> Declaration getDeclaration(
      List<Declaration> declarations, ClassOrInterfaceType type, Class<T> t) {
    for (Declaration declaration : declarations.stream().filter(t::isInstance).toList()) {
      if (declaration.getType().getLocalResolvedName().equals(getResolvedName(type))) {
        return declaration;
      }
    }

    return null;
  }

  private void unreferenceDeclaration(ClassDeclaration declaration) {
    Either<ClassDeclaration, Type> superClass = declaration.getSuperClass();

    if (superClass == null || superClass.isRight()) {
      return;
    }

    ClassDeclaration superDeclaration = superClass.getLeft();

    for (ClassDeclaration subClass : superDeclaration.getSubClasses()) {
      if (subClass.isReferenced()) {
        return;
      }
    }

    superDeclaration.setReferenced(false);
    unreferenceDeclaration(superDeclaration);
  }

  private String getResolvedTypeName(ClassOrInterfaceType type) {
    String resolvedName = getResolvedName(type);
    return resolvedName != null ? resolvedName : type.getNameAsString();
  }

  private String getResolvedName(ClassOrInterfaceType type) {
    try {
      return type.resolve().asReferenceType().getQualifiedName();
    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
      return null;
    }
  }

  private String getDisplayName(String resolvedName) {
    return resolvedName.replaceFirst(config.getPackage() + ".", "");
  }

  private boolean isTemplateName(String name) {
    return templateNames.contains(name);
  }
}
