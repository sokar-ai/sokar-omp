package org.fuin.sokar.agent.impl.omp;

import org.fuin.sokar.wire.Json;

/**
 * The models file that points Oh My Pi at Sokar's broker.
 * <p>
 * omp cannot be redirected with an environment variable, and its extension API cannot do it
 * either: an extension calling {@code registerProvider(name, {baseUrl})} for a built-in provider
 * loads and runs, and the requests still go to the provider's own host. Measured against 18.1.13.
 * What does redirect it is {@code providers.<name>.baseUrl} in the models file, which is
 * data rather than code and is read before the first request.
 * <p>
 * Neither the provider's name nor the dialect's path is written here: the name arrives with the
 * task, and the path is already on the endpoint Sokar hands over.
 */
final class OmpModelsFile {

    /**
     * Where omp reads it. {@code ~/.omp/agent} is the default agent directory; {@code PI_PROFILE}
     * and {@code PI_CONFIG_DIR} move it, and Sokar sets neither.
     */
    static final String PATH = "/home/agent/.omp/agent/models.yml";

    private OmpModelsFile() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the file that sends omp's requests to the broker instead of the provider.
     *
     * @param provider Name of the provider, as omp already knows it.
     * @param endpoint Base URL Sokar is listening on, inside this container's namespace, with the
     *        dialect's path already on it.
     * @param token Task-scoped token to present, standing in for the real credential.
     * @return File content.
     */
    static String document(String provider, String endpoint, String token) {
        // JSON string literals rather than bare YAML scalars: a token is opaque, and JSON's
        // escapes are a subset of YAML's double-quoted style, so this cannot end the string early.
        return """
                # Written by Sokar for one task. The key here is a task-scoped token, not a
                # credential: it is only accepted by the broker this baseUrl points at.
                providers:
                  %s:
                    baseUrl: %s
                    apiKey: %s
                """.formatted(Json.write(provider), Json.write(endpoint), Json.write(token));
    }
}
