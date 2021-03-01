package com.devwithimagination.sonar.alloweddependencies.plugin.maven;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.rules.DependencyRulesDefinition;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.xml.XmlFile;

/**
 * Generates issues on any dependencies in a pom.xml file which are not in an
 * approved list.
 *
 * This rule must be activated in the Quality profile.
 */
public class CreateIssuesOnMavenDependenciesSensor implements Sensor {

    private static final Logger LOG = Loggers.get(CreateIssuesOnMavenDependenciesSensor.class);

    protected final Configuration config;

    /**
     * Create a new instance of this sensor.
     *
     * This is expected to be called using IoC to inject the configuration.
     *
     * @param config the plugin configuration.
     */
    public CreateIssuesOnMavenDependenciesSensor(final Configuration config) {
        this.config = config;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Add issues to all dependencies in pom files.");

        /*
         * Optimisation to disable execution of sensor if a project does not contain
         * Java files or if the rule is not activated in the Quality profile
         */
        descriptor.onlyOnLanguage(DependencyRulesDefinition.MAVEN_DEPENDENCY_LANGUAGE);
        descriptor.createIssuesForRuleRepositories(DependencyRulesDefinition.REPOSITORY_MAVEN);
    }

    @Override
    public void execute(SensorContext context) {

        /* Create our rule checkers, one per active template rule */
        final List<AllowedMavenDependenciesCheck> checks = context.activeRules()
            .findByRepository(DependencyRulesDefinition.REPOSITORY_MAVEN)
            .stream()
            .filter(rule -> DependencyRulesDefinition.RULE_MAVEN_ALLOWED.rule().equals(rule.templateRuleKey()))
            .map(AllowedMavenDependenciesCheck::new)
            .collect(Collectors.toList());

        /*
         * Scan for the xml files for the project. We use this predicate so we (may)
         * still be able to be compatible with sonarlint (the absolute path predicates
         * note they may not work).
         *
         * This does require pom.xml to be in the scannable sources for the project.
         */
        final FileSystem fs = context.fileSystem();
        final Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().matchesPathPattern("**/*.xml"));
        for (InputFile file : inputFiles) {
            LOG.info("Got xml file {}", file);
        }

        for (InputFile inputFile : inputFiles) {

            LOG.info("Input file {}", inputFile);

            /* Need to read the XML file */
            XmlFile xmlFile;
            try {
                xmlFile = XmlFile.create(inputFile);
            } catch (IOException e) {
                LOG.debug("Skipped '{}' due to parsing error", inputFile);
                return;
            } catch (Exception e) {
                // Our own XML parsing may have failed somewhere, so logging as warning to
                // appear in logs
                LOG.warn(String.format("Unable to analyse file '%s'.", inputFile), e);
                return;
            }

            /* Scan using our checks */
            checks.forEach(check -> check.scanFile(context, DependencyRulesDefinition.RULE_MAVEN_ALLOWED, xmlFile));
        }
    }
}
