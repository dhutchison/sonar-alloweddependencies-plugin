package com.devwithimagination.sonar.alloweddependencies.plugin.maven.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenExtensionsCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenPluginsCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;

class TestAllowedMavenArtifactCheckConfigs {

    @Test
    void pluginConfigNormalizesOnlyExactArtifactShorthand() {
        final ActiveRule rule = rule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_PLUGINS,
            MavenRulesDefinition.PLUGINS_PARAM_KEY,
            String.join("\n", "maven-compiler-plugin", "regex:org\\.example:.*"));

        final AllowedMavenPluginsCheckConfig config = new AllowedMavenPluginsCheckConfig(rule);

        assertSame(rule, config.getRule());
        assertTrue(config.getAllowedPluginsPredicate()
            .test("org.apache.maven.plugins:maven-compiler-plugin"));
        assertTrue(config.getAllowedPluginsPredicate().test("org.example:a-plugin"));
        assertFalse(config.getAllowedPluginsPredicate().test("com.example:maven-compiler-plugin"));
    }

    @Test
    void extensionConfigRequiresFullExactCoordinate() {
        final ActiveRule rule = rule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_EXTENSIONS,
            MavenRulesDefinition.EXTENSIONS_PARAM_KEY, "approved-extension");

        final AllowedMavenExtensionsCheckConfig config = new AllowedMavenExtensionsCheckConfig(rule);

        assertSame(rule, config.getRule());
        assertFalse(config.getAllowedExtensionsPredicate().test("com.example:approved-extension"));
        assertTrue(config.getAllowedExtensionsPredicate().test("approved-extension"));
    }

    @Test
    void configsRejectRulesFromTheOtherArtifactFamily() {
        final ActiveRule extensionRule = rule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_EXTENSIONS,
            MavenRulesDefinition.EXTENSIONS_PARAM_KEY, "");
        final ActiveRule pluginRule = rule(MavenRulesDefinition.RULE_MAVEN_ALLOWED_PLUGINS,
            MavenRulesDefinition.PLUGINS_PARAM_KEY, "");

        assertThrows(IllegalArgumentException.class, () -> new AllowedMavenPluginsCheckConfig(extensionRule));
        assertThrows(IllegalArgumentException.class, () -> new AllowedMavenExtensionsCheckConfig(pluginRule));
    }

    private static ActiveRule rule(final RuleKey key, final String parameter, final String value) {
        final NewActiveRule newRule = new NewActiveRule.Builder()
            .setRuleKey(key)
            .setParam(parameter, value)
            .build();
        return new DefaultActiveRules(Arrays.asList(newRule)).find(key);
    }
}
