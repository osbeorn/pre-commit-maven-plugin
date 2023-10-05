package io.github.osbeorn.maven.plugin.precommit.lib;

import java.io.File;

public interface CacheResolver {
  File resolve(CacheDescriptor cacheDescriptor);
}