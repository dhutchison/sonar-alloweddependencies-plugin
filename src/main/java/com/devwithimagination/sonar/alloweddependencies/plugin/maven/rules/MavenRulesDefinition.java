package com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Implementation of {@link RulesDefinition} which defines all the maven related
 * rules used by this plugin.
 */
public class MavenRulesDefinition implements RulesDefinition {

    /**
     * The name of the rule repository the Maven rule is registered against.
     */
    public static final String REPOSITORY_MAVEN = "allowed-dependencies-maven";
    /**
     * The language this plugin registers the Maven rule against.
     */
    public static final String MAVEN_DEPENDENCY_LANGUAGE = "java";

    /**
     * The rule key for the templated Maven allowed dependency check
     */
    public static final RuleKey RULE_MAVEN_ALLOWED = RuleKey.of(REPOSITORY_MAVEN, "maven-allowed-dependencies");

    /**
     * The rule key for the Maven allowed dependency check for non-test scopes
     */
    public static final RuleKey RULE_MAVEN_ALLOWED_MAIN = RuleKey.of(REPOSITORY_MAVEN, "maven-allowed-dependencies-main");

    /**
     * The scopes which are included for {@link #RULE_MAVEN_ALLOWED_MAIN}
     */
    public static final String MAIN_SCOPES = "compile, provided, runtime";

    /**
     * The rule key for the Maven allowed dependency check for the test scope
     */
    public static final RuleKey RULE_MAVEN_ALLOWED_TEST = RuleKey.of(REPOSITORY_MAVEN, "maven-allowed-dependencies-test");

    /**
     * The setting key for the maven allowed dependency list.
     */
    public static final String DEPS_PARAM_KEY = "mavenDependencies";

    /**
     * The setting parameter for the maven rule scope.
     */
    public static final String SCOPES_PARAM_KEY = "mavenScopes";

    @Override
    public void define(Context context) {

        final NewRepository javaRepository = context.createRepository(REPOSITORY_MAVEN, MAVEN_DEPENDENCY_LANGUAGE)
                .setName("Maven Allowed Dependency Analyzer");

        /* Create the templated rule that will allow any combination of scopes. */
        final NewRule templateRule = createMavenRuleWithCommonConfigurationParts(javaRepository, RULE_MAVEN_ALLOWED,
                "Allowed Dependencies (template)",
                "<p>This rule is created as a template to allow multiple instances of " +
                "the rule to be created for different scopes. " +
                "Note that if you have multiple instances of this rule in the same quality profile for the same scope " +
                "the configuration will not be combined which may lead to unexpected behaviour.</p>")

            /*
            * Configure as a template rule so we can set parameters on it when it is added
            * to the profile. The alternative here would have been to define properties at
            * the plugin value for approved dependencies, but that would mean we could not
            * have different quality profiles with different versions of the rule.
            */
            .setTemplate(true);

        templateRule.createParam(SCOPES_PARAM_KEY).setName("Check scope(s)")
                .setDescription("Comma seperated list of Maven scope(s) to restrict checks to. ").setDefaultValue(null)
                .setType(RuleParamType.STRING);


        /* Create the fixed scope rules */
        createMavenRuleWithCommonConfigurationParts(javaRepository, RULE_MAVEN_ALLOWED_MAIN, "Allowed Dependencies (Main Scopes)",
            "<p>This rule will look at dependencies in the following scopes: " + MAIN_SCOPES + ".</p>");

        createMavenRuleWithCommonConfigurationParts(javaRepository, RULE_MAVEN_ALLOWED_TEST, "Allowed Dependencies (Test Scope)",
            "<p>This rule will look at dependencies in the \"test\" scope only.</p>");

        // don't forget to call done() to finalize the definition
        javaRepository.done();
    }

    /**
     * Create a new maven allowed dependency rule containing the configuration parts which are common.
     * @param repository the repository to create the rule in
     * @param ruleKey the key for the rule to create
     * @param name the name of the rule to create
     * @param descriptionExtra and extra part of HTML text to include int he rule description
     * @return the created rule.
     */
    private NewRule createMavenRuleWithCommonConfigurationParts(final NewRepository repository, final RuleKey ruleKey,
            final String name, final String descriptionExtra) {

        final NewRule createdRule = repository.createRule(ruleKey.rule())
                .setName(name)
                .setHtmlDescription("<p>Only approved dependencies should be used.</p>" +

                        "<p>Out of the box SonarQube has rule <a href=\"https://rules.sonarsource.com/java/tag/maven/RSPEC-3417\">" +
                        "RSPEC-3417</a> for reporting if a specific banned dependency is used. This provides the opposite approach " +
                        "by allowing the definition of an approved list of dependencies, with issues raised for any dependencies " +
                        "used which are not in the approved list. </p>" +

                        descriptionExtra +

                        "<p>Generates an issue for every Maven dependency which is not in the allowed list</p>")

                // optional tags
                .setTags("maven", "dependency")

                // optional status. Default value is READY.
                .setStatus(RuleStatus.BETA)

                // default severity when the rule is activated on a Quality profile. Default
                // value is MAJOR.
                .setSeverity(Severity.MINOR);

        createdRule.setDebtRemediationFunction(
                createdRule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

        /* Configure the parameters we want to configure in our rule template */
        createdRule.createParam(DEPS_PARAM_KEY).setName("Allowed Maven Dependencies")
                .setDescription("Newline seperated list of <groupId>:<artifactId> items").setType(RuleParamType.TEXT);

        return createdRule;

    }

}
