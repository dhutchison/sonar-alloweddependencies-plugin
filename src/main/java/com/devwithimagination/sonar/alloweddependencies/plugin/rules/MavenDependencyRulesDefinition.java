package com.devwithimagination.sonar.alloweddependencies.plugin.rules;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class MavenDependencyRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY = "allowed-dependencies";
  public static final String DEPENDENCY_LANGUAGE = "java";
  public static final RuleKey RULE_MAVEN_ALLOWED = RuleKey.of(REPOSITORY, "maven-allowed-dependencies");

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY, DEPENDENCY_LANGUAGE).setName("My Custom Java Analyzer");

    NewRule mavenAllowedRule = repository.createRule(RULE_MAVEN_ALLOWED.rule())
      .setName("Allowed Dependencies (Maven)")
      .setHtmlDescription("Generates an issue for every Maven dependency which is not in the allowed list")

      // optional tags
      .setTags("maven", "dependency")

      // optional status. Default value is READY.
      .setStatus(RuleStatus.BETA)

      // default severity when the rule is activated on a Quality profile. Default value is MAJOR.
      .setSeverity(Severity.MINOR);

      mavenAllowedRule.setDebtRemediationFunction(
          mavenAllowedRule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

    // don't forget to call done() to finalize the definition
    repository.done();
  }
}
