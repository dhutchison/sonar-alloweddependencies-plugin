package com.devwithimagination.sonar.alloweddependencies.plugin.rules;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.devwithimagination.sonar.alloweddependencies.maven.AllowedMavenDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.settings.AllowedDependenciesProperties;

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
        descriptor.onlyOnLanguage(MavenDependencyRulesDefinition.DEPENDENCY_LANGUAGE);
        descriptor.createIssuesForRuleRepositories(MavenDependencyRulesDefinition.REPOSITORY);
    }

    @Override
    public void execute(SensorContext context) {

        /* Load our configuration for allowed dependencies */
        final List<String> allowedDependencies = getAllowedDependencies();
        LOG.info("Allowed dependecies: '{}'", allowedDependencies);

        /* Create our rule checker */
        final AllowedMavenDependenciesCheck check = new AllowedMavenDependenciesCheck(allowedDependencies);

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

            /* Scan using our check */
            check.scanFile(context, MavenDependencyRulesDefinition.RULE_MAVEN_ALLOWED, xmlFile);
        }
    }

    /**
     * Get the list of allowed dependencies loaded from the configuration.
     *
     * @return list of strings, with the expected format of "groupId:artifactId".
     *         This will always return a non-null value. The returned list will be
     *         empty.
     */
    private List<String> getAllowedDependencies() {

        Optional<String> allowedDependencies = config.get(AllowedDependenciesProperties.MAVEN_KEY);

        if (allowedDependencies.isPresent()) {
            /* Convert into a list based on lines */
            return Arrays.asList(allowedDependencies.get().split("\\r?\\n"));
        } else {
            return Collections.emptyList();
        }

    }
}
