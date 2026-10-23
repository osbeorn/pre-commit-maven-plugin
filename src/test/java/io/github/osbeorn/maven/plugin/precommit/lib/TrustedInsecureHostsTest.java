package io.github.osbeorn.maven.plugin.precommit.lib;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrustedInsecureHostsTest {

    @Test
    public void matchesExactHostCaseInsensitively() {
        TrustedInsecureHosts hosts = TrustedInsecureHosts.parse("nexus.pronetdev.com, Other.Example");
        assertTrue(hosts.matches("nexus.pronetdev.com"));
        assertTrue(hosts.matches("NEXUS.PRONETDEV.COM"));
        assertTrue(hosts.matches("other.example"));
        assertFalse(hosts.matches("sub.nexus.pronetdev.com"));
        assertFalse(hosts.matches("github.com"));
    }

    @Test
    public void emptyConfigurationMatchesNothing() {
        TrustedInsecureHosts hosts = TrustedInsecureHosts.parse("");
        assertTrue(hosts.isEmpty());
        assertFalse(hosts.matches("nexus.pronetdev.com"));
    }
}
