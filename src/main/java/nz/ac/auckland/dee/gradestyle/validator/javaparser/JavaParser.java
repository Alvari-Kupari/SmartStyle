package nz.ac.auckland.dee.gradestyle.validator.javaparser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.config.CommentingConfig;
import nz.ac.auckland.dee.gradestyle.config.Config;
import nz.ac.auckland.dee.gradestyle.config.ProgrammingPracticeConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocClassConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocConstructorConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocFieldConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocMethodConfig;
import nz.ac.auckland.dee.gradestyle.util.FileUtils;
import nz.ac.auckland.dee.gradestyle.validator.Category;
import nz.ac.auckland.dee.gradestyle.validator.Type;
import nz.ac.auckland.dee.gradestyle.validator.Validator;
import nz.ac.auckland.dee.gradestyle.validator.ValidatorException;
import nz.ac.auckland.dee.gradestyle.validator.Violation;
import nz.ac.auckland.dee.gradestyle.validator.Violations;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class JavaParser implements Validator {
  private CommentingConfig commentingConfig;

  private JavadocClassConfig javadocClassConfig;
  private JavadocMethodConfig javadocMethodConfig;
  private JavadocFieldConfig javadocFieldConfig;
  private JavadocConstructorConfig javadocConstructorConfig;
  private JavadocConfig javadocConfig;
  private ProgrammingPracticeConfig programmingPracticeConfig;

  @Override
  public void setup(Config config) {
    this.commentingConfig = config.getCategoryConfig(CommentingConfig.class);

    this.javadocClassConfig = config.getCategoryConfig(JavadocClassConfig.class);
    this.javadocMethodConfig = config.getCategoryConfig(JavadocMethodConfig.class);
    this.javadocFieldConfig = config.getCategoryConfig(JavadocFieldConfig.class);
    this.javadocConstructorConfig = config.getCategoryConfig(JavadocConstructorConfig.class);
    this.javadocConfig = config.getCategoryConfig(JavadocConfig.class);
    this.programmingPracticeConfig = config.getCategoryConfig(ProgrammingPracticeConfig.class);
  }

  @Override
  public Violations validate(Repo repo) throws ValidatorException {
    Violations violations = new Violations();


    try {
      runJavaparser(repo, violations);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ValidatorException(e);
    }

    return violations;
  }

  private void runJavaparser(Repo repo, Violations violations)
      throws ValidatorException, IOException {
    com.github.javaparser.JavaParser javaParser;
    try {
      javaParser = nz.ac.auckland.dee.gradestyle.util.JavaParser.get(repo);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    // for (Path file : FileUtils.getJavaSrcFiles(repo.getDir()).toList()) {
    for (Path file : FileUtils.getJavaSrcFiles(repo).toList()) {
      ParseResult<CompilationUnit> result = javaParser.parse(file);

      if (!result.isSuccessful()) {
        List<Problem> problems = result.getProblems();
        int numProblems = problems.size();

        if (numProblems == 1) {
          throw new ValidatorException(file, problems.get(0).getVerboseMessage());
        } else if (numProblems > 1) {
          StringBuilder message = new StringBuilder();
          for (int i = 0; i < numProblems - 1; i++) {
            message.append(String.valueOf(i)).append(") ");
            message.append(problems.get(i).getMessage()).append("\n");
          }
        }
      }

      CompilationUnit cu = result.getResult().get();

      privateFieldViolations(file).visit(cu, violations);
      classOrderingViolations(file).visit(cu, violations);
      

      if (commentingConfig != null) {
        IOException e = commentFrequencyViolations(file).visit(cu, violations);

        if (e != null) {
          throw e;
        }

        commentMeaningViolations(file).visit(cu, violations);
      }

      if (javadocFieldConfig != null) {
        javadocFieldViolations(file).visit(cu, violations);
      }
      if (javadocMethodConfig != null) {
        javadocMethodViolations(file).visit(cu, violations);
      }
      if (javadocConstructorConfig != null) {
        javadocConstructorViolations(file).visit(cu, violations);
      }
      if (javadocClassConfig != null) {
        javadocClassViolations(file).visit(cu, violations);
      }

      if (programmingPracticeConfig != null) {
        finalizeNotAllowedViolation(file).visit(cu, violations);
        unqualifiedStaticMemberViolations(file).visit(cu, violations);
      }

      commentViolations(repo, file, cu, violations);
    }
  }

  private GenericVisitorAdapter<IOException, Violations> commentFrequencyViolations(Path file) {
    return new GenericVisitorAdapter<IOException, Violations>() {
      @Override
      public IOException visit(MethodDeclaration decl, Violations violations) {
        IOException superE = super.visit(decl, violations);

        if (superE != null) {
          return superE;
        }

        try {
          visitException(decl, violations);
        } catch (IOException e) {
          return e;
        }

        return null;
      }

      private void visitException(MethodDeclaration decl, Violations violations)
          throws IOException {
        long methodLines = numLines(decl) - 1; // Don't count signature.
        if (methodLines <= commentingConfig.getMinLines()) {
          return;
        }

        long commentLines = 0;

        for (Comment comment : decl.getAllContainedComments()) {
          commentLines += numLines(comment);
        }

        int ratio = (int) ((float) commentLines / methodLines * 100);

        if (ratio >= commentingConfig.getMinFrequency()
            && ratio <= commentingConfig.getMaxFrequency()) {
          return;
        }

        Type type =
            ratio < commentingConfig.getMinFrequency()
                ? Type.Commenting_FrequencyLow
                : Type.Commenting_FrequencyHigh;

        addViolation(violations, type, file, getFirstLine(decl.getName()));

        return;
      }

      private long numLines(Node node) throws IOException {

        try (Stream<String> lines = Files.lines(file)) {
          return lines
              .skip(getFirstLine(node) - 1)
              .limit(getLastLine(node) - getFirstLine(node) + 1)
              .map(String::trim)
              .filter(line -> !line.isEmpty())
              .filter(line -> !line.equals("{"))
              .filter(line -> !line.equals("}"))
              .count();
        }
      }
    };
  }

  private VoidVisitorAdapter<Violations> unqualifiedStaticMemberViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(MethodCallExpr methodCall, Violations violations) {
        super.visit(methodCall, violations);
        try {
          ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
          if (resolvedMethod.isStatic()) {
            Expression scope = methodCall.getScope().orElse(null);
            if (scope != null
                && !(scope instanceof NameExpr
                    && ((NameExpr) scope)
                        .getNameAsString()
                        .equals(resolvedMethod.getClassName()))) {
              addViolation(
                  violations,
                  Type.StaticMemberNotQualified,
                  file,
                  getFirstLine(methodCall));
            }
          }
        } catch (RuntimeException e) {
          // Log and gracefully skip unresolved or problematic method calls
          // System.err.println("Runtime exception while resolving: " + methodCall + " - "
          // + e.getMessage());
        }
      }
    };
  }

  private VoidVisitorAdapter<Violations> finalizeNotAllowedViolation(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(MethodDeclaration method, Violations violations) {
        super.visit(method, violations);

        if (method.getNameAsString().equals("finalize")
            && method.getParameters().isEmpty()
            && method.getTypeAsString().equals("void")
            && (method.getModifiers().contains(Modifier.publicModifier())
                || method.getModifiers().contains(Modifier.protectedModifier()))) {

          addViolation(
              violations, Type.FinalizeOverride, file, getFirstLine(method));
        }
      }
    };
  }

  private VoidVisitorAdapter<Violations> commentMeaningViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(LineComment comment, Violations violations) {
        super.visit(comment, violations);
        visitComment(comment, violations);
      }

      @Override
      public void visit(BlockComment comment, Violations violations) {
        super.visit(comment, violations);
        visitComment(comment, violations);
      }

      private void visitComment(Comment comment, Violations violations) {
        if (comment.isJavadocComment()) {
          return;
        }

        Node node = comment.getCommentedNode().orElse(null);

        if (node == null) {
          return;
        }

        String text = comment.getContent();
        String code = node.removeComment().toString();
        int distance = new LevenshteinDistance().apply(text, code);

        if (commentingConfig == null || distance >= commentingConfig.getLevenshteinDistance()) {
          return;
        }

        addViolation(violations, Type.Commenting_Meaningful, file, getFirstLine(comment));
      }
    };
  }

  private VoidVisitorAdapter<Violations> privateFieldViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(FieldDeclaration decl, Violations violations) {
        super.visit(decl, violations);

        if (decl.isPrivate() || decl.isProtected() || decl.isStatic() || decl.isFinal()) {
          return;
        }

        addViolation(violations, Type.PrivateInstances, file, getFirstLine(decl.getVariable(0)));
      }
    };
  }

  private VoidVisitorAdapter<Violations> classOrderingViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(ClassOrInterfaceDeclaration decl, Violations violations) {
        super.visit(decl, violations);
        if (!decl.isInterface()) {
          visitAll(decl, violations);
        }
      }

      @Override
      public void visit(EnumDeclaration decl, Violations violations) {
        super.visit(decl, violations);
        visitAll(decl, violations);
      }

      private <T extends TypeDeclaration<?>> void visitAll(
          TypeDeclaration<T> decl, Violations violations) {
        // Inner Classes
        @SuppressWarnings("rawtypes")
        List<TypeDeclaration> innerClasses =
            decl.getMembers().stream()
                .filter(
                    member -> member.isClassOrInterfaceDeclaration() || member.isEnumDeclaration())
                .map(BodyDeclaration::asTypeDeclaration)
                .toList();

        // Static Fields
        List<FieldDeclaration> staticFields =
            decl.getMembers().stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .map(BodyDeclaration::asFieldDeclaration)
                .filter(FieldDeclaration::isStatic)
                .toList();

        // Static Methods
        List<MethodDeclaration> staticMethods =
            decl.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .filter(MethodDeclaration::isStatic)
                .toList();

        // Instance Fields
        List<FieldDeclaration> fields =
            decl.getMembers().stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .map(BodyDeclaration::asFieldDeclaration)
                .filter(Predicate.not(FieldDeclaration::isStatic))
                .toList();

        // Constructors
        List<ConstructorDeclaration> constructors =
            decl.getMembers().stream()
                .filter(BodyDeclaration::isConstructorDeclaration)
                .map(BodyDeclaration::asConstructorDeclaration)
                .toList();

        // Instance Methods
        List<MethodDeclaration> methods =
            decl.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .filter(Predicate.not(MethodDeclaration::isStatic))
                .toList();

        // Enforce Ordering

        // Static Fields should follow Inner Classes
        if (!innerClasses.isEmpty()) {
          staticFields.stream()
              .filter(field -> isBefore(field, getLastElement(innerClasses)))
              .forEach(
                  field ->
                      addOrderViolation(
                          violations,
                          Type.Ordering_StaticField,
                          file,
                          field.getVariable(0).getNameAsString(),
                          getNodeName(getLastElement(innerClasses)),
                          getFirstLine(field)));
        }

        // Static Methods should follow Static Fields
        if (!staticFields.isEmpty() || !innerClasses.isEmpty()) {
          Node lastDeclaration =
              !staticFields.isEmpty() ? getLastElement(staticFields) : getLastElement(innerClasses);

          staticMethods.stream()
              .filter(method -> isBefore(method, lastDeclaration))
              .forEach(
                  method ->
                      addOrderViolation(
                          violations,
                          Type.Ordering_StaticMethod,
                          file,
                          method.getNameAsString(),
                          getNodeName(lastDeclaration),
                          getFirstLine(method)));
        }

        // Instance Fields should follow Static Methods
        if (!staticMethods.isEmpty() || !staticFields.isEmpty() || !innerClasses.isEmpty()) {
          Node lastDeclaration =
              !staticMethods.isEmpty()
                  ? getLastElement(staticMethods)
                  : (!staticFields.isEmpty()
                      ? getLastElement(staticFields)
                      : getLastElement(innerClasses));

          fields.stream()
              .filter(field -> isBefore(field, lastDeclaration))
              .forEach(
                  field ->
                      addOrderViolation(
                          violations,
                          Type.Ordering_Field,
                          file,
                          field.getVariable(0).getNameAsString(),
                          getNodeName(lastDeclaration),
                          getFirstLine(field)));
        }

        // Constructors should follow Instance Fields
        if (!fields.isEmpty()
            || !staticMethods.isEmpty()
            || !staticFields.isEmpty()
            || !innerClasses.isEmpty()) {
          Node lastDeclaration =
              !fields.isEmpty()
                  ? getLastElement(fields)
                  : (!staticMethods.isEmpty()
                      ? getLastElement(staticMethods)
                      : (!staticFields.isEmpty()
                          ? getLastElement(staticFields)
                          : getLastElement(innerClasses)));

          constructors.stream()
              .filter(constructor -> isBefore(constructor, lastDeclaration))
              .forEach(
                  constructor ->
                      addOrderViolation(
                          violations,
                          Type.Ordering_Constructor,
                          file,
                          constructor.getNameAsString(),
                          getNodeName(lastDeclaration),
                          getFirstLine(constructor)));
        }

        // Instance Methods should follow Constructors
        if (!constructors.isEmpty()
            || !fields.isEmpty()
            || !staticMethods.isEmpty()
            || !staticFields.isEmpty()
            || !innerClasses.isEmpty()) {
          Node lastDeclaration =
              !constructors.isEmpty()
                  ? getLastElement(constructors)
                  : (!fields.isEmpty()
                      ? getLastElement(fields)
                      : (!staticMethods.isEmpty()
                          ? getLastElement(staticMethods)
                          : (!staticFields.isEmpty()
                              ? getLastElement(staticFields)
                              : getLastElement(innerClasses))));

          methods.stream()
              .filter(method -> isBefore(method, lastDeclaration))
              .forEach(
                  method ->
                      addOrderViolation(
                          violations,
                          Type.Ordering_Method,
                          file,
                          method.getNameAsString(),
                          getNodeName(lastDeclaration),
                          getFirstLine(method)));
        }
      }

      private String getNodeName(Node node) {

        if (node instanceof MethodDeclaration) {
          return ((MethodDeclaration) node).getNameAsString();
        } else if (node instanceof FieldDeclaration) {
          return ((FieldDeclaration) node).getVariables().get(0).getNameAsString();
        } else if (node instanceof ConstructorDeclaration) {
          return ((ConstructorDeclaration) node).getNameAsString();
        } else if (node instanceof EnumDeclaration) {
          return ((EnumDeclaration) node).getNameAsString();
        } else if (node instanceof ClassOrInterfaceDeclaration) {
          return ((ClassOrInterfaceDeclaration) node).getNameAsString();
        } else if (node instanceof TypeDeclaration<?>) {
          return ((TypeDeclaration<?>) node).getNameAsString();
        } else if (node instanceof ImportDeclaration) {
          return ((ImportDeclaration) node).getNameAsString();
        } else if (node instanceof AnnotationDeclaration) {
          return ((AnnotationDeclaration) node).getNameAsString();
        } else if (node instanceof InitializerDeclaration) {
          return "Static Initializer Block";
        } else {
          return "unknown";
        }
      }

      private <T> T getLastElement(List<T> list) {
        return list.get(list.size() - 1);
      }

      private boolean isBefore(Node a, Node b) {
        return getFirstLine(a) < getFirstLine(b);
      }
    };
  }

  private void addOrderViolation(
      Violations violations, Type type, Path file, String element, String reference, int line) {
    type.formatOrderingMessage(element, reference);
    violations.getViolations().add(new Violation(type, file, line));
  }

  private VoidVisitorAdapter<Violations> javadocClassViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(ClassOrInterfaceDeclaration node, Violations violations) {
        super.visit(node, violations);
        Optional<Comment> comment = node.getComment();
        if (comment.isEmpty()) {
          addViolation(violations, Type.JavadocClass_Missing, file, getFirstLine(node));
        } else if (comment.get() instanceof JavadocComment) {
          // Validate Javadoc if it exists and is a JavadocComment
          validateJavadoc((JavadocComment) comment.get(), violations, file, Category.JavadocClass);
        } else {
          // Optionally handle cases where the comment is not a JavadocComment
          addViolation(violations, Type.Javadoc_Invalid, file, getFirstLine(node));
        }
      }
    };
  }

  private VoidVisitorAdapter<Violations> javadocFieldViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(FieldDeclaration node, Violations violations) {
        super.visit(node, violations);
        Optional<Comment> comment = node.getComment();
        if (comment.isEmpty()) {
          addViolation(violations, Type.JavadocField_Missing, file, getFirstLine(node));
        } else if (comment.get() instanceof JavadocComment) {
          // Validate Javadoc if it exists and is a JavadocComment
          validateJavadoc((JavadocComment) comment.get(), violations, file, Category.JavadocField);
        } else {
          // Optionally handle cases where the comment is not a JavadocComment
          addViolation(violations, Type.Javadoc_Invalid, file, getFirstLine(node));
        }
      }
    };
  }

  private VoidVisitorAdapter<Violations> javadocConstructorViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(ConstructorDeclaration node, Violations violations) {
        super.visit(node, violations);
        Optional<Comment> comment = node.getComment();
        if (comment.isEmpty()) {
          addViolation(violations, Type.JavadocConstructor_Missing, file, getFirstLine(node));
        } else if (comment.get() instanceof JavadocComment) {
          // Validate Javadoc if it exists and is a JavadocComment
          validateJavadoc(
              (JavadocComment) comment.get(), violations, file, Category.JavadocConstructor);
        } else {
          // Optionally handle cases where the comment is not a JavadocComment
          addViolation(violations, Type.Javadoc_Invalid, file, getFirstLine(node));
        }
      }
    };
  }

  private VoidVisitorAdapter<Violations> javadocMethodViolations(Path file) {
    return new VoidVisitorAdapter<Violations>() {
      @Override
      public void visit(MethodDeclaration node, Violations violations) {
        super.visit(node, violations);

        if (node.getAnnotationByName("Override").isPresent()) {
          return;
        }

        Optional<Comment> comment = node.getComment();

        if (comment.isEmpty()) {
          addViolation(violations, Type.JavadocMethod_Missing, file, getFirstLine(node));
        } else if (comment.get() instanceof JavadocComment) {
          // Validate Javadoc if it exists and is a JavadocComment
          validateJavadoc((JavadocComment) comment.get(), violations, file, Category.JavadocMethod);
        } else {
          // handle cases where the comment is not a javadocComment
          addViolation(violations, Type.Javadoc_Invalid, file, getFirstLine(node));
        }
      }
    };
  }

  private void validateJavadoc(
      JavadocComment comment, Violations violations, Path file, Category category) {
    JavadocDescription description = comment.parse().getDescription();

    if (description.isEmpty() || description.getElements().get(0) instanceof JavadocInlineTag) {
      addViolation(violations, Type.Javadoc_MissingSummary, file, getFirstLine(comment));
      return;
    }

    long words = Pattern.compile("[\\w-]+").matcher(description.toText()).results().count();

    int minWords;

    switch (category) {
      case JavadocClass:
        minWords = javadocClassConfig.getMinWords();
        break;

      case JavadocMethod:
        minWords = javadocMethodConfig.getMinWords();
        break;

      case JavadocField:
        minWords = javadocFieldConfig.getMinWords();
        break;
      case JavadocConstructor:
        minWords = javadocConstructorConfig.getMinWords();
        break;

      default:
        throw new IllegalArgumentException("Javadoc category not found when finding minWords");
    }
    if (words < minWords) {
      addViolation(violations, Type.Javadoc_SummaryLength, file, getFirstLine(comment));
    }
  }

  private void commentViolations(Repo repo, Path file, CompilationUnit cu, Violations violations) {
    for (Comment comment : getMergedComments(cu)) {
      Optional<Node> parent = comment.getParentNode();
      String contents = comment.getContent();

      if (comment.isJavadocComment() || contents.isBlank() || parent.isEmpty()) {
        continue;
      }

      String code;
      if (parent.get() instanceof BlockStmt) {
        code = "class X { void x() {" + contents + " } }";
      } else if (parent.get() instanceof ClassOrInterfaceDeclaration) {
        code = "class X {" + contents + " }";
      } else {
        continue;
      }

      ParseResult<CompilationUnit> result =
          nz.ac.auckland.dee.gradestyle.util.JavaParser.get(repo).parse(code);

      if (result.isSuccessful()) {
        addViolation(violations, Type.Useless_CommentedCode, file, getFirstLine(comment));
      }
    }
  }

  private List<Comment> getMergedComments(CompilationUnit cu) {
    List<Comment> comments = new ArrayList<>();

    if (cu.getAllComments().isEmpty()) {
      return comments;
    }

    Comment lastComment = cu.getAllComments().get(0);

    for (int i = 1; i < cu.getAllComments().size(); i++) {
      Comment comment = cu.getAllComments().get(i);

      if (lastComment.isLineComment()
          && comment.isLineComment()
          && getLastLine(lastComment) == getFirstLine(comment) - 1
          && getFirstColumn(lastComment) == getFirstColumn(comment)) {
        TokenRange range =
            lastComment.getTokenRange().get().withEnd(comment.getTokenRange().get().getEnd());
        String content = lastComment.getContent() + comment.getContent();
        Optional<Node> parent = lastComment.getParentNode();

        lastComment = new LineComment(range, content);

        if (parent.isPresent()) {
          lastComment.setParentNode(parent.get());
        }
      } else {
        comments.add(lastComment);

        lastComment = comment;
      }
    }

    comments.add(lastComment);

    return comments;
  }

  private int getFirstLine(Node node) {
    return node.getRange().get().begin.line;
  }

  private int getLastLine(Node node) {
    return node.getRange().get().end.line;
  }

  private int getFirstColumn(Node node) {
    return node.getRange().get().begin.column;
  }

  private void addViolation(Violations violations, Type type, Path file, int line) {
    violations.getViolations().add(new Violation(type, file, line));
  }
}
