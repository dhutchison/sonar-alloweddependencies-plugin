package com.devwithimagination.sonar.alloweddependencies.plugin.maven.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Stream;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.analyzer.commons.xml.XmlFile;

public class TestAllowedMavenDependenciesCheck {

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
     * @param ruleKey the rule key the test is for
     * @param allowedDeps the newline seperated list of allowed dependencies
     */
    @ParameterizedTest
    @MethodSource("provideNoViolationParameters")
    void checkForNoViolations(final String ruleKey, final String allowedDeps, final String dependencyScopes) {

        /* Configure our rule with configuration */
        final ActiveRule rule = createTestRule(ruleKey, allowedDeps, dependencyScopes);

        /* Scan our test file, and confirm no issues were raised */
        final AllowedMavenDependenciesCheck check = new AllowedMavenDependenciesCheck(rule);

        check.scanFile(sensorContext, rule.ruleKey(), inputFile);

        verify(sensorContext, never()).newIssue();
    }

    /**
     * Creates tests for the dev and regular dependencies with not all of the appropriate test file
     * dependencies configured, to ensure we get issues raised.
     *
     * @param ruleKey the rule key the test is for
     * @param allowedDeps the newline seperated list of allowed dependencies
     * @param expectedIssues the number of issues expected to be raised
     */
    @ParameterizedTest
    @MethodSource("provideViolationParameters")
    void checkForViolations(final String ruleKey, final String allowedDeps, final String dependencyScopes, final int expectedIssues) {

        /* Configure our rule with configuration */
        final ActiveRule rule = createTestRule(ruleKey, allowedDeps, dependencyScopes);

        /* Scan our test file, and confirm the right number of issues were raised */
        final AllowedMavenDependenciesCheck check = new AllowedMavenDependenciesCheck(rule);
        check.scanFile(sensorContext, rule.ruleKey(), inputFile);

        verify(sensorContext, times(expectedIssues)).newIssue();

    }

    /**
     * Create a mock {@link ActiveRule} with the supplied configuration.
     *
     * @param ruleKey the rule key
     * @param allowedDeps the comma seperated dependency name string
     * @param restrictionScope the comman seperated dependency restriction scopes
     *
     * @return a Mockito mock for the rule, configured with the expected values.
     */
    private ActiveRule createTestRule(final String ruleKey, final String allowedDeps, final String restrictionScope) {

        final ActiveRule rule = mock(ActiveRule.class);
        when(rule.ruleKey()).thenReturn(RuleKey.of(MavenRulesDefinition.REPOSITORY_MAVEN, ruleKey));
        when(rule.templateRuleKey()).thenReturn(MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule());
        when(rule.param(MavenRulesDefinition.DEPS_PARAM_KEY)).thenReturn(allowedDeps);
        when(rule.param(MavenRulesDefinition.SCOPES_PARAM_KEY)).thenReturn(restrictionScope);

        return rule;
    }

    /**
     * Method to create the parameters for {@link #checkForNoViolations()}.
     * @return Stream containing the argument pairs.
     */
    private static Stream<Arguments> provideNoViolationParameters() {

        return Stream.of(
            Arguments.of(
                "my-compile-deps",
                "com.github.javafaker:javafaker",
                "compile"),
            Arguments.of(
                "my-test-deps",
                String.join("\n",
                    "junit:junit",
                    "com.nimbusds:nimbus-jose-jwt",
                    "org.glassfish.jersey.core:jersey-client",
                    "org.bouncycastle:bcpkix-jdk15on",
                    "org.apache.logging.log4j:log4j-slf4j18-impl",
                    "org.apache.cxf:cxf-rt-rs-mp-client",
                    "org.eclipse.microprofile.rest.client:microprofile-rest-client-api",
                    "com.opentable.components:otj-pg-embedded",
                    "org.flywaydb:flyway-core",
                    "org.jacoco:org.jacoco.agent"),
                "test"
            )
        );

    }

    /**
     * Method to create the parameters for {@link #checkForViolations()}.
     * @return Stream containing the argument pairs.
     */
    private static Stream<Arguments> provideViolationParameters() {

        return Stream.of(
            Arguments.of(
                "no-compile-deps",
                "",
                "compile",
                1),
            Arguments.of(
                "no-provided-deps",
                "",
                "provided",
                2),
            Arguments.of(
                "no-combined-compile-provided-deps",
                "",
                "compile,provided",
                3),
            Arguments.of(
                "some-combined-compile-provided-deps",
                "javax.cache:cache-api",
                "compile,provided",
                2),
            Arguments.of(
                "one-provided-deps",
                "javax.cache:cache-api",
                "provided",
                1),
            Arguments.of(
                "some-test-deps",
                String.join("\n",
                    "junit:junit",
                    "com.nimbusds:nimbus-jose-jwt",
                    "org.glassfish.jersey.core:jersey-client"),
                "test",
                7
            )
        );

    }


}
