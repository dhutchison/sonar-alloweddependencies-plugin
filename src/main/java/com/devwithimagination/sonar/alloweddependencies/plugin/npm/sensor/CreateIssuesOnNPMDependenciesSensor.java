package com.devwithimagination.sonar.alloweddependencies.plugin.npm.sensor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
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
        descriptor.onlyOnLanguage(NpmRulesDefinition.NPM_DEPENDENCY_LANGUAGE);
        descriptor.createIssuesForRuleRepositories(NpmRulesDefinition.REPOSITORY_NPM);
    }

    @Override
    public void execute(final SensorContext context) {

        /* Get the enabled rule config */
        final ActiveRule activeRule = context.activeRules().find(NpmRulesDefinition.RULE_NPM_ALLOWED);

        /* Only scan if the rule is active */
        if (activeRule != null) {

            /* Load our configuration for allowed dependencies */
            final List<String> allowedDependencies = getAllowedDependencies(activeRule);
            final boolean devDependencies = getDevDependencyScope(activeRule);
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
                final Set<String> dependencies = parseDependencies(inputFile, devDependencies);

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
    }

    /**
     * Get the list of allowed dependencies loaded from the configuration.
     *
     * @param activeRule the active rule definition to load parameters from
     *
     * @return list of strings, which are package names. This will always return a
     *         non-null value. The returned list will be empty.
     */
    private List<String> getAllowedDependencies(final ActiveRule activeRule) {

        final String allowedDependencies = activeRule.param(NpmRulesDefinition.DEPS_PARAM_KEY);

        if (allowedDependencies != null) {
            /* Convert into a list based on lines */
            return Arrays.asList(allowedDependencies.split("\\r?\\n"));
        } else {
            return Collections.emptyList();
        }

    }

    /**
     * Get a boolean representing if the scan scope is for dev dependencies or
     * regular dependencies.
     *
     * @param activeRule the active rule definition to load parameters from
     * @return true if <code>devDependencies</code> are to be checked, false for
     *         <code>dependencies</code>.
     */
    private boolean getDevDependencyScope(final ActiveRule activeRule) {

        final String param = activeRule.param(NpmRulesDefinition.SCOPES_PARAM_KEY);

        return Boolean.parseBoolean(param);
    }

    /**
     * Parse out the dependencies held in the given input file.
     *
     * @param packageJsonFile the input file
     * @param devDependencies boolean representing if <code>devDependencies</code> (true) or <code>dependencies</code>(false) should be scanned.
     * @return set containing the dependency names. This will always return a
     *         non-null value.
     */
    private Set<String> parseDependencies(final InputFile packageJsonFile, boolean devDependencies) {

        final Set<String> dependencies = new HashSet<>();

        try (InputStream in = packageJsonFile.inputStream(); JsonReader jsonReader = Json.createReader(in)) {

            JsonObject packageJson = jsonReader.readObject();

            final String jsonObjectName;
            if (devDependencies) {
                jsonObjectName = "devDependencies";
            } else {
                jsonObjectName = "dependencies";
            }

            final JsonObject packageJsonDependencies = packageJson.getJsonObject(jsonObjectName);
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
    private void createIssue(final InputFile inputFile, final String dependency, final SensorContext sensorContext) {

        LOG.info("Dependency " + dependency + " is not on the allowed list");

        NewIssue issue = sensorContext.newIssue().forRule(NpmRulesDefinition.RULE_NPM_ALLOWED)
                .at(new DefaultIssueLocation().on(inputFile)
                        .message("Dependency " + dependency + " is not on the allowed list"));
        issue.save();
    }
}
