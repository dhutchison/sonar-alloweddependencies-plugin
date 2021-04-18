package com.devwithimagination.sonar.alloweddependencies.plugin.maven.check;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenDependenciesCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.analyzer.commons.xml.XmlFile;

class TestAllowedMavenDependenciesCheck {

    /**
     * The file we will scan for tests.
     */
    private XmlFile inputFile;

    /**
     * The sensor context used in the scan.
     */
    private SensorContext sensorContext;

    /**
     * Setup any configuration between tests.
     * @throws IOException
     */
    @BeforeEach
    void setup() throws IOException {

        /* Setup the test project location */
        final File moduleBaseDir = new File("src/test/resources/maven");
        final File testFile = new File(moduleBaseDir, "pom.xml");
        final String fileContents = String.join(System.lineSeparator(), Files.readAllLines(testFile.toPath()));

        final InputFile testInputFile = TestInputFileBuilder.create(
                    getClass().getName(), moduleBaseDir, testFile)
                .setCharset(Charset.forName("UTF-8"))
                .setContents(fileContents)
                .build();
        this.inputFile = XmlFile.create(testInputFile);



        /* Setup a sensor context spy */
        final SensorStorage sensorStorage = mock(SensorStorage.class);

        this.sensorContext = mock(SensorContext.class);
        when(sensorContext.newIssue()).then(i -> new DefaultIssue(null, sensorStorage));
    }


    /**
     * Creates tests for the dev and regular dependencies with all the appropriate test file
     * dependencies configured, to ensure we get no issues raised.
     *
     * @param rule the rule being tested.
     */
    @ParameterizedTest
    @MethodSource("provideNoViolationParameters")
    void checkForNoViolations(final ActiveRule rule) {

        /* Scan our test file, and confirm no issues were raised */
        final AllowedMavenDependenciesCheckConfig config = new AllowedMavenDependenciesCheckConfig(rule);
        final AllowedMavenDependenciesCheck check = new AllowedMavenDependenciesCheck(config);

        check.scanFile(sensorContext, rule.ruleKey(), inputFile);

        verify(sensorContext, never()).newIssue();
    }

    /**
     * Creates tests for the dev and regular dependencies with not all of the appropriate test file
     * dependencies configured, to ensure we get issues raised.
     *
     * @param rule the rule being tested
     * @param expectedIssues the number of issues expected to be raised
     */
    @ParameterizedTest
    @MethodSource("provideViolationParameters")
    void checkForViolations(final ActiveRule rule, final int expectedIssues) {

        /* Scan our test file, and confirm the right number of issues were raised */
        final AllowedMavenDependenciesCheckConfig config = new AllowedMavenDependenciesCheckConfig(rule);
        final AllowedMavenDependenciesCheck check = new AllowedMavenDependenciesCheck(config);
        check.scanFile(sensorContext, rule.ruleKey(), inputFile);

        verify(sensorContext, times(expectedIssues)).newIssue();

    }

    /**
     * Method to create the parameters for {@link #checkForNoViolations()}.
     * @return Stream containing the argument pairs.
     */
    private static Stream<Arguments> provideNoViolationParameters() {

        return Stream.of(
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "my-compile-deps"),
                    "com.github.javafaker:javafaker",
                    "compile")),
            Arguments.of(
                createNonTemplatedTestRule(
                    MavenRulesDefinition.RULE_MAVEN_ALLOWED_TEST,
                    String.join("\n",
                        "junit:junit",
                        "regex:org\\.junit\\.jupiter:.*",
                        "com.nimbusds:nimbus-jose-jwt",
                        "org.glassfish.jersey.core:jersey-client",
                        "org.bouncycastle:bcpkix-jdk15on",
                        "regex:org\\.apache\\..*:.*",
                        "org.eclipse.microprofile.rest.client:microprofile-rest-client-api",
                        "com.opentable.components:otj-pg-embedded",
                        "org.flywaydb:flyway-core",
                        "org.jacoco:org.jacoco.agent")))
        );

    }

    /**
     * Method to create the parameters for {@link #checkForViolations()}.
     * @return Stream containing the argument pairs.
     */
    private static Stream<Arguments> provideViolationParameters() {

        return Stream.of(
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "no-compile-deps"),
                    "",
                    "compile"),
                1),
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "no-provided-deps"),
                    "",
                    "provided"),
                2),
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "no-combined-compile-provided-deps"),
                    "",
                    "compile,provided"),
                3),
            Arguments.of(
                createNonTemplatedTestRule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_MAIN,
                    "javax.cache:cache-api"),
                2),
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "one-provided-deps"),
                    "javax.cache:cache-api",
                    "provided"),
                1),
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "some-test-deps"),
                    String.join("\n",
                        "junit:junit",
                        // Not the right groupId, shouldn't match
                        "regex:org\\.junit:.*",
                        "com.nimbusds:nimbus-jose-jwt",
                        "org.glassfish.jersey.core:jersey-client"),
                    "test"),
                10
            ),
            Arguments.of(
                createTemplatedTestRule(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, "some-test-deps"),
                    String.join("\n",
                        "junit:junit",
                        "regex:org\\.junit\\.jupiter:.*",
                        "com.nimbusds:nimbus-jose-jwt",
                        "org.glassfish.jersey.core:jersey-client"),
                    "test"),
                7
            ),
            Arguments.of(
                createNonTemplatedTestRule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_TEST,
                    String.join("\n",
                        "junit:junit",
                        "com.nimbusds:nimbus-jose-jwt",
                        "org.glassfish.jersey.core:jersey-client")),
                10
            )
        );

    }

    /**
     * Create an {@link ActiveRule} with the supplied configuration.
     *
     * @param ruleKey the rule key
     * @param allowedDeps the comma seperated dependency name string
     *
     * @return a rule configured with the expected values.
     */
    private static ActiveRule createNonTemplatedTestRule(final RuleKey ruleKey, final String allowedDeps) {

        final NewActiveRule rule = new NewActiveRule.Builder()
            .setRuleKey(ruleKey)
            .setParam(MavenRulesDefinition.DEPS_PARAM_KEY, allowedDeps)
            .build();

        return new DefaultActiveRules(Arrays.asList(rule)).find(ruleKey);
    }

    /**
     * Create an {@link ActiveRule} with the supplied configuration. Rules created by this method will be set as created by
     * the templated rule, {@link MavenRulesDefinition#RULE_MAVEN_ALLOWED}.
     *
     * @param ruleKey the rule key
     * @param allowedDeps the comma seperated dependency name string
     *
     * @return a rule configured with the expected values.
     */
    private static ActiveRule createTemplatedTestRule(final RuleKey ruleKey, final String allowedDeps, final String scopes) {

        final NewActiveRule rule = new NewActiveRule.Builder()
            .setRuleKey(ruleKey)
            .setTemplateRuleKey(MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule())
            .setParam(MavenRulesDefinition.DEPS_PARAM_KEY, allowedDeps)
            .setParam(MavenRulesDefinition.SCOPES_PARAM_KEY, scopes)
            .build();

        return new DefaultActiveRules(Arrays.asList(rule)).find(ruleKey);

    }


}
