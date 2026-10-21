package io.github.osbeorn.maven.plugin.precommit.mojo;

import io.github.osbeorn.maven.plugin.precommit.lib.PluginFactory;
import io.github.osbeorn.maven.plugin.precommit.lib.PrecommitProjectRoot;
import io.github.osbeorn.maven.plugin.precommit.lib.RepositoryCacheResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;

public abstract class AbstractPrecommitMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * When {@code true}, the plugin runs only on the Maven execution root module
     * (the top-level project in a reactor build).
     */
    @Parameter(property = "precommit.runOnlyAtExecutionRoot", defaultValue = "true")
    private boolean runOnlyAtExecutionRoot;

    /**
     * When {@code true}, skip quietly if no {@code .git} directory and
     * {@code .pre-commit-config.yaml} can be found when walking up from the working directory.
     */
    @Parameter(property = "precommit.skipIfMissingGitOrConfig", defaultValue = "true")
    private boolean skipIfMissingGitOrConfig;

    /**
     * The base directory for running all commands.
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory")
    protected File workingDirectory;

    /**
     * The base directory for installing the binary into
     */
    @Parameter(defaultValue = "${basedir}/.pre-commit-files", property = "installDirectory")
    protected File installDirectory;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    /**
     * Implemented by children to define an execution
     */
    protected abstract void execute(PluginFactory pluginFactory) throws MojoExecutionException;

    /**
     * Implemented by children to determine if this execution should be skipped.
     */
    protected abstract boolean skipExecution();

    @Override
    public void execute() throws MojoExecutionException {
        if (skipExecution()) {
            getLog().info("Skipping execution.");
            return;
        }

        if (runOnlyAtExecutionRoot && !project.isExecutionRoot()) {
            getLog().debug(
                    "Skipping pre-commit on non-execution-root module " + project.getArtifactId() + "."
            );
            return;
        }

        if (skipIfMissingGitOrConfig && !PrecommitProjectRoot.find(workingDirectory).isPresent()) {
            getLog().info(
                    "Skipping pre-commit install: could not find a Git root with "
                            + PrecommitProjectRoot.CONFIG_FILE_NAME
                            + " when walking up from "
                            + workingDirectory
            );
            return;
        }

        if (installDirectory == null) {
            installDirectory = workingDirectory;
        }

        execute(
                new PluginFactory(
                        workingDirectory,
                        installDirectory,
                        new RepositoryCacheResolver(repositorySystemSession),
                        new MavenPluginLog(getLog())
                )
        );
    }
}
