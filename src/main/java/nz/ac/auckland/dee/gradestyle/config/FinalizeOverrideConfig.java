package nz.ac.auckland.dee.gradestyle.config;

public class FinalizeOverrideConfig extends CategoryConfig {
    public FinalizeOverrideConfig(CategoryConfig config) {
      super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
    }
  }
