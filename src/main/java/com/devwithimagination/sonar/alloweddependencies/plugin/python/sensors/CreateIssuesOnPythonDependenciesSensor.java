package com.devwithimagination.sonar.alloweddependencies.plugin.python.sensors;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.checks.AllowedPythonDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers.PyprojectTomlDependencyParser;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers.RequirementsDependencyParser;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;

/**
 * Generates issues for Python dependency descriptor files.
 */
public class CreateIssuesOnPythonDependenciesSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(CreateIssuesOnPythonDependenciesSensor.class);

    private static final List<String> PYPROJECT_FILE_PATTERNS = Arrays.asList(
        "**/pyproject.toml"
    );

    protected final Configuration config;

    public CreateIssuesOnPythonDependenciesSensor(final Configuration config) {
        this.config = config;
    }

    @Override
    public void describe(final SensorDescriptor descriptor) {
        descriptor.name("Add issues to Python dependency descriptor files.");
        descriptor.onlyOnLanguage(PythonRulesDefinition.PYTHON_DEPENDENCY_LANGUAGE);
        descriptor.createIssuesForRuleRepositories(PythonRulesDefinition.REPOSITORY_PYTHON);
    }

    @Override
    public void execute(final SensorContext context) {
        final List<AllowedPythonDependenciesCheck> checks = context.activeRules()
            .findByRepository(PythonRulesDefinition.REPOSITORY_PYTHON)
            .stream()
            .filter(CreateIssuesOnPythonDependenciesSensor::isSupportedRule)
            .map(AllowedPythonDependenciesCheck::new)
            .collect(Collectors.toList());

        if (checks.isEmpty()) {
            return;
        }

        final FileSystem fs = context.fileSystem();
        final Set<InputFile> allInputFiles = new LinkedHashSet<>();
        fs.inputFiles(fs.predicates().all()).forEach(allInputFiles::add);

        final Set<InputFile> pyprojectFiles = new LinkedHashSet<>();
        PYPROJECT_FILE_PATTERNS.forEach(pattern ->
            fs.inputFiles(fs.predicates().matchesPathPattern(pattern))
                .forEach(pyprojectFiles::add));

        final PyprojectTomlDependencyParser tomlParser = new PyprojectTomlDependencyParser();
        final RequirementsDependencyParser requirementsParser = new RequirementsDependencyParser(allInputFiles);

        for (AllowedPythonDependenciesCheck check : checks) {
            for (InputFile pyprojectFile : pyprojectFiles) {
                LOG.info("Python dependency input file {}", pyprojectFile);
                final List<DependencyOccurrence> dependencies = tomlParser.parse(pyprojectFile,
                    check.getGroupType(), check.getGroups());
                dependencies.forEach(dependency -> check.scanDependency(dependency, context));
            }

            final List<DependencyOccurrence> requirementsDependencies = requirementsParser.parse(
                check.getGroupType(), check.getRequirementsFiles());
            requirementsDependencies.forEach(dependency -> check.scanDependency(dependency, context));
        }
    }

    private static boolean isSupportedRule(final ActiveRule rule) {
        return PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN.equals(rule.ruleKey())
            || PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV.equals(rule.ruleKey())
            || PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule().equals(rule.templateRuleKey());
    }
}
