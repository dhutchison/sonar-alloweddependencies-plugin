package com.devwithimagination.sonar.alloweddependencies.plugin.maven.check;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenPluginsCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenPluginsCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonarsource.analyzer.commons.xml.XmlFile;

class TestAllowedMavenPluginsCheck {

    @Test
    void checksBuildProfileAndReportingPluginsButNotPluginManagement() throws IOException {
        final SensorContext context = context();
        final ActiveRule rule = rule(String.join("\n",
            "# Maven's default plugin group may be omitted",
            "maven-compiler-plugin",
            "",
            "regex:com\\.example\\.reporting:approved-.*"));

        new AllowedMavenPluginsCheck(new AllowedMavenPluginsCheckConfig(rule))
            .scanFile(context, rule.ruleKey(), xml("plugins/pom.xml"));

        // Four active forbidden declarations; both pluginManagement declarations are ignored.
        verify(context, times(4)).newIssue();
    }

    @Test
    void canonicalCoordinatesAndVersionsDoNotAffectApproval() throws IOException {
        final SensorContext context = context();
        final ActiveRule rule = rule(String.join("\n",
            "org.apache.maven.plugins:maven-compiler-plugin",
            "com.example:forbidden-build-plugin",
            "com.example.reporting:forbidden-report",
            "regex:com\\.example\\.(ci|profile):forbidden-.*"));

        new AllowedMavenPluginsCheck(new AllowedMavenPluginsCheckConfig(rule))
            .scanFile(context, rule.ruleKey(), xml("plugins/pom.xml"));

        verify(context, never()).newIssue();
    }

    private static ActiveRule rule(final String allowed) {
        final NewActiveRule rule = new NewActiveRule.Builder()
            .setRuleKey(MavenRulesDefinition.RULE_MAVEN_ALLOWED_PLUGINS)
            .setParam(MavenRulesDefinition.PLUGINS_PARAM_KEY, allowed)
            .build();
        return new DefaultActiveRules(Arrays.asList(rule)).find(rule.ruleKey());
    }

    private static SensorContext context() {
        final SensorStorage storage = mock(SensorStorage.class);
        final SensorContext context = mock(SensorContext.class);
        when(context.newIssue()).then(i -> new DefaultIssue(null, storage));
        return context;
    }

    private static XmlFile xml(final String path) throws IOException {
        final File base = new File("src/test/resources/maven");
        final File file = new File(base, path);
        final InputFile input = TestInputFileBuilder.create(TestAllowedMavenPluginsCheck.class.getName(), base, file)
            .setCharset(StandardCharsets.UTF_8)
            .setContents(Files.readString(file.toPath()))
            .build();
        return XmlFile.create(input);
    }
}
