#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
readonly COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yaml"
readonly COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-alloweddependencies-e2e}"
readonly SONARQUBE_PORT="${SONARQUBE_PORT:-19000}"
readonly SONARQUBE_URL="http://localhost:${SONARQUBE_PORT}"
readonly SONARQUBE_CONTAINER_URL="http://sonarqube:9000"
readonly SCANNER_IMAGE="${SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:12.1.0.3233_8.0.1}"
readonly E2E_DOCKER_CONFIG="${REPOSITORY_ROOT}/target/e2e-docker-config"
readonly REPORT_DIR="${REPOSITORY_ROOT}/target/e2e-reports"
readonly WORK_DIR="${REPOSITORY_ROOT}/target/e2e-work"
readonly ADMIN_USERNAME="admin"
readonly ADMIN_PASSWORD="AllowedDependencies-E2E-Admin-42!"

SONAR_TOKEN=""
PROFILE_KEY=""
RESULT_FAILURES=0
RESULT_FIXTURES=()
RESULT_SCANS=()
RESULT_EXPECTED=()
RESULT_ACTUAL=()
RESULT_ASSERTIONS=()
RESULT_DETAILS=()

log() {
    printf '[e2e] %s\n' "$*"
}

fail() {
    printf '[e2e] ERROR: %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

compose() {
    docker compose --project-name "${COMPOSE_PROJECT_NAME}" -f "${COMPOSE_FILE}" "$@"
}

api() {
    local method="$1"
    local path="$2"
    shift 2

    curl --fail-with-body --silent --show-error \
        --request "${method}" \
        --user "${SONAR_TOKEN}:" \
        "${SONARQUBE_URL}${path}" \
        "$@"
}

cleanup() {
    local exit_code=$?
    compose logs --no-color sonarqube >"${REPORT_DIR}/sonarqube.log" 2>&1 || true
    if [[ ${E2E_KEEP_RUNNING:-false} == "true" ]]; then
        log "SonarQube has been left running at ${SONARQUBE_URL}"
        log "Log in with username '${ADMIN_USERNAME}' and password '${ADMIN_PASSWORD}'"
        log "Stop and remove it with:"
        log "SONAR_PLUGIN_JAR=\"${SONAR_PLUGIN_JAR}\" SONARQUBE_PORT=\"${SONARQUBE_PORT}\" docker compose --project-name \"${COMPOSE_PROJECT_NAME}\" -f \"${COMPOSE_FILE}\" down --volumes --remove-orphans"
    else
        compose down --volumes --remove-orphans >/dev/null 2>&1 || true
    fi
    rm -rf "${E2E_DOCKER_CONFIG}"
    exit "${exit_code}"
}

prepare_docker_config() {
    rm -rf "${REPORT_DIR}"
    rm -rf "${WORK_DIR}"
    mkdir -p "${REPORT_DIR}"
    mkdir -p "${WORK_DIR}"
    mkdir -p "${E2E_DOCKER_CONFIG}"
    printf '{"auths":{}}\n' >"${E2E_DOCKER_CONFIG}/config.json"
    export DOCKER_CONFIG="${E2E_DOCKER_CONFIG}"
}

wait_for_sonarqube() {
    local deadline=$((SECONDS + 180))
    local status=""

    until [[ ${SECONDS} -ge ${deadline} ]]; do
        status="$(curl --silent "${SONARQUBE_URL}/api/system/status" | jq -r '.status // empty')" || true
        if [[ ${status} == "UP" ]]; then
            return
        fi
        sleep 2
    done

    fail "SonarQube did not become ready within 180 seconds"
}

create_token() {
    curl --fail-with-body --silent --show-error \
        --user "${ADMIN_USERNAME}:admin" \
        --request POST \
        --data-urlencode "login=${ADMIN_USERNAME}" \
        --data-urlencode "previousPassword=admin" \
        --data-urlencode "password=${ADMIN_PASSWORD}" \
        "${SONARQUBE_URL}/api/users/change_password" >/dev/null

    SONAR_TOKEN="$(curl --fail-with-body --silent --show-error \
        --user "${ADMIN_USERNAME}:${ADMIN_PASSWORD}" \
        --request POST \
        --data-urlencode "name=allowed-dependencies-e2e-${RANDOM}" \
        "${SONARQUBE_URL}/api/user_tokens/generate" | jq -r '.token')"

    [[ -n ${SONAR_TOKEN} && ${SONAR_TOKEN} != "null" ]] || fail "Failed to create SonarQube token"
}

assert_plugin_registration() {
    api GET "/api/plugins/installed" |
        jq --exit-status 'any(.plugins[]; .key == "alloweddependencies")' >/dev/null ||
        fail "The alloweddependencies plugin was not registered by SonarQube"

    local repository rule_count
    for repository in \
        allowed-dependencies-npm \
        allowed-dependencies-maven \
        allowed-dependencies-python; do
        rule_count="$(api GET "/api/rules/search?repositories=${repository}&ps=10" |
            jq -r '.total')"
        [[ ${rule_count} == "3" ]] ||
            fail "Expected 3 rules in ${repository}, but SonarQube registered ${rule_count}"
    done
}

