package nz.ac.auckland.dee.gradestyle.validator.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader.IgnoredModulesOptions;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.util.FileUtils;
import nz.ac.auckland.dee.gradestyle.validator.Validator;
import nz.ac.auckland.dee.gradestyle.validator.ValidatorException;
import nz.ac.auckland.dee.gradestyle.validator.Violations;
import org.xml.sax.InputSource;

public class Checkstyle implements Validator {
  private static final URL config = Checkstyle.class.getResource("checkstyle.xml");

  @Override
  public Violations validate(Repo repo) throws ValidatorException {
    Violations violations = new Violations();

    try {
      runCheckstyle(repo, violations);
    } catch (CheckstyleException e) {
      String message = "Exception was thrown while processing ";
      int index = e.getMessage().indexOf(message);
      Path path = Path.of(e.getMessage().substring(index + message.length()));
      e.printStackTrace();
      throw new ValidatorException(path);
    } catch (IOException e) {
      throw new ValidatorException(e);
    }

    return violations;
  }

  private void runCheckstyle(Repo repo, Violations violations)
      throws CheckstyleException, IOException {
    InputSource source = new InputSource(config.openStream());
    PropertiesExpander props = new PropertiesExpander(System.getProperties());
    Configuration configuration =
        ConfigurationLoader.loadConfiguration(source, props, IgnoredModulesOptions.EXECUTE);

    Checker checker = new Checker();

    checker.setModuleClassLoader(Checker.class.getClassLoader());
    checker.configure(configuration);
    checker.addListener(new Listener(violations));
    // checker.process(getJavaFiles(repo));
    checker.process(FileUtils.getJavaSrcFiles(repo).map(Path::toFile).toList());
    // checker.process(FileUtils.getJavaRootFiles(repo).map(Path::toFile).toList());
    checker.destroy();
  }

  // private static List<File> getJavaFiles(Repo repo) throws IOException {
  //   return FileUtils.getJavaSrcFiles(repo.getDir()).map(Path::toFile).toList();
  // }
}
