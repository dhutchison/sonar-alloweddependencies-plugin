package com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Implementation of {@link RulesDefinition} which defines all the NPM related
 * rules used by this plugin.
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
     * The rule key for the NPM allowed dependency check, for regular dependencies.
     */
    public static final RuleKey RULE_NPM_ALLOWED = RuleKey.of(REPOSITORY_NPM, "allowed-dependencies-main");

    /**
     * The rule key for the NPM allowed dependency check, for development dependencies.
     */
    public static final RuleKey RULE_NPM_ALLOWED_DEV = RuleKey.of(REPOSITORY_NPM, "allowed-dependencies-dev");

    /**
     * The rule key for the NPM allowed dependency check, for perr dependencies.
     */
    public static final RuleKey RULE_NPM_ALLOWED_PEER = RuleKey.of(REPOSITORY_NPM, "allowed-dependencies-peer");

    /**
     * The setting key for the NPM allowed dependency list.
     */
    public static final String DEPS_PARAM_KEY = "npmDependencies";

    @Override
    public void define(Context context) {

        final NewRepository npmRepository = context.createRepository(REPOSITORY_NPM, NPM_DEPENDENCY_LANGUAGE)
            .setName("NPM  Allowed Dependency Analyzer");

        createRule(npmRepository, RULE_NPM_ALLOWED,
            "Allowed Dependencies (NPM)",
            "<p>This rule applies to dependencies in the main <code>dependencies</code> block.</p>");


        createRule(npmRepository, RULE_NPM_ALLOWED_DEV,
            "Allowed Development Dependencies (NPM)",
            "<p>This rule applies to dependencies in the <code>devDependencies</code> block.</p>");

        createRule(npmRepository, RULE_NPM_ALLOWED_PEER,
            "Allowed Peer Dependencies (NPM)",
            "<p>This rule applies to dependencies in the <code>peerDependencies</code> block.</p>");

        // don't forget to call done() to finalize the definition
        npmRepository.done();
    }


    /**
     * Create an allowed dependency rule.
     *
     * As we create a few of these, they have a standard patten with a few configurable elements.
     *
     * @param repository the repository to create the rule in.
     * @param key the key for the rule.
     * @param name the name of the rule.
     * @param description an extra part of the description which is specific to the rule.
     */
    private void createRule(final NewRepository repository,
        final RuleKey key, final String name, final String description) {

        final NewRule npmAllowedRule = repository.createRule(key.rule())
            .setName(name)
            .setHtmlDescription(
                "<p>Only approved dependencies should be used.</p>" +
                description +
                "<p>Generates an issue for every NPM dependency which is not in the allowed list</p>")

            // optional tags
            .setTags("npm", "dependency")

            // optional status. Default value is READY.
            .setStatus(RuleStatus.READY)

            // default severity when the rule is activated on a Quality profile. Default
            // value is MAJOR.
            .setSeverity(Severity.MINOR);

        npmAllowedRule.setDebtRemediationFunction(
            npmAllowedRule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

        /* Configure the parameters we want to configure in our rule template */
        npmAllowedRule.createParam(DEPS_PARAM_KEY)
            .setName("Allowed NPM Dependencies")
            .setDescription("Newline seperated list of dependencies items")
            .setType(RuleParamType.TEXT);

    }

}
