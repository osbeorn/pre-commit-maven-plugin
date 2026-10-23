package io.github.osbeorn.maven.plugin.precommit.lib;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Exact host names for which TLS certificate validation is disabled on pre-commit downloads.
 */
final class TrustedInsecureHosts {

    private final Set<String> hosts;

    private TrustedInsecureHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    static TrustedInsecureHosts parse(String configuredHosts) {
        if (configuredHosts == null || configuredHosts.trim().isEmpty()) {
            return empty();
        }
        Set<String> parsed = new HashSet<>();
        for (String part : configuredHosts.split(",")) {
            String host = part.trim().toLowerCase(Locale.ROOT);
            if (!host.isEmpty()) {
                parsed.add(host);
            }
        }
        if (parsed.isEmpty()) {
            return empty();
        }
        return new TrustedInsecureHosts(Collections.unmodifiableSet(parsed));
    }

    static TrustedInsecureHosts empty() {
        return new TrustedInsecureHosts(Collections.<String>emptySet());
    }

    boolean isEmpty() {
        return hosts.isEmpty();
    }

    boolean matches(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        return hosts.contains(host.toLowerCase(Locale.ROOT));
    }

    Set<String> hosts() {
        return hosts;
    }
}
