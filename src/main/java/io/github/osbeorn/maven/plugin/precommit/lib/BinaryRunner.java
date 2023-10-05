package io.github.osbeorn.maven.plugin.precommit.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BinaryRunner {
    public static final String INSTALL_PATH = "/pre-commit";
    private final Logger logger;
    private final InstallConfig config;
    private final PythonHandle pythonHandle;

    public BinaryRunner(InstallConfig config, PythonHandle pythonHandle) {
        logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.pythonHandle = pythonHandle;
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            logger.debug("Creating install directory {}.", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }
}
