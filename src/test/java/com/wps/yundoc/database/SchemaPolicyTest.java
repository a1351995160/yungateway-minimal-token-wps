package com.wps.yundoc.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaPolicyTest {

    private static final Pattern INDEX_DEFINITION = Pattern.compile(
            "(?im)^\\s*(?:unique\\s+)?(?:key|index)\\s+[`\\w]+\\s*\\(([^)]*)\\)");

    @Test
    void schemaDoesNotUseAutoIncrementOrLargeFields() throws IOException {
        List<Path> schemaFiles = schemaFiles();

        assertThat(schemaFiles).isNotEmpty();
        for (Path schemaFile : schemaFiles) {
            String sql = readSql(schemaFile);
            assertThat(sql).doesNotContain("auto_increment");
            assertThat(sql).doesNotContain(" text");
            assertThat(sql).doesNotContain(" blob");
            assertThat(sql).doesNotContain(" mediumtext");
            assertThat(sql).doesNotContain(" mediumblob");
            assertThat(sql).doesNotContain(" json");
            assertThat(sql).doesNotContain("auth_mode");
            assertThat(sql).doesNotContain("access_token");
            assertThat(sql).doesNotContain("refresh_token");
        }
    }

    @Test
    void schemaIndexesUseAtMostFiveColumns() throws IOException {
        List<Path> schemaFiles = schemaFiles();

        for (Path schemaFile : schemaFiles) {
            Matcher matcher = INDEX_DEFINITION.matcher(readSql(schemaFile));
            while (matcher.find()) {
                int columns = matcher.group(1).split(",").length;
                assertThat(columns)
                        .as("index column count in %s", schemaFile)
                        .isLessThanOrEqualTo(5);
            }
        }
    }

    @Test
    void bizSystemSchemaContainsOnlyMvpTables() throws IOException {
        String sql = schemaFiles().stream()
                .map(SchemaPolicyTest::readSqlUnchecked)
                .collect(Collectors.joining("\n"));

        assertThat(sql).contains(
                "create table if not exists biz_system",
                "create table if not exists biz_system_api_permission",
                "primary key (business_system_id)",
                "unique key uk_biz_system_client (client_id)",
                "primary key (business_system_id, api_code)");
    }

    private static List<Path> schemaFiles() throws IOException {
        Path schemaDir = Paths.get("src", "main", "resources", "db");
        if (!Files.exists(schemaDir)) {
            return java.util.Collections.emptyList();
        }
        try (java.util.stream.Stream<Path> paths = Files.list(schemaDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().startsWith("schema"))
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .collect(Collectors.toList());
        }
    }

    private static String readSql(Path schemaFile) throws IOException {
        return new String(Files.readAllBytes(schemaFile), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    }

    private static String readSqlUnchecked(Path schemaFile) {
        try {
            return readSql(schemaFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read schema " + schemaFile, ex);
        }
    }
}
