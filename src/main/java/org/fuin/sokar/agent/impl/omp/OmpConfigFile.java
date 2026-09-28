package org.fuin.sokar.agent.impl.omp;

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
     * @return File content.
     */
    static String document() {
        return """
                startup:
                  checkUpdate: false
                setupVersion: 2
                """;
    }
}
