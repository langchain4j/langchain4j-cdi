package dev.langchain4j.cdi.mcp.integrationtests;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;

public class ArquillianDeploymentHelper {

    private ArquillianDeploymentHelper() {}

    /**
     * Finds the first JAR in {@code folder} whose name starts with {@code prefix}. Throws {@link IllegalStateException}
     * if no match is found — run {@code mvn install} on the parent modules first.
     */
    public static File findBuildFile(String folder, String prefix) throws IOException {
        return Files.find(
                        new File(folder).toPath(),
                        1,
                        (BiPredicate<Path, BasicFileAttributes>) (t, u) -> {
                            String fileName = t.getFileName().toString();
                            return fileName.startsWith(prefix) && fileName.endsWith(".jar");
                        },
                        FileVisitOption.FOLLOW_LINKS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No JAR with prefix '" + prefix + "' found in '" + folder
                        + "'. " + "Run 'mvn install -DskipTests' from the langchain4j-cdi-mcp directory first."))
                .toFile();
    }
}
