package org.fuin.sokar.agent.impl.omp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.sokar.agent.api.SessionIds;
import org.fuin.sokar.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * Where Oh My Pi names its session, read against what 18.4.1 wrote.
 * <p>
 * That the id is still where this says at the version the package pins is the acceptance suite's
 * question; this one fails when the declaration stops fitting what was measured.
 */
class SessionIdDeclarationTest {

    private static final String ID = "01a0edd8-ec3c-7733-954e-330e6a288de4";

    /** The first record of an unattended run, as it wrote it. */
    private static final List<Object> RECORDS = List.of(
            Json.parse("{\"type\":\"session\",\"version\":3,\"id\":\"" + ID
                    + "\",\"timestamp\":\"2026-09-29T15:46:51.580Z\",\"cwd\":\"/workspace\"}"),
            Json.parse("{\"type\":\"agent_start\"}"));

    private SessionIds ids() {
        final SessionIds ids = new OmpAgent().definition().sessionIds();
        assertThat(ids).as("omp.yaml declares where its session id is").isNotNull();
        return ids;
    }

    @Test
    void readsTheSessionAnUnattendedRunOpened() {

        assertThat(ids().of(RECORDS)).isEqualTo(ID);
    }

    @Test
    void findsNoSessionWhereNoRecordNamesOne() {

        // The other half: a declaration that took any record's id would pass the test above.
        assertThat(ids().of(List.of(Json.parse("{\"type\":\"message_start\",\"id\":\"not-a-session\"}")))).isNull();
    }

    @Test
    void looksForAnAttendedSessionInItsSessionFiles() {

        // Measured: ~/.omp/agent/sessions/--workspace--/<timestamp>_<id>.jsonl, with a title record and then the
        // same {"type":"session"} this declares. Sokar reads the id from inside the newest file.
        assertThat(ids().directory()).isEqualTo(".omp/agent/sessions/--workspace--");
        assertThat(ids().suffix()).isEqualTo(".jsonl");
        assertThat(ids().of(List.of(Json.parse("{\"type\":\"title\",\"title\":\"Remember PAPAYA\"}"),
                RECORDS.getFirst()))).isEqualTo(ID);
    }
}
