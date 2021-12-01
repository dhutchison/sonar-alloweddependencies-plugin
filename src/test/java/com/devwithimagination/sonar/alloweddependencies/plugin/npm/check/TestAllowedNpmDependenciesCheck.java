package com.devwithimagination.sonar.alloweddependencies.plugin.npm.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.stream.Stream;

import com.devwithimagination.sonar.alloweddependencies.plugin.npm.checks.AllowedNpmDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;

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

/**
 * Test case for {@link AllowedNpmDependenciesCheck}
 */
class TestAllowedNpmDependenciesCheck {

    /**
     * The file we will scan for tests.
     */
    private InputFile inputFile;

    /**
     * The sensor context used in the scan.
     */
    private SensorContext sensorContext;

    /**
     * Setup any configuration between tests.
     */
    @BeforeEach
    void setup() {

        /* Setup the test project location */
        final File moduleBaseDir = new File("src/test/resources/npm");
        final File basePath = new File(moduleBaseDir, "package.json");

        this.inputFile = new TestInputFileBuilder(getClass().getName(), moduleBaseDir, basePath).build();



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
    void checkForNoViolations(final RuleKey ruleKey, final String allowedDeps) {

        /* Configure our rule with configuration */
        final ActiveRule rule = createTestRule(ruleKey, allowedDeps);

        /* Scan our test file, and confirm no issues were raised */
        final AllowedNpmDependenciesCheck check = new AllowedNpmDependenciesCheck(rule);
        check.scanFile(inputFile, sensorContext);

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
    void checkForViolations(final RuleKey ruleKey, final String allowedDeps, final int expectedIssues) {

        /* Configure our rule with configuration */
        final ActiveRule rule = createTestRule(ruleKey, allowedDeps);

        /* Scan our test file, and confirm the right number of issues were raised */
        final AllowedNpmDependenciesCheck check = new AllowedNpmDependenciesCheck(rule);
        check.scanFile(inputFile, sensorContext);

        verify(sensorContext, times(expectedIssues)).newIssue();

    }

    /**
     * Test that when presented with an unknown rule key, the check throws an
     * exception.
     */
    @Test
    void testUnknownRuleKey() {

        final ActiveRule rule = createTestRule(RuleKey.of(NpmRulesDefinition.REPOSITORY_NPM, "my-fake-rule"), null);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AllowedNpmDependenciesCheck(rule));

        assertEquals("Unsupported rule key: allowed-dependencies-npm:my-fake-rule", exception.getMessage());

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

        final ActiveRule rule = mock(ActiveRule.class);
        when(rule.ruleKey()).thenReturn(ruleKey);
        when(rule.param(NpmRulesDefinition.DEPS_PARAM_KEY)).thenReturn(allowedDeps);

        return rule;
    }

    /**
     * Method to create the parameters for {@link #checkForNoViolations()}.
     * @return Stream containing the argument pairs.
     */
    private static Stream<Arguments> provideNoViolationParameters() {

        return Stream.of(
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_DEV,
                String.join("\n",
                    "@angular/cli",
                    "@angular/compiler-cli",
                    "@angular/language-service",
                    "@cypress/code-coverage")),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_DEV,
                String.join("\n",
                    "regex:@angular/.*",
                    "@cypress/code-coverage")),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED,
                String.join("\n",
                    "primeicons",
                    "primeng",
                    "rxjs",
                    "tslib",
                    "uuid")),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_PEER,
                String.join("\n",
                    "tea",
                    "coffee"))
        );

    }

    /**
     * Method to create the parameters for {@link #checkForViolations()}.
     * @return Stream containing the arguments.
     */
    private static Stream<Arguments> provideViolationParameters() {

        return Stream.of(
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_DEV,
                "@cypress/code-coverage",
                3 ),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_DEV,
                "regex:@angular/.*",
                1),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_DEV,
                null,
                4),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_DEV,
                "",
                4),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED,
                String.join("\n",
                    "primeicons",
                    "tslib",
                    "uuid"),
                2),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_PEER,
                "",
                2),
            Arguments.of(
                NpmRulesDefinition.RULE_NPM_ALLOWED_PEER,
                "tea",
                1)

        );

    }


}
