package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import org.sonar.api.batch.rule.ActiveRule;

/** Configuration for {@link AllowedMavenExtensionsCheck}. */
public class AllowedMavenExtensionsCheckConfig {

    private final ActiveRule rule;
    private final Predicate<String> allowedExtensionsPredicate;

    public AllowedMavenExtensionsCheckConfig(final ActiveRule rule) {
        if (!MavenRulesDefinition.RULE_MAVEN_ALLOWED_EXTENSIONS.equals(rule.ruleKey())) {
            throw new IllegalArgumentException("Unsupported Maven extension rule: " + rule.ruleKey());
        }
        this.rule = rule;
        this.allowedExtensionsPredicate = new MavenCoordinatePredicateFactory().create(
            rule.param(MavenRulesDefinition.EXTENSIONS_PARAM_KEY), UnaryOperator.identity());
    }

    public ActiveRule getRule() {
        return rule;
    }

    public Predicate<String> getAllowedExtensionsPredicate() {
        return allowedExtensionsPredicate;
    }
}
