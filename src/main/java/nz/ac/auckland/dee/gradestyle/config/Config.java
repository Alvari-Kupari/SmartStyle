package nz.ac.auckland.dee.gradestyle.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nz.ac.auckland.dee.gradestyle.config.CategoryConfig.Mode;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocClassConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocConstructorConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocFieldConfig;
import nz.ac.auckland.dee.gradestyle.config.javadocconfig.JavadocMethodConfig;
import nz.ac.auckland.dee.gradestyle.oop.Requirement;
import nz.ac.auckland.dee.gradestyle.validator.Category;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Config {
  public static Config parse(String[] args) {
    if (args.length != 1) {
      System.err.println("Missing config file argument.");
      return null;
    }

    try {
      return createConfig(args[0]);
    } catch (ConfigurationException e) {
      System.err.println("Invalid config file: " + args[0]);
    }

    return null;
  }

  private static Config createConfig(String filename) throws ConfigurationException {

    PropertiesBuilderParameters params =
        new Parameters()
            .properties()
            .setFileName(filename)
            .setListDelimiterHandler(new DefaultListDelimiterHandler(','));

    Configuration config =
        new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
            .configure(params)
            .getConfiguration();

    Path parent = Path.of(filename).toAbsolutePath().getParent();
    Path repos = resolveOptionalPath(parent, config.getString("repos"), null);
    String packageString = config.getString("package");

    Path parentTemplate = repos != null ? repos : parent;
    Path defaultTemplate = repos != null ? repos.resolve(repos.getFileName()) : null;
    Path template =
        resolveOptionalPath(parentTemplate, config.getString("template"), defaultTemplate);

    boolean templateIgnoreViolations = config.getBoolean("template.ignoreViolations", false);

    FeedbackReportConfig styleFeedback = createFeedbackReportConfig(config, "style", parent);
    FeedbackReportConfig oopFeedback = createFeedbackReportConfig(config, "oop", parent);

    boolean github = config.getBoolean("github", false);
    String githubToken = config.getString("github.token");
    String githubClassroom = config.getString("github.classroom");
    String githubAssignment = config.getString("github.assignment");
    boolean githubFeedback = config.getBoolean("github.feedback", false);

    List<CategoryConfig> categoryConfigs = createCategoryConfigs(config);
    List<RequirementConfig> oopRequirements = createRequirementConfigs(config);

    return new Config(
        repos,
        packageString,
        template,
        templateIgnoreViolations,
        styleFeedback,
        oopFeedback,
        github,
        githubToken,
        githubClassroom,
        githubAssignment,
        github && !categoryConfigs.isEmpty() && githubFeedback,
        categoryConfigs,
        oopRequirements);
  }

  private static Path resolveOptionalPath(Path path, String other, Path fallback) {
    if (other == null) {
      return fallback;
    }

    if (Path.of(other).isAbsolute()) {
      return Path.of(other);
    }

    return path.resolve(other);
  }

  private static FeedbackReportConfig createFeedbackReportConfig(
      Configuration config, String prefix, Path parent) {
    Path reportsCsv = resolveOptionalPath(parent, config.getString(prefix + ".reports.csv"), null);
    Path reportsMd = resolveOptionalPath(parent, config.getString(prefix + ".reports.md"), null);
    String feedbackTitle = config.getString(prefix + ".feedback.title");
    String feedbackMessage = config.getString(prefix + ".feedback.message");
    String feedbackError = config.getString(prefix + ".feedback.error");

    return new FeedbackReportConfig(
        reportsCsv, reportsMd, feedbackTitle, feedbackMessage, feedbackError);
  }

  private static List<CategoryConfig> createCategoryConfigs(Configuration config) {
    List<CategoryConfig> categoryConfigs = new ArrayList<>();

    for (Category category : Category.values()) {
      if (!config.getBoolean(category.name(), false)) {
        continue;
      }

      int examples = config.getInt(category.name() + ".examples", Integer.MAX_VALUE);
      Mode mode = config.get(Mode.class, category.name() + ".mode");
      List<Integer> scoreList = config.getList(Integer.class, category.name() + ".scores");

      Collections.sort(scoreList);
      int minWords;

      CategoryConfig categoryConfig = new CategoryConfig(category, examples, mode, scoreList);

      switch (category) {
        case Commenting:
          {
            int minLines = config.getInt(category.name() + ".minLines");
            int minFrequency = config.getInt(category.name() + ".minFrequency");
            int maxFrequency = config.getInt(category.name() + ".maxFrequency");
            int levenshteinDistance = config.getInt(category.name() + ".levenshteinDistance");

            categoryConfig =
                new CommentingConfig(
                    categoryConfig, minLines, minFrequency, maxFrequency, levenshteinDistance);
            break;
          }

        case JavadocClass:
          minWords = config.getInt("JavadocClass.minWords", 5);
          categoryConfig = new JavadocClassConfig(categoryConfig, minWords);

          break;
        case JavadocConstructor:
          minWords = config.getInt("JavadocConstructor.minWords", 5);
          categoryConfig = new JavadocConstructorConfig(categoryConfig, minWords);
          break;
        case JavadocField:
          minWords = config.getInt("JavadocField.minWords", 5);
          categoryConfig = new JavadocFieldConfig(categoryConfig, minWords);
          break;
        case JavadocMethod:
          minWords = config.getInt("JavadocMethod.minWords", 5);
          categoryConfig = new JavadocMethodConfig(categoryConfig, minWords);
          break;

        case Clones:
          {
            int tokens = config.getInt(category.name() + ".tokens");

            categoryConfig = new ClonesConfig(categoryConfig, tokens);
            break;
          }

        case FinalizeOverride:
          categoryConfig = new FinalizeOverrideConfig(categoryConfig);
          break;

        case MissingOverride:
          categoryConfig = new MissingOverrideConfig(categoryConfig);
          break;

        case EmptyCatchBlock:
          categoryConfig = new EmptyCatchBlockConfig(categoryConfig);
          break;

        case StaticMemberNotQualified:
          categoryConfig = new UnqualifiedStaticConfig(categoryConfig);
          break;

        default:
          break;
      }

      if (!categoryConfigs.contains(categoryConfig)) {
        categoryConfigs.add(categoryConfig);
      }
    }


    return categoryConfigs;
  }

  private static List<RequirementConfig> createRequirementConfigs(Configuration config) {
    List<RequirementConfig> oopRequirements = new ArrayList<>();

    for (Requirement requirement : Requirement.values()) {
      if (!config.getBoolean(requirement.name(), false)) {
        continue;
      }

      int require = config.getInt(requirement.name() + ".require", Integer.MIN_VALUE);
      String label = config.getString(requirement.name() + ".label");

      oopRequirements.add(new RequirementConfig(requirement, require, label));
    }

    return oopRequirements;
  }

  private Path repos;

  private String packageString;

  private Path templateRepo;

  private boolean templateIgnoreViolations;

  private FeedbackReportConfig styleFeedback;

  private FeedbackReportConfig oopFeedback;

  private boolean github;

  private String githubToken;

  private String githubClassroom;

  private String githubAssignment;

  private boolean githubFeedback;

  private List<CategoryConfig> categoryConfigs;

  private List<RequirementConfig> oopRequirements;

  private Config(
      Path repos,
      String packageString,
      Path templateRepo,
      boolean templateIgnoreViolations,
      FeedbackReportConfig styleFeedback,
      FeedbackReportConfig oopFeedback,
      boolean github,
      String githubToken,
      String githubClassroom,
      String githubAssignment,
      boolean githubFeedback,
      List<CategoryConfig> categoryConfigs,
      List<RequirementConfig> oopRequirements) {
    this.repos = repos;
    this.packageString = packageString;
    this.templateRepo = templateRepo;
    this.templateIgnoreViolations = templateIgnoreViolations;
    this.styleFeedback = styleFeedback;
    this.oopFeedback = oopFeedback;
    this.github = github;
    this.githubToken = githubToken;
    this.githubClassroom = githubClassroom;
    this.githubAssignment = githubAssignment;
    this.githubFeedback = githubFeedback;
    this.categoryConfigs = categoryConfigs;
    this.oopRequirements = oopRequirements;
  }

  public Path getRepos() {
    return repos;
  }

  public String getPackage() {
    return packageString;
  }

  public Path getTemplateRepo() {
    return templateRepo;
  }

  public boolean getTemplateIgnoreViolations() {
    return templateIgnoreViolations;
  }

  public FeedbackReportConfig getStyleFeedback() {
    return styleFeedback;
  }

  public FeedbackReportConfig getOopFeedback() {
    return oopFeedback;
  }

  public boolean getGithub() {
    return github;
  }

  public String getGithubToken() {
    return githubToken;
  }

  public String getGithubClassroom() {
    return githubClassroom;
  }

  public String getGithubAssignment() {
    return githubAssignment;
  }

  public boolean getGithubFeedback() {
    return githubFeedback;
  }

  public List<CategoryConfig> getCategoryConfigs() {
    return categoryConfigs;
  }

  public CategoryConfig getCategoryConfig(Category category) {
    return getCategoryConfigs().stream()
        .filter(config -> config.getCategory() == category)
        .findFirst()
        .orElse(null);
  }

  public <T extends CategoryConfig> T getCategoryConfig(Class<T> category) {
    return getCategoryConfigs().stream()
        .filter(category::isInstance)
        .map(category::cast)
        .findFirst()
        .orElse(null);
  }

  public List<RequirementConfig> getOopRequirements() {
    return oopRequirements;
  }
}
