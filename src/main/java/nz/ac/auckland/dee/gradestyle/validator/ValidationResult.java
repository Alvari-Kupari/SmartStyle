package nz.ac.auckland.dee.gradestyle.validator;

import java.nio.file.Path;
import java.util.Map;

import nz.ac.auckland.dee.gradestyle.Repo;

public class ValidationResult {
  private Repo repo;

  private Violations violations;

  private Path error;
  

  ValidationResult(Repo repo, Violations violations, Path error) {
    this.repo = repo;
    this.violations = violations;
    this.error = error;
  }

  public Repo getRepo() {
    return repo;
  }

  public Violations getViolations() {
    return violations;
  }

  public Path getError() {
    return error;
  }
}
