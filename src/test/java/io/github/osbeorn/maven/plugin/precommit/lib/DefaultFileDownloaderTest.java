package io.github.osbeorn.maven.plugin.precommit.lib;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultFileDownloaderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousMaxAttempts;

    @Before
    public void rememberMaxAttemptsProperty() {
        previousMaxAttempts = System.getProperty("precommit.download.maxAttempts");
    }

    @After
    public void restoreMaxAttemptsProperty() {
        if (previousMaxAttempts == null) {
            System.clearProperty("precommit.download.maxAttempts");
        } else {
            System.setProperty("precommit.download.maxAttempts", previousMaxAttempts);
        }
    }

    @Test
    public void downloadsChecksumWithoutJavaDefaultAcceptHeader406() throws Exception {
        String checksumBody = "abc123  pre-commit.pyz\n";
        stubFor(get(urlEqualTo("/pre-commit.pyz.sha256sum"))
                .atPriority(1)
                .withHeader("Accept", com.github.tomakehurst.wiremock.client.WireMock.containing("text/html"))
                .willReturn(aResponse().withStatus(406)));
        stubFor(get(urlEqualTo("/pre-commit.pyz.sha256sum"))
                .atPriority(5)
                .willReturn(aResponse().withStatus(200).withBody(checksumBody)));

        File destination = temporaryFolder.newFile("checksum.sha256sum");
        DefaultFileDownloader downloader = new DefaultFileDownloader();
        downloader.download(baseUrl() + "/pre-commit.pyz.sha256sum", destination.getPath());

        assertEquals(checksumBody, new String(Files.readAllBytes(destination.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void retriesTransientServerErrors() throws Exception {
        System.setProperty("precommit.download.maxAttempts", "3");

        stubFor(get(urlEqualTo("/archive.pyz"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second"));
        stubFor(get(urlEqualTo("/archive.pyz"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(200).withBody("payload")));

        File destination = temporaryFolder.newFile("archive.pyz");
        DefaultFileDownloader downloader = new DefaultFileDownloader();
        downloader.download(baseUrl() + "/archive.pyz", destination.getPath());

        assertEquals("payload", new String(Files.readAllBytes(destination.toPath()), StandardCharsets.UTF_8));
        verify(2, getRequestedFor(urlEqualTo("/archive.pyz")));
    }

    @Test
    public void doesNotRetryPermanentClientErrors() throws Exception {
        System.setProperty("precommit.download.maxAttempts", "3");

        stubFor(get(urlEqualTo("/missing.pyz"))
                .willReturn(aResponse().withStatus(404)));

        File destination = temporaryFolder.newFile("missing.pyz");
        DefaultFileDownloader downloader = new DefaultFileDownloader();

        try {
            downloader.download(baseUrl() + "/missing.pyz", destination.getPath());
            fail("Expected DownloadException");
        } catch (DownloadException expected) {
            assertTrue(expected.getMessage().contains("404"));
        }

        verify(1, getRequestedFor(urlEqualTo("/missing.pyz")));
    }

    private String baseUrl() {
        return "http://localhost:" + wireMockRule.port();
    }
}
