package org.fuin.sokar.agent.impl.omp;

import org.fuin.sokar.agent.api.AgentMain;
import org.fuin.sokar.agent.api.ContainerSetup;
import org.fuin.sokar.agent.api.YamlAgent;

/**
 * Oh My Pi.
 * <p>
 * One override: the endpoint and the token go into the models file omp reads at start-up, because
 * it has no variable for either. Everything else is {@code omp.yaml}.
 */
public final class OmpAgent extends YamlAgent {

    /**
     * Constructor.
     */
    public OmpAgent() {
        super("omp");
    }

    @Override
    public ContainerSetup containerSetup() {
        return new OmpContainerSetup();
    }

    /**
     * Entry point of the {@code sokar-agent-omp} binary.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        AgentMain.run(new OmpAgent(), args);
    }
}
