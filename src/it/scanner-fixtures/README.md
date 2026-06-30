# Scanner Fixtures

These fixtures are small projects for manual validation against a local SonarQube
Community Build instance running this plugin.

## Automated E2E Suite

The opt-in suite builds the plugin, starts a disposable SonarQube container,
provisions JSON, XML, and Python quality profiles through the Web API, scans the
NPM, Maven POM, Maven flattened POM, Maven core-extension, non-POM XML, and
Python fixtures, and
asserts the exact plugin issues returned by SonarQube:

```bash
src/it/sonarqube/run-e2e.sh
```

It requires Maven, Docker Compose, `curl`, and `jq`. The suite removes its
containers and volumes when it finishes. It prints a fixture-by-fixture result
table and writes scanner logs, assertion details, and the SonarQube log to
`target/e2e-reports`, including the console report as `summary.txt`.
To reuse an already-built plugin JAR while developing the harness:

```bash
E2E_SKIP_BUILD=true src/it/sonarqube/run-e2e.sh
```

To leave SonarQube running after the suite for manual visual checks:

```bash
E2E_KEEP_RUNNING=true src/it/sonarqube/run-e2e.sh
```

When this is enabled, the script prints the exact `docker compose down` command
to stop and remove the container and volumes when you are done. It also prints
the local administrator credentials needed to log in to the retained instance.

The same suite can be run manually from the `SonarQube E2E` GitHub Actions
workflow.

The suite also creates a Python rule from the template, selecting the `docs`
dependency group and `requirements-template.txt`. Its exact issue assertions
verify both configured sources and ensure the custom rule does not also scan the
main or development defaults.

## Start SonarQube

Build the plugin and start the local SonarQube instance:

```bash
mvn verify
export SONAR_PLUGIN_JAR="$PWD/target/$(mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout).jar"
docker compose -f src/it/sonarqube/docker-compose.yaml up
```

If you rebuild the plugin jar while SonarQube is already running, restart
SonarQube before scanning. SonarQube reads plugin metadata at startup, and a
scanner can fail with a plugin checksum mismatch if the mounted jar changes
after startup:

```bash
mvn verify
export SONAR_PLUGIN_JAR="$PWD/target/$(mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout).jar"
docker compose -f src/it/sonarqube/docker-compose.yaml down
docker compose -f src/it/sonarqube/docker-compose.yaml up
```

If SonarQube fails with an old embedded H2 database in the Docker volume, reset
the disposable local data volume:

```bash
docker compose -f src/it/sonarqube/docker-compose.yaml down -v
docker compose -f src/it/sonarqube/docker-compose.yaml up
```

## Quality Profile Setup

Create or update local JSON and XML quality profiles, then activate these rules.
The fixtures below intentionally include both allowed and forbidden dependencies.

### JSON Profile

Activate:

* `allowed-dependencies-npm:allowed-dependencies-main`
* `allowed-dependencies-npm:allowed-dependencies-dev`
* `allowed-dependencies-npm:allowed-dependencies-peer`

Use this value for each rule's `npmDependencies` parameter:

```text
# exact matches
@acme/ui
eslint
react

# regex matches
regex:^@acme/.*
```

### XML Profile

Activate:

* `allowed-dependencies-maven:maven-allowed-dependencies-main`
* `allowed-dependencies-maven:maven-allowed-dependencies-test`
* `allowed-dependencies-maven:maven-allowed-plugins`
* `allowed-dependencies-maven:maven-allowed-extensions`

Use this value for the main-scopes rule's `mavenDependencies` parameter:

```text
# exact matches
org.slf4j:slf4j-api
com.acme:platform-core

# regex matches
regex:^com\.acme:.*$
```

Use this value for the test-scope rule's `mavenDependencies` parameter:

```text
org.junit.jupiter:junit-jupiter-api
regex:^org\.assertj:.*$
```

Use this value for the `mavenPlugins` parameter:

```text
maven-compiler-plugin
regex:^org\.codehaus\.mojo:build-helper-.*$
```

Use this value for the `mavenExtensions` parameter:

```text
com.acme:allowed-extension
regex:^com\.trusted:.*$
```

### Python Profile

Activate:

* `allowed-dependencies-python:python-allowed-dependencies-main`
* `allowed-dependencies-python:python-allowed-dependencies-dev`

Use this value for the main rule's `pythonDependencies` parameter:

```text
requests
fastapi
urllib3
```

Use this value for the dev rule's `pythonDependencies` parameter:

```text
pytest
ruff
mypy
sphinx
editable-package
```

## Run Scans With Docker

Create a local token in SonarQube, then run the scanner from Docker. The
commands mount the current fixture directory into the scanner container and join
the Docker Compose network used by the local SonarQube instance.

```bash
cd src/it/scanner-fixtures/npm-package
docker run --rm \
  --network sonarqube_default \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=<token>

cd ../maven-pom
docker run --rm \
  --network sonarqube_default \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=<token>

cd ../maven-flattened-pom
mvn process-resources org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<token> \
  -Dsonar.qualitygate.wait=true
```

This fixture intentionally sets `sonar.sources=.flattened-pom.xml`. Using
`sonar.sources=.` with `sonar.inclusions=.flattened-pom.xml` can index the file
without exposing it to XML sensors.

```bash
cd ../maven-extensions-file
docker run --rm \
  --network sonarqube_default \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=<token>
```

This fixture intentionally sets `sonar.sources=.mvn/extensions.xml`. The
scanner does not reliably make hidden XML files available to XML sensors when
only a directory or inclusion pattern is configured, so projects that govern
core extensions should list this exact file in `sonar.sources`.

```bash
cd ../xml-non-pom
docker run --rm \
  --network sonarqube_default \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=<token>

cd ../python-pyproject
docker run --rm \
  --network sonarqube_default \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=<token>

cd ../python-pip
docker run --rm \
  --network sonarqube_default \
  -v "$PWD:/usr/src" \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=<token>
```

If the compose project name changes, the network may not be
`sonarqube_default`. List Docker networks with `docker network ls` and use the
network created for this compose file.

### Optional Local CLI

The native SonarScanner CLI also works if installed locally. Run it from each
fixture directory and point it at the published local SonarQube port:

```bash
sonar-scanner -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
```

Expected plugin issues:

* `npm-package`: `left-pad`, `jest`, and `@external/plugin` should be reported.
* `maven-pom`: the two forbidden dependencies, four forbidden active plugins,
  and one forbidden POM build extension should be reported; allowed
  shorthand/regex entries and both `pluginManagement` declarations should not
  be reported.
* `maven-flattened-pom`: `com.external:flattened-lib` and `org.hamcrest:hamcrest` should be reported.
* `maven-extensions-file`: `com.external:forbidden-core-extension` should be reported from `.mvn/extensions.xml`; the regex-allowed extension should not be reported.
* `xml-non-pom`: no allowed-dependencies plugin issues should be reported.
* `python-pyproject`: the fixed rules should report `external-main`, `external-poetry`, `external-dev`, `external-pep735-dev`, and `external-lint`; the template-derived docs rule should report `external-docs-poetry`, `external-docs-pep735`, and `external-lint`.
* `python-pip`: the fixed rules should report `external-main`, `external-shared`, `external-dev`, and `external-dev-shared`; the template-derived rule should report `external-template` from `requirements-template.txt`. The dev rule should not report dependencies reached through `-r requirements.txt`.
