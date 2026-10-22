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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BinaryRunnerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runsPrecommitFromGitRootWithAllFiles() throws Exception {
        byte[] archiveBytes = "pre-commit archive".getBytes(StandardCharsets.UTF_8);
        String checksum = Hashing.sha256().hashBytes(archiveBytes).toString();
        String archivePath = "/v1.0.0/pre-commit-1.0.0.pyz";
        String checksumPath = archivePath + ".sha256sum";

        stubFor(get(urlEqualTo(archivePath)).willReturn(aResponse().withStatus(200).withBody(archiveBytes)));
        stubFor(get(urlEqualTo(checksumPath))
                .willReturn(aResponse().withStatus(200).withBody(checksum + "  pre-commit-1.0.0.pyz\n")));

        File repositoryRoot = temporaryFolder.newFolder("repo");
        File moduleDirectory = new File(repositoryRoot, "module");
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
        BinaryRunner runner = new BinaryRunner(
                installConfig,
                new DefaultFileCopier(),
                new DefaultFileDownloader(PluginLogs.silent()),
                pythonHandle,
                PluginLogs.silent()
        );

        runner.setVersion("v1.0.0")
                .setDownloadRoot("http://localhost:" + wireMockRule.port() + "/")
                .run(true, new String[]{"maven-spotless-apply"});

        assertEquals(repositoryRoot.getCanonicalFile(), pythonHandle.workingDirectory.getCanonicalFile());
        assertTrue(pythonHandle.allFiles);
        assertEquals(1, pythonHandle.hookIds.length);
        assertEquals("maven-spotless-apply", pythonHandle.hookIds[0]);
        assertTrue(pythonHandle.setupFile.exists());
    }

    private static void writeGitMetadataAndConfig(File repositoryRoot) throws Exception {
        new File(repositoryRoot, ".git").mkdir();
        Files.write(
                new File(repositoryRoot, PrecommitProjectRoot.CONFIG_FILE_NAME).toPath(),
                "repos: []\n".getBytes(StandardCharsets.UTF_8)
        );
    }

    private static final class RecordingPythonHandle implements PythonHandle {
        private File setupFile;
        private File workingDirectory;
        private boolean allFiles;
        private String[] hookIds;

        @Override
        public void installPrecommit(File setupFile, String[] hookTypes, File workingDirectory) {
        }

        @Override
        public void runPrecommit(File setupFile, File workingDirectory, boolean allFiles, String[] hookIds) {
            this.setupFile = setupFile;
            this.workingDirectory = workingDirectory;
            this.allFiles = allFiles;
            this.hookIds = hookIds;
        }
    }
}
