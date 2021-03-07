package com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(1, repository.rules().size(), "Expected one templated rule");

        final Rule rule = repository.rules().get(0);
        assertNotNull(rule);
        assertTrue(rule.template(), "Rule should be templated");
        assertEquals(MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule(), rule.key());
        assertEquals(2, rule.params().size(), "Expecting two parameters");
    }

}
