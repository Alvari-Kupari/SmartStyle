package nz.ac.auckland.dee.gradestyle.validator.pmd;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.Report.ProcessingError;
import net.sourceforge.pmd.reporting.RuleViolation;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.util.FileUtils;
import nz.ac.auckland.dee.gradestyle.util.MavenReader;
import nz.ac.auckland.dee.gradestyle.validator.Type;
import nz.ac.auckland.dee.gradestyle.validator.Validator;
import nz.ac.auckland.dee.gradestyle.validator.ValidatorException;
import nz.ac.auckland.dee.gradestyle.validator.Violation;
import nz.ac.auckland.dee.gradestyle.validator.Violations;

public class Pmd implements Validator {
  private static final URL config = Pmd.class.getResource("pmd.xml");

  private static final boolean includeDepedencies = false;

  @Override
  public Violations validate(Repo repo) throws ValidatorException {
    PMDConfiguration configuration = new PMDConfiguration();
    configuration.setSourceEncoding(StandardCharsets.UTF_8);

    JavaLanguageModule lan = new JavaLanguageModule();
    configuration.setDefaultLanguageVersion(lan.getVersion("22"));

    Path ruleSetPath;
    try {
      ruleSetPath = Paths.get(config.toURI());
    } catch (Exception e) {
      throw new ValidatorException(e);
    }

    if (!Files.exists(ruleSetPath)) {
      throw new IllegalStateException("ERROR: pmd.xml ruleset not found.");
    }

    configuration.addRuleSet(ruleSetPath.toString());

    configuration.setIgnoreIncrementalAnalysis(true);

    try {

      // List<Path> sourceFiles = FileUtils.getJavaSrcFiles(repo.getDir()).toList();
      List<Path> sourceFiles = FileUtils.getJavaSrcFiles(repo).toList();

      configuration.setInputPathList(sourceFiles);

      if (includeDepedencies) {
        MavenReader.configureMavenDependencies(repo, configuration);
      }

    } catch (IOException e) {
      e.printStackTrace();
      throw new ValidatorException(e);
    } catch (Exception e) {
      System.err.println("Error resolving Maven dependencies.");
    }

    Report report = PmdAnalysis.create(configuration).performAnalysisAndCollectReport();

    if (!report.getProcessingErrors().isEmpty()) {
      for (ProcessingError error : report.getProcessingErrors()) {
        System.out.println(error.getMsg() + error.getDetail());
      }
      throw new ValidatorException(
          Path.of(report.getProcessingErrors().get(0).getFileId().getAbsolutePath()));
    }

    try {
      return getViolations(report.getViolations());
    } catch (IOException e) {
      e.printStackTrace();
      throw new ValidatorException(e);
    }
  }

  private Violations getViolations(List<RuleViolation> ruleViolations) throws IOException {

    Violations violations = new Violations();

    for (RuleViolation violation : ruleViolations) {
      Type type = getType(violation.getRule());
      Path file = Path.of(violation.getFileId().getAbsolutePath());
      int start = violation.getBeginLine();
      int end = violation.getEndLine();

      // PMD violates a useless import on all wildcard imports unless compiled classes
      // are provided.
      if (type == Type.Useless_Import) {
        String line =
            // Files.lines(Path.of(violation.getFileId().getFileName()))
            //     .skip(violation.getBeginLine() - 1)
            //     .findFirst()
            //     .get();

            Files.lines(Path.of(violation.getFileId().getAbsolutePath()))
                .skip(violation.getBeginLine() - 1)
                .findFirst()
                .get();

        if (line.contains("*")) {
          continue;
        }
      }

      violations.getViolations().add(new Violation(type, file, start, end));
    }

    return violations;
  }

  private Type getType(Rule rule) {
    switch (rule.getName()) {
      case "UnusedAssignment":
        return Type.Useless_Assignment;
      case "UnusedLocalVariable":
        return Type.Useless_LocalVariable;
      case "UnusedPrivateField":
        return Type.Useless_Field;
      case "UnusedPrivateMethod":
        return Type.Useless_Method;
      case "UnnecessaryCast":
        return Type.Useless_Cast;
      case "UnnecessaryConstructor":
        return Type.Useless_Constructor;
      case "UnnecessaryFullyQualifiedName":
        return Type.Useless_FullyQualifiedName;
      case "UnnecessaryImport":
        return Type.Useless_Import;
      case "UnnecessaryReturn":
        return Type.Useless_Return;
      case "StringConcatenation":
        return Type.StringConcatenation;

      case "EmptyCatchBlock":
        return Type.EmptyCatchBlock;

      case "MissingOverride":
        return Type.MissingOverride;

      default:
        throw new IllegalArgumentException("Unknown rule: " + rule.getName());
    }
  }
}
