package nz.ac.auckland.dee.gradestyle.oop;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import nz.ac.auckland.dee.gradestyle.Csv.Writer;
import nz.ac.auckland.dee.gradestyle.config.RequirementConfig;

public class AnalysisCsv implements Writer {
  private List<RequirementConfig> configs;

  private List<AnalysisResult> results;

  public AnalysisCsv(List<RequirementConfig> configs, List<AnalysisResult> results) {
    this.configs = configs;
    this.results = results;
  }

  @Override
  public List<String> getHeaders() throws IOException {
    Stream<String> names = Stream.of("Name", "Hash");
    Stream<String> requirementNames = Arrays.stream(Requirement.values()).map(Object::toString);
    Stream<String> unreferenced = Stream.of("Unreferenced Declarations");
    Stream<String> requirements =
        configs.stream().map(RequirementConfig::getLabel).map(c -> c + " Score");
    Stream<String> total = Stream.of("Score");

    return Stream.of(names, requirementNames, unreferenced, requirements, total)
        .flatMap(Function.identity())
        .toList();
  }

  @Override
  public List<List<Object>> getRows() throws IOException {
    return results.stream().map(this::getRow).toList();
  }

  private List<Object> getRow(AnalysisResult result) {
    Stream<String> names = Stream.of(result.getRepo().getName(), result.getRepo().getCommit());
    Stream<Object> requirementValues, unreferenced, requirements, total;

    if (result.getError() == null) {
      Map<RequirementConfig, Long> results = Requirement.getRequirementResult(result, configs);
      List<Long> requirementScores =
          configs.stream().map(r -> Math.min(results.get(r), r.getRequire())).toList();

      requirementValues = Arrays.stream(Requirement.values()).map(r -> r.getFound(result));
      unreferenced = Stream.of(result.getUnreferencedDeclarations());
      requirements = requirementScores.stream().map(Function.identity());
      total = Stream.of(requirementScores.stream().mapToLong(Long::longValue).sum());
    } else {
      requirementValues = Arrays.stream(Requirement.values()).map(r -> "N/A");
      unreferenced = Stream.of("N/A");
      requirements = configs.stream().map(c -> "N/A");
      total = Stream.of("N/A");
    }

    return Stream.of(names, requirementValues, unreferenced, requirements, total)
        .flatMap(Function.identity())
        .toList();
  }
}
