package com.wps.yundoc.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveLoggingPolicyTest {

    private static final String[] LOG_SINKS = {
            "log.",
            "logger.",
            "System.out",
            "System.err",
            "printStackTrace"
    };

    private static final String[] SENSITIVE_TOKENS = {
            "authorization",
            "clientsecret",
            "appsecret",
            "accesstoken",
            "refreshtoken",
            "wps token",
            "signature"
    };

    @Test
    void productionCodeDoesNotLogSensitiveMaterials() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get("src/main/java"))) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertThat(violations).isEmpty();
    }

    private void collectViolations(Path path, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                collectLineViolation(path, index + 1, lines.get(index), violations);
            }
        } catch (IOException ex) {
            throw new AssertionError("Could not read " + path, ex);
        }
    }

    private void collectLineViolation(Path path, int lineNumber, String line, List<String> violations) {
        String normalized = normalize(line);
        if (!containsAny(normalized, LOG_SINKS)) {
            return;
        }
        if (containsAny(normalized, SENSITIVE_TOKENS)) {
            violations.add(path + ":" + lineNumber + " " + line.trim());
        }
    }

    private boolean containsAny(String value, String[] candidates) {
        for (String candidate : candidates) {
            if (value.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value.replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
    }
}
