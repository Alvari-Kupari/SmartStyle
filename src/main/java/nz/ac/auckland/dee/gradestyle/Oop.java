package nz.ac.auckland.dee.gradestyle;

import java.io.IOException;
import java.util.List;
import nz.ac.auckland.dee.gradestyle.config.Config;
import nz.ac.auckland.dee.gradestyle.oop.Analysis;
import nz.ac.auckland.dee.gradestyle.oop.AnalysisCsv;
import nz.ac.auckland.dee.gradestyle.oop.AnalysisMarkdown;
import nz.ac.auckland.dee.gradestyle.oop.AnalysisResult;

public class Oop {
  public static void main(String[] args) {

    Config config = Config.parse(args);

    if (config == null) {
      System.exit(1);
    }

    Github github = new Github(config);
    List<Repo> repos = Repo.getRepos(github);
    List<AnalysisResult> results = runAnalysis(config, repos);

    outputCsv(config, results);
    outputMarkdown(config, results);
  }

  private static List<AnalysisResult> runAnalysis(Config config, List<Repo> repos) {
    Analysis analysis = new Analysis(config);

    try {
      return analysis.analyse(repos);
    } catch (IOException e) {
      System.err.println("Unable to run OOP analysis.");
      e.printStackTrace();
      System.exit(1);
    }

    return null;
  }

  private static void outputCsv(Config config, List<AnalysisResult> results) {
    if (config.getOopFeedback().getReportsCsv() == null) {
      return;
    }

    AnalysisCsv writer = new AnalysisCsv(config.getOopRequirements(), results);
    Csv csv = new Csv(config.getOopFeedback().getReportsCsv(), writer);

    try {
      csv.write();
    } catch (IOException e) {
      System.err.println("Unable to write CSV file.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void outputMarkdown(Config config, List<AnalysisResult> results) {
    if (config.getOopFeedback().getReportsMd() == null) {
      return;
    }

    AnalysisMarkdown writer = new AnalysisMarkdown(config);
    Markdown<AnalysisResult> md = new Markdown<>(config.getOopFeedback().getReportsMd(), writer);

    try {
      md.write(results);
    } catch (IOException e) {
      System.err.println("Unable to write markdown files.");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
