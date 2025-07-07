package nz.ac.auckland.dee.gradestyle.config.javadocconfig;

import nz.ac.auckland.dee.gradestyle.config.CategoryConfig;

public class JavadocConstructorConfig extends CategoryConfig {
  private int minWords;

  public JavadocConstructorConfig(CategoryConfig config, int minWords) {
    super(config.getCategory(), config.getExamples(), config.getMode(), config.getScores());
    this.minWords = minWords;
  }

  public int getMinWords() {
    return minWords;
  }
}
