package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OmpJsonFormatter}, with lines in the shapes of a real {@code --mode json} run, trimmed.
 */
class OmpJsonFormatterTest {

    private final OmpJsonFormatter formatter = new OmpJsonFormatter();

    @Test
    void showsNothingForTheBookkeeping() {

        assertThat(formatter.format("{\"type\":\"session\",\"version\":3,\"id\":\"a1b2\",\"cwd\":\"/workspace\"}")).isNull();
        assertThat(formatter.format("{\"type\":\"agent_start\"}")).isNull();
        assertThat(formatter.format("{\"type\":\"turn_start\"}")).isNull();
        assertThat(formatter.format("{\"type\":\"turn_end\",\"message\":{\"role\":\"assistant\",\"content\":[]},\"toolResults\":[]}"))
                .isNull();
        assertThat(formatter.format("{\"type\":\"advisor_cost_changed\",\"cost\":0.01}")).isNull();
    }

    @Test
    void showsNothingForThePartials() {

        // These repeat the message so far on every token; message_end and tool_execution_end say it whole.
        assertThat(formatter.format("{\"type\":\"message_start\",\"message\":{\"role\":\"assistant\",\"content\":[]}}"))
                .isNull();
        assertThat(formatter.format("{\"type\":\"message_update\",\"assistantMessageEvent\":{\"type\":\"text_delta\","
                + "\"delta\":\"I'll\"},\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"I'll\"}]}}"))
                .isNull();
        assertThat(formatter.format("{\"type\":\"tool_execution_update\",\"toolCallId\":\"t1\",\"toolName\":\"bash\","
                + "\"partialResult\":{\"content\":[{\"type\":\"text\",\"text\":\"src\"}]}}")).isNull();
    }

    @Test
    void showsTheAssistantsTextWithoutItsThinking() {

        assertThat(formatter.format("{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\",\"content\":["
                + "{\"type\":\"thinking\",\"thinking\":\"Let me look first.\"},"
                + "{\"type\":\"text\",\"text\":\"I'll read the contracts\\n\\nfirst.\"},"
                + "{\"type\":\"toolCall\",\"id\":\"t1\",\"name\":\"read\",\"arguments\":{\"path\":\"README.md\"}},"
                + "{\"type\":\"text\",\"text\":\"Then  the tests.\"}],\"stopReason\":\"toolUse\"}}"))
                .isEqualTo("I'll read the contracts first. Then the tests.");
    }

    @Test
    void cutsALongMessage() {

        final String text = "word ".repeat(100);
        final String shown = formatter.format("{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}],\"stopReason\":\"stop\"}}");

        assertThat(shown).hasSize(301).endsWith("…").startsWith("word word");
    }

    @Test
    void showsNothingForAMessageWithoutText() {

        assertThat(formatter.format("{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\",\"content\":["
                + "{\"type\":\"thinking\",\"thinking\":\"Hmm.\"},"
                + "{\"type\":\"toolCall\",\"id\":\"t1\",\"name\":\"bash\",\"arguments\":{}}],\"stopReason\":\"toolUse\"}}"))
                .isNull();
    }

    @Test
    void showsNothingForThePromptAndTheToolResults() {

        assertThat(formatter.format("{\"type\":\"message_end\",\"message\":{\"role\":\"user\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"Review the contracts.\"}]}}")).isNull();
        assertThat(formatter.format("{\"type\":\"message_end\",\"message\":{\"role\":\"toolResult\",\"toolCallId\":\"t1\","
                + "\"toolName\":\"bash\",\"content\":[{\"type\":\"text\",\"text\":\"ok\"}],\"isError\":false}}")).isNull();
    }

    @Test
    void showsTheProvidersRefusal() {

        assertThat(formatter.format("{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\",\"content\":[],"
                + "\"stopReason\":\"error\",\"errorStatus\":403,\"errorMessage\":\"403 Key limit exceeded\\n (total limit).\"}}"))
                .isEqualTo("-- error: 403 Key limit exceeded (total limit).");
    }

    @Test
    void namesTheToolCalledAndOnlyTheOneThatFailed() {

        assertThat(formatter.format("{\"type\":\"tool_execution_start\",\"toolCallId\":\"t1\",\"toolName\":\"bash\","
                + "\"args\":{\"command\":\"ls\"}}")).isEqualTo("[bash]");
        assertThat(formatter.format("{\"type\":\"tool_execution_end\",\"toolCallId\":\"t1\",\"toolName\":\"bash\","
                + "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"src\"}]},\"isError\":false}")).isNull();
        assertThat(formatter.format("{\"type\":\"tool_execution_end\",\"toolCallId\":\"t2\",\"toolName\":\"read\","
                + "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"ENOENT\"}]},\"isError\":true}"))
                .isEqualTo("[read failed]");
    }

    @Test
    void endsWithTheTurnsCounted() {

        assertThat(formatter.format("{\"type\":\"agent_end\",\"messages\":["
                + "{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"Review.\"}]},"
                + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"toolUse\"},"
                + "{\"role\":\"toolResult\",\"toolCallId\":\"t1\",\"toolName\":\"bash\",\"content\":[],\"isError\":false},"
                + "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Done.\"}],\"stopReason\":\"stop\"}]}"))
                .isEqualTo("-- done, 2 turns");
        assertThat(formatter.format("{\"type\":\"agent_end\",\"messages\":["
                + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"stop\"}]}"))
                .isEqualTo("-- done, 1 turn");
    }

    @Test
    void endsWithTheProvidersRefusal() {

        // The real run's end: the key's limit reached on the last call, after the earlier turns had answered.
        assertThat(formatter.format("{\"type\":\"agent_end\",\"messages\":["
                + "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Reading.\"}],\"stopReason\":\"toolUse\"},"
                + "{\"role\":\"toolResult\",\"toolCallId\":\"t1\",\"toolName\":\"read\",\"content\":[],\"isError\":false},"
                + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"error\",\"errorStatus\":403,"
                + "\"errorMessage\":\"403 Key limit exceeded (total limit). Manage it using the provider's settings.\"}]}"))
                .isEqualTo("-- ended with an error: 403 Key limit exceeded (total limit). Manage it using the provider's"
                        + " settings., 2 turns");
    }

    @Test
    void showsAnUnknownEventCut() {

        final String small = "{\"type\":\"something_new\",\"value\":1}";
        assertThat(formatter.format(small)).isEqualTo(small);

        final String huge = "{\"type\":\"something_new\",\"value\":\"" + "x".repeat(1000) + "\"}";
        assertThat(formatter.format(huge)).isEqualTo(huge.substring(0, 300) + "…");
    }

    @Test
    void showsALineThatIsNotJson() {

        assertThat(formatter.format("plain text from a crash")).isEqualTo("plain text from a crash");
        assertThat(formatter.format("{not valid json")).isEqualTo("{not valid json");
        assertThat(formatter.format("y".repeat(400))).isEqualTo("y".repeat(300) + "…");
    }

    @Test
    void marksAKnownEventItCannotRead() {

        final String noMessage = "{\"type\":\"message_end\"}";
        assertThat(formatter.format(noMessage)).isEqualTo("[unreadable] " + noMessage);
        final String noTool = "{\"type\":\"tool_execution_start\"}";
        assertThat(formatter.format(noTool)).isEqualTo("[unreadable] " + noTool);
        final String noMessages = "{\"type\":\"agent_end\"}";
        assertThat(formatter.format(noMessages)).isEqualTo("[unreadable] " + noMessages);
    }
}
