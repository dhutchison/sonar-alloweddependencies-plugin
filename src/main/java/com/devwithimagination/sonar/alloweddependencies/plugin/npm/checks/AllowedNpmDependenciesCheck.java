package com.devwithimagination.sonar.alloweddependencies.plugin.npm.checks;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.util.PredicateFactory;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * A check which compares declared NPM dependencies against a list of approved
 * dependencies, taken from the rule configuration, raising issues for any which
 * are not found.
 */
public class AllowedNpmDependenciesCheck {

    /**
     * Logger
     */
    private static final Logger LOG = Loggers.get(AllowedNpmDependenciesCheck.class);

    /**
     * Predicate for matching against the names for dependencies which are
     * allowed.
     */
    private final Predicate<String> allowedDependenciesPredicate;

    /**
     * Boolean holding if this check is for development dependencies (true) or not (false).
     */
    private final boolean devDependencies;

    /**
     * The key for the rule this instance of the check was created for.
     */
    private final RuleKey ruleKey;

    /**
     * Create a new {@link AllowedNpmDependenciesCheck} based on an active rule.
     *
     * @param activeRuleDefinition the rule containing the parameter configuration.
     */
    public AllowedNpmDependenciesCheck(final ActiveRule activeRuleDefinition) {

        LOG.info("Creating AllowedNpmDependenciesCheck for {}", activeRuleDefinition.ruleKey());
        this.ruleKey = activeRuleDefinition.ruleKey();

        /* Configure the check scope */
        if (NpmRulesDefinition.RULE_NPM_ALLOWED.equals(activeRuleDefinition.ruleKey())) {
            devDependencies = false;
        } else if (NpmRulesDefinition.RULE_NPM_ALLOWED_DEV.equals(activeRuleDefinition.ruleKey())) {
            devDependencies = true;
        } else {
            throw new IllegalArgumentException("Unsupported rule key: " + activeRuleDefinition.ruleKey());
        }

        /* Configure the allowed dependency names */
        final String deps = activeRuleDefinition.param(NpmRulesDefinition.DEPS_PARAM_KEY);
        final PredicateFactory predicateFactory = new PredicateFactory();
        this.allowedDependenciesPredicate = predicateFactory.createPredicateForDependencyListString(deps);
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

        try (InputStream in = packageJsonFile.inputStream();
                JsonReader jsonReader = Json.createReader(in)) {

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

        NewIssue issue = sensorContext.newIssue()
            .forRule(ruleKey)
            .at(
                new DefaultIssueLocation()
                    .on(inputFile)
                    .message("Dependency " + dependency + " is not on the allowed list"));

        issue.save();
    }

    /**
     * Scan the supplied file for issues.
     * @param inputFile the file to scan
     * @param sensorContext the sensor context
     */
    public void scanFile(final InputFile inputFile, final SensorContext sensorContext) {

        /* Need to read the file and extract the dependencies */
        final Set<String> dependencies = parseDependencies(inputFile);

        /*
         * Iterate through the dependencies and create issues for any not on the allow
         * list
         */
        dependencies.forEach(dep -> {
            if (!allowedDependenciesPredicate.test(dep)) {
                createIssue(inputFile, dep, sensorContext);
            }
        });
    }
}
