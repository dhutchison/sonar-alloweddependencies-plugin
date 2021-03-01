package com.devwithimagination.sonar.alloweddependencies.plugin.rules;

import com.devwithimagination.sonar.alloweddependencies.settings.AllowedDependenciesProperties;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Implementation of {@link RulesDefinition} which defines all the rules used by this plugin.
 */
public class DependencyRulesDefinition implements RulesDefinition {

  /**
   * The name of the rule repository the Maven rule is registered against.
   */
  public static final String REPOSITORY_MAVEN = "allowed-dependencies-maven";
  /**
   * The language this plugin registers the Maven rule against.
   */
  public static final String MAVEN_DEPENDENCY_LANGUAGE = "java";
  /**
   * The rule key for the Maven allowed dependency check
   */
  public static final RuleKey RULE_MAVEN_ALLOWED = RuleKey.of(REPOSITORY_MAVEN, "maven-allowed-dependencies");

  /**
   * The name of the rule repository the NPM rule is registered against.
   */
  public static final String REPOSITORY_NPM = "allowed-dependencies-npm";
  /**
   * The language this plugin registers the NPM rule against.
   */
  public static final String NPM_DEPENDENCY_LANGUAGE = "js";
  /**
   * The rule key for the NPM allowed dependency check
   */
  public static final RuleKey RULE_NPM_ALLOWED = RuleKey.of(REPOSITORY_NPM, "npm-allowed-dependencies");

  @Override
  public void define(Context context) {

    defineNpmRules(context);
    defineMavenRules(context);
  }

  /**
   * Define the rules for Maven dependencies.
   * @param context the SonarQube plugin context
   */
  private void defineMavenRules(final Context context) {
    final NewRepository javaRepository = context.createRepository(REPOSITORY_MAVEN, MAVEN_DEPENDENCY_LANGUAGE)
        .setName("My Custom Java Analyzer");

    final NewRule mavenAllowedRule = javaRepository.createRule(RULE_MAVEN_ALLOWED.rule())
      .setName("Allowed Dependencies (Maven)")
      .setHtmlDescription("Generates an issue for every Maven dependency which is not in the allowed list")

      /* Configure as a template rule so we can set parameters on it when it is added to the profile.
       * The alternative here would have been to define properties at the plugin value for approved
       * dependencies, but that would mean we could not have different quality profiles with different versions
       * of the rule.
       */
      .setTemplate(true)

      // optional tags
      .setTags("maven", "dependency")

      // optional status. Default value is READY.
      .setStatus(RuleStatus.BETA)

      // default severity when the rule is activated on a Quality profile. Default value is MAJOR.
      .setSeverity(Severity.MINOR);

      mavenAllowedRule.setDebtRemediationFunction(
          mavenAllowedRule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

    /* Configure the parameters we want to configure in our rule template */
    mavenAllowedRule.createParam(AllowedDependenciesProperties.MAVEN_KEY)
        .setName("Allowed Maven Dependencies")
        .setDescription("Newline seperated list of <groupId>:<artifactId> items")
        .setType(RuleParamType.TEXT);

    mavenAllowedRule.createParam(AllowedDependenciesProperties.MAVEN_SCOPE_KEY)
        .setName("Check scope")
        .setDescription("Maven scope to restrict checks to")
        .setDefaultValue(null)
        .setType(RuleParamType.STRING);

    // don't forget to call done() to finalize the definition
    javaRepository.done();
  }

  /**
   * Define the rules for NPM dependencies.
   * @param context the SonarQube plugin context
   */
  private void defineNpmRules(final Context context) {
    final NewRepository npmRepository = context.createRepository(REPOSITORY_NPM, NPM_DEPENDENCY_LANGUAGE)
        .setName("My Custom NPM Analyzer");

    final NewRule npmAllowedRule = npmRepository.createRule(RULE_NPM_ALLOWED.rule())
      .setName("Allowed Dependencies (NPM)")
      .setHtmlDescription("Generates an issue for every NPM dependency which is not in the allowed list")

      /* Configure as a template rule so we can set parameters on it when it is added to the profile.
       * The alternative here would have been to define properties at the plugin value for approved
       * dependencies, but that would mean we could not have different quality profiles with different versions
       * of the rule.
       */
      .setTemplate(true)

      // optional tags
      .setTags("npm", "dependency")

      // optional status. Default value is READY.
      .setStatus(RuleStatus.BETA)

      // default severity when the rule is activated on a Quality profile. Default value is MAJOR.
      .setSeverity(Severity.MINOR);

    npmAllowedRule.setDebtRemediationFunction(
        npmAllowedRule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

    /* Configure the parameters we want to configure in our rule template */
    npmAllowedRule.createParam(AllowedDependenciesProperties.NPM_KEY)
        .setName("Allowed NPM Dependencies")
        .setDescription("Newline seperated list of dependencies items")
        .setType(RuleParamType.TEXT);

    // don't forget to call done() to finalize the definition
    npmRepository.done();
  }


}
