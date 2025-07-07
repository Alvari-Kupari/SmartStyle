package nz.ac.auckland.dee.gradestyle.validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.config.Config;
import nz.ac.auckland.dee.gradestyle.util.FileUtils;

public class Validation {
  private Validator[] validators;

  private Config config;

  public Validation(Validator[] validators, Config config) {
    this.validators = validators;
    this.config = config;
  }

  public ValidationResult validateSingleRepo(Repo repo) throws ValidatorException {
    Map<String, List<String>> templateRepoLines;

    try {
      templateRepoLines = readTemplateRepoLines();
    } catch (IOException e) {
      throw new ValidatorException(e);
    }

    for (Validator validator : validators) {
      validator.setup(config);
    }

    List<Violation> violations = new ArrayList<>();
    Path error = null;

    System.out.print("Validating " + repo.getName() + "... ");
    boolean success = true;

    for (Validator validator : validators) {
      try {
        for (Violation violation : validator.validate(repo).getViolations()) {
          if (notInTemplate(violation, repo, templateRepoLines)) {
            violations.add(violation);
          }
        }
      } catch (ValidatorException e) {
        error = e.getPath();
        e.printStackTrace();
        success = false;
      } catch (IOException e) {
        e.printStackTrace();
        throw new ValidatorException(e);
      }
    }

    if (success) {
      System.out.println("done.");
    } else {
      System.out.println("failed.");
    }

    return new ValidationResult(repo, new Violations(violations), error);
  }

  public List<ValidationResult> validate(List<Repo> repos) throws ValidatorException {
    Map<String, List<String>> templateRepoLines;

    try {
      templateRepoLines = readTemplateRepoLines();
    } catch (IOException e) {
      throw new ValidatorException(e);
    }

    for (Validator validator : validators) {
      validator.setup(config);
    }

    List<ValidationResult> results = new ArrayList<>();

    int count = 0;
    int skipped = 0;
    outer:
    for (Repo repo : repos) {
      List<Violation> violations = new ArrayList<>();
      Path error = null;

      System.out.print("Validating " + repo.getName() + "... ");
      boolean success = true;

      for (Validator validator : validators) {
        System.out.println(validator.getClass().getSimpleName());
        try {
          for (Violation violation : validator.validate(repo).getViolations()) {
            if (notInTemplate(violation, repo, templateRepoLines)) {
              violations.add(violation);
            }
          }
        } catch (ValidatorException e) {
          error = e.getPath();
          e.printStackTrace();

          if (error == null) {
            skipped++;
            continue outer;
            // throw e;
          }

          System.out.println("*** FAILED ***");
          System.out.println(
              "\tStyle validation of \""
                  + repo.getName()
                  + "\" using \""
                  + validator.getClass().getSimpleName()
                  + "\" failed \n\t@ \""
                  + error
                  + "\"");
          System.out.println("\t" + e.getMessage());
          success = false;
          // break;
        } catch (IOException e) {
          throw new ValidatorException(e);
        }
      }

      if (success) {
        count++;
        System.out.println("done.");
      }

      results.add(new ValidationResult(repo, new Violations(violations), error));
    }

    // MavenReader.clearDependencies();

    System.out.println("REPOS SUCCESSFUL: " + count);
    System.out.println("REPOS SKIPPED: " + skipped);

    return results;
  }

  private Map<String, List<String>> readTemplateRepoLines() throws IOException {
    Map<String, List<String>> templateRepoLines = new HashMap<>();

    if (!config.getTemplateIgnoreViolations()) {
      return templateRepoLines;
    }

    try (Stream<Path> files = FileUtils.getJavaFiles(config.getTemplateRepo())) {
      for (Path file : files.toList()) {
        try (Stream<String> lines = Files.lines(file)) {
          templateRepoLines
              .computeIfAbsent(file.getFileName().toString(), x -> new ArrayList<>())
              .addAll(lines.toList());
        }
      }
    }

    return templateRepoLines;
  }

  private boolean notInTemplate(
      Violation violation, Repo repo, Map<String, List<String>> templateRepoLines)
      throws IOException {
    if (!config.getTemplateIgnoreViolations()) {
      return true;
    }

    String fileName = violation.getPath().getFileName().toString();

    if (!templateRepoLines.containsKey(fileName)) {
      return true;
    }

    List<String> templateLines = templateRepoLines.get(fileName);

    try (Stream<String> repoLines = Files.lines(violation.getPath())) {
      Optional<String> repoLine = repoLines.skip(violation.getLine() - 1).findFirst();

      return repoLine.isEmpty() || !templateLines.contains(repoLine.get());
    }
  }
}
