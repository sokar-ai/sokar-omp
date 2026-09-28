package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.fuin.sokar.agent.api.AgentException;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.SetupContext;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Tests for {@link OmpContainerSetup}.
 */
class OmpContainerSetupTest {

    private static final String TOKEN = "sokar_pt_example";

    private List<ContainerFile> files(String endpoint) {
        return new OmpContainerSetup()
                .files(new SetupContext(TOKEN, "api-key", "/workspace", endpoint, "openrouter"));
    }

    private static List<String> paths(List<ContainerFile> files) {
        return files.stream().map(ContainerFile::path).toList();
    }

    private static ContainerFile named(List<ContainerFile> files, String path) {
        return files.stream().filter(f -> f.path().equals(path)).findFirst().orElseThrow();
    }

    private ContainerFile models(String endpoint) {
        return named(files(endpoint), OmpModelsFile.PATH);
    }

    @Test
    void writesTheModelsFileOmpReadsAtStartUp() {

        // ~/.omp/agent, not ~/.pi/agent: the fork moved its configuration directory, and the two
        // are installed side by side.
        assertThat(paths(files("http://127.0.0.1:9419/api/v1")))
                .containsExactlyInAnyOrder("/home/agent/.omp/agent/models.yml",
                        "/home/agent/.omp/agent/config.yml");
    }

    @Test
    void writesTheEndpointAndProviderItWasGiven() {

        // Neither is this agent's to decide. The dialect's path is already on the endpoint -
        // OpenRouter serves the OpenAI dialect under /api/v1, and omp appends /responses to it -
        // and the provider's name arrives with the task.
        assertThat(models("http://127.0.0.1:9419/api/v1").content())
                .contains("\"http://127.0.0.1:9419/api/v1\"")
                .contains("\"openrouter\":");
    }

    @Test
    void carriesTheTaskTokenRatherThanACredential() {

        assertThat(models("http://127.0.0.1:9419").content()).contains(TOKEN);
        assertThat(models("http://127.0.0.1:9419").ownerOnly())
                .as("it holds a token, so it is not world readable").isTrue();
    }

    @Test
    void writesNoModelsFileWhenNothingWasBrokered() {

        // An agent pointed at nothing would otherwise get a models file naming an endpoint that
        // does not exist, which fails later and further away.
        assertThat(paths(files(""))).containsExactly(OmpConfigFile.PATH);
    }

    @Test
    void survivesATokenWithCharactersThatWouldBreakTheFile() {

        final List<ContainerFile> files = new OmpContainerSetup().files(new SetupContext(
                "tok\"en\\with\nquotes", "api-key", "/workspace", "http://127.0.0.1:9419",
                "openrouter"));

        // Read back as a YAML reader reads it: the claim is that the token arrives intact, not that
        // some escape appears in the text.
        final Map<?, ?> providers = (Map<?, ?>) new Yaml().<Map<?, ?>>load(named(files, OmpModelsFile.PATH).content())
                .get("providers");
        assertThat(((Map<?, ?>) providers.get("openrouter")).get("apiKey")).isEqualTo("tok\"en\\with\nquotes");
    }

    @Test
    void writesNoModelsFileWhenThereIsNoTokenToPresent() {

        // The endpoint alone is half a wiring: the models file would carry an empty token, which
        // omp rejects looking exactly like a wrong one.
        assertThat(paths(new OmpContainerSetup().files(new SetupContext(
                "  ", "api-key", "/workspace", "http://127.0.0.1:9419", "openrouter"))))
                .containsExactly(OmpConfigFile.PATH);
    }

    @Test
    void turnsTheStartupUpdateCheckOff() {

        // Measured on 18.1.13: with this file 'omp config get startup.checkUpdate' answers false,
        // without it true. The check only shows a notice, and in a task it cannot reach its hosts.
        assertThat(named(files("http://127.0.0.1:9419"), OmpConfigFile.PATH).content())
                .contains("startup:\n  checkUpdate: false\n");
    }

    @Test
    void marksTheSetupWizardDone() {

        // Measured on 18.1.13: a fresh task opens at a five-step setup wizard - providers, default
        // model, glyphs, layout, theme - even with a credential in place. Skipping it writes
        // setupVersion: 2 here and nothing else that matters; with that line the prompt comes first.
        assertThat(named(files(""), OmpConfigFile.PATH).content()).contains("setupVersion: 2\n");
    }

    @Test
    void writesTheSettingsWhetherOrNotTheTaskIsBrokered() {

        // Configuration, not a credential: it holds nothing worth hiding, and it applies to an
        // agent that was given no endpoint as much as to one that was.
        assertThat(named(files(""), OmpConfigFile.PATH).ownerOnly()).isFalse();
        assertThat(named(files("http://127.0.0.1:9419"), OmpConfigFile.PATH).ownerOnly()).isFalse();
    }

    @Test
    void refusesABrokeredTaskWithoutAProvider() {

        // Sokar never sends one: no provider chosen means no endpoint either. A file naming the
        // provider "" would override nothing and fail as a wrong credential, so a blank one is
        // Sokar's regression, and saying so is what would find it.
        assertThatThrownBy(() -> new OmpContainerSetup().files(new SetupContext(
                "sokar_pt_x", "api-key", "/workspace", "http://127.0.0.1:9419", " ")))
                .isInstanceOf(AgentException.class).hasMessageContaining("empty provider");
    }
}
