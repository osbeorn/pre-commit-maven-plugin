package io.github.osbeorn.maven.plugin.precommit.mojo;

import io.github.osbeorn.maven.plugin.precommit.lib.PluginLog;
import org.apache.maven.plugin.logging.Log;

public final class MavenPluginLog implements PluginLog {

    private final Log log;

    public MavenPluginLog(Log log) {
        this.log = log;
    }

    @Override
    public void debug(String message) {
        log.debug(message);
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }
}
