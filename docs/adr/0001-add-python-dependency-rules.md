# 0001. Add Python Dependency Allow-List Rules

## Status

Accepted

## Context

The plugin currently supports dependency allow-list rules for NPM `package.json`
files and Maven `pom.xml` / `.flattened-pom.xml` files. Python projects need
the same governance model, but Python dependency metadata is spread across
multiple commonly used formats:

* PEP 621 project metadata in `pyproject.toml`
* Poetry dependency tables in `pyproject.toml`
* PEP 735 dependency groups in `pyproject.toml`
* pip requirements files

The existing plugin model stores allow lists as SonarQube rule parameters owned
by quality profiles. Keeping this model for Python allows profile owners to
manage policy consistently across ecosystems.

## Decision

Add a Python rule repository attached to the SonarQube Python language profile:

* Repository key: `allowed-dependencies-python`
* Language key: `py`
* Required plugin languages: `json,xml,py`

Add three rules:

* `allowed-dependencies-python:python-allowed-dependencies-main`
* `allowed-dependencies-python:python-allowed-dependencies-dev`
* `allowed-dependencies-python:python-allowed-dependencies`

The first two are fixed main/dev rules. The third is a template rule with a
`pythonDependencyGroups` parameter for custom Poetry groups, PEP 735 groups, or
explicit requirements filenames.

Use the existing newline-separated allow-list model with a new
`pythonDependencies` parameter. Exact Python package names are normalized before
comparison by lowercasing and collapsing `.`, `_`, and `-` runs to `-`. Regex
rows keep the existing `regex:` behavior and are matched against normalized
candidate names.

Support these dependency sources:

* Main rule: `[project].dependencies`, `[tool.poetry.dependencies]`, and
  `requirements.txt`
* Dev rule: `[dependency-groups].dev`, `[tool.poetry.dev-dependencies]`,
  `[tool.poetry.group.dev.dependencies]`, `requirements-dev.txt`, and
  `dev-requirements.txt`
* Template rule: `[dependency-groups].<group>`,
  `[tool.poetry.group.<group>.dependencies]`, and explicit requirements files

PEP 735 `{include-group = "..."}` entries and pip `-r` / `--requirement` and
`-c` / `--constraint` includes are followed recursively with cycle detection.
When a dev requirements file includes `requirements.txt`, that include is
treated as delegated to the main rule and is not traversed by the dev rule.

## Validation

The implementation includes focused unit coverage for rule definitions,
configuration, parsers, and sensors, plus an optional SonarQube end-to-end suite
that provisions quality profiles, scans representative fixtures, and asserts the
issues returned by SonarQube.

The SonarQube end-to-end suite covers the new Python `pyproject.toml` and pip
requirements fixtures, and also keeps coverage for the existing NPM, Maven POM,
Maven `.flattened-pom.xml`, and non-POM XML descriptor behavior. The flattened
POM fixture documents a scanner requirement discovered during implementation:
generated `.flattened-pom.xml` files must be listed directly in `sonar.sources`
to be exposed to the XML/plugin sensors in this harness.

## Consequences

Positive:

* Python projects can use the same quality-profile-owned allow-list model as
  Maven and NPM projects.
* Common modern Python dependency metadata formats are supported from the first
  Python release.
* Python package normalization avoids surprising mismatches between equivalent
  package spellings.
* Optional black-box SonarQube validation gives release maintainers a way to
  test the plugin against scanner and server behavior before deployment.

Tradeoffs:

* Rules must be activated in Python quality profiles, and scanner fixtures need
  at least one `.py` file so SonarQube detects Python and downloads the plugin.
* `requirements.txt` parsing is necessarily heuristic because the format allows
  many pip options and direct references.
* Included requirements files can only be reported when they are part of the
  SonarQube indexed sources.
* `[project.optional-dependencies]` is intentionally out of scope for this
  decision unless it is later mapped to template groups.

## Alternatives Considered

* Attach rules to a descriptor language instead of Python. This was rejected
  because `pyproject.toml` and requirements files are not first-class supported
  SonarQube languages in the same way JSON and XML are for the existing
  descriptor rules.
* Store allow lists in repository files. This was rejected to keep policy owned
  by SonarQube quality profiles.
* Only support Poetry or only support pip initially. This was rejected because
  both formats are common enough that partial Python support would be hard to
  validate manually.
* Avoid following requirements includes. This was rejected because split
  requirements files are common, especially for dev and constraint files.
