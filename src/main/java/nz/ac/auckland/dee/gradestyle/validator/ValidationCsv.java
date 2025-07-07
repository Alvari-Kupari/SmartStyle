package nz.ac.auckland.dee.gradestyle.validator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.config.CategoryConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class ValidationCsv implements AutoCloseable {
  private BufferedWriter bufferedWriter;
  private CSVPrinter printer;
  private List<CategoryConfig> categoryConfigs;

  public ValidationCsv(List<CategoryConfig> categoryConfigs, Path file) throws IOException {
        this.categoryConfigs = categoryConfigs;

        // Ensure parent directories exist
        if (!Files.exists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }

        // Check if the file already exists to decide if headers are needed
        boolean append = Files.exists(file);
        
        // Open file for writing with append option if file exists
        this.bufferedWriter = Files.newBufferedWriter(file, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.WRITE, 
            append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);

        // Initialize headers dynamically only if the file did not exist
        List<String> headers = new ArrayList<>();
        headers.add("Name");
        headers.add("Hash");
        categoryConfigs.forEach(
            config -> {
                headers.addAll(config.getCategory().getTypes().stream().map(Object::toString).toList());
                headers.add(config.getCategory().toString() + " Absolute Score");
                headers.add(config.getCategory().toString() + " Normalized Score");
            });
        headers.add("Total Absolute Score");
        headers.add("Total Normalized Score");
        CSVFormat format = append ? CSVFormat.DEFAULT : CSVFormat.Builder.create().setHeader(headers.toArray(String[]::new)).build();

        this.printer = new CSVPrinter(bufferedWriter, format);
    }

  public void writeResult(ValidationResult result, Repo repo) throws IOException {
    List<Object> row = new ArrayList<>();

    row.add(result.getRepo().getName());
    row.add(result.getRepo().getCommit());

    if (result.getError() != null) {
      int remaining =
          categoryConfigs.stream()
                  .mapToInt(
                      config ->
                          (config.getCategory().getTypes().size() * 2)
                              + 2) // Absolute + Normalized columns
                  .sum()
              + 1; // Total Score column
      row.addAll(Collections.nCopies(remaining, "N/A"));
    } else {
      // Compute absolute and normalized scores
      Map<Category, Integer> absoluteScores = Category.getCategoryScores(result, categoryConfigs);
      Map<Category, Double> normalizedScores = calculateNormalizedScores(absoluteScores, repo);

      categoryConfigs.forEach(
          config -> {
            Category category = config.getCategory();

            // Add raw violations for each type under the category
            config
                .getCategory()
                .getTypes()
                .forEach(
                    type -> {
                      row.add(result.getViolations().filterByType(type).getViolations().size());
                    });

            // Add absolute and normalized scores
            row.add(absoluteScores.get(category)); // Absolute score
            row.add(normalizedScores.get(category)); // Normalized score
          });

      // Add total scores
      int totalAbsoluteScore = absoluteScores.values().stream().mapToInt(Integer::intValue).sum();
      double totalNormalizedScore =
          normalizedScores.values().stream().mapToDouble(Double::doubleValue).sum();
      row.add(totalAbsoluteScore);
      row.add(totalNormalizedScore);
    }

    printer.printRecord(row);
    printer.flush();
  }

  private Map<Category, Double> calculateNormalizedScores(
      Map<Category, Integer> absoluteScores, Repo repo) {
    Map<Category, Double> normalizedScores = new HashMap<>();

    for (CategoryConfig config : categoryConfigs) {
      Category category = config.getCategory();
      int absoluteScore = absoluteScores.getOrDefault(category, 0);

      try {
        // Use the normalization logic from Category
        long normalization = category.getNormalisation(repo);

        double normalizedValue = normalization != 0 ? (double) absoluteScore / normalization : 0.0;
        normalizedScores.put(category, normalizedValue);
      } catch (IOException e) {
        System.err.println("Error calculating normalization for category: " + category);
        normalizedScores.put(category, 0.0);
      }
    }

    return normalizedScores;
  }

  @Override
  public void close() throws IOException {
    if (printer != null) {
      printer.close();
    }
    if (bufferedWriter != null) {
      bufferedWriter.close();
    }
  }
}
