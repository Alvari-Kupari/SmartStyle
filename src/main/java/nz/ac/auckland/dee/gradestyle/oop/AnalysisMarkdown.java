package nz.ac.auckland.dee.gradestyle.oop;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.steppschuh.markdowngenerator.link.Link;
import net.steppschuh.markdowngenerator.list.ListBuilder;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.code.Code;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;
import nz.ac.auckland.dee.gradestyle.Markdown;
import nz.ac.auckland.dee.gradestyle.config.Config;
import nz.ac.auckland.dee.gradestyle.config.RequirementConfig;

public class AnalysisMarkdown implements Markdown.Writer<AnalysisResult> {
  private Config config;

  public AnalysisMarkdown(Config config) {
    this.config = config;
  }

  @Override
  public String getFileName(AnalysisResult result) throws IOException {
    String fileName = result.getRepo().getName();

    if (config.getOopFeedback().getReportsMd().equals(config.getStyleFeedback().getReportsMd())) {
      fileName += "-OOP";
    }

    return fileName;
  }

  @Override
  public String write(AnalysisResult result) throws IOException {
    StringBuilder sb = new StringBuilder();

    Markdown.title(sb, config.getOopFeedback().getFeedbackTitle());
    Markdown.message(sb, config.getOopFeedback().getFeedbackMessage());

    if (result.getError() == null) {
      declarations(sb, result);
      resultsTable(sb, result);
    } else {
      Markdown.message(sb, config.getOopFeedback().getFeedbackError());
    }

    Markdown.footer(sb, result.getRepo());

    return sb.toString();
  }

  private void declarations(StringBuilder sb, AnalysisResult result) {
    Markdown.heading(sb, "Declarations", 2);

    List<Declaration> declarations = result.getDeclarations();

    if (declarations.isEmpty()) {
      sb.append(new ItalicText("None.")).append("\n\n");
      return;
    }

    ListBuilder builder = new ListBuilder();

    for (Declaration declaration : declarations) {
      Object row = new Code(declaration);

      Path relative =
          result
              .getRepo()
              .getDir()
              .toAbsolutePath()
              .relativize(declaration.getPath().toAbsolutePath());

      String url = result.getRepo().getFileLineUrl(relative, declaration.getLine());

      if (url != null) {
        row = new Link(row, url);
      }

      builder.append(row);
    }

    sb.append(builder.build()).append("\n\n");
  }

  private void resultsTable(StringBuilder sb, AnalysisResult result) {
    Markdown.heading(sb, "OO Design", 2);

    Table.Builder builder =
        new Table.Builder()
            .addRow(new BoldText("Requirement"), new BoldText("Score"))
            .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_RIGHT);

    Map<RequirementConfig, Long> results =
        Requirement.getRequirementResult(result, config.getOopRequirements());
    long total = 0;
    long max = 0;

    for (RequirementConfig config : config.getOopRequirements()) {
      long score = Math.min(results.get(config), config.getRequire());

      total += score;
      max += config.getRequire();

      builder.addRow(config.getLabel(), score + " / " + config.getRequire());
    }

    builder.addRow(new BoldText("Total"), new BoldText(total + " / " + max));

    sb.append(builder.build()).append("\n\n");
  }
}
