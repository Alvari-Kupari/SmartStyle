package nz.ac.auckland.dee.gradestyle.oop;

public class Type {
  private String displayName;

  private String localResolvedName;

  public Type(String displayName, String localResolvedName) {
    this.displayName = displayName;
    this.localResolvedName = localResolvedName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getLocalResolvedName() {
    return localResolvedName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
