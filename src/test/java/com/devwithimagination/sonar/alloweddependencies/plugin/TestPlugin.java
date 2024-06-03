package com.devwithimagination.sonar.alloweddependencies.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devwithimagination.sonar.alloweddependencies.AllowedDependenciesPlugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.Plugin.Context;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;

/**
 * Basic test for testing project setup.
 */
class TestPlugin {

    /**
     * Tests that testing is setup correctly.
     */
    @Test
    void checkWorldIsSane() {
        assertTrue(true);
    }

    /**
     * Tests that the component registers the expected number of components.
     */
    @Test
    void checkPluginRegistersCorrectNumberOfComponents() {

        /* Variables we will use for counts */
        int sensorCount = 0;
        int ruleDefinitionCount = 0;

        /* Setup the component under test */
        final Plugin plugin = new AllowedDependenciesPlugin();

        /* Run the plugin definition */
        SonarRuntime runtime = mock(SonarRuntime.class);
        when(runtime.getEdition()).thenReturn(SonarEdition.COMMUNITY);
        when(runtime.getApiVersion()).thenReturn(Version.parse("9.9.5"));
        when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        final Context context = new Context(runtime);
        plugin.define(context);

        /* Check the registered extensions & count */
        assertEquals(4, context.getExtensions().size(), "Expected 4 extensions to be registered");
        for (Object obj : context.getExtensions()) {

            if (obj instanceof Class) {
                Class<?> clazz = ((Class<?>) obj);

                if (Sensor.class.isAssignableFrom(clazz)) {
                    sensorCount++;
                }
                if (RulesDefinition.class.isAssignableFrom(clazz)) {
                    ruleDefinitionCount++;
                }
            }
        }

        assertEquals(2, sensorCount, "Expected 2 sensor definitions");
        assertEquals(2, ruleDefinitionCount, "Expected 2 rule definitions");

    }


}
