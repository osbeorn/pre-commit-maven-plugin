package io.github.osbeorn.maven.plugin.precommit.lib;

public final class PluginLogs {

    private PluginLogs() {
    }

    public static PluginLog silent() {
        return new PluginLog() {
            @Override
            public void debug(String message) {
            }

            @Override
            public void info(String message) {
            }

            @Override
            public void warn(String message) {
            }

            @Override
            public void error(String message) {
            }

            @Override
            public void error(String message, Throwable throwable) {
            }
        };
    }
}
