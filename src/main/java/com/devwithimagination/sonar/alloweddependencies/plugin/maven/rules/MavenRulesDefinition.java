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

    private static final String ALLOW_LIST_PARAM_DESCRIPTION =
        "Newline separated list of <groupId>:<artifactId> dependency names. Exact matches are case-insensitive. " +
        "Prefix a row with regex: to allow dependencies matching a regular expression. " +
        "Blank lines and rows starting with # are ignored.";

    /**
     * The name of the rule repository the Maven rule is registered against.
     */
    public static final String REPOSITORY_MAVEN = "allowed-dependencies-maven";
    /**
     * The language this plugin registers the Maven rule against.
     */
    public static final String MAVEN_DEPENDENCY_LANGUAGE = "xml";

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

    /** The rule key for the Maven plugin allow-list check. */
    public static final RuleKey RULE_MAVEN_ALLOWED_PLUGINS =
        RuleKey.of(REPOSITORY_MAVEN, "maven-allowed-plugins");

    /** The rule key for the Maven extension allow-list check. */
    public static final RuleKey RULE_MAVEN_ALLOWED_EXTENSIONS =
        RuleKey.of(REPOSITORY_MAVEN, "maven-allowed-extensions");

    /**
     * The setting key for the maven allowed dependency list.
     */
    public static final String DEPS_PARAM_KEY = "mavenDependencies";

    /**
     * The setting parameter for the maven rule scope.
     */
    public static final String SCOPES_PARAM_KEY = "mavenScopes";

    /** The setting key for the Maven plugin allow list. */
    public static final String PLUGINS_PARAM_KEY = "mavenPlugins";

    /** The setting key for the Maven extension allow list. */
    public static final String EXTENSIONS_PARAM_KEY = "mavenExtensions";

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

        createCoordinateRule(javaRepository, RULE_MAVEN_ALLOWED_PLUGINS,
            new CoordinateRuleDefinition(
                new RulePresentation("Allowed Maven Plugins", "Only approved Maven plugins should be used.",
                    "Generates an issue for every activated Maven build or reporting plugin which is not in the allowed list.",
                    "plugin"),
                new ParameterDefinition(PLUGINS_PARAM_KEY, "Allowed Maven Plugins",
                    "Newline separated list of Maven plugin coordinates. Exact artifact-only rows use the "
                        + "org.apache.maven.plugins default group. Regex rows match canonical "
                        + "<groupId>:<artifactId> coordinates.")));

        createCoordinateRule(javaRepository, RULE_MAVEN_ALLOWED_EXTENSIONS,
            new CoordinateRuleDefinition(
                new RulePresentation("Allowed Maven Extensions", "Only approved Maven extensions should be used.",
                    "Generates an issue for every Maven extension which is not in the allowed list.", "extension"),
                new ParameterDefinition(EXTENSIONS_PARAM_KEY, "Allowed Maven Extensions",
                    "Newline separated list of fully-qualified <groupId>:<artifactId> Maven extension coordinates. "
                        + "Regex rows match canonical coordinates.")));

        // don't forget to call done() to finalize the definition
        javaRepository.done();
    }

    private NewRule createCoordinateRule(final NewRepository repository, final RuleKey ruleKey,
            final CoordinateRuleDefinition definition) {

        final NewRule createdRule = repository.createRule(ruleKey.rule())
            .setName(definition.presentation.name)
            .setHtmlDescription("<p>" + definition.presentation.summary + "</p><p>"
                + definition.presentation.issueDescription + "</p>")
            .setTags("maven", definition.presentation.tag)
            .setStatus(RuleStatus.BETA)
            .setSeverity(Severity.MINOR);

        createdRule.setDebtRemediationFunction(
            createdRule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
        createdRule.createParam(definition.parameter.key)
            .setName(definition.parameter.name)
            .setDescription(definition.parameter.description
                + " Exact matches are case-insensitive. Prefix a row with regex: "
                + "to use a regular expression. Blank lines and rows starting with # are ignored.")
            .setType(RuleParamType.TEXT);
        return createdRule;
    }

    private static final class CoordinateRuleDefinition {
        private final RulePresentation presentation;
        private final ParameterDefinition parameter;

        private CoordinateRuleDefinition(final RulePresentation presentation, final ParameterDefinition parameter) {
            this.presentation = presentation;
            this.parameter = parameter;
        }
    }

    private static final class RulePresentation {
        private final String name;
        private final String summary;
        private final String issueDescription;
        private final String tag;

        private RulePresentation(final String name, final String summary, final String issueDescription,
                final String tag) {
            this.name = name;
            this.summary = summary;
            this.issueDescription = issueDescription;
            this.tag = tag;
        }
    }

    private static final class ParameterDefinition {
        private final String key;
        private final String name;
        private final String description;

        private ParameterDefinition(final String key, final String name, final String description) {
            this.key = key;
            this.name = name;
            this.description = description;
        }
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
                .setDescription(ALLOW_LIST_PARAM_DESCRIPTION).setType(RuleParamType.TEXT);

        return createdRule;

    }

}
