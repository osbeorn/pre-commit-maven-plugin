package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;

public final class PluginFactory {
    private static final String DEFAULT_CACHE_PATH = "cache";

    private final File workingDirectory;
    private final File installDirectory;
    private final CacheResolver cacheResolver;
    private final PluginLog log;
    private final TrustedInsecureHosts trustedInsecureHosts;

    public PluginFactory(
            File workingDirectory,
            File installDirectory,
            CacheResolver cacheResolver,
            PluginLog log
    ) {
        this(workingDirectory, installDirectory, cacheResolver, log, null);
    }

    public PluginFactory(
            File workingDirectory,
            File installDirectory,
            CacheResolver cacheResolver,
            PluginLog log,
            String trustedInsecureHosts
    ) {
        this.workingDirectory = workingDirectory;
        this.installDirectory = installDirectory;
        this.cacheResolver = cacheResolver;
        this.log = log == null ? PluginLogs.silent() : log;
        this.trustedInsecureHosts = TrustedInsecureHosts.parse(trustedInsecureHosts);
    }

    public BinaryInstaller getBinaryInstaller() {
        return new BinaryInstaller(
                getInstallConfig(),
                new DefaultFileCopier(),
                new DefaultFileDownloader(log, trustedInsecureHosts),
                new DefaultPythonHandle(log),
                log
        );
    }

    public BinaryRunner getBinaryRunner() {
        return new BinaryRunner(
                getInstallConfig(),
                new DefaultFileCopier(),
                new DefaultFileDownloader(log, trustedInsecureHosts),
                new DefaultPythonHandle(log),
                log
        );
    }

    private InstallConfig getInstallConfig() {
        return new DefaultInstallConfig(installDirectory, workingDirectory, cacheResolver);
    }

    private static CacheResolver getDefaultCacheResolver(File root) {
        return new DirectoryCacheResolver(new File(root, DEFAULT_CACHE_PATH));
    }
}
