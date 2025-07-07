package nz.ac.auckland.dee.gradestyle.oop;

import io.vavr.control.Either;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InterfaceDeclaration extends Declaration {
  private Either<InterfaceDeclaration, Type> superInterface;

  private List<InterfaceDeclaration> subInterfaces = new ArrayList<>();

  private List<ClassDeclaration> implementedClasses = new ArrayList<>();

  public InterfaceDeclaration(Type type, Path path, int line) {
    super(type, path, line);
  }

  public Either<InterfaceDeclaration, Type> getSuperInterface() {
    return superInterface;
  }

  public void setSuperInterface(Either<InterfaceDeclaration, Type> superInterface) {
    this.superInterface = superInterface;
  }

  public List<InterfaceDeclaration> getSubInterfaces() {
    return subInterfaces;
  }

  public List<ClassDeclaration> getImplementedClasses() {
    return implementedClasses;
  }

  @Override
  public String toString() {
    String str = "interface " + getType();

    if (superInterface != null) {
      str += " extends " + superInterface.fold(Declaration::getType, Function.identity());
    }

    return str;
  }
}