create_profile() {
    local language="$1"
    local profile_name="$2"
    PROFILE_KEY="$(api POST "/api/qualityprofiles/create" \
        --data-urlencode "language=${language}" \
        --data-urlencode "name=${profile_name}" | jq -r '.profile.key')"

    [[ -n ${PROFILE_KEY} && ${PROFILE_KEY} != "null" ]] ||
        fail "Failed to create ${language} quality profile"
}

activate_rule() {
    local rule="$1"
    local parameter="$2"
    local value="$3"
    api POST "/api/qualityprofiles/activate_rule" \
        --data-urlencode "key=${PROFILE_KEY}" \
        --data-urlencode "rule=${rule}" \
        --data-urlencode "params=${parameter}=${value}" >/dev/null
}

activate_rule_without_parameters() {
    local rule="$1"
    api POST "/api/qualityprofiles/activate_rule" \
        --data-urlencode "key=${PROFILE_KEY}" \
        --data-urlencode "rule=${rule}" >/dev/null
}

create_python_template_rule() {
    api POST "/api/rules/create" \
        --data-urlencode "customKey=python_allowed_dependencies_e2e_template" \
        --data-urlencode "templateKey=allowed-dependencies-python:python-allowed-dependencies" \
        --data-urlencode "name=Allowed Dependencies Python Template E2E" \
        --data-urlencode "markdownDescription=E2E custom Python dependency rule" \
        --data-urlencode "params=pythonDependencies=regex:^(sphinx|mypy|mkdocs)$;pythonDependencyGroups=docs;pythonRequirementsFiles=requirements-template.txt" \
        >/dev/null
}

set_default_profile() {
    local language="$1"
    local profile_name="$2"
    api POST "/api/qualityprofiles/set_default" \
        --data-urlencode "language=${language}" \
        --data-urlencode "qualityProfile=${profile_name}" >/dev/null
}

create_quality_profiles() {
    create_profile json "Allowed Dependencies JSON E2E"
    activate_rule \
        "allowed-dependencies-npm:allowed-dependencies-main" \
        "npmDependencies" \
        "@acme/ui"
    activate_rule \
        "allowed-dependencies-npm:allowed-dependencies-dev" \
        "npmDependencies" \
        "eslint"
    activate_rule \
        "allowed-dependencies-npm:allowed-dependencies-peer" \
        "npmDependencies" \
        "react"
    set_default_profile json "Allowed Dependencies JSON E2E"

    create_profile xml "Allowed Dependencies XML E2E"
    activate_rule \
        "allowed-dependencies-maven:maven-allowed-dependencies-main" \
        "mavenDependencies" \
        $'org.slf4j:slf4j-api\ncom.acme:platform-core'
    activate_rule \
        "allowed-dependencies-maven:maven-allowed-dependencies-test" \
        "mavenDependencies" \
        $'org.junit.jupiter:junit-jupiter-api\nregex:^org\\.assertj:.*$'
    set_default_profile xml "Allowed Dependencies XML E2E"

    create_profile py "Allowed Dependencies Python E2E"
    create_python_template_rule
    activate_rule \
        "allowed-dependencies-python:python-allowed-dependencies-main" \
        "pythonDependencies" \
        $'requests\nfastapi\nurllib3'
    activate_rule \
        "allowed-dependencies-python:python-allowed-dependencies-dev" \
        "pythonDependencies" \
        $'pytest\nruff\nmypy\nsphinx\neditable-package'
    activate_rule_without_parameters \
        "allowed-dependencies-python:python_allowed_dependencies_e2e_template"
    set_default_profile py "Allowed Dependencies Python E2E"
}

