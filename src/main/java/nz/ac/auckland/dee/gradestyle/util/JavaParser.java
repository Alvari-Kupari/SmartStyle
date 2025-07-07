package nz.ac.auckland.dee.gradestyle.util;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.nio.file.Path;
import nz.ac.auckland.dee.gradestyle.Repo;

public class JavaParser {
  public static com.github.javaparser.JavaParser get(Path repoDir) {

    CombinedTypeSolver typeSolver = new CombinedTypeSolver();

    typeSolver.add(new ReflectionTypeSolver());

    //FileUtils.getSrcDirs(repoDir).forEach(dir -> typeSolver.add(new JavaParserTypeSolver(dir)));
    typeSolver.add(new JavaParserTypeSolver(repoDir));

    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);


    ParserConfiguration config =
        new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setSymbolResolver(symbolSolver);

    return new com.github.javaparser.JavaParser(config);
  }

  public static com.github.javaparser.JavaParser get(Repo repo) {
    return get(repo.getDir());
  }
}
