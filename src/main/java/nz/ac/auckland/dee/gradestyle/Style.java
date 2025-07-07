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
  public static void main(String[] args) {
    String propertiesFile = "config/test.properties";
    Config config = Config.parse(new String[] {propertiesFile});

    if (config == null) {
      System.exit(1);
    }

    Github github = new Github(config);
    List<Repo> repos = Repo.getRepos(github);

    // Initialize the CSV writer
    try (ValidationCsv writer =
        new ValidationCsv(config.getCategoryConfigs(), config.getStyleFeedback().getReportsCsv())) {

      ValidationMarkdown markdown = new ValidationMarkdown(config);
      Markdown<ValidationResult> marker =
          new Markdown<>(config.getStyleFeedback().getReportsMd(), markdown);

      Validator[] validators = {
        new Checkstyle(), new JavaParser(), new Pmd(), new Cpd(), new JavaFx()
      };

      Validation validation = new Validation(validators, config);

      for (Repo repo : repos) {

        try {
          ValidationResult result = validation.validateSingleRepo(repo);
          writer.writeResult(result, repo);

          marker.write(result);
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
}
