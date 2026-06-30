
package com.devwithimagination.sonar.alloweddependencies;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.sensors.CreateIssuesOnMavenArtifactsSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.sensor.CreateIssuesOnNPMDependenciesSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.sensors.CreateIssuesOnPythonDependenciesSensor;

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
        PythonRulesDefinition.class,
        // Sensors for the checks
        CreateIssuesOnMavenArtifactsSensor.class,
        CreateIssuesOnNPMDependenciesSensor.class,
        CreateIssuesOnPythonDependenciesSensor.class
        );

  }
}
