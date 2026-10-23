# pre-commit-maven-plugin

> A Maven plugin for [the pre-commit framework](https://pre-commit.com/)

[pre-commit](https://pre-commit.com/) manages Git commit hooks. Normally each developer runs
`pre-commit install --install-hooks` by hand. This plugin runs that step during Maven builds
so hooks stay installed without an extra onboarding step.

## Usage

Add a [`.pre-commit-config.yaml`](https://pre-commit.com/#2-add-a-pre-commit-configuration) at your
**Git repository root** (the plugin walks up from `${basedir}` to find `.git` and the config file).

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>io.github.osbeorn</groupId>
    <artifactId>pre-commit-maven-plugin</artifactId>
    <version>LATEST_VERSION</version>
    <inherited>false</inherited>
    <configuration>
        <precommitVersion>v3.4.0</precommitVersion>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

To run hooks in CI (without waiting for a Git commit), bind or invoke the **`run`** goal:

```bash
mvn pre-commit:run -Dprecommit.allFiles=true
```

Replace `LATEST_VERSION` with a release from [Maven Central][releases] or a snapshot (see below).

### Reactor / multi-module builds

| Parameter | Default | Description |
|-----------|---------|-------------|
| `precommit.runOnlyAtExecutionRoot` | `true` | Run only on the top-level Maven project. Set to `false` if the plugin is bound on a submodule (e.g. `casino-api`) and you build from the repo root with `-pl`. |

Bind the plugin on the **aggregator** or a **single module**—not every module—to avoid redundant downloads and hook installs.

### Other hook types

```xml
<hookTypes>
    <hookType>pre-commit</hookType>
    <hookType>pre-push</hookType>
    <hookType>commit-msg</hookType>
</hookTypes>
```

See [supported Git hooks](https://pre-commit.com/#supported-git-hooks).

### Configuration parameters

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `precommitVersion` | `precommitVersion` | *(required)* | pre-commit release tag (usually with `v`, e.g. `v3.4.0`). |
| `downloadRoot` | `downloadRoot` | GitHub releases URL | Base URL for the `.pyz` download (use an internal mirror if needed). |
| `trustedInsecureHosts` | `trustedInsecureHosts` | *(empty)* | Comma-separated hosts for which TLS certificate validation is skipped on downloads (**exact** host match only). Use for internal HTTPS mirrors when the JDK trust store is outdated; logs a warning when used. |
| `workingDirectory` | `workingDirectory` | `${basedir}` | Starting directory when searching for `.git` and `.pre-commit-config.yaml`. |
| `installDirectory` | `installDirectory` | `${basedir}/.pre-commit-files` | Where the extracted pre-commit bundle is stored locally. |
| `hookTypes` | `hookTypes` | `pre-commit` | Hook types passed to `pre-commit install --hook-type`. |
| `skip` | `skip.install` | `false` | Skip the `install` goal entirely. |
| `runOnlyAtExecutionRoot` | `precommit.runOnlyAtExecutionRoot` | `true` | See reactor section above. |
| `skipIfMissingGitOrConfig` | `precommit.skipIfMissingGitOrConfig` | `true` | Skip (info log) when not in a Git repo or config is missing—useful for CI without `.git`. |
| `allFiles` (`run` goal) | `precommit.allFiles` | `false` | Pass `--all-files` to `pre-commit run` (check entire tree vs staged files only). |
| `hookIds` (`run` goal) | `hookIds` | *(empty)* | Optional hook ids; omit to run all hooks from `.pre-commit-config.yaml`. |

Download retries: `-Dprecommit.download.maxAttempts=N` (default `3`).

### Goals

| Goal | Default phase | Purpose |
|------|---------------|---------|
| `install` | `initialize` | Download pre-commit and run `pre-commit install --install-hooks`. |
| `run` | `verify` | Run `pre-commit run` (fails the build if hooks fail). |

### Skipping execution

| Goal | How to skip |
|------|-------------|
| `install` | `-Dskip.install=true` |
| `run` | `-Dskip.run=true` |

With default `precommit.skipIfMissingGitOrConfig=true`, the goal also skips quietly when there is no
`.git` or `.pre-commit-config.yaml` in the tree above `${basedir}`.

### Gitignore

The plugin stores the downloaded bundle under **`${installDirectory}/pre-commit/`** (default:
`.pre-commit-files/pre-commit/`). Add to `.gitignore`:

```gitignore
.pre-commit-files/
```

### Consuming snapshot builds

Snapshots are published to Central Portal (not searchable on [search.maven.org](https://search.maven.org)):

```xml
<pluginRepositories>
    <pluginRepository>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </pluginRepository>
</pluginRepositories>
```

Or mirror that repository in Nexus and point developers at your mirror.

### Requirements

- **Python 3.7+** on the PATH (`python3` or `python`) to run the downloaded pre-commit binary.
  Newer pre-commit versions may require Python 3.9+—match `precommitVersion` to what your team installs.
- **Java version of your project** does not affect the pre-commit binary; hooks that invoke Maven use your existing JDK.

## FAQ

### Hooks are outdated

If hook repos use a floating `rev` (e.g. `master`), clear the hook cache:

```bash
rm -rf ~/.cache/pre-commit/
```

### pre-commit install failed in Maven

Failed runs include **stderr/stdout** from the underlying `pre-commit install` process in the Maven
error message. Run the same command manually from the Git root if you need more context.

### Publishing (maintainers)

Releases and snapshots are published via [Central Publisher Portal](https://central.sonatype.com/)
using `central-publishing-maven-plugin`. CI uses a Portal **user token** with server id `central` in
`settings.xml`, including `repositoryOrigins` for `https://central.sonatype.com` (required for
Maven 3.9+ credential scoping).

[releases]: https://central.sonatype.com/artifact/io.github.osbeorn/pre-commit-maven-plugin
