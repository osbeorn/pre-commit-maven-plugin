package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;
import java.util.Optional;

public final class PrecommitProjectRoot {

    public static final String CONFIG_FILE_NAME = ".pre-commit-config.yaml";

    private PrecommitProjectRoot() {
    }

    /**
     * Walks up from {@code startDirectory} until a directory contains both
     * {@value #CONFIG_FILE_NAME} and a {@code .git} entry (file or directory).
     */
    public static Optional<File> find(File startDirectory) {
        if (startDirectory == null) {
            return Optional.empty();
        }

        File current = startDirectory.getAbsoluteFile();
        while (current != null) {
            if (containsPrecommitConfig(current) && containsGitMetadata(current)) {
                return Optional.of(current);
            }
            current = current.getParentFile();
        }

        return Optional.empty();
    }

    public static File resolve(File startDirectory) throws InstallationException {
        return find(startDirectory).orElseThrow(() -> new InstallationException(
                "Could not find a Git repository root with " + CONFIG_FILE_NAME
                        + " starting from " + (startDirectory == null ? "null" : startDirectory.getAbsolutePath())
        ));
    }

    static boolean containsPrecommitConfig(File directory) {
        return new File(directory, CONFIG_FILE_NAME).isFile();
    }

    static boolean containsGitMetadata(File directory) {
        return new File(directory, ".git").exists();
    }
}
