package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.function.Predicate;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import org.sonar.api.batch.rule.ActiveRule;

/** Configuration for {@link AllowedMavenPluginsCheck}. */
public class AllowedMavenPluginsCheckConfig {

    private final ActiveRule rule;
    private final Predicate<String> allowedPluginsPredicate;

    public AllowedMavenPluginsCheckConfig(final ActiveRule rule) {
        if (!MavenRulesDefinition.RULE_MAVEN_ALLOWED_PLUGINS.equals(rule.ruleKey())) {
            throw new IllegalArgumentException("Unsupported Maven plugin rule: " + rule.ruleKey());
        }
        this.rule = rule;
        this.allowedPluginsPredicate = new MavenCoordinatePredicateFactory().create(
            rule.param(MavenRulesDefinition.PLUGINS_PARAM_KEY), MavenXmlCoordinates::normalizePluginAllowListRow);
    }

    public ActiveRule getRule() {
        return rule;
    }

    public Predicate<String> getAllowedPluginsPredicate() {
        return allowedPluginsPredicate;
    }
}
