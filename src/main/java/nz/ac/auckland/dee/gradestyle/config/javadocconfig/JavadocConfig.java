package nz.ac.auckland.dee.gradestyle.config.javadocconfig;

import nz.ac.auckland.dee.gradestyle.config.CategoryConfig;

public class JavadocConfig extends CategoryConfig {
  public JavadocConfig(CategoryConfig config) {
    super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
  }
}
