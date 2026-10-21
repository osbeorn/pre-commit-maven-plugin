package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class ProcessStreamDrainer extends Thread {

    private final InputStream inputStream;
    private final PluginLog log;
    private final boolean errorStream;
    private final StringBuilder buffer = new StringBuilder();

    ProcessStreamDrainer(InputStream inputStream, PluginLog log, boolean errorStream) {
        this.inputStream = inputStream;
        this.log = log;
        this.errorStream = errorStream;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append(System.lineSeparator());
                if (errorStream) {
                    log.error(line);
                } else {
                    log.debug(line);
                }
            }
        } catch (IOException e) {
            log.debug("Reading process stream failed: " + e.getMessage());
        }
    }

    String getContent() {
        return buffer.toString().trim();
    }
}
