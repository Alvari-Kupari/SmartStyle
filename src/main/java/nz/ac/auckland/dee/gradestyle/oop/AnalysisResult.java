package nz.ac.auckland.dee.gradestyle.oop;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.oop.Declaration.Nesting;

public class AnalysisResult {
  private Repo repo;

  private List<Declaration> declarations;

  private Path error;

  public AnalysisResult(Repo repo, List<Declaration> declarations) {
    this.repo = repo;
    this.declarations = declarations;
  }

  public AnalysisResult(Repo repo, Path error) {
    this.repo = repo;
    this.error = error;
  }

  public Repo getRepo() {
    return repo;
  }

  public List<Declaration> getDeclarations() {
    return declarations;
  }

  public List<Declaration> getReferencedDeclarations() {
    return declarations.stream().filter(Declaration::isReferenced).toList();
  }

  public Path getError() {
    return error;
  }

  public long getTotalClasses() {
    return streamFilterDeclarations(ClassDeclaration.class).count();
  }

  public long getClasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(Predicate.not(ClassDeclaration::isAbstract))
        .count();
  }

  public long getAbstractClasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(ClassDeclaration::isAbstract)
        .count();
  }

  public long getTopLevelClasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(c -> c.getSuperClass() == null)
        .count();
  }

  public long getLocalSubclasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(c -> c.getSuperClass() != null && c.getSuperClass().isLeft())
        .count();
  }

  public long getExternalSubclasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(c -> c.getSuperClass() != null && c.getSuperClass().isRight())
        .count();
  }

  public long getInnerClasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(c -> c.getNesting() == Nesting.Inner)
        .count();
  }

  public long getNestedClasses() {
    return streamFilterDeclarations(ClassDeclaration.class)
        .filter(c -> c.getNesting() == Nesting.Nested)
        .count();
  }

  public long getTotalInterfaces() {
    return streamFilterDeclarations(InterfaceDeclaration.class).count();
  }

  public long getTopLevelInterfaces() {
    return streamFilterDeclarations(InterfaceDeclaration.class)
        .filter(i -> i.getSuperInterface() == null)
        .count();
  }

  public long getLocalSubinterfaces() {
    return streamFilterDeclarations(InterfaceDeclaration.class)
        .filter(i -> i.getSuperInterface() != null && i.getSuperInterface().isLeft())
        .count();
  }

  public long getExternalSubinterfaces() {
    return streamFilterDeclarations(InterfaceDeclaration.class)
        .filter(i -> i.getSuperInterface() != null && i.getSuperInterface().isRight())
        .count();
  }

  public <T extends Declaration> List<T> filterDeclarations(Class<T> type) {
    return streamFilterDeclarations(type).toList();
  }

  private <T extends Declaration> Stream<T> streamFilterDeclarations(Class<T> type) {
    return getReferencedDeclarations().stream().filter(type::isInstance).map(type::cast);
  }

  public long getUnreferencedDeclarations() {
    return declarations.stream().filter(Predicate.not(Declaration::isReferenced)).count();
  }
}
