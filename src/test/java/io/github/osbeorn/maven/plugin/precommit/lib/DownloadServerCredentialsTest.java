package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadServerCredentialsTest {

    @Test
    public void resolvesUsernameAndPasswordFromSettingsServer() {
        Settings settings = new Settings();
        Server server = new Server();
        server.setId("nexus");
        server.setUsername("deploy");
        server.setPassword("pass");
        settings.addServer(server);

        DownloadHttpCredentials credentials = DownloadServerCredentials.resolve(
                "nexus",
                settings,
                null,
                PluginLogs.silent()
        );

        assertTrue(credentials.isPresent());
        assertEquals("deploy", credentials.getUsername());
        assertEquals("pass", credentials.getPassword());
    }

    @Test
    public void returnsNoneWhenServerIdMissing() {
        DownloadHttpCredentials credentials = DownloadServerCredentials.resolve(
                "missing",
                new Settings(),
                null,
                PluginLogs.silent()
        );
        assertFalse(credentials.isPresent());
    }
}
