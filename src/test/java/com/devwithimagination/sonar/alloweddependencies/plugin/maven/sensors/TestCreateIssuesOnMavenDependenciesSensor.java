package com.devwithimagination.sonar.alloweddependencies.plugin.maven.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.rule.RuleKey;

/**
 * Test case for {@link CreateIssuesOnMavenDependenciesSensor}.
 */
class TestCreateIssuesOnMavenDependenciesSensor {

    /**
     * The sensor under test
     */
    private CreateIssuesOnMavenDependenciesSensor sensor;

    /**
     * Setup common test dependencies
     */
    @BeforeEach
    void setup() {
        /* Create the sensor to test. We don't use the config so just pass null just now */
        this.sensor = new CreateIssuesOnMavenDependenciesSensor(null);
    }

    /**
     * Test that when executed with an enabled rule, that attempts are made to scan files.
     *
     * This will hit the check implemntations, but we only test the one case. Other cases will
     * be tested as part of the direct unit tests.
     *
     * @throws IOException if an unexpected error occurs while reading the test file
     */
    @Test
    void testExecuteWithRulesAndSingleFile() throws IOException {

        /* Configure the sensor context with the parts we need mocked */
        final List<NewActiveRule> newActiveRules = Arrays.asList(
            new NewActiveRule.Builder()
                .setRuleKey(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "my-rule-key"))
                .setTemplateRuleKey(MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule())
                .build()
        );

        final ActiveRules activeRules = new DefaultActiveRules(newActiveRules);

        final File testResourcesDir = new File("src/test/resources/maven");
        final File testFile = new File(testResourcesDir, "pom.xml");
        final String fileContents = String.join(System.lineSeparator(), Files.readAllLines(testFile.toPath()));

        final SensorContextTester sensorContext = SensorContextTester.create(
            testResourcesDir);
        sensorContext.setActiveRules(activeRules);
        sensorContext.fileSystem().add(
            TestInputFileBuilder.create(
                    "my-test-project",
                    testResourcesDir,
                    testFile)
                .setCharset(Charset.forName("UTF-8"))
                .setContents(fileContents)
                .build());

        /* Run the test component */
        sensor.execute(sensorContext);

        /* Verify things happened */
        assertEquals(13, sensorContext.allIssues().size(), "Expecting violations for all dependencies");

    }

}
