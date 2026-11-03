package io.github.osbeorn.maven.plugin.precommit.lib;

public final class DownloadHttpCredentials {

    private final String username;
    private final String password;

    private DownloadHttpCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static DownloadHttpCredentials none() {
        return new DownloadHttpCredentials(null, null);
    }

    public static DownloadHttpCredentials of(String username, String password) {
        if (username == null || username.isEmpty()) {
            return none();
        }
        return new DownloadHttpCredentials(username, password == null ? "" : password);
    }

    public boolean isPresent() {
        return username != null && !username.isEmpty();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
