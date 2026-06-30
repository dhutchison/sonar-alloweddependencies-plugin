package com.devwithimagination.sonar.alloweddependencies.plugin.python.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.Param;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.server.rule.RulesDefinition.Rule;

class TestPythonRulesDefinition {

    @Test
    void checkRuleCreation() {
        final Context context = new Context();
        final PythonRulesDefinition ruleCreator = new PythonRulesDefinition();

        ruleCreator.define(context);

        assertEquals(1, context.repositories().size(), "Expected a repository to be created.");

        final Repository repository = context.repository(PythonRulesDefinition.REPOSITORY_PYTHON);
        assertNotNull(repository, "Expected Python repository to be created");
        assertEquals(PythonRulesDefinition.REPOSITORY_PYTHON, repository.key());
        assertEquals(PythonRulesDefinition.PYTHON_DEPENDENCY_LANGUAGE, repository.language());
        assertEquals(3, repository.rules().size(), "Expected three Python rules");

        final List<String> ruleKeys = repository.rules()
            .stream()
            .map(Rule::key)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule(), ruleKeys.get(0));
        assertEquals(PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV.rule(), ruleKeys.get(1));
        assertEquals(PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN.rule(), ruleKeys.get(2));

        repository.rules().forEach(rule -> {
            final Param depsParam = rule.param(PythonRulesDefinition.DEPS_PARAM_KEY);
            assertNotNull(depsParam, "Expected dependency allow-list parameter");
            assertEquals("Allowed Python Dependencies", depsParam.name());
            assertTrue(depsParam.description().contains("regex:"));
            assertTrue(depsParam.description().contains("normalized"));
            assertEquals(RuleParamType.TEXT, depsParam.type());
        });

        final Rule templateRule = repository.rule(PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule());
        assertTrue(templateRule.template());
        assertEquals(3, templateRule.params().size());

        final Param groupsParam = templateRule.param(PythonRulesDefinition.GROUPS_PARAM_KEY);
        assertNotNull(groupsParam);
        assertEquals(RuleParamType.STRING, groupsParam.type());
        assertTrue(groupsParam.description().contains("group names"));

        final Param requirementsFilesParam =
            templateRule.param(PythonRulesDefinition.REQUIREMENTS_FILES_PARAM_KEY);
        assertNotNull(requirementsFilesParam);
        assertEquals(RuleParamType.STRING, requirementsFilesParam.type());
        assertTrue(requirementsFilesParam.description().contains("file paths"));

        assertFalse(repository.rule(PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN.rule()).template());
        assertFalse(repository.rule(PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV.rule()).template());
    }
}
