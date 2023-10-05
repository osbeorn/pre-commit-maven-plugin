package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;

public final class PluginFactory {
    private static final String DEFAULT_CACHE_PATH = "cache";

    private final File workingDirectory;
    private final File installDirectory;
    private final CacheResolver cacheResolver;

    public PluginFactory(File workingDirectory, File installDirectory) {
        this(workingDirectory, installDirectory, getDefaultCacheResolver(installDirectory));
    }

    public PluginFactory(File workingDirectory, File installDirectory, CacheResolver cacheResolver) {
        this.workingDirectory = workingDirectory;
        this.installDirectory = installDirectory;
        this.cacheResolver = cacheResolver;
    }

    public BinaryInstaller getBinaryInstaller() {
        return new BinaryInstaller(getInstallConfig(), new DefaultFileCopier(), new DefaultFileDownloader(),
                new DefaultPythonHandle());
    }

    public BinaryRunner getBinaryRunner() {
        return new BinaryRunner(getInstallConfig(), new DefaultPythonHandle());
    }

    private InstallConfig getInstallConfig() {
        return new DefaultInstallConfig(installDirectory, workingDirectory, cacheResolver);
    }

    private static CacheResolver getDefaultCacheResolver(File root) {
        return new DirectoryCacheResolver(new File(root, DEFAULT_CACHE_PATH));
    }
}
