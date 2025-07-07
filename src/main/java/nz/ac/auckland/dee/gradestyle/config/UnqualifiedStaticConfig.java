package nz.ac.auckland.dee.gradestyle.config;

public class UnqualifiedStaticConfig extends CategoryConfig {
    public UnqualifiedStaticConfig(CategoryConfig config) {
      super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
    }
  }