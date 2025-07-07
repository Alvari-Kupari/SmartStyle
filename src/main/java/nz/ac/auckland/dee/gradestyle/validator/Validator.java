package nz.ac.auckland.dee.gradestyle.validator;

import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.config.Config;

public interface Validator {
  default void setup(Config config) throws ValidatorException {}

  Violations validate(Repo repo) throws ValidatorException;
}
