package com.devwithimagination.sonar.alloweddependencies.plugin.npm.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.rule.RuleKey;

/**
 * Test case for {@link CreateIssuesOnNPMDependenciesSensor}.
 */
class TestCreateIssuesOnNPMDependenciesSensor {


    /**
     * The sensor under test
     */
    private CreateIssuesOnNPMDependenciesSensor sensor;


    /**
     * Setup common test dependencies
     */
    @BeforeEach
    void setup() {
        /* Create the sensor to test. We don't use the config so just pass null just now */
        this.sensor = new CreateIssuesOnNPMDependenciesSensor(null);
    }

    /**
     * Test that when executed with both enabled rules, that attempts are made to scan files.
     *
     * This will hit the check implemntations, but we only test the one case. Other cases will
     * be tested as part of the direct unit tests.
     */
    @Test
    void testExecuteWithRulesAndSingleFile() {

        /* Configure the sensor context with the parts we need mocked */
        final List<NewActiveRule> newActiveRules = Arrays.asList(
            new NewActiveRule.Builder()
                .setRuleKey(NpmRulesDefinition.RULE_NPM_ALLOWED)
                .build(),
            new NewActiveRule.Builder()
                .setRuleKey(NpmRulesDefinition.RULE_NPM_ALLOWED_DEV)
                .build(),
            /* A rule we don't support, that should be filtered out and not constructed into a check */
            new NewActiveRule.Builder()
                .setRuleKey(RuleKey.of(NpmRulesDefinition.REPOSITORY_NPM, "my-fake-rule"))
                .build()
        );

        final ActiveRules activeRules = new DefaultActiveRules(newActiveRules);

        //TODO: Create issue - javadocs on adding files are wrong here
        final SensorContextTester sensorContext = SensorContextTester.create(
            new File("src/test/resources/npm"));
        sensorContext.setActiveRules(activeRules);
        sensorContext.fileSystem().add(
            new DefaultInputFile(
                new DefaultIndexedFile(
                    "my-test-project",
                    new File("src/test/resources/npm").toPath(),
                    "package.json",
                    "java"),
                null));

        /* Run the test component */
        sensor.execute(sensorContext);

        /* Verify things happened */
        assertEquals(9, sensorContext.allIssues().size(), "Expecting violations for all dependencies");

    }

}
