package io.github.osbeorn.maven.plugin.precommit.lib;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrecommitProjectRootTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesRootFromSubmoduleDirectory() throws Exception {
        File repositoryRoot = temporaryFolder.newFolder("repo");
        File module = new File(repositoryRoot, "casino-common");
        assertTrue(module.mkdirs());

        writePrecommitConfig(repositoryRoot);
        new File(repositoryRoot, ".git").mkdir();

        File resolved = PrecommitProjectRoot.resolve(module);

        assertEquals(repositoryRoot.getCanonicalFile(), resolved.getCanonicalFile());
    }

    @Test
    public void resolvesRootWhenStartingAtRepositoryRoot() throws Exception {
        File repositoryRoot = temporaryFolder.newFolder("repo");
        writePrecommitConfig(repositoryRoot);
        new File(repositoryRoot, ".git").mkdir();

        File resolved = PrecommitProjectRoot.resolve(repositoryRoot);

        assertEquals(repositoryRoot.getCanonicalFile(), resolved.getCanonicalFile());
    }

    @Test(expected = InstallationException.class)
    public void failsWhenConfigFileIsMissing() throws Exception {
        File repositoryRoot = temporaryFolder.newFolder("repo");
        new File(repositoryRoot, ".git").mkdir();

        PrecommitProjectRoot.resolve(repositoryRoot);
    }

    @Test
    public void findReturnsEmptyWhenGitOrConfigIsMissing() throws Exception {
        File repositoryRoot = temporaryFolder.newFolder("repo");
        new File(repositoryRoot, ".git").mkdir();

        assertFalse(PrecommitProjectRoot.find(repositoryRoot).isPresent());
    }

    private static void writePrecommitConfig(File directory) throws Exception {
        Files.write(
                new File(directory, PrecommitProjectRoot.CONFIG_FILE_NAME).toPath(),
                "repos: []\n".getBytes(StandardCharsets.UTF_8)
        );
    }
}
