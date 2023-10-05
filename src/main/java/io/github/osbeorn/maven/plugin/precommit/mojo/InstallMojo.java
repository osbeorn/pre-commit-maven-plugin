package io.github.osbeorn.maven.plugin.precommit.mojo;

import io.github.osbeorn.maven.plugin.precommit.lib.BinaryInstaller;
import io.github.osbeorn.maven.plugin.precommit.lib.InstallationException;
import io.github.osbeorn.maven.plugin.precommit.lib.PluginFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which downloads and activates pre-commit goals
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE)
public class InstallMojo extends AbstractPrecommitMojo {

    /**
     * Where to download binary from. Defaults to https://github.com/pre-commit/pre-commit/releases/download/...
     */
    @Parameter(property = "downloadRoot", defaultValue = BinaryInstaller.DEFAULT_DOWNLOAD_ROOT)
    private String downloadRoot;

    /**
     * The precommitVersion of the pre-commit binary to install. IMPORTANT! Most precommitVersion names start with 'v', for example
     * 'v1.10.1'
     */
    @Parameter(property = "precommitVersion", required = true)
    private String precommitVersion;

    /**
     * The hook types to install. Defaults to pre-commit only. See https://pre-commit.com/#supported-git-hooks for
     * possible options.
     */
    @Parameter(property = "hookTypes", defaultValue = "pre-commit")
    private String[] hookTypes;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.install", alias = "skip.install", defaultValue = "${skip.install}")
    private boolean skip;

    @Override
    public void execute(PluginFactory pluginFactory) throws MojoExecutionException {
        try {
            pluginFactory.getBinaryInstaller()
                    .setDownloadRoot(downloadRoot)
                    .setVersion(precommitVersion)
                    .setHookTypes(hookTypes)
                    .install();
        } catch (InstallationException e) {
            throw new MojoExecutionException("Failed to install pre-commit", e);
        }
    }

    @Override
    protected boolean skipExecution() {
        return skip;
    }
}
