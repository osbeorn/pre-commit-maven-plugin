package io.github.osbeorn.maven.plugin.precommit.mojo;

import io.github.osbeorn.maven.plugin.precommit.lib.PluginFactory;
import io.github.osbeorn.maven.plugin.precommit.lib.RepositoryCacheResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;

public abstract class AbstractPrecommitMojo extends AbstractMojo {

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
        if (!skipExecution()) {
            if (installDirectory == null) {
                installDirectory = workingDirectory;
            }

            execute(
                    new PluginFactory(
                            workingDirectory,
                            installDirectory,
                            new RepositoryCacheResolver(repositorySystemSession)
                    )
            );
        } else {
            getLog().info("Skipping execution.");
        }
    }
}
