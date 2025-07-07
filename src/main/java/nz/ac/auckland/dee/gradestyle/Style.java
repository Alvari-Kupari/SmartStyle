package nz.ac.auckland.dee.gradestyle;

import java.io.IOException;
import java.util.List;
import nz.ac.auckland.dee.gradestyle.config.Config;
import nz.ac.auckland.dee.gradestyle.validator.Validation;
import nz.ac.auckland.dee.gradestyle.validator.ValidationCsv;
import nz.ac.auckland.dee.gradestyle.validator.ValidationMarkdown;
import nz.ac.auckland.dee.gradestyle.validator.ValidationResult;
import nz.ac.auckland.dee.gradestyle.validator.Validator;
import nz.ac.auckland.dee.gradestyle.validator.ValidatorException;
import nz.ac.auckland.dee.gradestyle.validator.checkstyle.Checkstyle;
import nz.ac.auckland.dee.gradestyle.validator.cpd.Cpd;
import nz.ac.auckland.dee.gradestyle.validator.javafx.JavaFx;
import nz.ac.auckland.dee.gradestyle.validator.javaparser.JavaParser;
import nz.ac.auckland.dee.gradestyle.validator.pmd.Pmd;

public class Style {
  // Updated entry point
  public static void main(String[] args) {
    Config config = Config.parse(args);

    if (config == null) {
      System.exit(1);
    }

    Github github = new Github(config);
    List<Repo> repos = Repo.getRepos(github);

    // Initialize the CSV writer
    try (ValidationCsv writer =
        new ValidationCsv(config.getCategoryConfigs(), config.getStyleFeedback().getReportsCsv())) {
      Validator[] validators = {
        new Checkstyle(), new JavaParser(), new Pmd(), new Cpd(), new JavaFx()
      };

      Validation validation = new Validation(validators, config);

      for (Repo repo : repos) {

        try {
          ValidationResult result = validation.validateSingleRepo(repo);
          writer.writeResult(result, repo);
          System.out.println("Successfully wrote results for repo: " + repo.getName());
        } catch (ValidatorException e) {
          System.err.println("Validation failed for repo: " + repo.getName());
          e.printStackTrace();
        } catch (IOException e) {
          System.err.println("Failed to write CSV for repo: " + repo.getName());
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      System.err.println("Unable to initialize CSV writer.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  // Updated Validation class to validate a single repository

  private static List<ValidationResult> runValidation(Config config, List<Repo> repos) {
    Validator[] validators = {
      new Checkstyle(), new JavaParser(), new Pmd(), new Cpd(), new JavaFx()
    };

    Validation validation = new Validation(validators, config);

    try {
      return validation.validate(repos);
    } catch (ValidatorException e) {
      System.err.println("Unable to run style validation.");
      e.printStackTrace();
      System.exit(1);
    }

    return null;
  }

  // private static void outputCsv(Config config, List<ValidationResult> results) {
  //   if (config.getStyleFeedback().getReportsCsv() == null) {
  //     return;
  //   }

  //   ValidationCsv writer = new ValidationCsv(config.getCategoryConfigs(), results);
  //   Csv csv = new Csv(config.getStyleFeedback().getReportsCsv(), writer);

  //   try {
  //     csv.write();
  //   } catch (IOException e) {
  //     System.err.println("Unable to write CSV file.");
  //     e.printStackTrace();
  //     System.exit(1);
  //   }
  // }

  private static void outputMarkdown(Config config, List<ValidationResult> results) {
    if (config.getStyleFeedback().getReportsMd() == null) {
      return;
    }

    ValidationMarkdown writer = new ValidationMarkdown(config);
    Markdown<ValidationResult> md =
        new Markdown<>(config.getStyleFeedback().getReportsMd(), writer);

    try {
      md.write(results);
    } catch (IOException e) {
      System.err.println("Unable to write markdown files.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void sendGithubFeedback(Github github, List<ValidationResult> results) {
    if (!github.getConfig().getGithubFeedback()) {
      return;
    }

    try {
      github.sendFeedback(results);
    } catch (IOException e) {
      System.err.println("Unable to send GitHub feedback.");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
