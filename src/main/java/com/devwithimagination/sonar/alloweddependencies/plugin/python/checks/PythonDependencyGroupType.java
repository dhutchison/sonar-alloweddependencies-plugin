package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.sonar.api.batch.rule.ActiveRule;

/**
 * Resolves the configured Python dependency groups for an active rule.
 */
public enum PythonDependencyGroupType {

    MAIN(Arrays.asList("main")),
    DEV(Arrays.asList("dev")),
    CUSTOM(Collections.emptyList());

    private final List<String> defaultGroups;

    private PythonDependencyGroupType(final List<String> defaultGroups) {
        this.defaultGroups = defaultGroups;
    }

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public static PythonDependencyGroupType forRule(final ActiveRule activeRule) {
        if (PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN.equals(activeRule.ruleKey())) {
            return MAIN;
        } else if (PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV.equals(activeRule.ruleKey())) {
            return DEV;
        } else if (PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule().equals(activeRule.templateRuleKey())) {
            return CUSTOM;
        }
        throw new IllegalArgumentException("Unsupported rule: " + activeRule.ruleKey());
    }
}

