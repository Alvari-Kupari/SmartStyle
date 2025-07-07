package nz.ac.auckland.dee.gradestyle.oop;

import io.vavr.control.Either;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class Declaration {
  public enum Nesting {
    Outer,
    Inner,
    Nested,
  }

  private Type type;

  private Path path;

  private int line;

  private boolean isReferenced = false;

  private Nesting nesting = Nesting.Outer;

  private Either<Declaration, Type> outerDeclaration;

  private List<Declaration> innerDeclarations = new ArrayList<>();

  private List<Declaration> nestedDeclarations = new ArrayList<>();

  public Declaration(Type type, Path path, int line) {
    this.type = type;
    this.path = path;
    this.line = line;
  }

  public Type getType() {
    return type;
  }

  public Path getPath() {
    return path;
  }

  public int getLine() {
    return line;
  }

  public void setReferenced(boolean isReferenced) {
    this.isReferenced = isReferenced;
  }

  public boolean isReferenced() {
    return isReferenced;
  }

  public Nesting getNesting() {
    return nesting;
  }

  public Either<Declaration, Type> getOuterDeclaration() {
    return outerDeclaration;
  }

  public void setOuterDeclaration(Either<Declaration, Type> outerClass, Nesting nesting) {
    this.outerDeclaration = outerClass;
    this.nesting = nesting;
  }

  public List<Declaration> getInnerDeclarations() {
    return innerDeclarations;
  }

  public List<Declaration> getNestedDeclarations() {
    return nestedDeclarations;
  }
}
