package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.sokar.agent.api.Agent;
import org.fuin.sokar.agent.api.AgentRegistry;
import org.fuin.sokar.agent.api.RunRequest;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Oh My Pi agent module.
 */
class OmpAgentTest {

    private final Agent agent = new OmpAgent();

    @Test
    void isDiscoveredThroughTheServiceLoader() {

        // Nothing names OmpAgent for this to work: it is found through META-INF/services, which
        // is what lets Sokar stay ignorant of it.
        final AgentRegistry registry = AgentRegistry.discover();

        assertThat(registry.names()).contains("omp");
        assertThat(registry.require("omp").definition().binary()).isEqualTo("omp");
    }

    @Test
    void readsItsOwnDefinition() {

        assertThat(agent.definition().label()).isEqualTo("Oh My Pi");
        assertThat(agent.definition().gitIdentity().email()).isEqualTo("noreply@omp.sh");
    }

    @Test
    void isNotTheOtherPi() {

        // The whole reason this repository exists. sokar-pi adapts earendil-works/pi, invoked as
        // 'pi'; this adapts can1357/oh-my-pi, invoked as 'omp'. Nothing about either may drift
        // into the other's names.
        assertThat(agent.definition().name()).isEqualTo("omp");
        assertThat(agent.definition().binary()).isEqualTo("omp");
        assertThat(agent.definition().artifacts()).allSatisfy(artifact ->
                assertThat(artifact.url()).contains("can1357/oh-my-pi"));
    }

    @Test
    void needsNoDomainOfItsOwn() {

        // Measured on three runs against a stub resolver: everything it asks for beyond the
        // provider is refused and the run still completes, so nothing belongs in the allowance.
        assertThat(agent.definition().allowedDomains()).isEmpty();
        assertThat(agent.definition().refusedDomains()).contains("catalog.stencil.so");
    }

    @Test
    void declaresHowItMustBePointedAtAProvider() {

        assertThat(agent.definition().provider()).isNotNull();
        // 'native': it speaks whichever dialect its provider does, rather than one fixed format.
        assertThat(agent.definition().provider().dialect()).isEqualTo("native");
        assertThat(agent.definition().provider().defaultProvider()).isEqualTo("openrouter");
        // A URL, because a base URL in a file is the only way omp can be redirected at all.
        assertThat(agent.definition().provider().endpoint().name()).isEqualTo("URL");
    }

    @Test
    void buildsAHeadlessCommandWithThePromptLast() {

        // The prompt is positional, so it has to come after the flags rather than behind one.
        assertThat(agent.headlessCommand(
                new RunRequest("fix the bug", "z-ai/glm-4.6", null, null, false, true)))
                .containsExactly("omp", "--auto-approve", "--model", "z-ai/glm-4.6", "--print", "--mode", "json",
                        "fix the bug");
    }

    @Test
    void resumesWithTheFlagOmpItselfUses() {

        assertThat(agent.headlessCommand(
                new RunRequest("carry on", null, null, "01a07a41", false, false)))
                .containsExactly("omp", "--auto-approve", "--session", "01a07a41", "carry on");
    }

    @Test
    void installsAPinnedAndVerifiedBinary() {

        // Not 'curl | bash'. Upstream publishes SHA256SUMS.txt beside each release, so the digest
        // here is the publisher's rather than one computed from whatever was served today.
        final var definition = agent.definition();

        // Not the version itself: a literal here made every automated bump fail.
        assertThat(definition.version()).matches("[0-9]+\\.[0-9]+\\.[0-9]+");
        assertThat(definition.artifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.url()).contains("/releases/download/v" + definition.version() + "/");
            assertThat(artifact.url()).endsWith("omp-linux-x64");
            assertThat(artifact.sha256()).matches("[a-f0-9]{64}");
            assertThat(artifact.unverified()).isFalse();
            assertThat(artifact.target()).isEqualTo("/home/agent/.local/bin/omp");
        });
        assertThat(definition.unverifiedArtifacts()).isEmpty();
    }

    @Test
    void contributesAnImageLayerThatChecksBeforeItInstalls() {

        final List<String> lines = agent.imageLayer().asAgent();

        assertThat(agent.imageLayer().isEmpty()).isFalse();

        final int check = indexOf(lines, "sha256sum -c -");
        final int install = indexOf(lines, "install -D");
        assertThat(check).isNotNegative();
        assertThat(install).isGreaterThan(check);
    }

    private static int indexOf(List<String> lines, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(text)) {
                return i;
            }
        }
        return -1;
    }
}
