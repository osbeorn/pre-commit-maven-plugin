package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;

public final class DownloadServerCredentials {

    private DownloadServerCredentials() {
    }

    public static DownloadHttpCredentials resolve(
            String downloadServerId,
            Settings settings,
            SettingsDecrypter settingsDecrypter,
            PluginLog log
    ) {
        if (downloadServerId == null || downloadServerId.trim().isEmpty()) {
            return DownloadHttpCredentials.none();
        }
        if (settings == null) {
            log.warn("Maven settings are not available; cannot use downloadServerId " + downloadServerId + ".");
            return DownloadHttpCredentials.none();
        }

        String serverId = downloadServerId.trim();
        Server server = settings.getServer(serverId);
        if (server == null) {
            log.warn(
                    "No <server><id>" + serverId + "</id></server> in settings.xml; "
                            + "pre-commit download will be unauthenticated."
            );
            return DownloadHttpCredentials.none();
        }

        Server effective = server;
        if (settingsDecrypter != null) {
            effective = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server)).getServer();
        }

        String username = effective.getUsername();
        if (username == null || username.isEmpty()) {
            log.warn("Server '" + serverId + "' in settings.xml has no username; download will be unauthenticated.");
            return DownloadHttpCredentials.none();
        }

        log.debug("Using settings.xml credentials from server id '" + serverId + "' for pre-commit downloads.");
        return DownloadHttpCredentials.of(username, effective.getPassword());
    }
}
