[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dhutchison_sonar-alloweddependencies-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=dhutchison_sonar-alloweddependencies-plugin)

This [SonarQube](http://www.sonarqube.org/) plugin ensures that projects in an organization adhere to a set of standard dependencies. This enables the governance of the used libraries and enforcement of an approved set of dependencies.

This plugin exposes rules for the following dependency descriptor files:
* NPM `package.json`
* Maven `pom.xml` / `.flattened-pom.xml` dependencies, plugins, and build
  extensions
* Maven `.mvn/extensions.xml` core extensions
* Python `pyproject.toml` and pip requirements files

This plugin does not:
* Check dependency licencies, [sonarqube-licensecheck](https://github.com/porscheinformatik/sonarqube-licensecheck) does that
* Check dependencies for vunlerabilities, [dependency-check-sonar-plugin](https://github.com/dependency-check/dependency-check-sonar-plugin) is an option for that

## License

This software is licensed under the [MIT License](https://spdx.org/licenses/MIT.html)

## Compatibility

This plugin is tested with SonarQube Community Build 26.4. It targets the modern
SonarQube plugin API and as of 1.0.0 is no longer intended to preserve compatibility with older
SonarQube versions.

## Installation

Download the latest (non-snapshot) version of the [package](https://github.com/dhutchison/sonar-alloweddependencies-plugin/packages/675558), then follow the standard SonarQube plugin manual installation steps for your SonarQube Community Build version.

## Usage

This plugin requires that the files to be scanned (e.g. `pom.xml`,
`.flattened-pom.xml`, `.mvn/extensions.xml`, `package.json`, `pyproject.toml` or
`requirements.txt`) are included in the sources path which SonarQube is
configured to analyse.

Python rules also require at least one `.py` file in the analyzed sources so
SonarQube detects the Python language, downloads the plugin, and runs the Python
dependency sensor.

Example in sonar-project.properties
```
sonar.sources=src,package.json
```

### NPM Rules

Three rules are made available in the `JSON` language by this plugin:
* Allowed Dependencies (NPM) - `allowed-dependencies-npm:allowed-dependencies-main`
* Allowed Development Dependencies (NPM) - `allowed-dependencies-npm:allowed-dependencies-dev`
* Allowed Peer Dependencies (NPM) - `allowed-dependencies-npm:allowed-dependencies-peer`

All of these take a configuration element for a newline separated list of dependencies which are allowed in a given scope (`dependencies`, `devDependencies` and `peerDependencies` respectively). When a rule in enabled for a scope, a rule violation will be raised for any dependencies which are not in the allowed list.

This plugin does not support:
* version numbers

Example configuration value for the `npmDependencies` parameter value:
```
# comments and blank lines are ignored
@angular-devkit/build-angular
regex:@angular-eslint/.*

@angular/cli
@angular/compiler-cli
@angular/language-service
@typescript-eslint/eslint-plugin
@typescript-eslint/parser
```

The default behaviour is to treat each row as an exact string match. Exact matches are case-insensitive. Rows can be prefixed with `regex:` to interpret them as a regular expression. Blank rows and rows starting with `#` are ignored. For example, `regex:@angular-eslint/.*` will allow all dependencies in the `@angular-eslint` scope.

### Maven Rules

Five rules are made available in the `XML` language by this plugin. Four of these are regular rules:
* Allowed Dependencies (Test Scope) - `allowed-dependencies-maven:maven-allowed-dependencies-test`
    * Applies to dependencies with a `test` scope only
* Allowed Dependencies (Main Scopes) - `allowed-dependencies-maven:maven-allowed-dependencies-main`
    * Applies to dependencies with one of the scopes `compile`, `provided`, `runtime`. Any dependency listed without an explicit scope defaults to `compile`
* Allowed Maven Plugins - `allowed-dependencies-maven:maven-allowed-plugins`
    * Applies to activating build and reporting plugin declarations, including
      declarations in inactive profiles
* Allowed Maven Extensions - `allowed-dependencies-maven:maven-allowed-extensions`
    * Applies to POM build extensions and core extensions in `.mvn/extensions.xml`
* Allowed Dependencies (template) - `allowed-dependencies-maven:maven-allowed-dependencies`
    * A template rule which allows custom rules to be created targeting a set list of scopes. This has an extra `mavenScopes` parameter for supplying a comma seperated list of scopes.

The three dependency rules take a configuration element for a newline separated
list of dependencies, as `groupId:artifactId` entries, which are allowed in the
scopes associated with the rule. The default behaviour is to treat each row as
an exact string match. Exact matches are case-insensitive. Rows can be prefixed
with `regex:` to interpret them as a regular expression. Blank rows and rows
starting with `#` are ignored. For example, `regex:org.\\junit\\.jupiter:.*`
will allow all dependencies with the `groupId` of `org.junit.jupiter`. When a
rule is enabled for a scope, a rule violation will be raised for any dependencies
which are not in the allowed list.

Where a project uses the [maven-flatten-plugin](https://www.mojohaus.org/flatten-maven-plugin/index.html), this plugin will scan the created `.flattened-pom.xml` file. Note that depending on the `flattenMode` value used this may lose valuable information. This has primarily been tested with `resolveCiFriendliesOnly` which only flattens version number properties.

This plugin does not support:
* version numbers


Example configuration for the `mavenDependencies` parameter:
```
# comments and blank lines are ignored
regex:org\.eclipse\.microprofile:.*

com.github.javafaker:javafaker
```

The `mavenPlugins` parameter uses the same newline-separated exact and `regex:`
syntax. Plugins are compared as `groupId:artifactId`; versions are ignored. A
plugin declaration or exact allow-list row without a group defaults to
`org.apache.maven.plugins`, so these entries are equivalent:

```text
maven-compiler-plugin
org.apache.maven.plugins:maven-compiler-plugin
```

Regex rows always match the full canonical coordinate. The plugin rule checks
`build/plugins` and `reporting/plugins` at project and profile level, regardless
of whether a profile is active during analysis. It deliberately ignores
`pluginManagement`, because those entries configure plugins without activating
them.

The `mavenExtensions` parameter requires full `groupId:artifactId` entries for
exact matches; extensions have no default group. It checks both
`build/extensions` in POM files and extension entries in
`.mvn/extensions.xml`. Versions are ignored for extensions too.

All Maven checks are source-based. They inspect only declarations physically
present in SonarQube-indexed project files; they do not resolve parent POMs or
construct Maven's effective model. Hidden and generated files may need to be
listed explicitly in `sonar.sources`. For example:

```properties
sonar.sources=src,pom.xml,.flattened-pom.xml,.mvn/extensions.xml
```

In particular, use the exact `.mvn/extensions.xml` path. Relying on
`sonar.sources=.` or an inclusion pattern can leave hidden XML files unavailable
to XML sensors, depending on the scanner used.

### Python Rules

Three rules are made available in the `Python` language by this plugin:
* Allowed Dependencies (Python Main) - `allowed-dependencies-python:python-allowed-dependencies-main`
    * Applies to `[project].dependencies`, `[tool.poetry.dependencies]` and `requirements.txt`
* Allowed Development Dependencies (Python) - `allowed-dependencies-python:python-allowed-dependencies-dev`
    * Applies to `[dependency-groups].dev`, `[tool.poetry.dev-dependencies]`, `[tool.poetry.group.dev.dependencies]`, `requirements-dev.txt` and `dev-requirements.txt`
* Allowed Dependencies (Python template) - `allowed-dependencies-python:python-allowed-dependencies`
    * A template rule for custom Poetry groups, PEP 735 dependency groups, and explicit requirements files. The `pythonDependencyGroups` parameter supplies a comma separated list of group names, while `pythonRequirementsFiles` supplies a comma separated list of file paths.

These rules take a `pythonDependencies` configuration element containing a newline separated list of allowed Python package names. Python package names are normalized before exact matching, so `requests-extra`, `requests_extra` and `requests.extra` are treated as the same package name. Rows can be prefixed with `regex:` to interpret them as a regular expression. Blank rows and rows starting with `#` are ignored.

The Python rules ignore version numbers and compare only package names. Poetry's `python` interpreter constraint is ignored. Requirement-file includes using `-r` and `--requirement` are followed when the included files are part of the scanned sources. Constraint files referenced using `-c` or `--constraint` are not analyzed because they restrict versions rather than declare direct dependencies. For development requirements, an include of `requirements.txt` is treated as belonging to the main rule and is not reported by the dev rule.

Example configuration for the `pythonDependencies` parameter:
```
# comments and blank lines are ignored
requests
fastapi

regex:^types-.*
```

Example template rule configuration:
```
pythonDependencyGroups: docs, lint
pythonRequirementsFiles: requirements-tools.txt, config/requirements-audit.txt
```

## Upgrading from older versions

Older versions registered the NPM rules under the JavaScript language and the
Maven rules under the Java language. Current versions register these descriptor
file rules under JSON and XML respectively. Existing quality profile activations
must be recreated in the JSON and XML quality profiles after upgrading.

## Development

Run the unit and package verification suite with:
```
mvn verify
```

Run the additional Failsafe packaging checks with:
```
mvn verify -Pintegration-tests
```

An optional black-box test deploys the built plugin to a disposable SonarQube
container and verifies the NPM, Maven POM, Maven flattened POM, Maven plugin and
extension, non-POM XML, and Python fixed and template rules through the
SonarQube Web API:

```bash
src/it/sonarqube/run-e2e.sh
```

Set `E2E_KEEP_RUNNING=true` to leave SonarQube running after the suite for
manual visual checks. The script prints the exact `docker compose down` command
to stop and remove the container and volumes when you are done.

Install pre-commit hooks with:
```
pre-commit install
```

The pre-commit configuration runs `actionlint` when GitHub Actions workflow files are modified.

## Releasing

Releases are published by manually running the `Build` GitHub Actions workflow.

Before starting a release:
* Ensure the release changes are merged to `main`
* Ensure the `main` branch build is passing
* Choose the Maven release version, without a leading `v`, for example `1.0.0`

To publish a release:
1. Open the `Build` workflow in GitHub Actions
2. Select `Run workflow`
3. Enter the `releaseversion` value, for example `1.0.0`
4. Run the workflow from the `main` branch

The release job runs `mvn release:prepare`, then `mvn release:perform`. It creates and pushes the Maven release tag as `v<releaseversion>`, publishes the release artifact to GitHub Packages, and creates a GitHub Release with the plugin jar attached.

Snapshot builds are published automatically to GitHub Packages when changes are pushed to `main`.

After building the plugin jar, a pinned SonarQube Community Build 26.4 instance
can be started for manual smoke testing with:
```
docker compose -f src/it/sonarqube/docker-compose.yaml up
```
