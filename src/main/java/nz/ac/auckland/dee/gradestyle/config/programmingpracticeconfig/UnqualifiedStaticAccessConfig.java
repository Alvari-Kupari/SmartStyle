package nz.ac.auckland.dee.gradestyle.config.programmingpracticeconfig;

import nz.ac.auckland.dee.gradestyle.config.CategoryConfig;

public class UnqualifiedStaticAccessConfig extends CategoryConfig {
    public UnqualifiedStaticAccessConfig(CategoryConfig config) {
      super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
    }
  }
  