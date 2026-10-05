package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowRulesTest {

    private static Path workflows() {
        return RepositoryDocuments.root().resolve(".github/workflows");
    }

    @Test
    void noWorkflowBuildsPackagesWithoutTheirTests() throws IOException {

        // -DskipTests also skips NativeLinkageCheck, the one test that needs the binary.
        final List<String> skipping = new ArrayList<>();
        try (Stream<Path> files = Files.list(workflows())) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                final StringBuilder joined = new StringBuilder();
                for (final String line : Files.readAllLines(file)) {
                    if (line.endsWith("\\")) {
                        joined.append(line, 0, line.length() - 1).append(' ');
                        continue;
                    }
                    joined.append(line);
                    final String command = joined.toString();
                    if (command.contains("-Pnative,dist") && command.contains("-DskipTests")) {
                        skipping.add(file.getFileName() + ": " + command.strip());
                    }
                    joined.setLength(0);
                }
            }
        }
        assertThat(skipping).as("package builds that skip their tests").isEmpty();
    }

    @Test
    void anAcceptanceLegRefusesToRunWithoutTheKey() throws IOException {

        // Without the key the credential scenarios are skipped, and a green leg proves less.
        final String build = Files.readString(workflows().resolve("build.yml"));
        final String update = Files.readString(workflows().resolve("update.yml"));
        assertThat(build).contains("-n \"${SOKAR_E2E_OPENROUTER_API_KEY:-}\"");
        assertThat(update).contains("-n \"$KEY\"").contains("secrets.OPEN_ROUTER_API_KEY");
    }

    @Test
    void aReleaseIsRefusedAgainstASokarSnapshot() throws IOException {

        // Both jobs that pick the channel, the build job and the release job.
        final String build = Files.readString(workflows().resolve("build.yml"));
        final String refusal = "is a snapshot; a release is built against a released Sokar";
        assertThat(build.split(Pattern.quote(refusal), -1)).as("refusals").hasSize(3);
    }

    @Test
    void noFoldedPackageBuildSkipsItsTests() throws IOException {

        // A command folded onto more-indented lines, with or without a backslash, is still one command.
        final List<String> untested = new ArrayList<>();
        try (Stream<Path> files = Files.list(RepositoryDocuments.root().resolve(".github/workflows"))) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                final List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).contains("-Pnative,dist")) {
                        continue;
                    }
                    final int indent = indentOf(lines.get(i));
                    for (int j = i; j < lines.size() && (j == i || !lines.get(j).isBlank() && indentOf(lines.get(j)) > indent); j++) {
                        if (lines.get(j).contains("-DskipTests")) {
                            untested.add(file.getFileName() + ":" + (j + 1));
                        }
                    }
                }
            }
        }
        assertThat(untested).as("package builds that skip their tests on a folded line").isEmpty();
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
