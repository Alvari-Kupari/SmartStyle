package nz.ac.auckland.dee.gradestyle.config;

import nz.ac.auckland.dee.gradestyle.oop.Requirement;

public class RequirementConfig {
  private Requirement requirement;

  private int require;

  private String label;

  public RequirementConfig(Requirement requirement, int require, String label) {
    this.requirement = requirement;
    this.require = require;
    this.label = label;
  }

  public Requirement getRequirement() {
    return requirement;
  }

  public int getRequire() {
    return require;
  }

  public String getLabel() {
    return label;
  }
}
