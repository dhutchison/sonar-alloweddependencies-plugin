package com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.server.rule.RulesDefinition.Rule;

/**
 * Test case for checking that rule definition works as expected.
 */
public class TestNpmRulesDefinition {

    @Test
    public void checkRuleCreation() {

        /* SonarQube context */
        final Context context = new Context();

        /* Create the component under test */
        final NpmRulesDefinition ruleCreator = new NpmRulesDefinition();

        /* Create the rules */
        ruleCreator.define(context);

        /*
         * Perform basic verifications. A repository will not be included in this list
         * if done() was not created on the new repository.
         */
        assertEquals(1, context.repositories().size(),
                "Expected a repository to be created.");

        final Repository repository = context.repository(NpmRulesDefinition.REPOSITORY_NPM);


        assertNotNull(repository, "Expected NPM repository to be created");

        assertEquals(NpmRulesDefinition.REPOSITORY_NPM, repository.key());
        assertEquals(NpmRulesDefinition.NPM_DEPENDENCY_LANGUAGE, repository.language());
        // TODO: Implement
        // assertEquals(2, repository.rules().size(), "Expected two rules, one for each
        // type of dependency");

    }

}
