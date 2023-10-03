package no.oms.maven.precommit.lib;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
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
    VirtualEnvDescriptor setupVirtualEnv(File directory, String envName) throws PythonException;
    void installPyYaml(VirtualEnvDescriptor env) throws PythonException;
    void installIntoVirtualEnv(VirtualEnvDescriptor env, File setupFile) throws PythonException;
    void installPrecommit(VirtualEnvDescriptor env, String version) throws PythonException;
    void installGitHooks(VirtualEnvDescriptor env, HookType[] hookTypes) throws PythonException;
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
    public VirtualEnvDescriptor setupVirtualEnv(File directory, String envName) throws PythonException {
        LOGGER.info("About to setup virtual env {}", envName);

        VirtualEnvDescriptor env = new VirtualEnvDescriptor(directory, envName);

        if (env.directory.exists()) {
            LOGGER.info("Virtual env already exists, skipping");
            return env;
        }

        String[] command = new String[]{ getPython3Executable(), "-m", "venv", env.directory.getAbsolutePath() };
        LOGGER.debug("Running {}", (Object) command);

        try {
            Process child = Runtime.getRuntime().exec(command);
            int result = child.waitFor();
            String stdout = IOUtils.toString(child.getInputStream());

            if (result != 0) {
                throw new PythonException(
                        "Could not create virtual env " + env.directory.getAbsolutePath() + ". return code " + result +
                                "\nPython said: " + stdout
                );
            }
        } catch (IOException e) {
            throw new PythonException("Failed to execute python", e);
        } catch (InterruptedException e) {
            throw new PythonException("Unexpected interruption of while waiting for python virtualenv process", e);
        }

        return env;
    }

    @Override
    public void installPyYaml(VirtualEnvDescriptor env) throws PythonException {
        LOGGER.info("About to install pyyaml into env {}", env.name);

        if (!env.directory.exists()) {
            throw new PythonException("Virtual env " + env.name + " does not exist");
        }

        String[] command = new String[]{
                env.directory.getAbsolutePath() + (SystemUtils.IS_OS_WINDOWS ? "\\Scripts\\pip.exe" : "/bin/pip"),
                "install",
                "pyyaml",
                "--disable-pip-version-check"
        };
        String[] environment = new String[]{ "VIRTUAL_ENV=" + env.directory.getAbsolutePath() };
        LOGGER.debug("Running {} {} in {}", environment, command);

        try {
            Process child = Runtime.getRuntime().exec(command, environment);

            // Write messages to output
            BackgroundStreamLogger errorGobbler = new BackgroundStreamLogger(child.getErrorStream(), "ERROR");
            BackgroundStreamLogger outputGobbler = new BackgroundStreamLogger(child.getInputStream(), "DEBUG");
            errorGobbler.start();
            outputGobbler.start();

            int result = child.waitFor();

            if (result != 0) {
                throw new PythonException("Failed to install pyyaml into " + env.name + ". return code " + result);
            }
        } catch (IOException e) {
            throw new PythonException("Failed to execute python", e);
        } catch (InterruptedException e) {
            throw new PythonException("Unexpected interruption of while waiting for python virtualenv process", e);
        }

        LOGGER.info("Successfully installed pyyaml into {}", env.name);
    }

    @Override
    public void installIntoVirtualEnv(VirtualEnvDescriptor env, File setupFile) throws PythonException {
        LOGGER.info("About to install binary into virtual env {}", env.name);

        if (!env.directory.exists()) {
            throw new PythonException("Virtual env " + env.name + " does not exist");
        }

        String[] command = new String[]{
                env.directory.getAbsolutePath() + (SystemUtils.IS_OS_WINDOWS ? "\\Scripts\\python.exe" : "/bin/python"),
                setupFile.getAbsolutePath(),
                "install"
        };
        String[] environment = new String[]{
                "VIRTUAL_ENV=" + env.directory.getAbsolutePath(),
                "SETUPTOOLS_USE_DISTUTILS=stdlib"
        };
        LOGGER.debug("Running {} {} in {}", environment, command, setupFile.getParentFile());

        try {
            Process child = Runtime.getRuntime().exec(command, environment, setupFile.getParentFile());

            // Write messages to output
            BackgroundStreamLogger errorGobbler = new BackgroundStreamLogger(child.getErrorStream(), "ERROR");
            BackgroundStreamLogger outputGobbler = new BackgroundStreamLogger(child.getInputStream(), "DEBUG");
            errorGobbler.start();
            outputGobbler.start();

            int result = child.waitFor();

            if (result != 0) {
                throw new PythonException("Failed to install into virtual env " + env.name + ". return code " + result);
            }
        } catch (IOException e) {
            throw new PythonException("Failed to execute python", e);
        } catch (InterruptedException e) {
            throw new PythonException("Unexpected interruption of while waiting for python virtualenv process", e);
        }

        LOGGER.info("Successfully installed into {}", env.name);
    }

    @Override
    public void installPrecommit(VirtualEnvDescriptor env, String version) throws PythonException {
        LOGGER.info("About to install pre-commit into env {}", env.name);

        if (!env.directory.exists()) {
            throw new PythonException("Virtual env " + env.name + " does not exist");
        }

        String[] command = new String[]{
                env.directory.getAbsolutePath() + (SystemUtils.IS_OS_WINDOWS ? "\\Scripts\\pip.exe" : "/bin/pip"),
                "install",
                String.format("pre-commit==%s", version.replace("v", "")),
                "--disable-pip-version-check"
        };
        String[] environment = new String[]{ "VIRTUAL_ENV=" + env.directory.getAbsolutePath() };
        LOGGER.debug("Running {} {} in {}", environment, command);

        try {
            Process child = Runtime.getRuntime().exec(command, environment);

            // Write messages to output
            BackgroundStreamLogger errorGobbler = new BackgroundStreamLogger(child.getErrorStream(), "ERROR");
            BackgroundStreamLogger outputGobbler = new BackgroundStreamLogger(child.getInputStream(), "DEBUG");
            errorGobbler.start();
            outputGobbler.start();

            int result = child.waitFor();

            if (result != 0) {
                throw new PythonException("Failed to install pre-commit into " + env.name + ". return code " + result);
            }
        } catch (IOException e) {
            throw new PythonException("Failed to execute python", e);
        } catch (InterruptedException e) {
            throw new PythonException("Unexpected interruption of while waiting for python virtualenv process", e);
        }

        LOGGER.info("Successfully installed pre-commit into {}", env.name);
    }

    @Override
    public void installGitHooks(VirtualEnvDescriptor env, HookType[] hookTypes) throws PythonException {
        LOGGER.info("About to install commit hooks into virtual env {}", env.name);

        if (!env.directory.exists()) {
            throw new PythonException("Virtual env " + env.name + " does not exist");
        }

        if (hookTypes == null || hookTypes.length == 0) {
            throw new PythonException("Providing the hook types to install are required");
        }

        // There is seemingly no way to install all hooks at once
        // Thus we run pre-commit as many times as necessary
        for (HookType type : hookTypes) {
            String[] command = new String[]{
                    env.directory.getAbsolutePath() + (SystemUtils.IS_OS_WINDOWS ? "\\Scripts\\pre-commit.exe" : "/bin/pre-commit"),
                    "install",
                    "--install-hooks",
                    "--overwrite",
                    "--hook-type",
                    type.getValue()
            };
            String[] environment = new String[]{
                    "VIRTUAL_ENV=" + env.directory.getAbsolutePath(),
                    // PATH is not inherited when we explicitly set environment.
                    // Set it to retain access to the git binary
                    "PATH=" + System.getenv("PATH")
            };
            LOGGER.debug("Running {} {}", environment, command);

            try {
                Process child = Runtime.getRuntime().exec(command, environment);

                // Write messages to output
                BackgroundStreamLogger errorGobbler = new BackgroundStreamLogger(child.getErrorStream(), "ERROR");
                BackgroundStreamLogger outputGobbler = new BackgroundStreamLogger(child.getInputStream(), "INFO");
                errorGobbler.start();
                outputGobbler.start();

                int result = child.waitFor();

                if (result != 0) {
                    throw new PythonException("Failed to install git hooks. return code " + result);
                }
            } catch (IOException e) {
                throw new PythonException("Failed to execute python", e);
            } catch (InterruptedException e) {
                throw new PythonException("Unexpected interruption of while waiting for the pre-commit binary", e);
            }
        }

        LOGGER.info("Successfully installed Git commit hooks");
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
                LOGGER.debug("Located python binary `{}`", binaryName);
                return true;
            }
        } catch (Exception ignored) {
        }

        LOGGER.debug("Did not locate a python binary called `{}`", binaryName);
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