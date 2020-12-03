package com.devwithimagination.sonar.alloweddependencies.settings;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

/**
 * Class holding any property definitions which this plugin exposes.
 */
public class AllowedDependenciesProperties {

  /**
   * The name of the category to display these settings in within the Sonarqube UI.
   */
  public static final String CATEGORY = "Allowed Dependencies";

  /**
   * The setting key for the maven allowed dependency list.
   */
  public static final String MAVEN_KEY = "sonar.allowed-dependencies.maven";

  /**
   * The setting key for the NPM allowed dependency list.
   */
  public static final String NPM_KEY = "sonar.allowed-dependencies.npm";

  /**
   * There is no need to create one of these, only contains static methods and constants.
   */
  private AllowedDependenciesProperties() {

  }

  /**
   * Get a list of the property definitions for this plugin.
   * @return the list of property definitions.
   */
  public static List<PropertyDefinition> getProperties() {
    return Arrays.asList(

      PropertyDefinition.builder(MAVEN_KEY)
        .name("Allowed Maven Dependencies")
        .description("Newline seperated list of <groupId>:<artifactId> items")
        .category(CATEGORY)
        .type(PropertyType.TEXT)
        .build(),

        PropertyDefinition.builder(NPM_KEY)
        .name("Allowed NPM Dependencies")
        .description("Newline seperated list of dependencies items")
        .category(CATEGORY)
        .type(PropertyType.TEXT)
        .build()
    );
  }

}
