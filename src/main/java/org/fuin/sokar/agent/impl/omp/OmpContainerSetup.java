package org.fuin.sokar.agent.impl.omp;

import java.util.List;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.ContainerSetup;
import org.fuin.sokar.agent.api.SetupContext;

/**
 * What Oh My Pi needs in a fresh container before it will run unattended.
 * <p>
 * The models file carries both halves of the wiring: where to send requests, and what to present.
 * Neither can be an environment variable here - omp has no variable for an endpoint, and this
 * agent's broker starts after the container exists, so its environment is already fixed. The
 * settings file beside it goes into every container, brokered or not.
 */
public class OmpContainerSetup implements ContainerSetup {

    @Override
    public List<ContainerFile> files(SetupContext context) {

        final ContainerFile config = ContainerFile.of(OmpConfigFile.PATH, OmpConfigFile.document());
        if (!context.brokered() || !credentialed(context)) {
            // The models file carries both halves, so with either missing it names an endpoint or
            // a token that is not there, and omp fails looking like a wrong credential.
            return List.of(config);
        }
        return List.of(config, ContainerFile.secret(OmpModelsFile.PATH,
                OmpModelsFile.document(context.provider(), context.endpoint(), context.token())));
    }

    /**
     * Returns whether this task was given a token to present.
     *
     * @param context What the agent was told about the task.
     * @return {@code true} when there is a token to write.
     */
    private static boolean credentialed(final SetupContext context) {
        return context.token() != null && !context.token().isBlank();
    }
}
