package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

final class DownloadException extends Exception {
    public DownloadException(String message) {
        super(message);
    }

    DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}

interface FileDownloader {
    void download(String downloadUrl, String destination) throws DownloadException;
}

final class DefaultFileDownloader implements FileDownloader {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 500L;

    private final PluginLog log;
    private final TrustedInsecureHosts trustedInsecureHosts;
    private final Set<String> insecureSslWarningLogged = new HashSet<>();

    DefaultFileDownloader(PluginLog log) {
        this(log, TrustedInsecureHosts.empty());
    }

    DefaultFileDownloader(PluginLog log, TrustedInsecureHosts trustedInsecureHosts) {
        this.log = log;
        this.trustedInsecureHosts = trustedInsecureHosts == null
                ? TrustedInsecureHosts.empty()
                : trustedInsecureHosts;
    }

    @Override
    public void download(String downloadUrl, String destination) throws DownloadException {
        System.setProperty("https.protocols", "TLSv1.2");
        String fixedDownloadUrl = downloadUrl;

        try {
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);

            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                FileUtils.copyFile(new File(downloadURI), new File(destination));
                return;
            }
        } catch (IOException | URISyntaxException e) {
            throw new DownloadException("Could not download " + fixedDownloadUrl, e);
        }

        int maxAttempts = maxAttempts();
        DownloadException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                downloadOnce(fixedDownloadUrl, destination);
                return;
            } catch (DownloadException e) {
                lastFailure = e;
                deleteQuietly(new File(destination));
                if (attempt == maxAttempts || !isRetriable(e)) {
                    throw e;
                }
                long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                log.warn(String.format(
                        "Download attempt %d/%d failed for %s (%s). Retrying in %d ms.",
                        attempt,
                        maxAttempts,
                        fixedDownloadUrl,
                        e.getMessage(),
                        backoffMs
                ));
                sleep(backoffMs);
            }
        }

        throw lastFailure;
    }

    private void downloadOnce(String downloadUrl, String destination) throws DownloadException {
        String host = hostFromUrl(downloadUrl);
        try (CloseableHttpClient client = buildHttpClient(host, null)) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(downloadUrl))) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    throw new DownloadException("Got error code " + statusCode + " from the server.");
                }

                new File(FilenameUtils.getFullPathNoEndSeparator(destination)).mkdirs();
                try (ReadableByteChannel rbc = Channels.newChannel(response.getEntity().getContent());
                     FileOutputStream fos = new FileOutputStream(destination)) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
            }
        } catch (IOException e) {
            throw new DownloadException("Could not download " + downloadUrl, e);
        }
    }

    private CloseableHttpClient buildHttpClient(String host, CredentialsProvider credentialsProvider)
            throws DownloadException {
        if (host != null && trustedInsecureHosts.matches(host)) {
            logInsecureSslWarning(host);
            try {
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        NoopHostnameVerifier.INSTANCE
                );
                Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslSocketFactory)
                        .build();
                PoolingHttpClientConnectionManager connectionManager =
                        new PoolingHttpClientConnectionManager(registry);
                return HttpClients.custom()
                        .disableContentCompression()
                        .useSystemProperties()
                        .setConnectionManager(connectionManager)
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .build();
            } catch (Exception e) {
                throw new DownloadException("Could not configure insecure SSL for host " + host, e);
            }
        }

        return HttpClients.custom()
                .disableContentCompression()
                .useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    private void logInsecureSslWarning(String host) {
        synchronized (insecureSslWarningLogged) {
            if (insecureSslWarningLogged.add(host)) {
                log.warn(
                        "SSL certificate validation is disabled for download host "
                                + host
                                + " (trustedInsecureHosts). Use only for trusted internal mirrors."
                );
            }
        }
    }

    private static String hostFromUrl(String downloadUrl) throws DownloadException {
        try {
            return new URI(FilenameUtils.separatorsToUnix(downloadUrl)).getHost();
        } catch (URISyntaxException e) {
            throw new DownloadException("Invalid download URL " + downloadUrl, e);
        }
    }

    private static int maxAttempts() {
        String property = System.getProperty("precommit.download.maxAttempts");
        if (property == null || property.isEmpty()) {
            return DEFAULT_MAX_ATTEMPTS;
        }
        try {
            int parsed = Integer.parseInt(property);
            return parsed < 1 ? DEFAULT_MAX_ATTEMPTS : parsed;
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_ATTEMPTS;
        }
    }

    private static boolean isRetriable(DownloadException e) {
        String message = e.getMessage();
        if (message != null && message.contains("Got error code")) {
            for (int nonRetriable : new int[]{400, 401, 403, 404, 406, 410, 451}) {
                if (message.contains(String.valueOf(nonRetriable))) {
                    return false;
                }
            }
            return true;
        }
        return e.getCause() instanceof IOException;
    }

    private static void sleep(long backoffMs) throws DownloadException {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DownloadException("Download interrupted while waiting to retry.", e);
        }
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.debug("Could not delete incomplete download " + file);
        }
    }
}