scan_fixture() {
    local fixture="$1"
    local fixture_path="${REPOSITORY_ROOT}/src/it/scanner-fixtures/${fixture}"
    local scanner_log="${REPORT_DIR}/${fixture}-scanner.log"

    log "Scanning ${fixture}"
    docker run --rm \
        --network "${COMPOSE_PROJECT_NAME}_default" \
        --volume "${fixture_path}:/usr/src:ro" \
        "${SCANNER_IMAGE}" \
        -Dsonar.host.url="${SONARQUBE_CONTAINER_URL}" \
        -Dsonar.token="${SONAR_TOKEN}" \
        -Dsonar.qualitygate.wait=true >"${scanner_log}" 2>&1
}

scan_maven_fixture() {
    local fixture="$1"
    local fixture_path="${REPOSITORY_ROOT}/src/it/scanner-fixtures/${fixture}"
    local fixture_work_dir="${WORK_DIR}/${fixture}"
    local scanner_log="${REPORT_DIR}/${fixture}-scanner.log"

    log "Scanning ${fixture}"
    mkdir -p "${fixture_work_dir}"
    cp -R "${fixture_path}/." "${fixture_work_dir}/"

    (
        cd "${fixture_work_dir}"
        mvn -B --no-transfer-progress \
            process-resources \
            org.sonarsource.scanner.maven:sonar-maven-plugin:5.6.0.6792:sonar \
            -Dsonar.host.url="${SONARQUBE_URL}" \
            -Dsonar.token="${SONAR_TOKEN}" \
            -Dsonar.qualitygate.wait=true
    ) >"${scanner_log}" 2>&1
}

record_result() {
    RESULT_FIXTURES+=("$1")
    RESULT_SCANS+=("$2")
    RESULT_EXPECTED+=("$3")
    RESULT_ACTUAL+=("$4")
    RESULT_ASSERTIONS+=("$5")
    RESULT_DETAILS+=("$6")
}

verify_fixture() {
    local fixture="$1"
    local project_key="$2"
    local repository="$3"
    shift 3
    local expected_count=$#
    local expected_json actual_json

    if [[ ${expected_count} -eq 0 ]]; then
        expected_json='[]'
    else
        expected_json="$(printf '%s\n' "$@" | jq -R . | jq -s 'sort')"
    fi

    actual_json="$(api GET "/api/issues/search?componentKeys=${project_key}&ps=100" |
        jq '[.issues[]
            | select(.rule | startswith("'"${repository}"':"))
            | {
                rule: .rule,
                message: .message,
                component: (.component | split(":") | last),
                line: .line
              }
            | "\(.rule)|\(.message)|\(.component)|\(.line)"
        ] | sort')"

    local actual_count
    actual_count="$(jq 'length' <<<"${actual_json}")"

    if jq --exit-status --argjson expected "${expected_json}" \
        '. == $expected' <<<"${actual_json}" >/dev/null; then
        record_result "${fixture}" PASS "${expected_count}" "${actual_count}" PASS ""
        return
    fi

    local detail_file="${REPORT_DIR}/${fixture}-issues.json"
    jq -n \
        --arg fixture "${fixture}" \
        --argjson expected "${expected_json}" \
        --argjson actual "${actual_json}" \
        '{fixture: $fixture, expected: $expected, actual: $actual}' >"${detail_file}"
    record_result "${fixture}" PASS "${expected_count}" "${actual_count}" FAIL "${detail_file}"
    RESULT_FAILURES=$((RESULT_FAILURES + 1))
}

