package com.devwithimagination.sonar.alloweddependencies.plugin.npm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.devwithimagination.sonar.alloweddependencies.plugin.rules.DependencyRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.settings.AllowedDependenciesProperties;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Generates issues on any dependencies in a package.json file which are not in
 * an approved list.
 *
 * This rule must be activated in the Quality profile.
 */
public class CreateIssuesOnNPMDependenciesSensor implements Sensor {

    private static final Logger LOG = Loggers.get(CreateIssuesOnNPMDependenciesSensor.class);

    protected final Configuration config;

    /**
     * Create a new instance of this sensor.
     *
     * This is expected to be called using IoC to inject the configuration.
     *
     * @param config the plugin configuration.
     */
    public CreateIssuesOnNPMDependenciesSensor(final Configuration config) {
        this.config = config;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Add issues to all dependencies in package.json files.");

        /*
         * Optimisation to disable execution of sensor if a project does not contain the
         * right type of files or if the rule is not activated in the Quality profile
         */
        descriptor.onlyOnLanguage(DependencyRulesDefinition.NPM_DEPENDENCY_LANGUAGE);
        descriptor.createIssuesForRuleRepositories(DependencyRulesDefinition.REPOSITORY_NPM);
    }

    @Override
    public void execute(final SensorContext context) {

        /* Load our configuration for allowed dependencies */
        final List<String> allowedDependencies = getAllowedDependencies();
        LOG.info("Allowed NPM dependencies: '{}'", allowedDependencies);

        /*
         * Scan for the package.json files for the project. We use this predicate so we
         * (may) still be able to be compatible with Sonarlint (the absolute path
         * predicates note they may not work).
         *
         * This does require package.json to be in the scannable sources for the
         * project.
         */
        final FileSystem fs = context.fileSystem();
        final Iterable<InputFile> inputFiles = fs.inputFiles(
            fs.predicates().matchesPathPattern("**/package.json"));

        for (InputFile inputFile : inputFiles) {

            LOG.info("Input file {}", inputFile);

            /* Need to read the file and extract the dependencies */
            final Set<String> dependencies = parseDependencies(inputFile);

            /*
             * Iterate through the dependencies and create issues for any not on the allow
             * list
             */
            dependencies.forEach(dep -> {
                if (!allowedDependencies.contains(dep)) {
                    createIssue(inputFile, dep, context);
                }
            });
        }
    }

    /**
     * Get the list of allowed dependencies loaded from the configuration.
     *
     * @return list of strings, which are package names. This will always return a
     *         non-null value. The returned list will be empty.
     */
    private List<String> getAllowedDependencies() {

        Optional<String> allowedDependencies = config.get(AllowedDependenciesProperties.NPM_KEY);

        if (allowedDependencies.isPresent()) {
            /* Convert into a list based on lines */
            return Arrays.asList(allowedDependencies.get().split("\\r?\\n"));
        } else {
            return Collections.emptyList();
        }

    }

    /**
     * Parse out the dependencies held in the given input file.
     *
     * @param packageJsonFile the input file
     * @return set containing the dependency names. This will always return a
     *         non-null value.
     */
    private Set<String> parseDependencies(final InputFile packageJsonFile) {

        final Set<String> dependencies = new HashSet<>();

        try (InputStream in = packageJsonFile.inputStream(); JsonReader jsonReader = Json.createReader(in)) {

            JsonObject packageJson = jsonReader.readObject();

            JsonObject packageJsonDependencies = packageJson.getJsonObject("dependencies");
            if (packageJsonDependencies != null) {
                dependencies.addAll(packageJsonDependencies.keySet());
            }
        } catch (IOException e) {
            LOG.error("Error reading package.json", e);
        }

        return dependencies;
    }

    /**
     * Creates a new issue for our rule violation.
     *
     * @param inputFile     the file being scanned
     * @param dependency    the name of the dependency which was found
     * @param sensorContext the sensor context
     */
    private void createIssue(final InputFile inputFile, final String dependency,
        final SensorContext sensorContext) {

        LOG.info("Dependency " + dependency + " is not on the allowed list");

        NewIssue issue = sensorContext.newIssue()
            .forRule(DependencyRulesDefinition.RULE_NPM_ALLOWED)
            .at(
                new DefaultIssueLocation()
                    .on(inputFile)
                    .message("Dependency " + dependency + " is not on the allowed list")
            );
        issue.save();
    }
}
