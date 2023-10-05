package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

final class PythonException extends Exception {
    PythonException(String message){
        super(message);
    }
    PythonException(String message, Throwable cause) {
        super(message, cause);
    }
}


interface PythonHandle {
    void installPrecommit(File setupFile, String[] hookTypes) throws PythonException;
}

final class VirtualEnvDescriptor {
    File directory;
    String name;

    VirtualEnvDescriptor(File directory, String name) {
        this.directory = new File(directory + "/." + name + "-virtualenv");
        this.name = name;
    }
}

final class DefaultPythonHandle implements PythonHandle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonHandle.class);

    @Override
    public void installPrecommit(File setupFile, String[] hookTypes) throws PythonException {
        LOGGER.info("About to install Git hooks.");

        // There is seemingly no way to install all hooks at once
        // Thus we run pre-commit as many times as necessary
        for (String hookType : hookTypes) {
            String[] command = new String[]{
                    getPython3Executable(),
                    setupFile.getAbsolutePath(),
                    "install",
                    "--install-hooks",
                    "--overwrite",
                    "--hook-type",
                    hookType
            };

            LOGGER.debug("Running {}.", (Object) command);

            try {
                Process child = Runtime.getRuntime().exec(command/*, environment, setupFile.getParentFile()*/);

                // Write messages to output
                BackgroundStreamLogger errorGobbler = new BackgroundStreamLogger(child.getErrorStream(), "ERROR");
                BackgroundStreamLogger outputGobbler = new BackgroundStreamLogger(child.getInputStream(), "DEBUG");
                errorGobbler.start();
                outputGobbler.start();

                int result = child.waitFor();

                if (result != 0) {
                    throw new PythonException("Failed to install Git hook " + hookType + ". Return code " + result);
                }
            } catch (IOException e) {
                throw new PythonException("Failed to execute python", e);
            } catch (InterruptedException e) {
                throw new PythonException("Unexpected interruption while waiting for pre-commit install process", e);
            }
        }

        LOGGER.info("Successfully installed Git hooks.");
    }

    private String getPython3Executable() throws PythonException {
        if (binaryExists("python3")) return "python3";
        if (binaryExists("python")) return "python";

        throw new PythonException(
                "Could not find a compatible python 3 version on your system. 3.3 is the minimum supported python version. " +
                "Please check you have a compatible 'python' or 'python3' executable on your PATH"
        );
    }

    private boolean binaryExists(String binaryName) {
        Runtime runtime = Runtime.getRuntime();

        try {
            Process proc = runtime.exec(new String[]{binaryName, "--version"});
            String output = IOUtils.toString(proc.getInputStream());

            if (proc.waitFor() == 0 && checkVersion(output)) {
                LOGGER.debug("Located python binary `{}`.", binaryName);
                return true;
            }
        } catch (Exception ignored) {
        }

        LOGGER.debug("Did not locate a python binary called `{}`.", binaryName);
        return false;
    }

    private boolean checkVersion(String pythonOutput) throws PythonException {
        try {
            String versionString = pythonOutput.split(" ")[1];
            int majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
            int minorVersion = Integer.parseInt(versionString.split("\\.")[1]);

            return majorVersion >= 3 && minorVersion >= 3;
        } catch (Exception exception) {
            throw new PythonException("Unexpected python version output: " + pythonOutput, exception);
        }
    }
}