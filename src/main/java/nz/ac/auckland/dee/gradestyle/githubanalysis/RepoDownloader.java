package nz.ac.auckland.dee.gradestyle.githubanalysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import nz.ac.auckland.dee.gradestyle.config.Config;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositorySearchBuilder;
import org.kohsuke.github.GHRepositorySearchBuilder.Sort;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterator;

public class RepoDownloader {
  private static final int maxRepoSize = 2000000;

  private GitHub github;
  private Config config;
  private RepoValidator validator;

  private boolean seenRepo;

  public RepoDownloader(Config config) throws IOException {
    this.config = config;
    this.github = new GitHubBuilder().withOAuthToken(config.getGithubToken()).build();
    this.seenRepo = false;
  }

  public void download(int n, List<Sort> sortCriteria) throws Exception {
    // Determine how many repositories are already downloaded
    File repoDir = config.getRepos().toFile();
    if (!repoDir.exists()) {
      repoDir.mkdirs();
    }
    int alreadyDownloaded = repoDir.list() == null ? 0 : repoDir.list().length;
    int needed = Math.max(0, n - alreadyDownloaded);

    System.out.println("Already downloaded: " + alreadyDownloaded + " repositories.");
    System.out.println("Need to download: " + needed + " more repositories.");

    if (needed == 0) {
      System.out.println("Target already met. No additional downloads required.");
      return;
    }

    // Search repositories and sort by the specified criteria
    GHRepositorySearchBuilder searchBuilder =
        github
            .searchRepositories()
            .q("language:java created:<2021-01-01") // Only repositories created before 2021
            .order(GHDirection.DESC);

    for (Sort sort : sortCriteria) {
      searchBuilder.sort(sort);
    }

    // Optimize API usage: set page size to 100
    PagedIterator<GHRepository> iterator = searchBuilder.list().withPageSize(100).iterator();
    int count = 0;

    while (iterator.hasNext() && count < needed) {

      // Check and handle rate limits
      GHRateLimit rateLimit = github.getRateLimit();
      if (rateLimit.getRemaining() < 10) {
        System.out.println("Rate limit approaching. Pausing until reset...");
        Thread.sleep((rateLimit.getResetDate().getTime() - System.currentTimeMillis()) + 1000);
      }

      System.out.println("Count: " + count);
      GHRepository repo = iterator.next();

      if (repo.getName().equals("curator")) {
        seenRepo = true;
      }

      if (!seenRepo) {
        continue;
      }

      if (repo.getSize() > maxRepoSize) {
        System.out.println("Skipping: " + repo.getName() + " due to large size: " + repo.getSize());
        continue;
      }

      // Determine the target directory for this repository
      File repoPath = new File(repoDir, repo.getName());

      if (repoPath.exists()) {
        System.out.println("Skipping " + repo.getName() + " (already downloaded).");
        continue;
      }

      // Check if the repo name exists in the failed_repos directory
      File failedReposDir = new File("D:/failed_repos");
      if (failedReposDir.exists() && failedReposDir.isDirectory()) {
        File failedRepo = new File(failedReposDir, repo.getName());
        if (failedRepo.exists()) {
          System.out.println("Skipping: " + repo.getName() + " (found in failed repos directory).");
          continue;
        }
      }

      // Repository's clone URL
      String cloneUrl = repo.getHttpTransportUrl();

      try {
        System.out.println("Cloning " + repo.getName() + " into " + repoPath.getAbsolutePath());

        // Use Git CLI for shallow clone
        ProcessBuilder builder =
            new ProcessBuilder(
                "git",
                "clone",
                "--depth=1",
                "--config",
                "core.longpaths=true",
                cloneUrl,
                repoPath.getAbsolutePath());

        builder.inheritIO(); // Redirect output to console
        Process process = builder.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
          // Validate repo and make sure it is good for processing
          validator = new RepoValidator(repoPath);
          boolean success = validator.validate();

          if (success) {
            System.out.println("All checks passed: " + repo.getName());
            count++;
            continue;
          }
        } else {
          System.err.println("Failed to clone repository: " + repo.getName());
        }
      } catch (Exception e) {
        System.err.println(
            "Error processing repository: " + repo.getName() + ": " + e.getMessage());
        e.printStackTrace();
      }

      // Delete if repo wasn't successfully validated or an error occurred.
      System.out.println("Deleting: " + repo.getName());
      deleteDirectory(repoPath);
    }

    System.out.println(
        "Downloaded "
            + count
            + " repositories. Total repositories: "
            + (alreadyDownloaded + count));
  }

  private void deleteDirectory(File directory) {
    if (directory.isDirectory()) {
      for (File file : directory.listFiles()) {
        deleteDirectory(file);
      }
    }
    directory.delete();
  }

  public void copyUniqueRepos(String sourceDir, String targetDir) {
    File source = new File(sourceDir);
    File target = new File(targetDir);

    // Ensure source and target directories exist
    if (!source.exists() || !source.isDirectory()) {
      System.err.println("Source directory does not exist or is not a directory: " + sourceDir);
      return;
    }
    if (!target.exists()) {
      target.mkdirs(); // Create target directory if it doesn't exist
    }

    File[] sourceRepos = source.listFiles(File::isDirectory); // Only directories
    if (sourceRepos == null) {
      System.err.println("No repositories found in source directory: " + sourceDir);
      return;
    }

    for (File repo : sourceRepos) {
      File targetRepo = new File(target, repo.getName());

      // Check if the repository already exists in the target directory
      if (targetRepo.exists()) {
        System.out.println(
            "Skipping: " + repo.getName() + " (already exists in target directory).");
        continue;
      }

      // Copy the repository to the target directory
      try {
        System.out.println("Copying: " + repo.getName() + " to " + targetRepo.getAbsolutePath());
        copyDirectory(repo.toPath(), targetRepo.toPath());
      } catch (IOException e) {
        System.err.println(
            "Failed to copy repository: " + repo.getName() + ". Error: " + e.getMessage());
      }
    }

    // Calculate total repositories in the target directory
    String[] targetRepos = target.list((current, name) -> new File(current, name).isDirectory());
    int totalRepos = targetRepos == null ? 0 : targetRepos.length;
    System.out.println("Total repositories in " + targetDir + ": " + totalRepos);
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.walk(source)
        .forEach(
            sourcePath -> {
              Path targetPath = target.resolve(source.relativize(sourcePath));
              try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
              } catch (IOException e) {
                System.err.println("Error copying file: " + sourcePath + " to " + targetPath);
              }
            });
  }
}
