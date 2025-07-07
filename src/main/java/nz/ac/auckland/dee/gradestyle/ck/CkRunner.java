package nz.ac.auckland.dee.gradestyle.ck;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import nz.ac.auckland.dee.gradestyle.Github;
import nz.ac.auckland.dee.gradestyle.Repo;
import nz.ac.auckland.dee.gradestyle.config.Config;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CkRunner {
  public static void main(String[] args) {

    Config config = Config.parse(args);

    // get the repos
    if (config == null) {
      System.out.println("Usage: <config-location>");
      System.exit(1);
    }

    Github github = new Github(config);
    List<Repo> repos = Repo.getRepos(github);

    Path outputPath =
        Path.of("C:\\Users\\Alvari\\Documents\\UNI\\summer_research\\reports\\ck-report.csv");

    try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
        CSVPrinter printer =
            new CSVPrinter(
                writer,
                CSVFormat.DEFAULT.withHeader(
                    "Repository",
                    "Average WMC",
                    "Average CBO",
                    "Average DIT",
                    "Average NOC",
                    "Average RFC",
                    "Average LOC",
                    "Average FanIn",
                    "Average FanOut",
                    "Average LCOM",
                    "Average Tight Cohesion",
                    "Average Loose Cohesion",
                    "Average NOSI",
                    "Average Return Qty",
                    "Average Loop Qty",
                    "Average Comparisons Qty",
                    "Average Try-Catch Qty",
                    "Average Parenthesized Exp Qty",
                    "Average String Literals Qty",
                    "Average Numbers Qty",
                    "Average Assignments Qty",
                    "Average Math Operations Qty",
                    "Average Variables Qty",
                    "Average Max Nested Blocks",
                    "Average Anonymous Classes Qty",
                    "Average Lambdas Qty",
                    "Average Unique Words Qty",
                    "Average Number of Methods"))) {

      for (Repo repo : repos) {
        System.out.println("Analyzing repository: " + repo.getName());

        Path repoDir = repo.getDir();

        if (!Files.isDirectory(repoDir)) {
          System.err.println("Skipping: " + repoDir + " is not a directory.");
          continue;
        }

        System.out.println("Running CK analysis on directory: " + repoDir);

        List<CKClassResult> classResults = new ArrayList<>();

        // Perform the CK analysis for this repository
        CK ck = new CK();
        ck.calculate(
            repoDir.toString(),
            new CKNotifier() {
              @Override
              public void notify(CKClassResult result) {
                classResults.add(result);
              }

              @Override
              public void notifyError(String sourceFilePath, Exception e) {
                System.err.println("Error analyzing file: " + sourceFilePath);
                e.printStackTrace();
              }
            });

        System.out.println("CK analysis completed for repository: " + repoDir);

        // Calculate averages for this repository
        if (!classResults.isEmpty()) {

          double avgWMC = classResults.stream().mapToInt(CKClassResult::getWmc).average().orElse(0);
          double avgCBO = classResults.stream().mapToInt(CKClassResult::getCbo).average().orElse(0);
          double avgDIT = classResults.stream().mapToInt(CKClassResult::getDit).average().orElse(0);
          double avgNOC = classResults.stream().mapToInt(CKClassResult::getNoc).average().orElse(0);
          double avgRFC = classResults.stream().mapToInt(CKClassResult::getRfc).average().orElse(0);
          double avgLOC = classResults.stream().mapToInt(CKClassResult::getLoc).average().orElse(0);
          double avgFanIn =
              classResults.stream().mapToInt(CKClassResult::getFanin).average().orElse(0);
          double avgFanOut =
              classResults.stream().mapToInt(CKClassResult::getFanout).average().orElse(0);
          double avgLCOM =
              classResults.stream().mapToInt(CKClassResult::getLcom).average().orElse(0);
          double avgTightCohesion =
              classResults.stream()
                  .mapToDouble(
                      r -> Double.isNaN(r.getTightClassCohesion()) ? 0 : r.getTightClassCohesion())
                  .average()
                  .orElse(0);

          double avgLooseCohesion =
              classResults.stream()
                  .mapToDouble(
                      r -> Double.isNaN(r.getLooseClassCohesion()) ? 0 : r.getLooseClassCohesion())
                  .average()
                  .orElse(0);

          double avgNOSI =
              classResults.stream().mapToInt(CKClassResult::getNosi).average().orElse(0);
          double avgReturnQty =
              classResults.stream().mapToInt(CKClassResult::getReturnQty).average().orElse(0);
          double avgLoopQty =
              classResults.stream().mapToInt(CKClassResult::getLoopQty).average().orElse(0);
          double avgComparisonsQty =
              classResults.stream().mapToInt(CKClassResult::getComparisonsQty).average().orElse(0);
          double avgTryCatchQty =
              classResults.stream().mapToInt(CKClassResult::getTryCatchQty).average().orElse(0);
          double avgParenthesizedExpQty =
              classResults.stream()
                  .mapToInt(CKClassResult::getParenthesizedExpsQty)
                  .average()
                  .orElse(0);
          double avgStringLiteralsQty =
              classResults.stream()
                  .mapToInt(CKClassResult::getStringLiteralsQty)
                  .average()
                  .orElse(0);
          double avgNumbersQty =
              classResults.stream().mapToInt(CKClassResult::getNumbersQty).average().orElse(0);
          double avgAssignmentsQty =
              classResults.stream().mapToInt(CKClassResult::getAssignmentsQty).average().orElse(0);
          double avgMathOperationsQty =
              classResults.stream()
                  .mapToInt(CKClassResult::getMathOperationsQty)
                  .average()
                  .orElse(0);
          double avgVariablesQty =
              classResults.stream().mapToInt(CKClassResult::getVariablesQty).average().orElse(0);
          double avgMaxNestedBlocks =
              classResults.stream().mapToInt(CKClassResult::getMaxNestedBlocks).average().orElse(0);
          double avgAnonymousClassesQty =
              classResults.stream()
                  .mapToInt(CKClassResult::getAnonymousClassesQty)
                  .average()
                  .orElse(0);
          double avgLambdasQty =
              classResults.stream().mapToInt(CKClassResult::getLambdasQty).average().orElse(0);
          double avgUniqueWordsQty =
              classResults.stream().mapToInt(CKClassResult::getUniqueWordsQty).average().orElse(0);
          double avgNumberOfMethods =
              classResults.stream().mapToInt(CKClassResult::getNumberOfMethods).average().orElse(0);

          // Write results to CSV incrementally
          printer.printRecord(
              repo.getName(),
              avgWMC,
              avgCBO,
              avgDIT,
              avgNOC,
              avgRFC,
              avgLOC,
              avgFanIn,
              avgFanOut,
              avgLCOM,
              avgTightCohesion,
              avgLooseCohesion,
              avgNOSI,
              avgReturnQty,
              avgLoopQty,
              avgComparisonsQty,
              avgTryCatchQty,
              avgParenthesizedExpQty,
              avgStringLiteralsQty,
              avgNumbersQty,
              avgAssignmentsQty,
              avgMathOperationsQty,
              avgVariablesQty,
              avgMaxNestedBlocks,
              avgAnonymousClassesQty,
              avgLambdasQty,
              avgUniqueWordsQty,
              avgNumberOfMethods);
          printer.flush(); // Ensure the row is written immediately
        } else {
          System.err.println("No class results for repository: " + repo.getName());
        }
      }

    } catch (IOException e) {
      System.err.println("Unable to write CSV file.");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
