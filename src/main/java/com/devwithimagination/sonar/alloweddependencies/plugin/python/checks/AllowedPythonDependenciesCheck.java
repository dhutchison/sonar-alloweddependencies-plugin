package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyIssueReporter;
import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;

/**
 * Compares Python dependencies against the configured allow list.
 */
public class AllowedPythonDependenciesCheck {

    private static final Logger LOG = LoggerFactory.getLogger(AllowedPythonDependenciesCheck.class);

    private final RuleKey ruleKey;

    private final Predicate<String> allowedDependenciesPredicate;

    private final PythonDependencyGroupType groupType;

    private final List<String> groups;

    private final List<String> requirementsFiles;

    public AllowedPythonDependenciesCheck(final ActiveRule activeRule) {
        LOG.info("Creating AllowedPythonDependenciesCheck for {}", activeRule.ruleKey());
        this.ruleKey = activeRule.ruleKey();
        this.groupType = PythonDependencyGroupType.forRule(activeRule);
        this.groups = resolveGroups(activeRule, groupType);
        this.requirementsFiles = resolveRequirementsFiles(activeRule, groupType);
        this.allowedDependenciesPredicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate(activeRule.param(PythonRulesDefinition.DEPS_PARAM_KEY));
    }

    public void scanDependency(final DependencyOccurrence dependency, final SensorContext sensorContext) {
        if (!allowedDependenciesPredicate.test(dependency.getName())) {
            LOG.info("Forbidden Python dependency: {}", dependency.getName());
            DependencyIssueReporter.reportIssue(sensorContext, ruleKey,
                dependency.getInputFile(), dependency.getLineNumber(), dependency.getName());
        }
    }

    private static List<String> resolveGroups(final ActiveRule activeRule, final PythonDependencyGroupType groupType) {
        if (!PythonDependencyGroupType.CUSTOM.equals(groupType)) {
            return groupType.getDefaultGroups();
        }

        return resolveCommaSeparatedParam(activeRule, PythonRulesDefinition.GROUPS_PARAM_KEY);
    }

    private static List<String> resolveRequirementsFiles(final ActiveRule activeRule,
            final PythonDependencyGroupType groupType) {

        if (!PythonDependencyGroupType.CUSTOM.equals(groupType)) {
            return Collections.emptyList();
        }

        return resolveCommaSeparatedParam(activeRule, PythonRulesDefinition.REQUIREMENTS_FILES_PARAM_KEY);
    }

    private static List<String> resolveCommaSeparatedParam(final ActiveRule activeRule, final String paramKey) {
        final String value = activeRule.param(paramKey);
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .collect(Collectors.toList());
    }

    public PythonDependencyGroupType getGroupType() {
        return groupType;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getRequirementsFiles() {
        return requirementsFiles;
    }
}
