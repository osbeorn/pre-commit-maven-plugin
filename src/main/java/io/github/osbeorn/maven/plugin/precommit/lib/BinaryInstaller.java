package io.github.osbeorn.maven.plugin.precommit.lib;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;

public class BinaryInstaller {
    public static final String INSTALL_PATH = "pre-commit";
    public static final String DEFAULT_DOWNLOAD_ROOT = "https://github.com/pre-commit/pre-commit/releases/download/";
    private final PluginLog log;
    private final InstallConfig config;
    private final FileCopier fileCopier;
    private final FileDownloader fileDownloader;
    private final PythonHandle pythonHandle;
    private String version, downloadRoot;
    private String[] hookTypes;

    public BinaryInstaller(InstallConfig config, FileCopier fileCopier, FileDownloader fileDownloader, PythonHandle pythonHandle, PluginLog log) {
        this.log = log;
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

    public File ensureSetupFileAvailable() throws InstallationException {
        try {
            log.info("Ensuring pre-commit version " + version + " is available.");
            File setupFile = provisionSetupFile();
            if (!setupFile.exists()) {
                throw new InstallationException("Could not find " + setupFile.getName());
            }
            return setupFile;
        } catch (DownloadException e) {
            throw new InstallationException("Could not download pre-commit", e);
        } catch (FileCopyException e) {
            throw new InstallationException("Could not copy the pre-commit archive", e);
        }
    }

    public void install() throws InstallationException {
        try {
            log.info("Installing pre-commit version " + version + ".");
            File setupFile = ensureSetupFileAvailable();

            File precommitRoot = PrecommitProjectRoot.resolve(config.getWorkingDirectory());
            log.info("Installing Git hooks from " + precommitRoot + ".");
            pythonHandle.installPrecommit(setupFile, hookTypes, precommitRoot);

            log.info("Successfully installed pre-commit.");
        } catch (PythonException e) {
            throw new InstallationException("Python encountered an issue when installing the pre-commit binary", e);
        }
    }

    private File provisionSetupFile() throws DownloadException, FileCopyException {
        String downloadUrl = downloadRoot + version + "/";
        String extension = "pyz";
        String setupFileName = setupFileName(version);
        downloadUrl += setupFileName;

        CacheDescriptor cacheDescriptor = new CacheDescriptor("pre-commit", version, extension);
        File archive = config.getCacheResolver().resolve(cacheDescriptor);

        downloadFileIfMissing(downloadUrl, archive);

        File installDirectory = getInstallDirectory();
        copyFileIfMissing(archive, installDirectory);

        return new File(installDirectory, setupFileName);
    }

    static String setupFileName(String version) {
        return String.format("pre-commit-%s.pyz", version.replace("v", ""));
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            log.debug("Creating install directory " + installDirectory + ".");
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
                log.info("Copying " + archive + " into " + destinationDirectory + ".");
                fileCopier.copy(archive.getPath(), destinationDirectory.getPath());
            } else {
                log.info("File " + archive.getName() + " already in " + destinationDirectory + ", skipping copy.");
            }
        } catch (IOException e) {
            throw new FileCopyException("Failed to copy to destination directory");
        }
    }

    private void downloadFileIfMissing(String downloadUrl, File destination) throws DownloadException {
        withDownloadLock(destination, () -> {
            boolean fileExists = destination.exists();
            boolean fileValid = fileExists && validateFile(downloadUrl, destination);

            if (!fileExists || !fileValid) {
                if (fileExists) {
                    log.info("Removing invalid cached file " + destination + ".");
                    deleteQuietly(destination);
                }
                downloadFile(downloadUrl, destination);
            } else {
                log.info("File " + destination + " already exists, skipping download.");
            }
        });
    }

    private void downloadFile(String downloadUrl, File destination) throws DownloadException {
        File partFile = new File(destination.getPath() + ".part");
        deleteQuietly(partFile);

        log.info("Downloading " + downloadUrl + " to " + partFile + ".");
        fileDownloader.download(downloadUrl, partFile.getPath());

        if (!validateFile(downloadUrl, partFile)) {
            deleteQuietly(partFile);
            throw new DownloadException("Downloaded file checksum does not match.");
        }

        moveIntoPlace(partFile, destination);
    }

    private boolean validateFile(String downloadUrl, File destination) throws DownloadException {
        log.info("Verifying " + destination + " checksum.");
        File tempChecksum = null;
        try {
            tempChecksum = File.createTempFile("pre-commit-checksum-", ".sha256sum");
            fileDownloader.download(downloadUrl + ".sha256sum", tempChecksum.getPath());
            String hash = Files.asCharSource(tempChecksum, StandardCharsets.UTF_8)
                    .readFirstLine()
                    .split("\\s+")[0];
            String destinationHash = Files.asByteSource(destination).hash(Hashing.sha256()).toString();

            boolean hashEquals = hash.equals(destinationHash);

            if (hashEquals) {
                log.info(destination + " checksum verified.");
            } else {
                log.info(destination + " checksum doesn't match.");
            }

            return hashEquals;
        } catch (IOException e) {
            throw new DownloadException("Failed to verify downloaded file checksum.", e);
        } finally {
            deleteQuietly(tempChecksum);
        }
    }

    private void withDownloadLock(File destination, DownloadAction action) throws DownloadException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File lockFile = new File(parent, destination.getName() + ".lock");
        try (RandomAccessFile lockAccess = new RandomAccessFile(lockFile, "rw");
             FileChannel channel = lockAccess.getChannel();
             FileLock ignored = channel.lock()) {
            action.run();
        } catch (IOException e) {
            throw new DownloadException("Could not acquire download lock for " + destination, e);
        } finally {
            deleteQuietly(lockFile);
        }
    }

    private void moveIntoPlace(File partFile, File destination) throws DownloadException {
        try {
            java.nio.file.Files.move(
                    partFile.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException atomicMoveFailed) {
            try {
                java.nio.file.Files.move(
                        partFile.toPath(),
                        destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException e) {
                deleteQuietly(partFile);
                throw new DownloadException("Could not move downloaded file into place.", e);
            }
        }
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.debug("Could not delete " + file + ".");
        }
    }

    @FunctionalInterface
    private interface DownloadAction {
        void run() throws DownloadException;
    }
}
