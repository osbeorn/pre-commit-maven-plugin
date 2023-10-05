package io.github.osbeorn.maven.plugin.precommit.lib;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

class FileCopyException extends Exception {

	FileCopyException(String message) {
		super(message);
	}

	FileCopyException(String message, Throwable cause) {
		super(message, cause);
	}
}

interface FileCopier {
	void copy(String archive, String destinationDirectory) throws FileCopyException;
}

final class DefaultFileCopier implements FileCopier {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultFileCopier.class);

	private void prepDestination(File path, boolean directory) throws IOException {
		if (directory) {
			path.mkdirs();
		} else {
			if (!path.getParentFile().exists()) {
				path.getParentFile().mkdirs();
			}
			if (!path.getParentFile().canWrite()) {
				throw new AccessDeniedException(
						String.format("Could not get write permissions for '%s'", path.getParentFile().getAbsolutePath()));
			}
		}
	}


	@Override
	public void copy(String archive, String destinationDirectory) throws FileCopyException {
		final File archiveFile = new File(archive);
		final File destinationFile = new File(destinationDirectory + File.separator + archiveFile.getName());

		try (FileInputStream fis = new FileInputStream(archiveFile)) {
			prepDestination(destinationFile, false);
			FileUtils.copyFile(archiveFile, destinationFile);
		} catch (IOException e) {
			throw new FileCopyException("Could not copy archive: '" + archive + "'", e);
		}
	}
}