package io.github.osbeorn.maven.plugin.precommit.mojo;

import io.github.osbeorn.maven.plugin.precommit.lib.BinaryInstaller;
import io.github.osbeorn.maven.plugin.precommit.lib.BinaryRunner;
import io.github.osbeorn.maven.plugin.precommit.lib.InstallationException;
import io.github.osbeorn.maven.plugin.precommit.lib.PluginFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Runs pre-commit hooks via {@code pre-commit run} (optionally {@code --all-files}).
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class RunMojo extends AbstractPrecommitMojo {

    @Parameter(property = "downloadRoot", defaultValue = BinaryInstaller.DEFAULT_DOWNLOAD_ROOT)
    private String downloadRoot;

    @Parameter(property = "precommitVersion", required = true)
    private String precommitVersion;

    /**
     * When {@code true}, passes {@code --all-files} to pre-commit (checks the whole repo, not just staged files).
     */
    @Parameter(property = "precommit.allFiles", defaultValue = "false")
    private boolean allFiles;

    /**
     * Optional hook ids to run; when empty, pre-commit runs all configured hooks.
     */
    @Parameter(property = "hookIds")
    private String[] hookIds;

    @Parameter(property = "skip.run", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute(PluginFactory pluginFactory) throws MojoExecutionException, MojoFailureException {
        try {
            pluginFactory.getBinaryRunner()
                    .setDownloadRoot(downloadRoot)
                    .setVersion(precommitVersion)
                    .run(allFiles, hookIds);
        } catch (InstallationException e) {
            throw new MojoFailureException("pre-commit run failed", e);
        }
    }

    @Override
    protected boolean skipExecution() {
        return skip;
    }
}
