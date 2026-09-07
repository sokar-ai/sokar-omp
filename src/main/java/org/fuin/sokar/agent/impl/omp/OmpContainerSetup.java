package org.fuin.sokar.agent.impl.omp;

import java.util.List;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.ContainerSetup;
import org.fuin.sokar.agent.api.SetupContext;

/**
 * What Oh My Pi needs in a fresh container before it will run unattended.
 * <p>
 * One file, and it carries both halves of the wiring: where to send requests, and what to present.
 * Neither can be an environment variable here - omp has no variable for an endpoint, and this
 * agent's broker starts after the container exists, so its environment is already fixed.
 */
public class OmpContainerSetup implements ContainerSetup {

    @Override
    public List<ContainerFile> files(SetupContext context) {

        if (!context.brokered()) {
            // Nothing was brokered, so there is nothing to point anywhere. omp will report that it
            // has no credential for the provider, which is the honest outcome.
            return List.of();
        }
        return List.of(ContainerFile.secret(OmpModelsFile.PATH,
                OmpModelsFile.document(context.provider(), context.endpoint(), context.token())));
    }
}
