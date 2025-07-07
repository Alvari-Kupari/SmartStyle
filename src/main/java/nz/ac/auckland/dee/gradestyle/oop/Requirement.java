package nz.ac.auckland.dee.gradestyle.oop;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import nz.ac.auckland.dee.gradestyle.config.RequirementConfig;

public enum Requirement {
  TotalClasses,
  Classes,
  AbstractClasses,
  TopLevelClasses,
  LocalSubclasses,
  ExternalSubclasses,
  InnerClasses,
  NestedClasses,
  TotalInterfaces,
  TopLevelInterfaces,
  LocalSubinterfaces,
  ExternalSubinterfaces;

  public static Map<RequirementConfig, Long> getRequirementResult(
      AnalysisResult result, List<RequirementConfig> configs) {
    Function<RequirementConfig, Long> collector =
        requirement -> requirement.getRequirement().getFound(result);
    return configs.stream().collect(Collectors.toMap(Function.identity(), collector));
  }

  public long getFound(AnalysisResult result) {
    switch (this) {
      case TotalClasses:
        return result.getTotalClasses();
      case Classes:
        return result.getClasses();
      case AbstractClasses:
        return result.getAbstractClasses();
      case TopLevelClasses:
        return result.getTopLevelClasses();
      case LocalSubclasses:
        return result.getLocalSubclasses();
      case ExternalSubclasses:
        return result.getExternalSubclasses();
      case InnerClasses:
        return result.getInnerClasses();
      case NestedClasses:
        return result.getNestedClasses();
      case TotalInterfaces:
        return result.getTotalInterfaces();
      case TopLevelInterfaces:
        return result.getTopLevelInterfaces();
      case LocalSubinterfaces:
        return result.getLocalSubinterfaces();
      case ExternalSubinterfaces:
        return result.getExternalSubinterfaces();
      default:
        throw new IllegalArgumentException("Unknown requirement: " + this);
    }
  }

  @Override
  public String toString() {
    return name().replaceAll("(.)([A-Z])", "$1 $2");
  }
}
