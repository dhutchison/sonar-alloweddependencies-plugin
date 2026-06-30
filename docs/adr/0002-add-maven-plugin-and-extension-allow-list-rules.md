# 0002. Add Maven Plugin and Extension Allow-List Rules

## Status

Accepted

## Context

[Issue #16](https://github.com/dhutchison/sonar-alloweddependencies-plugin/issues/16)
requests governance for the Maven plugins used by a project. The existing Maven
rules govern dependency coordinates by scanning indexed `pom.xml` and
`.flattened-pom.xml` files, but Maven plugins and extensions also load executable
code into a build. They require their own policies because they have different
declaration and activation semantics from project dependencies and from each
other.

The analysis remains source-based rather than an evaluation of Maven's effective
model. It must produce the same findings regardless of which profiles happen to
be active in the environment running SonarQube analysis.

## Decision

Add two non-template rules to the existing `allowed-dependencies-maven` XML rule
repository:

* `allowed-dependencies-maven:maven-allowed-plugins`, named **Allowed Maven
  Plugins**, with a `mavenPlugins` text parameter
* `allowed-dependencies-maven:maven-allowed-extensions`, named **Allowed Maven
  Extensions**, with a `mavenExtensions` text parameter

Both parameters use the existing newline-separated allow-list conventions:
case-insensitive exact matches, `regex:` rows, comments beginning with `#`, and
ignored blank rows. Regexes match the canonical `groupId:artifactId` coordinate.
Versions are deliberately ignored, as they are for every other rule in this
plugin.

### Maven plugins

The plugin rule checks declarations in all of these POM locations:

* `/project/build/plugins/plugin`
* `/project/profiles/profile/build/plugins/plugin`
* `/project/reporting/plugins/plugin`
* `/project/profiles/profile/reporting/plugins/plugin`

Every profile declaration is checked whether or not its profile is active during
analysis. All declarations below `pluginManagement` are excluded because they
supply defaults without activating a plugin.

Plugin identity is the canonical `groupId:artifactId` coordinate. When a plugin
declaration omits `groupId`, it is normalized to Maven's default
`org.apache.maven.plugins` group. Exact allow-list rows may likewise omit the
group, so these entries are equivalent:

```text
maven-compiler-plugin
org.apache.maven.plugins:maven-compiler-plugin
```

Artifact-only shorthand is not applied to regex rows: regexes always match the
canonical full coordinate. Each unapproved declaration produces an issue on its
`plugin` element with this message:

```text
Remove this forbidden Maven plugin: <groupId>:<artifactId>.
```

### Maven extensions

The extension rule checks both project-controlled coordinate-based extension
mechanisms:

* `/project/build/extensions/extension` in `pom.xml` and `.flattened-pom.xml`
* `/extensions/extension` in `.mvn/extensions.xml`

Extension exact allow-list rows always require the full `groupId:artifactId`
coordinate because Maven extensions have no equivalent default group. Each
unapproved declaration produces an issue on its `extension` element with this
message:

```text
Remove this forbidden Maven extension: <groupId>:<artifactId>.
```

The `maven.ext.class.path` mechanism is outside this decision because it refers
to filesystem paths rather than Maven coordinates.

### Source and inheritance boundary

Only declarations physically present in SonarQube-indexed project files are
checked. The implementation will not resolve parent POMs, download artifacts,
construct an effective Maven model, or read files outside SonarQube's indexed
filesystem. A parent project's declarations are expected to be governed when
that parent project is scanned.

If `.mvn/extensions.xml` is not indexed by the Maven scanner's normal
configuration, users must include it explicitly in `sonar.sources`, as is
already required for generated `.flattened-pom.xml` files in some scanner
configurations. The end-to-end work will establish the scanner behavior and the
README will document the proven configuration.

## Implementation Plan

1. Extend `MavenRulesDefinition` with the two rule keys and parameters. Keep the
   existing repository, severity, remediation model, allow-list syntax, and
   tags, while giving plugins and extensions accurate descriptions and tags.
2. Introduce Maven coordinate matching that can share the existing predicate
   behavior. Apply default-group normalization only to plugin declarations and
   exact plugin allow-list rows; keep extension rows fully qualified and keep
   regex matching against canonical coordinates.
3. Add separate plugin and extension check/configuration types. Use explicit
   POM paths rather than a broad `//plugins/plugin` expression so that
   `pluginManagement` cannot be reported accidentally. Make checks file-aware so
   `.mvn/extensions.xml` is interpreted as an extension descriptor rather than
   as a POM.
4. Refactor `CreateIssuesOnMavenDependenciesSensor` into a shared Maven
   governance sensor. It will dispatch each active Maven rule to the matching
   dependency, plugin, or extension check; discover `pom.xml`,
   `.flattened-pom.xml`, and `.mvn/extensions.xml`; parse each indexed XML file
   once; and run only the checks applicable to that file type. Update plugin
   registration to use the renamed sensor.
5. Preserve the existing dependency rule keys, parameters, matching behavior,
   and issue messages during the refactor. Unsupported Maven repository rule
   keys should fail clearly during check construction rather than being treated
   as dependency rules.
6. Update the README with the two rules, coordinate examples, default plugin
   group shorthand, checked and excluded declaration locations, version and
   inheritance limitations, and the verified source-inclusion instructions for
   `.mvn/extensions.xml`.

## Validation

Focused unit tests will cover:

* registration and parameters for both new rules
* build, profile, and reporting plugin declarations
* exclusion of project and profile `pluginManagement`
* profile-independent findings
* default plugin group normalization in declarations and exact allow-list rows
* canonical full-coordinate regex matching
* POM build extensions and `.mvn/extensions.xml` core extensions
* ignored versions, comments, blank rows, and case-insensitive exact matches
* type-specific issue counts, messages, and XML locations
* shared-sensor dispatch with each combination of active Maven rule types
* continued behavior of all existing Maven dependency rules

The SonarQube end-to-end suite will activate the dependency, plugin, and
extension rules independently and assert exact issue sets for representative
POM and `.mvn/extensions.xml` fixtures. It will specifically validate whether
the Maven scanner indexes `.mvn/extensions.xml` by default. If explicit source
configuration is required, the fixture and user documentation will contain the
same proven setting. Existing POM, flattened-POM, and non-POM XML assertions
remain in the suite as regression coverage for the shared-sensor refactor.

## Consequences

Positive:

* Organizations can govern executable Maven build tooling independently from
  project libraries.
* CI-only, local-only, and reporting profiles receive consistent governance.
* Plugin shorthand behaves like Maven while findings and regexes use an
  unambiguous canonical coordinate.
* A shared sensor avoids repeated POM discovery and parsing as Maven governance
  rules expand.

Tradeoffs:

* The rules cannot report inherited declarations unless the declaring parent
  POM is itself scanned.
* Hidden or generated XML files may need explicit SonarQube source inclusion.
* Version approval, Maven lifecycle defaults, CLI-injected extensions, and
  effective-model resolution remain outside the plugin's scope.
* Refactoring the existing Maven sensor increases the regression surface, so
  retaining dependency unit and end-to-end coverage is required.

## Alternatives Considered

* Add plugin and extension coordinates to the existing dependency rules. This
  was rejected because the policies have different activation semantics and
  need to be independently enabled and configured in quality profiles.
* Treat extensions as plugins. This was rejected because Maven models them as a
  separate executable mechanism and users may approve different coordinate
  sets.
* Check `pluginManagement`. This was rejected because it configures potential
  plugin use rather than activating a plugin.
* Inspect only profiles active during analysis. This was rejected because
  findings would then vary between local and CI environments and could miss
  plugins that execute elsewhere.
* Resolve Maven's effective model. This was rejected because it introduces
  parent resolution, network/repository behavior, and duplicate ownership of
  findings, conflicting with the existing indexed-source analysis model.
* Add one sensor per rule family. This was rejected because each sensor would
  rediscover and reparse the same POM files.
* Read `.mvn/extensions.xml` directly from disk when SonarQube does not index it.
  This was rejected because it bypasses SonarQube's source boundaries and makes
  reliable issue locations impossible.
