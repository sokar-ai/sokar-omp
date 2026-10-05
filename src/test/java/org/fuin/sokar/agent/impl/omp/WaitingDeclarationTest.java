package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.sokar.agent.api.Waiting;
import org.junit.jupiter.api.Test;

/**
 * The waiting declaration read against the screens it was written from.
 * <p>
 * Each screen is what the attached agent drew on 18.4.1, captured at a terminal - so a
 * change to the declaration that stops fitting what was measured fails here. Whether the agent still
 * draws these at the version it pins is the acceptance suite's question, not this one's.
 */
class WaitingDeclarationTest {

    /** Asked to put a question, it opened its ask box; above it, the line it works under. */
    private static final String ASKING = """
              ⎋ Asking color preference                                  Ask Preference Between Red or Blue
            ╭─ Ask ──────────────────────────────────────────────────────────────────────╮
            │ Which color do you prefer?                                                 │
            ├────────────────────────────────────────────────────────────────────────────┤
            │ ❯ ○ Red                                                                    │
            │   ○ Blue                                                                   │
            │   ○ Other (type your own)                                                  │
            │                                                                            │
            ├────────────────────────────────────────────────────────────────────────────┤
            │ ⏎ select · n note · ↑/↓ move · ⎋ cancel                                    │
            ╰────────────────────────────────────────────────────────────────────────────╯
            """;

    /** At its prompt, having asked nothing - what the ready marker waits for. */
    private static final String AT_REST = """
            ╰──────────────────────────┴─────────────────────────────────────────────────╯
             Tip: `/force read` pins the next turn to one specific tool when the model keeps reaching for the
                  wrong one
             π > ◒ GPT-5.5 > 📁 /workspace > ⑂ master ▶─0.6%──────────────────────────────┃────1.1M─
            ╰─
            """;

    /** At work: its working line starts with the same escape symbol the ask box cancels with. */
    private static final String WORKING = """
             π > ◒ GPT-5.5 > 📁 /workspace > ⑂ master ▶─0.6%──────────────────────────────┃────1.1M─
              ⎋ Asking color preference                                  Ask Preference Between Red or Blue
            """;

    private Waiting waiting() {
        final Waiting waiting = new OmpAgent().definition().waiting();
        assertThat(waiting).as("omp.yaml declares what waiting looks like").isNotNull();
        return waiting;
    }

    @Test
    void readsItsAskBoxAsWaitingForAnAnswer() {

        final Waiting.Reading reading = waiting().read(ASKING);

        assertThat(reading.seen()).isEqualTo(Waiting.Seen.WAITING);
        assertThat(reading.waitingFor()).isEqualTo("an answer to its question");
    }

    @Test
    void readsItsPromptAsNotWaiting() {

        // The other half: a rule that matched everything would pass the test above.
        assertThat(waiting().read(AT_REST).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    @Test
    void readsItsWorkingLineAsNotWaiting() {

        // Why the rule is not the escape symbol alone.
        assertThat(waiting().read(WORKING).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }
}
