package com.devwithimagination.sonar.alloweddependencies.plugin.python.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.Param;
import org.sonar.api.server.rule.RulesDefinition.Repository;

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
            .map(rule -> rule.key())
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
        });

        assertNotNull(repository.rule(PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule())
            .param(PythonRulesDefinition.GROUPS_PARAM_KEY));
    }
}

