package nz.ac.auckland.dee.gradestyle.oop;

import io.vavr.control.Either;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ClassDeclaration extends Declaration {
  private boolean isAbstract;

  private Either<ClassDeclaration, Type> superClass;

  private List<ClassDeclaration> subClasses = new ArrayList<>();

  private List<Either<InterfaceDeclaration, Type>> implementsInterfaces = new ArrayList<>();

  public ClassDeclaration(Type type, Path path, int line, boolean isAbstract) {
    super(type, path, line);

    this.isAbstract = isAbstract;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public Either<ClassDeclaration, Type> getSuperClass() {
    return superClass;
  }

  public void setSuperClass(Either<ClassDeclaration, Type> superClass) {
    this.superClass = superClass;
  }

  public List<ClassDeclaration> getSubClasses() {
    return subClasses;
  }

  public List<Either<InterfaceDeclaration, Type>> getImplementsInterfaces() {
    return implementsInterfaces;
  }

  @Override
  public String toString() {
    String line = "class " + getType();

    if (isAbstract) {
      line = "abstract " + line;
    }

    if (superClass != null) {
      line += " extends " + superClass.fold(Declaration::getType, Function.identity());
    }

    if (!implementsInterfaces.isEmpty()) {
      List<String> interfaceNames =
          implementsInterfaces.stream()
              .map(declaration -> declaration.fold(Declaration::getType, Function.identity()))
              .map(Object::toString)
              .toList();

      line += " implements " + String.join(", ", interfaceNames);
    }

    return line;
  }
}
