# Python Dependency Rules Implementation Plan

## Summary

Add Python dependency allow-list support for Poetry `pyproject.toml`, PEP 621/735
`pyproject.toml`, and pip requirements files. The feature should follow the
existing plugin model: rule parameters stay quality-profile owned, rule keys are
stable, scanner issues point at descriptor lines, and manual scanner fixtures
are available for live validation.

## Key Changes

* Add `allowed-dependencies-python` under SonarQube language `py`.
* Add fixed main/dev rules and one template rule:
  * `python-allowed-dependencies-main`
  * `python-allowed-dependencies-dev`
  * `python-allowed-dependencies`
* Add `pythonDependencies` for newline-separated allow lists.
* Add `pythonDependencyGroups` for custom Poetry groups, PEP 735 groups, or
  explicit requirements filenames.
* Normalize Python package names for exact matching by lowercasing and
  collapsing `.`, `_`, and `-` runs to `-`.
* Use `org.tomlj:tomlj:1.1.1` for TOML parsing.
* Update plugin packaging so `Plugin-RequiredForLanguages` is `json,xml,py`.

## Supported Sources

Main rule:

* `[project].dependencies`
* `[tool.poetry.dependencies]`
* `requirements.txt`

Dev rule:

* `[dependency-groups].dev`
* `[tool.poetry.dev-dependencies]`
* `[tool.poetry.group.dev.dependencies]`
* `requirements-dev.txt`
* `dev-requirements.txt`

Template rule:

* `[dependency-groups].<group>`
* `[tool.poetry.group.<group>.dependencies]`
* explicit requirements filenames from `pythonDependencyGroups`

Shared behavior:

* Ignore Poetry's `python` interpreter constraint.
* Ignore versions and compare package names only.
* Expand PEP 735 `{include-group = "..."}` entries recursively with cycle
  detection.
* Follow pip `-r`, `--requirement`, `-c`, and `--constraint` includes
  recursively with cycle detection.
* In dev requirements files, skip includes of `requirements.txt` because those
  dependencies belong to the main rule.
* Skip invalid TOML or unreadable/unindexed include targets per file without
  aborting the scan.

## Tests

Unit and integration-level checks should cover:

* Python rule definitions, parameter descriptions, and template rule metadata.
* Plugin extension registration and manifest `Plugin-RequiredForLanguages`.
* Sensor descriptor language/repository configuration and no-rule no-op
  behavior.
* PEP 621 `[project].dependencies`.
* Poetry main, legacy dev, modern dev group, and custom groups.
* PEP 735 dev/custom groups, include expansion, and cycle detection.
* Requirements main/dev files, both dev filename conventions, include traversal,
  constraints, cycles, comments, extras, markers, editable/direct references,
  hashes/options, and dev-to-main delegation.
* Python package name normalization and existing exact/regex allow-list behavior.
* Manual scanner fixtures for pyproject and pip requirements projects.

Acceptance command:

```bash
mvn verify -Pintegration-tests
```

## Manual Validation Fixtures

Add scanner fixtures under `src/it/scanner-fixtures`:

* `python-pyproject` with `[project].dependencies`, Poetry dependencies, PEP 735
  groups, and a small `.py` file.
* `python-pip` with `requirements.txt`, `requirements-dev.txt`,
  `dev-requirements.txt`, include files, and a small `.py` file.

Update `src/it/scanner-fixtures/README.md` with Python profile setup, Docker
scanner commands, allow-list values, and expected issues.

## Assumptions

* Python rules live in Python quality profiles.
* Python scanner fixtures include a `.py` source file so SonarQube detects
  Python and downloads the plugin.
* `[project.optional-dependencies]` is out of scope unless later mapped to
  template groups.
* Allow lists remain SonarQube rule parameters rather than repository files.

