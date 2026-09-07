package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.SetupContext;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OmpContainerSetup}.
 */
class OmpContainerSetupTest {

    private static final String TOKEN = "sokar_pt_example";

    private List<ContainerFile> files(String endpoint) {
        return new OmpContainerSetup()
                .files(new SetupContext(TOKEN, "api-key", "/workspace", endpoint, "openrouter"));
    }

    @Test
    void writesTheModelsFileOmpReadsAtStartUp() {

        // ~/.omp/agent, not ~/.pi/agent: the fork moved its configuration directory, and the two
        // are installed side by side.
        assertThat(files("http://127.0.0.1:9419/api/v1")).singleElement()
                .extracting(ContainerFile::path)
                .isEqualTo("/home/agent/.omp/agent/models.yml");
    }

    @Test
    void writesTheEndpointAndProviderItWasGiven() {

        // Neither is this agent's to decide. The dialect's path is already on the endpoint -
        // OpenRouter serves the OpenAI dialect under /api/v1, and omp appends /responses to it -
        // and the provider's name arrives with the task.
        assertThat(files("http://127.0.0.1:9419/api/v1").getFirst().content())
                .contains("\"http://127.0.0.1:9419/api/v1\"")
                .contains("\"openrouter\":");
    }

    @Test
    void carriesTheTaskTokenRatherThanACredential() {

        assertThat(files("http://127.0.0.1:9419").getFirst().content()).contains(TOKEN);
        assertThat(files("http://127.0.0.1:9419").getFirst().ownerOnly())
                .as("it holds a token, so it is not world readable").isTrue();
    }

    @Test
    void writesNothingWhenNothingWasBrokered() {

        // An agent pointed at nothing would otherwise get a models file naming an endpoint that
        // does not exist, which fails later and further away.
        assertThat(files("")).isEmpty();
    }

    @Test
    void survivesATokenWithCharactersThatWouldBreakTheFile() {

        final List<ContainerFile> files = new OmpContainerSetup().files(new SetupContext(
                "tok\"en\\with\nquotes", "api-key", "/workspace", "http://127.0.0.1:9419",
                "openrouter"));

        assertThat(files.getFirst().content())
                .as("written as a JSON literal, which YAML reads as a double-quoted scalar")
                .contains("\"tok\\\"en\\\\with\\nquotes\"");
    }
}
