package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.util.PredicateFactory;

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Object holding the configuration for an
 * {@link AllowedMavenDependenciesCheck}.
 */
public class AllowedMavenDependenciesCheckConfig {

    /**
     * Logger
     */
    private static final Logger LOG = Loggers.get(AllowedMavenDependenciesCheckConfig.class);

    /**
     * The rule this configuration was created for.
     */
    private final ActiveRule rule;

    /**
     * Predicate created which can be used for matching dependencies against the configured the allowed dependency list.
     */
    private final Predicate<String> allowedDependenciesPredicate;

    /**
     * If a non-empty value is set for this, restrict to only dependencies with the
     * given scope.
     */
    private final List<String> restrictToScopes;

    /**
     * Create a new {@link AllowedMavenDependenciesCheck} based on an active rule.
     *
     * @param activeRuleDefinition the rule containing the parameter configuration.
     */
    public AllowedMavenDependenciesCheckConfig(final ActiveRule activeRuleDefinition) {

        LOG.info("Creating AllowedMavenDependenciesCheck for {}", activeRuleDefinition.ruleKey());
        this.rule = activeRuleDefinition;

        /* Configure the allowed dependency predicate */
        final String deps = activeRuleDefinition.param(MavenRulesDefinition.DEPS_PARAM_KEY);
        final PredicateFactory predicateFactory = new PredicateFactory();
        this.allowedDependenciesPredicate = predicateFactory.createPredicateForDependencyListString(deps);

        /* Configure the check scope */
        this.restrictToScopes = getScopeConfiguration(activeRuleDefinition);
    }

    /**
     * Get out the list of scopes this configuration will apply to, based on the
     * rule definition.
     *
     * @param activeRuleDefinition the rule to extract the configuration from
     * @return list of scope names.
     */
    private static List<String> getScopeConfiguration(final ActiveRule activeRuleDefinition) {

        final String checkScope;
        if (activeRuleDefinition.ruleKey().equals(MavenRulesDefinition.RULE_MAVEN_ALLOWED_MAIN)) {
            checkScope = MavenRulesDefinition.MAIN_SCOPES;
        } else if (activeRuleDefinition.ruleKey().equals(MavenRulesDefinition.RULE_MAVEN_ALLOWED_TEST)) {
            checkScope = "test";
        } else if (MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule().equals(activeRuleDefinition.templateRuleKey())) {
            checkScope = activeRuleDefinition.param(MavenRulesDefinition.SCOPES_PARAM_KEY);
        } else {
            throw new IllegalArgumentException("Unsupported rule: " + activeRuleDefinition.ruleKey());
        }

        final List<String> scopes;
        if (checkScope == null) {
            scopes = Collections.emptyList();
        } else {
            scopes = Arrays.asList(checkScope.split(","))
                .stream()
                .map(String::trim)
                .sorted()
                .collect(Collectors.toList());
        }

        return scopes;
    }

    /**
     * Get the predicate for the allowed dependencies included in this configuration.
     *
     * @return created predicate.
     */
    public Predicate<String> getAllowedDependenciesPredicate() {
        return allowedDependenciesPredicate;
    }

    /**
     * Get the scopes included in this configuration.
     *
     * @return list of scope names.
     */
    public List<String> getScopes() {
        return restrictToScopes;
    }

    /**
     * Get the rule this config was created from.
     *
     * @return the rule
     */
    public ActiveRule getRule() {
        return rule;
    }

}
