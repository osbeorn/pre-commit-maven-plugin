package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class PythonException extends Exception {
    PythonException(String message) {
        super(message);
    }

    PythonException(String message, Throwable cause) {
        super(message, cause);
    }
}

interface PythonHandle {
    void installPrecommit(File setupFile, String[] hookTypes, File workingDirectory) throws PythonException;
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

    private static final int MIN_PYTHON_MAJOR = 3;
    private static final int MIN_PYTHON_MINOR = 7;

    private final PluginLog log;

    DefaultPythonHandle(PluginLog log) {
        this.log = log;
    }

    @Override
    public void installPrecommit(File setupFile, String[] hookTypes, File workingDirectory) throws PythonException {
        log.info("About to install Git hooks in " + workingDirectory);

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

            log.debug("Running " + String.join(" ", command));

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(workingDirectory);
                Process child = processBuilder.start();

                ProcessStreamDrainer errorDrainer = new ProcessStreamDrainer(child.getErrorStream(), log, true);
                ProcessStreamDrainer outputDrainer = new ProcessStreamDrainer(child.getInputStream(), log, false);
                errorDrainer.start();
                outputDrainer.start();

                int result = child.waitFor();
                errorDrainer.join();
                outputDrainer.join();

                if (result != 0) {
                    throw new PythonException(buildFailureMessage(hookType, result, errorDrainer.getContent(),
                            outputDrainer.getContent()));
                }
            } catch (IOException e) {
                throw new PythonException("Failed to execute python", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PythonException("Unexpected interruption while waiting for pre-commit install process", e);
            }
        }

        log.info("Successfully installed Git hooks.");
    }

    private static String buildFailureMessage(String hookType, int exitCode, String stderr, String stdout) {
        StringBuilder message = new StringBuilder()
                .append("Failed to install Git hook ")
                .append(hookType)
                .append(". Return code ")
                .append(exitCode)
                .append('.');
        if (!stderr.isEmpty()) {
            message.append(System.lineSeparator()).append("stderr:").append(System.lineSeparator()).append(stderr);
        }
        if (!stdout.isEmpty()) {
            message.append(System.lineSeparator()).append("stdout:").append(System.lineSeparator()).append(stdout);
        }
        return message.toString();
    }

    private String getPython3Executable() throws PythonException {
        if (binaryExists("python3")) {
            return "python3";
        }
        if (binaryExists("python")) {
            return "python";
        }

        throw new PythonException(
                "Could not find a compatible Python 3 executable on PATH. "
                        + MIN_PYTHON_MAJOR + "." + MIN_PYTHON_MINOR + "+ is required to run the pre-commit binary."
        );
    }

    private boolean binaryExists(String binaryName) {
        try {
            Process proc = new ProcessBuilder(binaryName, "--version").start();
            String output = IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8);

            if (proc.waitFor() == 0 && checkVersion(output)) {
                log.debug("Located python binary `" + binaryName + "`.");
                return true;
            }
        } catch (Exception ignored) {
        }

        log.debug("Did not locate a python binary called `" + binaryName + "`.");
        return false;
    }

    private boolean checkVersion(String pythonOutput) throws PythonException {
        try {
            String versionString = pythonOutput.split(" ")[1];
            int majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
            int minorVersion = Integer.parseInt(versionString.split("\\.")[1]);

            return majorVersion > MIN_PYTHON_MAJOR
                    || majorVersion == MIN_PYTHON_MAJOR && minorVersion >= MIN_PYTHON_MINOR;
        } catch (Exception exception) {
            throw new PythonException("Unexpected python version output: " + pythonOutput, exception);
        }
    }
}
