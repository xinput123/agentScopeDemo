package com.example.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectInfoTools {

    private static final int DEFAULT_POM_LIMIT = 4_000;

    private final Path projectRoot;

    public ProjectInfoTools(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Tool(
            name = "read_pom",
            description = "Read the current Maven project's pom.xml. Use it when the user asks about dependencies, Java version, Spring Boot version, or project coordinates.",
            readOnly = true,
            concurrencySafe = true)
    public String readPom(
            @ToolParam(
                    name = "max_chars",
                    description = "Maximum characters to return. Default is 4000.",
                    required = false)
            Integer maxChars) {
        Path pom = projectRoot.resolve("pom.xml").normalize();
        if (!pom.startsWith(projectRoot)) {
            return "Cannot read pom.xml outside the configured project root.";
        }
        if (!Files.isRegularFile(pom)) {
            return "pom.xml not found under the configured project root.";
        }
        try {
            String content = Files.readString(pom, StandardCharsets.UTF_8);
            int limit = maxChars == null || maxChars <= 0 ? DEFAULT_POM_LIMIT : maxChars;
            if (content.length() <= limit) {
                return content;
            }
            return content.substring(0, limit) + "\n... pom.xml truncated ...";
        } catch (IOException ex) {
            return "Failed to read pom.xml: " + ex.getMessage();
        }
    }

    @Tool(
            name = "list_source_folders",
            description = "List existing source folders in the current project. Use it before answering questions about project layout.",
            readOnly = true,
            concurrencySafe = true)
    public String listSourceFolders() {
        Path src = projectRoot.resolve("src").normalize();
        if (!Files.isDirectory(src)) {
            return "src directory not found under the configured project root.";
        }
        try (Stream<Path> paths = Files.walk(src, 3)) {
            String result = paths.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(projectRoot::relativize)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));
            return result.isBlank() ? "No source folders found." : result;
        } catch (IOException ex) {
            return "Failed to list source folders: " + ex.getMessage();
        }
    }

    @Tool(
            name = "find_main_class",
            description = "Find Java classes annotated with @SpringBootApplication in the current project.",
            readOnly = true,
            concurrencySafe = true)
    public String findMainClass() {
        Path javaRoot = projectRoot.resolve("src/main/java").normalize();
        if (!Files.isDirectory(javaRoot)) {
            return "src/main/java directory not found under the configured project root.";
        }
        try (Stream<Path> paths = Files.walk(javaRoot)) {
            String result = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsSpringBootApplication)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(projectRoot::relativize)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));
            return result.isBlank() ? "No @SpringBootApplication class found." : result;
        } catch (IOException ex) {
            return "Failed to find main class: " + ex.getMessage();
        }
    }

    private boolean containsSpringBootApplication(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8)
                    .contains("@SpringBootApplication");
        } catch (IOException ex) {
            return false;
        }
    }
}
