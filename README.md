[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dhutchison_sonar-alloweddependencies-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=dhutchison_sonar-alloweddependencies-plugin)

This [SonarQube](http://www.sonarqube.org/) plugin ensures that projects in an organization adhere to a set of standard dependencies. This enables the governance of the used libraries and enforcement of an approved set of dependencies.

This plugin exposes rules for the following dependency descriptor files:
* NPM `package.json`
* Maven `pom.xml` / `.flattened-pom.xml`

This plugin does not:
* Check dependency licencies, [sonarqube-licensecheck](https://github.com/porscheinformatik/sonarqube-licensecheck) does that
* Check dependencies for vunlerabilities, [dependency-check-sonar-plugin](https://github.com/dependency-check/dependency-check-sonar-plugin) is an option for that

## License

This software is licensed under the [MIT License](https://spdx.org/licenses/MIT.html)

## Compatibility

This plugin is compatible with SonarQube >= 7.9 LTS.

## Installation

Download the latest (non-snapshot) version of the [package](https://github.com/dhutchison/sonar-alloweddependencies-plugin/packages/675558), then follow the standard SonarQube plugin [Manual Installation steps](https://docs.sonarqube.org/7.9/setup/install-plugin/).

## Usage

This plugin requires that the files to be scanned (e.g. `pom.xml`, `.flattened-pom.xml` or `package.json`) is included in the sources path which SonarQube is configured to analyse.

Example in sonar-project.properties
```
sonar.sources=src,package.json
```

### NPM Rules

Three rules are made available in the `JavaScript` language by this plugin:
* Allowed Dependencies (NPM) - `allowed-dependencies-npm:allowed-dependencies-main`
* Allowed Development Dependencies (NPM) - `allowed-dependencies-npm:allowed-dependencies-dev`
* Allowed Peer Dependencies (NPM) - `allowed-dependencies-npm:allowed-dependencies-peer`

All of these take a configuration element for a newline seperated list of dependencies which are allowed in a given scope (`dependencies`, `devDependencies` and `peerDependencies` respectively). When a rule in enabled for a scope, a rule violation will be raised for any dependencies which are not in the allowed list.

This plugin does not support:
* version numbers
* identifying the line of the `package.json` file that the violation occured at

Example configuration value for the `npmDependencies` parameter value:
```
@angular-devkit/build-angular
@angular-eslint/builder
@angular-eslint/eslint-plugin
@angular-eslint/eslint-plugin-template
@angular-eslint/schematics
@angular-eslint/template-parser
@angular/cli
@angular/compiler-cli
@angular/language-service
@typescript-eslint/eslint-plugin
@typescript-eslint/parser
```

The default behaviour is to treat each row as an exact string match. Rows can be prefixed with `regex:` to interpret them as a regular expression. For example, `regex:@angular-eslint/.*` will allow all dependencies in the `@angular-eslint` scope.

### Maven Rules

Three rules are made available in the `Java` language by this plugin. Two of these are regular rules:
* Allowed Dependencies (Test Scope) - `allowed-dependencies-maven:maven-allowed-dependencies-test`
    * Applies to dependencies with a `test` scope only
* Allowed Dependencies (Main Scopes) - `allowed-dependencies-maven:maven-allowed-dependencies-main`
    * Applies to dependencies with one of the scopes `compile`, `provided`, `runtime`. Any dependency listed without an explicit scope defaults to `compile`
* Allowed Dependencies (template) - `allowed-dependencies-maven:maven-allowed-dependencies`
    * A template rule which allows custom rules to be created targeting a set list of scopes. This has an extra `mavenScopes` parameter for supplying a comma seperated list of scopes.

Both of these take a configuration element for a newline seperated list of dependencies, as `groupId:artifactId` entries, which are allowed in the scopes associated with the rule. The default behaviour is to treat each row as an exact string match. Rows can be prefixed with `regex:` to interpret them as a regular expression. For example, `regex:org.\\junit\\.jupiter:.*` will allow all dependencies with the `groupId` of `org.junit.jupiter`. When a rule in enabled for a scope, a rule violation will be raised for any dependencies which are not in the allowed list.

Where a project uses the [maven-flatten-plugin](https://www.mojohaus.org/flatten-maven-plugin/index.html), this plugin will scan the created `.flattened-pom.xml` file. Note that depending on the `flattenMode` value used this may lose valuable information. This has primarily been tested with `resolveCiFriendliesOnly` which only flattens version number properties.

This plugin does not support:
* version numbers


Example configuration for the `mavenDependencies` parameter:
```
regex:org\.eclipse\.microprofile:.*
com.github.javafaker:javafaker
```
