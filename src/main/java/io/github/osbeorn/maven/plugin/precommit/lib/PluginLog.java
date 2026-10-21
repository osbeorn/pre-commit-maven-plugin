package io.github.osbeorn.maven.plugin.precommit.lib;

public interface PluginLog {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);
}
