package io.github.osbeorn.maven.plugin.precommit.lib;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.hash.Hashing;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BinaryInstallerDownloadTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void downloadsArchiveVerifiesChecksumAndInstallsFromGitRoot() throws Exception {
        byte[] archiveBytes = "pre-commit archive".getBytes(StandardCharsets.UTF_8);
        String checksum = Hashing.sha256().hashBytes(archiveBytes).toString();
        String archivePath = "/v1.0.0/pre-commit-1.0.0.pyz";
        String checksumPath = archivePath + ".sha256sum";

        stubFor(get(urlEqualTo(archivePath)).willReturn(aResponse().withStatus(200).withBody(archiveBytes)));
        stubFor(get(urlEqualTo(checksumPath))
                .willReturn(aResponse().withStatus(200).withBody(checksum + "  pre-commit-1.0.0.pyz\n")));

        File repositoryRoot = temporaryFolder.newFolder("repo");
        File moduleDirectory = new File(repositoryRoot, "casino-common");
        assertTrue(moduleDirectory.mkdirs());
        writeGitMetadataAndConfig(repositoryRoot);

        File cacheDirectory = temporaryFolder.newFolder("cache");
        File installDirectory = temporaryFolder.newFolder("install");
        InstallConfig installConfig = new DefaultInstallConfig(
                installDirectory,
                moduleDirectory,
                new DirectoryCacheResolver(cacheDirectory)
        );

        RecordingPythonHandle pythonHandle = new RecordingPythonHandle();
        BinaryInstaller installer = new BinaryInstaller(
                installConfig,
                new DefaultFileCopier(),
                new DefaultFileDownloader(PluginLogs.silent()),
                pythonHandle,
                PluginLogs.silent()
        );

        installer.setVersion("v1.0.0")
                .setDownloadRoot(baseUrl() + "/")
                .setHookTypes(new String[]{"pre-commit", "pre-push"})
                .install();

        File cachedArchive = new File(cacheDirectory, "pre-commit-1.0.0.pyz");
        assertArrayEquals(archiveBytes, Files.readAllBytes(cachedArchive.toPath()));
        assertEquals(repositoryRoot.getCanonicalFile(), pythonHandle.workingDirectory.getCanonicalFile());
        assertArrayEquals(new String[]{"pre-commit", "pre-push"}, pythonHandle.hookTypes);
        assertTrue(pythonHandle.setupFile.exists());
    }

    @Test
    public void failsWhenChecksumDoesNotMatchDownloadedArchive() throws Exception {
        byte[] archiveBytes = "pre-commit archive".getBytes(StandardCharsets.UTF_8);
        String archivePath = "/v2.0.0/pre-commit-2.0.0.pyz";
        String checksumPath = archivePath + ".sha256sum";

        stubFor(get(urlEqualTo(archivePath)).willReturn(aResponse().withStatus(200).withBody(archiveBytes)));
        stubFor(get(urlEqualTo(checksumPath))
                .willReturn(aResponse().withStatus(200).withBody("deadbeef  pre-commit-2.0.0.pyz\n")));

        File repositoryRoot = temporaryFolder.newFolder("repo");
        writeGitMetadataAndConfig(repositoryRoot);

        File cacheDirectory = temporaryFolder.newFolder("cache");
        File installDirectory = temporaryFolder.newFolder("install");
        InstallConfig installConfig = new DefaultInstallConfig(
                installDirectory,
                repositoryRoot,
                new DirectoryCacheResolver(cacheDirectory)
        );

        BinaryInstaller installer = new BinaryInstaller(
                installConfig,
                new DefaultFileCopier(),
                new DefaultFileDownloader(PluginLogs.silent()),
                new RecordingPythonHandle(),
                PluginLogs.silent()
        );

        try {
            installer.setVersion("v2.0.0")
                    .setDownloadRoot(baseUrl() + "/")
                    .install();
            fail("Expected InstallationException");
        } catch (InstallationException expected) {
            assertTrue(expected.getMessage().contains("Could not download pre-commit"));
            assertTrue(expected.getCause() instanceof DownloadException);
            assertTrue(expected.getCause().getMessage().contains("checksum does not match"));
        }

        File cachedArchive = new File(cacheDirectory, "pre-commit-2.0.0.pyz");
        assertTrue(!cachedArchive.exists() || Files.size(cachedArchive.toPath()) == 0 || !cachedArchive.isFile());
    }

    private static void writeGitMetadataAndConfig(File repositoryRoot) throws Exception {
        new File(repositoryRoot, ".git").mkdir();
        Files.write(
                new File(repositoryRoot, PrecommitProjectRoot.CONFIG_FILE_NAME).toPath(),
                "repos: []\n".getBytes(StandardCharsets.UTF_8)
        );
    }

    private String baseUrl() {
        return "http://localhost:" + wireMockRule.port();
    }

    private static final class RecordingPythonHandle implements PythonHandle {
        private File setupFile;
        private String[] hookTypes;
        private File workingDirectory;

        @Override
        public void installPrecommit(File setupFile, String[] hookTypes, File workingDirectory) {
            this.setupFile = setupFile;
            this.hookTypes = hookTypes;
            this.workingDirectory = workingDirectory;
        }
    }
}