scan_and_verify() {
    local fixture="$1"
    local project_key="$2"
    local repository="$3"
    shift 3

    if ! scan_fixture "${fixture}"; then
        record_result \
            "${fixture}" \
            FAIL \
            "$#" \
            "-" \
            SKIP \
            "${REPORT_DIR}/${fixture}-scanner.log"
        RESULT_FAILURES=$((RESULT_FAILURES + 1))
        return
    fi

    verify_fixture "${fixture}" "${project_key}" "${repository}" "$@"
}

scan_maven_and_verify() {
    local fixture="$1"
    local project_key="$2"
    local repository="$3"
    shift 3

    if ! scan_maven_fixture "${fixture}"; then
        record_result \
            "${fixture}" \
            FAIL \
            "$#" \
            "-" \
            SKIP \
            "${REPORT_DIR}/${fixture}-scanner.log"
        RESULT_FAILURES=$((RESULT_FAILURES + 1))
        return
    fi

    verify_fixture "${fixture}" "${project_key}" "${repository}" "$@"
}

print_report() {
    local index
    local total_expected=0
    local total_actual=0

    printf '\nSonarQube E2E Results\n'
    printf '%-24s  %-6s  %-13s  %-10s\n' "Fixture" "Scan" "Issues (A/E)" "Assertions"
    printf '%-24s  %-6s  %-13s  %-10s\n' "------------------------" "------" "-------------" "----------"

    for ((index = 0; index < ${#RESULT_FIXTURES[@]}; index++)); do
        printf '%-24s  %-6s  %-13s  %-10s\n' \
            "${RESULT_FIXTURES[index]}" \
            "${RESULT_SCANS[index]}" \
            "${RESULT_ACTUAL[index]}/${RESULT_EXPECTED[index]}" \
            "${RESULT_ASSERTIONS[index]}"
        total_expected=$((total_expected + RESULT_EXPECTED[index]))
        if [[ ${RESULT_ACTUAL[index]} != "-" ]]; then
            total_actual=$((total_actual + RESULT_ACTUAL[index]))
        fi
    done

    printf '\nFixtures: %s  Expected issues: %s  Actual issues: %s  Failures: %s\n' \
        "${#RESULT_FIXTURES[@]}" "${total_expected}" "${total_actual}" "${RESULT_FAILURES}"
    printf 'Detailed logs: %s\n' "${REPORT_DIR}"

    if [[ ${RESULT_FAILURES} -eq 0 ]]; then
        printf 'Result: PASS\n'
        return
    fi

    printf 'Result: FAIL\n'
    for ((index = 0; index < ${#RESULT_FIXTURES[@]}; index++)); do
        if [[ -n ${RESULT_DETAILS[index]} ]]; then
            printf '  %s: %s\n' "${RESULT_FIXTURES[index]}" "${RESULT_DETAILS[index]}"
        fi
    done
}

main() {
    require_command curl
    require_command docker
    require_command jq
    require_command mvn
    require_command tee

    cd "${REPOSITORY_ROOT}"
    prepare_docker_config
    if [[ ${E2E_SKIP_BUILD:-false} != "true" ]]; then
        log "Building plugin"
        mvn -B --no-transfer-progress verify -Pintegration-tests
    fi

    local final_name
    final_name="$(mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout)"
    export SONAR_PLUGIN_JAR="${REPOSITORY_ROOT}/target/${final_name}.jar"
    export SONARQUBE_PORT

    [[ -f ${SONAR_PLUGIN_JAR} ]] || fail "Built plugin JAR not found: ${SONAR_PLUGIN_JAR}"

    trap cleanup EXIT
    compose down --volumes --remove-orphans >/dev/null 2>&1 || true

    log "Starting SonarQube"
    compose up --detach
    wait_for_sonarqube
    create_token
    assert_plugin_registration
    create_quality_profiles

    scan_and_verify \
        npm-package \
        alloweddependencies-fixture-npm \
        allowed-dependencies-npm \
        "allowed-dependencies-npm:allowed-dependencies-dev|Remove this forbidden dependency: jest.|package.json|11" \
        "allowed-dependencies-npm:allowed-dependencies-main|Remove this forbidden dependency: left-pad.|package.json|7" \
        "allowed-dependencies-npm:allowed-dependencies-peer|Remove this forbidden dependency: @external/plugin.|package.json|15"

    scan_and_verify \
        maven-pom \
        alloweddependencies-fixture-maven-pom \
        allowed-dependencies-maven \
        "allowed-dependencies-maven:maven-allowed-dependencies-main|Remove this forbidden dependency: com.external:runtime-lib.|pom.xml|17" \
        "allowed-dependencies-maven:maven-allowed-dependencies-test|Remove this forbidden dependency: org.mockito:mockito-core.|pom.xml|29"

    scan_maven_and_verify \
        maven-flattened-pom \
        alloweddependencies-fixture-maven-flattened-pom \
        allowed-dependencies-maven \
        "allowed-dependencies-maven:maven-allowed-dependencies-main|Remove this forbidden dependency: com.google.guava:guava.|.flattened-pom.xml|25" \
        "allowed-dependencies-maven:maven-allowed-dependencies-test|Remove this forbidden dependency: org.hamcrest:hamcrest.|.flattened-pom.xml|37"

    scan_and_verify \
        xml-non-pom \
        alloweddependencies-fixture-xml-non-pom \
        allowed-dependencies-maven

    scan_and_verify \
        python-pyproject \
        alloweddependencies-fixture-python-pyproject \
        allowed-dependencies-python \
        "allowed-dependencies-python:python_allowed_dependencies_e2e_template|Remove this forbidden dependency: external-docs-pep735.|pyproject.toml|34" \
        "allowed-dependencies-python:python_allowed_dependencies_e2e_template|Remove this forbidden dependency: external-docs-poetry.|pyproject.toml|20" \
        "allowed-dependencies-python:python_allowed_dependencies_e2e_template|Remove this forbidden dependency: external-lint.|pyproject.toml|29" \
        "allowed-dependencies-python:python-allowed-dependencies-dev|Remove this forbidden dependency: external-dev.|pyproject.toml|16" \
        "allowed-dependencies-python:python-allowed-dependencies-dev|Remove this forbidden dependency: external-lint.|pyproject.toml|29" \
        "allowed-dependencies-python:python-allowed-dependencies-dev|Remove this forbidden dependency: external-pep735-dev.|pyproject.toml|25" \
        "allowed-dependencies-python:python-allowed-dependencies-main|Remove this forbidden dependency: external-main.|pyproject.toml|5" \
        "allowed-dependencies-python:python-allowed-dependencies-main|Remove this forbidden dependency: external-poetry.|pyproject.toml|12"

    scan_and_verify \
        python-pip \
        alloweddependencies-fixture-python-pip \
        allowed-dependencies-python \
        "allowed-dependencies-python:python_allowed_dependencies_e2e_template|Remove this forbidden dependency: external-template.|requirements-template.txt|2" \
        "allowed-dependencies-python:python-allowed-dependencies-dev|Remove this forbidden dependency: external-dev-shared.|dev-shared.txt|2" \
        "allowed-dependencies-python:python-allowed-dependencies-dev|Remove this forbidden dependency: external-dev.|requirements-dev.txt|3" \
        "allowed-dependencies-python:python-allowed-dependencies-main|Remove this forbidden dependency: external-main.|requirements.txt|3" \
        "allowed-dependencies-python:python-allowed-dependencies-main|Remove this forbidden dependency: external-shared.|shared.txt|2"

    print_report | tee "${REPORT_DIR}/summary.txt"
    [[ ${RESULT_FAILURES} -eq 0 ]]
}

main "$@"
