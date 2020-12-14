
package com.devwithimagination.sonar.alloweddependencies;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.CreateIssuesOnMavenDependenciesSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.CreateIssuesOnNPMDependenciesSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.rules.DependencyRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.settings.AllowedDependenciesProperties;

import org.sonar.api.Plugin;

/**
 * This class is the entry point for all extensions. It is referenced in pom.xml.
 */
public class AllowedDependenciesPlugin implements Plugin {

  @Override
  public void define(Context context) {
    // Dependency rules
    context.addExtensions(
        DependencyRulesDefinition.class,
        CreateIssuesOnMavenDependenciesSensor.class,
        CreateIssuesOnNPMDependenciesSensor.class);

    // Configuration settings
    context.addExtensions(AllowedDependenciesProperties.getProperties());

  }
}
