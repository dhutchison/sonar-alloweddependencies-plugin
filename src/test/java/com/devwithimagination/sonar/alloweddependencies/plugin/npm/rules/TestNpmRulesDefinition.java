package com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.Repository;

/**
 * Test case for checking that rule definition works as expected.
 */
class TestNpmRulesDefinition {

    @Test
    void checkRuleCreation() {

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
        assertEquals(3, repository.rules().size(), "Expected three rules, one for each type of dependency");

        final List<String> ruleKeys = repository.rules()
            .stream()
            .map(r -> r.key())
            .sorted()
            .collect(Collectors.toList());
        assertEquals(NpmRulesDefinition.RULE_NPM_ALLOWED_DEV.rule(), ruleKeys.get(0));
        assertEquals(NpmRulesDefinition.RULE_NPM_ALLOWED.rule(), ruleKeys.get(1));
        assertEquals(NpmRulesDefinition.RULE_NPM_ALLOWED_PEER.rule(), ruleKeys.get(2));

    }

}
