package nz.ac.auckland.dee.gradestyle.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Repo;

public class FileUtils {
  public static final Path MAIN_DIR = Path.of("src/main/java");
  public static final Path TEST_DIR = Path.of("src/test/java");
  public static final Path RESOURCES_DIR = Path.of("src/main/resources");

  public static Path getMainDir(Repo repo) {
    return repo.getDir().resolve(MAIN_DIR);
  }

  public static Stream<Path> getJavaFiles(Path dir) throws IOException {
    return Files.walk(dir).filter(Files::isRegularFile).filter(FileUtils::isJavaFile);
  }

  public static Stream<Path> getJavaSrcFiles(Path repoDir) throws IOException {
    // Walk through the repository directory and find all "src/main/java" directories
    return Files.walk(repoDir)
        // Filter to include only directories
        .filter(Files::isDirectory)
        // Filter to include only paths ending with "src/main/java"
        .filter(dir -> dir.endsWith(MAIN_DIR))
        // Map each "src/main/java" directory to its Java files
        .flatMap(
            t -> {
              try {
                return getJavaFiles(t);
              } catch (IOException e) {
                e.printStackTrace();
                return null;
              }
            });
  }

  public static boolean isJavaFile(Path file) {
    return getFileExtension(file).equals("java");
  }

  public static boolean isInRepoTestDir(Repo repo, Path file) {
    return file.toAbsolutePath().startsWith(repo.getDir().resolve(TEST_DIR).toAbsolutePath());
  }

  public static Stream<Path> getFxmlResourceFiles(Path dir) throws IOException {
    return getFxmlFiles(dir.resolve(RESOURCES_DIR));
  }

  public static Stream<Path> getFxmlFiles(Path dir) throws IOException {
    return Files.walk(dir).filter(Files::isRegularFile).filter(FileUtils::isFxmlFile);
  }

  public static boolean isFxmlFile(Path file) {
    return getFileExtension(file).equals("fxml");
  }

  public static String getFileExtension(Path file) {
    String fileName = file.getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');

    return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
  }

  public static String getPomFilePath(Repo repo) {
    Path pomPath = repo.getDir().resolve("pom.xml");
    if (Files.exists(pomPath) && Files.isRegularFile(pomPath)) {
      return pomPath.toString();
    } else {
      throw new IllegalStateException("POM file not found in the repository root: " + pomPath);
    }
  }

  // public static Stream<Path> getJavaRootFiles(Repo repo) throws IOException {
  //   return FileUtils.getJavaFiles(repo.getDir());
  // }

  public static Stream<Path> getJavaSrcFiles(Repo repo) throws IOException {
    return getJavaSrcFiles(repo.getDir());
  }

  public static List<Path> getSrcDirs(Path repoDir) {
    List<Path> paths = new ArrayList<>();

    try {
      Files.walk(repoDir).filter(Files::isDirectory).filter(dir -> dir.endsWith(MAIN_DIR)).forEach(dir -> paths.add(dir));
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    };

    return paths;
  }
}
