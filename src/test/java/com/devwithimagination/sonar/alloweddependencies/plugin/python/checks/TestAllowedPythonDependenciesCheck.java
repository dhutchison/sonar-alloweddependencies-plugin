package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;

class TestAllowedPythonDependenciesCheck {

    @Test
    void checkMainRule() {
        final ActiveRule rule = createTestRule(PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN, null,
            "Requests");
        final AllowedPythonDependenciesCheck check = new AllowedPythonDependenciesCheck(rule);
        final SensorContext context = mock(SensorContext.class);

        check.scanDependency(new DependencyOccurrence("requests", null, 1), context);

        assertEquals(PythonDependencyGroupType.MAIN, check.getGroupType());
        assertEquals(Arrays.asList("main"), check.getGroups());
        assertTrue(check.getRequirementsFiles().isEmpty());
        verify(context, never()).newIssue();
    }

    @Test
    void checkDevRule() {
        final ActiveRule rule = createTestRule(PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV, null,
            "pytest");

        final AllowedPythonDependenciesCheck check = new AllowedPythonDependenciesCheck(rule);

        assertEquals(PythonDependencyGroupType.DEV, check.getGroupType());
        assertEquals(Arrays.asList("dev"), check.getGroups());
        assertTrue(check.getRequirementsFiles().isEmpty());
    }

    @Test
    void checkTemplateRule() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx", "docs, lint",
            "requirements-docs.txt, config/requirements-lint.txt");

        final AllowedPythonDependenciesCheck check = new AllowedPythonDependenciesCheck(rule);

        assertEquals(PythonDependencyGroupType.CUSTOM, check.getGroupType());
        assertEquals(Arrays.asList("docs", "lint"), check.getGroups());
        assertEquals(Arrays.asList("requirements-docs.txt", "config/requirements-lint.txt"),
            check.getRequirementsFiles());
    }

    @Test
    void checkTemplateRuleWithoutGroups() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx");

        final AllowedPythonDependenciesCheck check = new AllowedPythonDependenciesCheck(rule);

        assertEquals(PythonDependencyGroupType.CUSTOM, check.getGroupType());
        assertTrue(check.getGroups().isEmpty());
        assertTrue(check.getRequirementsFiles().isEmpty());
    }

    @Test
    void checkTemplateRuleWithBlankAndEmptyGroups() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx", " docs, , lint ");

        final AllowedPythonDependenciesCheck check = new AllowedPythonDependenciesCheck(rule);

        assertEquals(Arrays.asList("docs", "lint"), check.getGroups());
        assertTrue(check.getRequirementsFiles().isEmpty());
    }

    @Test
    void checkTemplateRuleWithBlankGroupsParameter() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx", "   ");

        final AllowedPythonDependenciesCheck check = new AllowedPythonDependenciesCheck(rule);

        assertTrue(check.getGroups().isEmpty());
    }

    @Test
    void checkUnsupportedRule() {
        final RuleKey ruleKey = RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "my-fake-rule");
        final ActiveRule rule = createTestRule(ruleKey, null, "");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new AllowedPythonDependenciesCheck(rule));

        assertEquals("Unsupported rule: allowed-dependencies-python:my-fake-rule", ex.getMessage());
    }

    private ActiveRule createTestRule(final RuleKey ruleKey, final String templateRuleKey,
            final String allowedDeps) {

        return createTestRule(ruleKey, templateRuleKey, allowedDeps, null);
    }

    private ActiveRule createTestRule(final RuleKey ruleKey, final String templateRuleKey,
            final String allowedDeps, final String groups) {

        return createTestRule(ruleKey, templateRuleKey, allowedDeps, groups, null);
    }

    private ActiveRule createTestRule(final RuleKey ruleKey, final String templateRuleKey,
            final String allowedDeps, final String groups, final String requirementsFiles) {

        final NewActiveRule.Builder builder = new NewActiveRule.Builder()
            .setRuleKey(ruleKey)
            .setParam(PythonRulesDefinition.DEPS_PARAM_KEY, allowedDeps);

        if (templateRuleKey != null) {
            builder.setTemplateRuleKey(templateRuleKey);
        }
        if (groups != null) {
            builder.setParam(PythonRulesDefinition.GROUPS_PARAM_KEY, groups);
        }
        if (requirementsFiles != null) {
            builder.setParam(PythonRulesDefinition.REQUIREMENTS_FILES_PARAM_KEY, requirementsFiles);
        }

        return new DefaultActiveRules(Arrays.asList(builder.build())).find(ruleKey);
    }
}
