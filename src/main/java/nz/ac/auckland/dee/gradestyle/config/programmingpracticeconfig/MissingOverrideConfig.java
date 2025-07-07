package nz.ac.auckland.dee.gradestyle.config.programmingpracticeconfig;

import nz.ac.auckland.dee.gradestyle.config.CategoryConfig;

public class MissingOverrideConfig extends CategoryConfig {
    public MissingOverrideConfig(CategoryConfig config) {
      super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
    }
  }
  