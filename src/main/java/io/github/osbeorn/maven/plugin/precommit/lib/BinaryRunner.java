package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;

public class BinaryRunner {
    public static final String INSTALL_PATH = "pre-commit";
    private final PluginLog log;
    private final InstallConfig config;
    private final PythonHandle pythonHandle;

    public BinaryRunner(InstallConfig config, PythonHandle pythonHandle) {
        this(config, pythonHandle, PluginLogs.silent());
    }

    BinaryRunner(InstallConfig config, PythonHandle pythonHandle, PluginLog log) {
        this.log = log;
        this.config = config;
        this.pythonHandle = pythonHandle;
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            log.debug("Creating install directory " + installDirectory + ".");
            installDirectory.mkdirs();
        }
        return installDirectory;
    }
}
