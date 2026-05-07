package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.sonar.api.batch.rule.ActiveRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for a Python dependency allow-list check.
 */
public class AllowedPythonDependenciesCheckConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AllowedPythonDependenciesCheckConfig.class);

    private final ActiveRule rule;

    private final Predicate<String> allowedDependenciesPredicate;

    private final PythonDependencyGroupType groupType;

    private final List<String> groups;

    public AllowedPythonDependenciesCheckConfig(final ActiveRule activeRule) {
        LOG.info("Creating AllowedPythonDependenciesCheck for {}", activeRule.ruleKey());
        this.rule = activeRule;
        this.groupType = PythonDependencyGroupType.forRule(activeRule);
        this.groups = resolveGroups(activeRule, groupType);
        this.allowedDependenciesPredicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate(activeRule.param(PythonRulesDefinition.DEPS_PARAM_KEY));
    }

    private static List<String> resolveGroups(final ActiveRule activeRule, final PythonDependencyGroupType groupType) {
        if (!PythonDependencyGroupType.CUSTOM.equals(groupType)) {
            return groupType.getDefaultGroups();
        }

        final String groups = activeRule.param(PythonRulesDefinition.GROUPS_PARAM_KEY);
        if (groups == null || groups.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(groups.split(","))
            .stream()
            .map(String::trim)
            .filter(group -> !group.isEmpty())
            .collect(Collectors.toList());
    }

    public ActiveRule getRule() {
        return rule;
    }

    public Predicate<String> getAllowedDependenciesPredicate() {
        return allowedDependenciesPredicate;
    }

    public PythonDependencyGroupType getGroupType() {
        return groupType;
    }

    public List<String> getGroups() {
        return groups;
    }
}

