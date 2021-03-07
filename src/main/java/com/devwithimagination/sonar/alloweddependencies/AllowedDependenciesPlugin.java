
package com.devwithimagination.sonar.alloweddependencies;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.sensors.CreateIssuesOnMavenDependenciesSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.sensor.CreateIssuesOnNPMDependenciesSensor;

import org.sonar.api.Plugin;

/**
 * This class is the entry point for all extensions. It is referenced in pom.xml.
 */
public class AllowedDependenciesPlugin implements Plugin {

  @Override
  public void define(Context context) {


    context.addExtensions(
        // Dependency rules
        MavenRulesDefinition.class,
        NpmRulesDefinition.class,
        // Sensors for the checks
        CreateIssuesOnMavenDependenciesSensor.class,
        CreateIssuesOnNPMDependenciesSensor.class
        );

  }
}
