package org.fuin.sokar.agent.impl.omp;

import org.fuin.sokar.wire.Json;

/**
 * Oh My Pi's global settings in a container: only what differs from its defaults.
 * <p>
 * <strong>No update check at start.</strong> {@code startup.checkUpdate} is on by default and asks
 * the npm registry or GitHub for a newer release, to show a notice - it installs nothing. In a task
 * neither host is reachable, so the check could only fail, and the version that runs is the pinned
 * one either way. {@code omp update} has no switch; what keeps it from replacing the binary is that
 * the task cannot reach where it downloads from. Measured against 18.1.13 with
 * {@code omp config get startup.checkUpdate}.
 * <p>
 * <strong>No setup wizard.</strong> A fresh omp opens at a five-step wizard - providers, default
 * model, glyphs, layout, theme - even with a credential in place, and a task started attended then
 * waits at a menu. Skipping every step writes {@code setupVersion: 2} to this file and nothing else
 * that matters, and with that line omp starts at its prompt. Measured against 18.1.13 at a terminal,
 * in a fresh task, with and without the line.
 * <p>
 * <strong>The task's provider first.</strong> Several providers offer the same model id - {@code
 * gpt-4.1} is both {@code openai}'s and {@code github-copilot}'s - and omp then picks by its own
 * order, not by which one the task has a credential for, so {@code --model gpt-4.1} under
 * {@code github-copilot} went to {@code openai} and failed for want of a key. {@code
 * modelProviderOrder} ranks a bare model id's providers, after the models a person has used, which
 * a fresh task has none of. Read in 18.4.1's model resolver.
 * <p>
 * Kept apart from {@link OmpModelsFile}, which holds a token and is written only when a task is
 * brokered: this file is plain configuration and belongs in every container.
 */
final class OmpConfigFile {

    /** Where omp reads its global settings, beside the models file in the agent directory. */
    static final String PATH = "/home/agent/.omp/agent/config.yml";

    private OmpConfigFile() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the settings document for a container.
     *
     * @param provider The task's provider, as omp knows it; blank when the task names none.
     * @return File content.
     */
    static String document(final String provider) {
        final String settings = """
                startup:
                  checkUpdate: false
                setupVersion: 2
                """;
        if (provider.isBlank()) {
            return settings;
        }
        return settings + """
                modelProviderOrder:
                  - %s
                """.formatted(Json.write(provider));
    }
}
