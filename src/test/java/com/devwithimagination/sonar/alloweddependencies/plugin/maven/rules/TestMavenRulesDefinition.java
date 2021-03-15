package com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.server.rule.RulesDefinition.Rule;

/**
 * Test case for checking that rule definition works as expected.
 */
public class TestMavenRulesDefinition {

    @Test
    public void checkRuleCreation() {

        /* SonarQube context */
        final Context context = new Context();

        /* Create the component under test */
        final MavenRulesDefinition ruleCreator = new MavenRulesDefinition();

        /* Create the rules */
        ruleCreator.define(context);

        /*
         * Perform basic verifications. A repository will not be included in this list
         * if done() was not created on the new repository.
         */
        assertEquals(1, context.repositories().size(),
                "Expected a repository to be created.");

        /* Verify the Maven repository */
        final Repository repository = context.repository(MavenRulesDefinition.REPOSITORY_MAVEN);

        assertNotNull(repository, "Expected Maven repository to be created");

        assertEquals(MavenRulesDefinition.REPOSITORY_MAVEN, repository.key());
        assertEquals(MavenRulesDefinition.MAVEN_DEPENDENCY_LANGUAGE, repository.language());
        assertEquals(3, repository.rules().size(), "Expected three rules");

        /* Find the templated rule, and check the parameter count */
        final List<Rule> templateRules = repository.rules()
            .stream()
            .filter(r -> r.template())
            .collect(Collectors.toList());
        assertEquals(1, templateRules.size(), "Expected one templated rule");

        final Rule templateRule = templateRules.get(0);
        assertEquals(MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule(), templateRule.key());
        assertEquals(2, templateRule.params().size(), "Expecting two parameters");

        /* Check the other two rules, they should only have one parameter */
        final List<Rule> nonTemplateRules = repository.rules()
            .stream()
            .filter(r -> !r.template())
            .collect(Collectors.toList());
        assertEquals(2, nonTemplateRules.size(), "Expected two non-templated rules");

        assertTrue(nonTemplateRules.stream().anyMatch(r -> r.key().equals(MavenRulesDefinition.RULE_MAVEN_ALLOWED_MAIN.rule())),
                "Expecting to find main scope rule");
        assertTrue(nonTemplateRules.stream().anyMatch(r -> r.key().equals(MavenRulesDefinition.RULE_MAVEN_ALLOWED_TEST.rule())),
                "Expecting to find test scope rule");
        nonTemplateRules.forEach(r -> assertEquals(1, r.params().size(), "Expecting one parameter"));
    }

}
