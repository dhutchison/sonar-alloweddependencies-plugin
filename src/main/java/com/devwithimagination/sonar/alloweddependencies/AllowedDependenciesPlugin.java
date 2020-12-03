
package com.devwithimagination.sonar.alloweddependencies;

import com.devwithimagination.sonar.alloweddependencies.plugin.rules.CreateIssuesOnMavenDependenciesSensor;
import com.devwithimagination.sonar.alloweddependencies.plugin.rules.MavenDependencyRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.settings.AllowedDependenciesProperties;

import org.sonar.api.Plugin;

/**
 * This class is the entry point for all extensions. It is referenced in pom.xml.
 */
public class AllowedDependenciesPlugin implements Plugin {

  @Override
  public void define(Context context) {
    // Maven dependency rules
    context.addExtensions(MavenDependencyRulesDefinition.class, CreateIssuesOnMavenDependenciesSensor.class);

    // Configuration settings
    context.addExtensions(AllowedDependenciesProperties.getProperties());

  }
}
