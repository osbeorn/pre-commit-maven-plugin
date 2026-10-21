package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;

public class DirectoryCacheResolver implements CacheResolver {
    private final File cacheDirectory;

    public DirectoryCacheResolver(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public File resolve(CacheDescriptor cacheDescriptor) {
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }

        String version = cacheDescriptor.getVersion().replaceFirst("^v", "");
        StringBuilder filename = new StringBuilder()
                .append(cacheDescriptor.getName())
                .append("-")
                .append(version);

        if (cacheDescriptor.getClassifier() != null) {
            filename.append("-").append(cacheDescriptor.getClassifier());
        }

        filename.append(".").append(cacheDescriptor.getExtension());

        return new File(cacheDirectory, filename.toString());
    }
}