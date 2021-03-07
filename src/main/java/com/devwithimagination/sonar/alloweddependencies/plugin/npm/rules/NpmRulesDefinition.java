package com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Implementation of {@link RulesDefinition} which defines all the NPM related rules used by this plugin.
 */
public class NpmRulesDefinition implements RulesDefinition {

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

  /**
     * The setting key for the NPM allowed dependency list.
     */
    public static final String DEPS_PARAM_KEY = "npmDependencies";

    /**
     * The setting parameter for the npm rule scope.
     */
    public static final String SCOPES_PARAM_KEY = "npmDependencyScopes";

  @Override
  public void define(Context context) {


   // TODO: There are only two scopes here - create two rules and use a common check implementation

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
    npmAllowedRule.createParam(DEPS_PARAM_KEY)
        .setName("Allowed NPM Dependencies")
        .setDescription("Newline seperated list of dependencies items")
        .setType(RuleParamType.TEXT);

    npmAllowedRule.createParam(SCOPES_PARAM_KEY)
        .setName("Development scope")
        .setDescription("Boolean controlling if this check is for devDependencies (true) or regular dependencies (false)")
        .setType(RuleParamType.BOOLEAN);

    // don't forget to call done() to finalize the definition
    npmRepository.done();
  }


}
