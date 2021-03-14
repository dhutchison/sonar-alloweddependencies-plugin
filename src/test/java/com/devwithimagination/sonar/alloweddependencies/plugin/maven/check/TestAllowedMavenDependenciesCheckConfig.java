package com.devwithimagination.sonar.alloweddependencies.plugin.maven.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenDependenciesCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;

/**
 * Test for checking that {@link AllowedMavenDependenciesCheckConfig} parses rules correctly.
 */
class TestAllowedMavenDependenciesCheckConfig {


    /**
     * Check that the {@link MavenRulesDefinition#RULE_MAVEN_ALLOWED_MAIN} is parsed correctly.
     */
    @Test
    void checkMainRule() {

        /* Setup the test rule */
        final ActiveRule rule = createTestRule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_MAIN,
            String.join("\n", "org.junit:junit", "abc:def"));

        /* Create the config */
        final AllowedMavenDependenciesCheckConfig config = new AllowedMavenDependenciesCheckConfig(rule);

        assertEquals(2, config.getAllowedDependencies().size(), "Expected 2 dependencies in the list");
        assertEquals(Arrays.asList("abc:def", "org.junit:junit"), config.getAllowedDependencies(), "Expected dependencies to match");

        assertEquals(3, config.getScopes().size(), "Expected 3 scopes");
        assertEquals(Arrays.asList("compile", "provided", "runtime"), config.getScopes(), "Expected scopes to match");

    }

    /**
     * Check that the {@link MavenRulesDefinition#RULE_MAVEN_ALLOWED_MAIN} is parsed correctly.
     */
    @Test
    void checkTestRule() {

        /* Setup the test rule */
        final ActiveRule rule = createTestRule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_TEST,
            String.join("\n", "group:artifact", "abc:def"));

        /* Create the config */
        final AllowedMavenDependenciesCheckConfig config = new AllowedMavenDependenciesCheckConfig(rule);

        /* Perform assertions */
        assertEquals(2, config.getAllowedDependencies().size(), "Expected 2 dependencies in the list");
        assertEquals(Arrays.asList("abc:def", "group:artifact"), config.getAllowedDependencies(), "Expected dependencies to match");

        assertEquals(1, config.getScopes().size(), "Expected 1 scope");
        assertEquals("test", config.getScopes().get(0));

    }

    /**
     * Test case checking that attempting to create a config for an unknown rule type will error.
     */
    @Test
    void checkUnsupportedRule() {


        final RuleKey ruleKey = RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "my-fake-rule");
        final ActiveRule rule = createTestRule(ruleKey, "");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new AllowedMavenDependenciesCheckConfig(rule));

        assertEquals("Unsupported rule: allowed-dependencies-maven:my-fake-rule", ex.getMessage());

    }


    /**
     * Create a mock {@link ActiveRule} with the supplied configuration.
     *
     * @param ruleKey the rule key
     * @param allowedDeps the comma seperated dependency name string
     *
     * @return a Mockito mock for the rule, configured with the expected values.
     */
    private ActiveRule createTestRule(final RuleKey ruleKey, final String allowedDeps) {

        final NewActiveRule rule = new NewActiveRule.Builder()
            .setRuleKey(ruleKey)
            .setParam(MavenRulesDefinition.DEPS_PARAM_KEY, allowedDeps)
            .build();

        return new DefaultActiveRules(Arrays.asList(rule)).find(ruleKey);
    }


}
