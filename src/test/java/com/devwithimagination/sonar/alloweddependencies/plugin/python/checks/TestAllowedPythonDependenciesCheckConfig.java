package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;

class TestAllowedPythonDependenciesCheckConfig {

    @Test
    void checkMainRule() {
        final ActiveRule rule = createTestRule(PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN, null,
            "Requests");

        final AllowedPythonDependenciesCheckConfig config = new AllowedPythonDependenciesCheckConfig(rule);

        assertNotNull(config.getAllowedDependenciesPredicate());
        assertEquals(PythonDependencyGroupType.MAIN, config.getGroupType());
        assertEquals(Arrays.asList("main"), config.getGroups());
        assertTrue(config.getAllowedDependenciesPredicate().test("requests"));
    }

    @Test
    void checkDevRule() {
        final ActiveRule rule = createTestRule(PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV, null,
            "pytest");

        final AllowedPythonDependenciesCheckConfig config = new AllowedPythonDependenciesCheckConfig(rule);

        assertEquals(PythonDependencyGroupType.DEV, config.getGroupType());
        assertEquals(Arrays.asList("dev"), config.getGroups());
    }

    @Test
    void checkTemplateRule() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx", "docs, lint");

        final AllowedPythonDependenciesCheckConfig config = new AllowedPythonDependenciesCheckConfig(rule);

        assertEquals(PythonDependencyGroupType.CUSTOM, config.getGroupType());
        assertEquals(Arrays.asList("docs", "lint"), config.getGroups());
    }

    @Test
    void checkTemplateRuleWithoutGroups() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx");

        final AllowedPythonDependenciesCheckConfig config = new AllowedPythonDependenciesCheckConfig(rule);

        assertEquals(PythonDependencyGroupType.CUSTOM, config.getGroupType());
        assertTrue(config.getGroups().isEmpty());
    }

    @Test
    void checkTemplateRuleWithBlankAndEmptyGroups() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx", " docs, , lint ");

        final AllowedPythonDependenciesCheckConfig config = new AllowedPythonDependenciesCheckConfig(rule);

        assertEquals(Arrays.asList("docs", "lint"), config.getGroups());
    }

    @Test
    void checkTemplateRuleWithBlankGroupsParameter() {
        final ActiveRule rule = createTestRule(RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "custom-rule"),
            PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), "sphinx", "   ");

        final AllowedPythonDependenciesCheckConfig config = new AllowedPythonDependenciesCheckConfig(rule);

        assertTrue(config.getGroups().isEmpty());
    }

    @Test
    void checkUnsupportedRule() {
        final RuleKey ruleKey = RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "my-fake-rule");
        final ActiveRule rule = createTestRule(ruleKey, null, "");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new AllowedPythonDependenciesCheckConfig(rule));

        assertEquals("Unsupported rule: allowed-dependencies-python:my-fake-rule", ex.getMessage());
    }

    private ActiveRule createTestRule(final RuleKey ruleKey, final String templateRuleKey,
            final String allowedDeps) {

        return createTestRule(ruleKey, templateRuleKey, allowedDeps, null);
    }

    private ActiveRule createTestRule(final RuleKey ruleKey, final String templateRuleKey,
            final String allowedDeps, final String groups) {

        final NewActiveRule.Builder builder = new NewActiveRule.Builder()
            .setRuleKey(ruleKey)
            .setParam(PythonRulesDefinition.DEPS_PARAM_KEY, allowedDeps);

        if (templateRuleKey != null) {
            builder.setTemplateRuleKey(templateRuleKey);
        }
        if (groups != null) {
            builder.setParam(PythonRulesDefinition.GROUPS_PARAM_KEY, groups);
        }

        return new DefaultActiveRules(Arrays.asList(builder.build())).find(ruleKey);
    }
}
