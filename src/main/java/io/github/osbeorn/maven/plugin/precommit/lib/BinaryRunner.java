package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;

public class BinaryRunner {
    private final PluginLog log;
    private final InstallConfig config;
    private final FileCopier fileCopier;
    private final FileDownloader fileDownloader;
    private final PythonHandle pythonHandle;
    private String version;
    private String downloadRoot;

    BinaryRunner(
            InstallConfig config,
            FileCopier fileCopier,
            FileDownloader fileDownloader,
            PythonHandle pythonHandle,
            PluginLog log
    ) {
        this.config = config;
        this.fileCopier = fileCopier;
        this.fileDownloader = fileDownloader;
        this.pythonHandle = pythonHandle;
        this.log = log;
    }

    public BinaryRunner setVersion(String version) {
        this.version = version;
        return this;
    }

    public BinaryRunner setDownloadRoot(String downloadRoot) {
        this.downloadRoot = downloadRoot;
        return this;
    }

    public void run(boolean allFiles, String[] hookIds) throws InstallationException {
        try {
            BinaryInstaller binaryInstaller = new BinaryInstaller(
                    config,
                    fileCopier,
                    fileDownloader,
                    pythonHandle,
                    log
            );
            binaryInstaller.setVersion(version).setDownloadRoot(downloadRoot);

            File setupFile = binaryInstaller.ensureSetupFileAvailable();
            File precommitRoot = PrecommitProjectRoot.resolve(config.getWorkingDirectory());
            pythonHandle.runPrecommit(setupFile, precommitRoot, allFiles, hookIds);
        } catch (PythonException e) {
            throw new InstallationException("pre-commit run failed", e);
        }
    }
}
