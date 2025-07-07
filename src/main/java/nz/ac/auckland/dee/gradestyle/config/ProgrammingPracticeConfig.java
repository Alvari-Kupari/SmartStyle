package nz.ac.auckland.dee.gradestyle.config;

public class ProgrammingPracticeConfig extends CategoryConfig {
  public ProgrammingPracticeConfig(CategoryConfig config) {
    super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
  }
}
