package com.devwithimagination.sonar.alloweddependencies.plugin.common.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.stream.Stream;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.sensors.CreateIssuesOnMavenDependenciesSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.sensor.CreateIssuesOnNPMDependenciesSensor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

/**
 * The implementation of our two sensors have a lot of commonality.
 *
 * This parameterised test will cover the common functionality.
 */
class TestCommonSensorBehavior {

    /**
     * Test the sensor desciptor gets populated.
     *
     * @param sensorUnderTest the sensor implementation we are testing.
     * @param expectedLanguage the language which the sensor is expected to be associated with
     * @param expectedRepository the repository which the sensor is expected to be associated with
     */
    @ParameterizedTest
    @MethodSource("provideDescribeParameters")
    void testDescribe(final Sensor sensorUnderTest, final String expectedLanguage, final String expectedRepository) {

        /* Create the descriptor */
        final DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

        /* Populate it */
        sensorUnderTest.describe(descriptor);

        /* Check some expected fields are populated */
        assertNotNull(descriptor.name(), "Name should be set");
        assertEquals(1, descriptor.languages().size(), "Should be configured for one language");
        assertEquals(expectedLanguage,
            descriptor.languages().toArray(new String[1])[0],
            "Language should match");

        assertEquals(1, descriptor.ruleRepositories().size(),
            "Should be configured for one rule repository");
        assertEquals(expectedRepository,
            descriptor.ruleRepositories().toArray(new String[1])[0],
            "Rule repository should match");

    }

    /**
     * Test that when executed with no enabled rules, the tests do not try scanning files.
     *
     * @param sensorUnderTest the sensor implementation we are testing.
     */
    @ParameterizedTest
    @MethodSource("provideExecuteNoRulesParameters")
    void testExecuteNoRules(final Sensor sensorUnderTest) {

        /* Configure the sensor context with the parts we need mocked */
        final ActiveRules activeRules = new DefaultActiveRules(Collections.emptyList());

        final SensorContext sensorContext = mock(SensorContext.class);
        when(sensorContext.activeRules()).thenReturn(activeRules);

        /* Run the test component */
        sensorUnderTest.execute(sensorContext);

        /* Check we never tried to hit the filesystem, we shouldn't have if no rules are enabled */
        verify(sensorContext, never()).fileSystem();

    }




    /**
     * Method to create the parameters for {@link #testDescribe(Sensor, String, String)}.
     * @return Stream containing the argument combinations.
     */
    private static Stream<Arguments> provideDescribeParameters() {

        return Stream.of(
            Arguments.of(
                new CreateIssuesOnNPMDependenciesSensor(null),
                NpmRulesDefinition.NPM_DEPENDENCY_LANGUAGE,
                NpmRulesDefinition.REPOSITORY_NPM),
            Arguments.of(
                new CreateIssuesOnMavenDependenciesSensor(null),
                MavenRulesDefinition.MAVEN_DEPENDENCY_LANGUAGE,
                MavenRulesDefinition.REPOSITORY_MAVEN)
        );

    }

    /**
     * Method to create the parameters for {@link #testExecuteNoRules(Sensor)}.
     * @return Stream containing the argument combinations.
     */
    private static Stream<Arguments> provideExecuteNoRulesParameters() {

        return Stream.of(
            Arguments.of(new CreateIssuesOnNPMDependenciesSensor(null)),
            Arguments.of(new CreateIssuesOnMavenDependenciesSensor(null))
        );

    }

}
