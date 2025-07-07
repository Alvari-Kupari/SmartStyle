package nz.ac.auckland.dee.gradestyle.config;

public class MissingOverrideConfig extends CategoryConfig {
    public MissingOverrideConfig(CategoryConfig config) {
      super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
    }
  }