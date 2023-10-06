package io.github.osbeorn.maven.plugin.precommit.lib;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class BinaryInstaller {
    public static final String INSTALL_PATH = "/pre-commit";
    public static final String DEFAULT_DOWNLOAD_ROOT = "https://github.com/pre-commit/pre-commit/releases/download/";
    private final Logger logger;
    private final InstallConfig config;
    private final FileCopier fileCopier;
    private final FileDownloader fileDownloader;
    private final PythonHandle pythonHandle;
    private String version, downloadRoot;
    private String[] hookTypes;

    public BinaryInstaller(InstallConfig config, FileCopier fileCopier, FileDownloader fileDownloader, PythonHandle pythonHandle) {
        logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.fileCopier = fileCopier;
        this.fileDownloader = fileDownloader;
        this.pythonHandle = pythonHandle;
    }

    public BinaryInstaller setVersion(String version) {
        this.version = version;
        return this;
    }

    public BinaryInstaller setDownloadRoot(String downloadRoot) {
        this.downloadRoot = downloadRoot;
        return this;
    }

    public BinaryInstaller setHookTypes(String[] hookTypes) {
        this.hookTypes = hookTypes;
        return this;
    }

    public void install() throws InstallationException {
        try {
            logger.info("Installing pre-commit version {}.", version);

            String downloadUrl = downloadRoot + version + "/";
            String extension = "pyz";
            String setupFileName = String.format("pre-commit-%s.%s", version.replace("v", ""), extension);
            downloadUrl += setupFileName;

            CacheDescriptor cacheDescriptor = new CacheDescriptor("pre-commit", version, extension);

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive);

            File installDirectory = getInstallDirectory();
            copyFileIfMissing(archive, installDirectory);

            File setupFile = new File(installDirectory + "/" + setupFileName);
            if (!setupFile.exists()) {
                throw new InstallationException("Could not find " + setupFileName);
            }

            pythonHandle.installPrecommit(setupFile, hookTypes);

            logger.info("Successfully installed pre-commit.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download pre-commit", e);
        } catch (FileCopyException e) {
            throw new InstallationException("Could not copy the pre-commit archive", e);
        } catch (PythonException e) {
            throw new InstallationException("Python encountered an issue when installing the pre-commit binary", e);
        }
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            logger.debug("Creating install directory {}.", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }

    private void copyFileIfMissing(File archive, File destinationDirectory) throws FileCopyException {
        try {
            File destinationFile = new File(destinationDirectory.getPath() + "/" + archive.getName());

            boolean fileExists = destinationFile.exists();
            boolean fileEqual = fileExists && Files.equal(archive, destinationFile);

            if (!fileExists || !fileEqual) {
                logger.info("Copying {} into {}.", archive, destinationDirectory);
                fileCopier.copy(archive.getPath(), destinationDirectory.getPath());
            }
        } catch (IOException e) {
            throw new FileCopyException("Failed to copy to destination directory");
        }
    }

    private void copyFile(File archive, File destinationDirectory) throws FileCopyException {
        logger.info("Copying {} into {}.", archive, destinationDirectory);
        fileCopier.copy(archive.getPath(), destinationDirectory.getPath());
    }

    private void downloadFileIfMissing(String downloadUrl, File destination) throws DownloadException {
        boolean fileExists = destination.exists();
        boolean fileValid = validateFile(downloadUrl, destination);

        if (!fileExists || !fileValid) {
            downloadFile(downloadUrl, destination);
        }
    }

    private void downloadFile(String downloadUrl, File destination) throws DownloadException {
        logger.info("Downloading {} to {}.", downloadUrl, destination);
        fileDownloader.download(downloadUrl, destination.getPath());
        validateFile(downloadUrl, destination);
    }

    private boolean validateFile(String downloadUrl, File destination) throws DownloadException {
        try {
            downloadUrl += ".sha256sum";
            String hash = IOUtils.toString(URI.create(downloadUrl), StandardCharsets.UTF_8);
            return Files.asByteSource(destination).hash(Hashing.sha256()).toString().equals(hash);
        } catch (IOException e) {
            throw new DownloadException("Failed to validate downloaded file.");
        }
    }
}
