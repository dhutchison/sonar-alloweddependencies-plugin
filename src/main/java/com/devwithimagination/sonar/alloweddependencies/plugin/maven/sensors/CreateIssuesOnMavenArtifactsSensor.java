package com.devwithimagination.sonar.alloweddependencies.plugin.maven.sensors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenDependenciesCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenExtensionsCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenExtensionsCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenPluginsCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks.AllowedMavenPluginsCheckConfig;
import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.analyzer.commons.xml.ParseException;
import org.sonarsource.analyzer.commons.xml.XmlFile;

/** Creates issues for governed Maven coordinates declared in indexed XML files. */
public class CreateIssuesOnMavenArtifactsSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(CreateIssuesOnMavenArtifactsSensor.class);

    private static final List<String> MAVEN_FILE_PATTERNS = Arrays.asList(
        "**/pom.xml",
        "**/.flattened-pom.xml",
        "**/.mvn/extensions.xml"
    );

    protected final Configuration config;

    public CreateIssuesOnMavenArtifactsSensor(final Configuration config) {
        this.config = config;
    }

    @Override
    public void describe(final SensorDescriptor descriptor) {
        descriptor.name("Add issues to governed Maven coordinates.");
        descriptor.onlyOnLanguage(MavenRulesDefinition.MAVEN_DEPENDENCY_LANGUAGE);
        descriptor.createIssuesForRuleRepositories(MavenRulesDefinition.REPOSITORY_MAVEN);
    }

    @Override
    public void execute(final SensorContext context) {
        final Collection<ActiveRule> rules = context.activeRules()
            .findByRepository(MavenRulesDefinition.REPOSITORY_MAVEN);

        final List<AllowedMavenDependenciesCheck> dependencyChecks = rules.stream()
            .filter(CreateIssuesOnMavenArtifactsSensor::isDependencyRule)
            .map(AllowedMavenDependenciesCheckConfig::new)
            .map(AllowedMavenDependenciesCheck::new)
            .collect(Collectors.toList());
        final List<AllowedMavenPluginsCheck> pluginChecks = rules.stream()
            .filter(rule -> MavenRulesDefinition.RULE_MAVEN_ALLOWED_PLUGINS.equals(rule.ruleKey()))
            .map(AllowedMavenPluginsCheckConfig::new)
            .map(AllowedMavenPluginsCheck::new)
            .collect(Collectors.toList());
        final List<AllowedMavenExtensionsCheck> extensionChecks = rules.stream()
            .filter(rule -> MavenRulesDefinition.RULE_MAVEN_ALLOWED_EXTENSIONS.equals(rule.ruleKey()))
            .map(AllowedMavenExtensionsCheckConfig::new)
            .map(AllowedMavenExtensionsCheck::new)
            .collect(Collectors.toList());

        rules.stream()
            .filter(rule -> !isSupportedRule(rule))
            .findFirst()
            .ifPresent(rule -> {
                throw new IllegalArgumentException("Unsupported Maven rule: " + rule.ruleKey());
            });

        if (dependencyChecks.isEmpty() && pluginChecks.isEmpty() && extensionChecks.isEmpty()) {
            return;
        }

        final FileSystem fileSystem = context.fileSystem();
        final Set<InputFile> inputFiles = new LinkedHashSet<>();
        MAVEN_FILE_PATTERNS.forEach(pattern ->
            fileSystem.inputFiles(fileSystem.predicates().matchesPathPattern(pattern))
                .forEach(inputFiles::add));

        for (InputFile inputFile : inputFiles) {
            final XmlFile xmlFile = parse(inputFile);
            if (xmlFile == null) {
                continue;
            }

            dependencyChecks.forEach(check -> check.scanFile(
                context, check.getConfig().getRule().ruleKey(), xmlFile));
            pluginChecks.forEach(check -> check.scanFile(
                context, check.getConfig().getRule().ruleKey(), xmlFile));
            extensionChecks.forEach(check -> check.scanFile(
                context, check.getConfig().getRule().ruleKey(), xmlFile));
        }
    }

    private static boolean isSupportedRule(final ActiveRule rule) {
        return isDependencyRule(rule)
            || MavenRulesDefinition.RULE_MAVEN_ALLOWED_PLUGINS.equals(rule.ruleKey())
            || MavenRulesDefinition.RULE_MAVEN_ALLOWED_EXTENSIONS.equals(rule.ruleKey());
    }

    private static boolean isDependencyRule(final ActiveRule rule) {
        return MavenRulesDefinition.RULE_MAVEN_ALLOWED_MAIN.equals(rule.ruleKey())
            || MavenRulesDefinition.RULE_MAVEN_ALLOWED_TEST.equals(rule.ruleKey())
            || MavenRulesDefinition.RULE_MAVEN_ALLOWED.rule().equals(rule.templateRuleKey());
    }

    private static XmlFile parse(final InputFile inputFile) {
        LOG.info("Maven input file {}", inputFile);
        try {
            return XmlFile.create(inputFile);
        } catch (IOException | ParseException exception) {
            LOG.debug("Skipped '{}' due to parsing error", inputFile);
            return null;
        } catch (Exception exception) {
            LOG.warn(String.format("Unable to analyse file '%s'.", inputFile), exception);
            return null;
        }
    }
}
