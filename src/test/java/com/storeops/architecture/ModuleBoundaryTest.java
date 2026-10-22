package com.storeops.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the module boundary rules by scanning source imports:
 * no circular imports, staff is read-only for other modules, and services/routes never throw raw exceptions.
 */
class ModuleBoundaryTest {

    private static final Path ROOT = Path.of("src/main/java/com/storeops");
    private static final Set<String> FEATURE_MODULES = Set.of("activities", "programmes", "staff", "alerts", "reports");
    private static final Pattern IMPORT =
            Pattern.compile("^import\\s+(?:static\\s+)?com\\.storeops\\.([a-z]+)\\.([a-z]+)", Pattern.MULTILINE);
    private static final Pattern RAW_THROW = Pattern.compile(
            "throw\\s+new\\s+(RuntimeException|Exception|Error|Throwable|IllegalArgumentException|IllegalStateException)\\b");

    private List<Path> javaFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(ROOT)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private String moduleOf(Path file) {
        Path relative = ROOT.relativize(file);
        return relative.getNameCount() > 1 ? relative.getName(0).toString() : "";
    }

    @Test
    void modulesOnlyDependOnCommonOrTheReadOnlyStaffApi() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String module = moduleOf(file);
            Matcher matcher = IMPORT.matcher(Files.readString(file));
            while (matcher.find()) {
                String target = matcher.group(1);
                String subPackage = matcher.group(2);
                boolean sameModule = target.equals(module);
                boolean common = target.equals("common");
                boolean staffApi = target.equals("staff") && subPackage.equals("api");
                boolean allowed = sameModule || (FEATURE_MODULES.contains(module) && (common || staffApi));
                if (!allowed) {
                    violations.add(file + " -> " + matcher.group());
                }
            }
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void servicesAndRoutesNeverThrowRawExceptions() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String path = file.toString();
            if (path.contains("/service/") || path.contains("/routes/")) {
                if (RAW_THROW.matcher(Files.readString(file)).find()) {
                    violations.add(path);
                }
            }
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void everyFeatureModuleHasRoutesServiceAndRepositoryLayers() {
        for (String module : FEATURE_MODULES) {
            for (String layer : List.of("routes", "service", "repository")) {
                assertTrue(Files.isDirectory(ROOT.resolve(module).resolve(layer)),
                        module + " is missing layer " + layer);
            }
        }
    }
}
