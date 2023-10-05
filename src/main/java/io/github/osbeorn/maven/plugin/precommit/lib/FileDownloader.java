package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

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

    @Override
    public void download(String downloadUrl, String destination) throws DownloadException {
        // force tls to 1.2 since GitHub removed weak cryptographic standards
        // https://blog.github.com/2018-02-02-weak-cryptographic-standards-removal-notice/
        System.setProperty("https.protocols", "TLSv1.2");
        String fixedDownloadUrl = downloadUrl;

        try {
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);

            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                FileUtils.copyFile(new File(downloadURI), new File(destination));
            } else {
                CloseableHttpResponse response = execute(fixedDownloadUrl);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    throw new DownloadException("Got error code " + statusCode + " from the server.");
                }

                new File(FilenameUtils.getFullPathNoEndSeparator(destination)).mkdirs();
                ReadableByteChannel rbc = Channels.newChannel(response.getEntity().getContent());
                FileOutputStream fos = new FileOutputStream(destination);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
            }
        } catch (IOException | URISyntaxException e) {
            throw new DownloadException("Could not download " + fixedDownloadUrl, e);
        }
    }

    private CloseableHttpResponse execute(String requestUrl) throws IOException {
        try (CloseableHttpClient httpClient = buildHttpClient(null)) {
            return httpClient.execute(new HttpGet(requestUrl));
        }
    }

    private CloseableHttpClient buildHttpClient(CredentialsProvider credentialsProvider) {
        return HttpClients.custom()
                .disableContentCompression()
                .useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }
}
