package nz.ac.auckland.dee.gradestyle.githubanalysis;

import com.github.javaparser.ParseProblemException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.reporting.Report;
import nz.ac.auckland.dee.gradestyle.util.FileUtils;
import nz.ac.auckland.dee.gradestyle.validator.pmd.Pmd;

public class RepoValidator {

  public static final boolean checkNumberOfFiles = true;
  public static final int maxJavaFiles = 6000;
  public static final int minJavaFiles = 1;
  public static final int maxFileLength = 10000;

  private static final URL config = Pmd.class.getResource("pmd.xml");

  private File repoDir;

  public RepoValidator(File repoDir) {
    this.repoDir = repoDir;
  }

  /**
   * Checks the repo for a few criteria. First, checks whether it has either too much or too few
   * java source files to analyse. Then, it checks whether there are any massive java files which
   * may crash the code style tools. Finally it tests whether the repo can be parsed by javaparser
   * to make sure the code analysis tools can actually process it.
   *
   * @return
   */
  public boolean validate() {
    try {
      List<Path> sourceFiles = FileUtils.getJavaSrcFiles(repoDir.toPath()).toList();

      int numJavaFiles = sourceFiles.size();

      if (numJavaFiles < minJavaFiles || numJavaFiles > maxJavaFiles) {
        System.out.println("Invalid number of java files. Number of files: " + numJavaFiles);
        return false;
      }

      // next check if there are any massive java files
      long longestJavaFile = getMaxLinesInJavaFiles(sourceFiles.stream());

      if (longestJavaFile > maxFileLength) {
        System.out.println("Too long of a file found, length: " + longestJavaFile);
        return false;
      }

      // next test if can be parsed
      PMDConfiguration configuration = new PMDConfiguration();
      configuration.setSourceEncoding(StandardCharsets.UTF_8);

      JavaLanguageModule lan = new JavaLanguageModule();
      lan.getVersion("17");
      configuration.setDefaultLanguageVersion(lan.getVersion("17"));

      Path ruleSetPath;
      ruleSetPath = Paths.get(config.toURI());

      configuration.addRuleSet(ruleSetPath.toString());

      configuration.setIgnoreIncrementalAnalysis(true);

      configuration.setInputPathList(sourceFiles);

      Report report = PmdAnalysis.create(configuration).performAnalysisAndCollectReport();

      if (!report.getProcessingErrors().isEmpty()) {
        System.out.println(
            "Parsing problem found: " + report.getProcessingErrors().get(0).getMsg());
        return false;
      }

      com.github.javaparser.JavaParser javaParser;

      javaParser = nz.ac.auckland.dee.gradestyle.util.JavaParser.get(repoDir.toPath());

      for (Path file : sourceFiles) {
        try {
          javaParser.parse(file);
        } catch (ParseProblemException e) {
          System.out.println("Problem parsing source code, reason: " + e.getMessage());
          return false;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  public long getMaxLinesInJavaFiles(Stream<Path> paths) {
    return paths
        .filter(Files::isRegularFile) // Ensure it's a regular file
        .filter(path -> path.toString().endsWith(".java")) // Only Java files
        .mapToLong(
            path -> {
              try {
                return Files.lines(path).count(); // Count lines in the file
              } catch (IOException e) {
                System.err.println("Error reading file: " + path);
                return 0L; // If an error occurs, treat it as 0 lines
              }
            })
        .max() // Find the maximum number of lines
        .orElse(0L); // Return 0 if no files are processed
  }

  public long getTotalLinesInJavaFiles() {
    try {
      // Get all Java source files in the repository
      List<Path> sourceFiles = FileUtils.getJavaSrcFiles(repoDir.toPath()).toList();

      // Calculate the total lines across all files
      return sourceFiles.stream()
          .filter(Files::isRegularFile) // Ensure it's a regular file
          .filter(path -> path.toString().endsWith(".java")) // Only Java files
          .mapToLong(
              path -> {
                try {
                  return Files.lines(path).count(); // Count lines in the file
                } catch (IOException e) {
                  System.err.println("Error reading file: " + path);
                  return 0L; // Treat unreadable files as 0 lines
                }
              })
          .sum(); // Sum the lines of all files
    } catch (IOException e) {
      System.err.println("Error retrieving Java source files: " + e.getMessage());
      return 0L; // Return 0 if an error occurs
    }
  }
}
