package nz.ac.auckland.dee.gradestyle.githubanalysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Style;
import nz.ac.auckland.dee.gradestyle.config.Config;
import org.kohsuke.github.*;
import org.kohsuke.github.GHRepositorySearchBuilder.Sort;

public class PopularRepositories {

  public static void main(String[] args) {
    try {
      Config config = Config.parse(args);
      RepoDownloader downloader = new RepoDownloader(config);

      List<Sort> criteria = List.of(Sort.STARS);

      downloader.download(1050, criteria);
      // downloader.copyUniqueRepos("D:/test", "D:/repos");

      // runRepos();

    } catch (IOException e) {
      System.err.println("Error fetching repositories: " + e.getMessage() + e.getCause());
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Error processing repositories: " + e.getMessage() + e.getCause());
    }
  }

  private static void runRepos() {
    String[] args = new String[] {"config/test.properties"};

    Style.main(args);
    // CkRunner.main(args);
  }

  public static void analyzeJavaFiles(Stream<Path> paths) {
    // Initialize variables to calculate max and average
    long[] results =
        paths
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
            .collect(
                () -> new long[3], // Array to hold count, sum, and max
                (acc, lineCount) -> {
                  acc[0]++; // Increment file count
                  acc[1] += lineCount; // Add to total line count
                  acc[2] = Math.max(acc[2], lineCount); // Update max lines
                },
                (acc1, acc2) -> {
                  acc1[0] += acc2[0];
                  acc1[1] += acc2[1];
                  acc1[2] = Math.max(acc1[2], acc2[2]);
                });

    // Calculate average
    long fileCount = results[0];
    long totalLines = results[1];
    long maxLines = results[2];

    double averageLines = fileCount > 0 ? (double) totalLines / fileCount : 0.0;

    // Print results
    System.out.println("Number of files processed: " + fileCount);
    System.out.println("Average number of lines: " + averageLines);
    System.out.println("Maximum number of lines: " + maxLines);
  }
}
