package com.devwithimagination.sonar.alloweddependencies.plugin.npm.checks;

import static com.devwithimagination.sonar.alloweddependencies.plugin.common.Constants.ISSUE_MESSAGE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;
import com.devwithimagination.sonar.alloweddependencies.plugin.util.PredicateFactory;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
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
     * Enum holding the type of dependencies we are checking.
     */
    private final DependencyBlockType dependencyType;

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
        this.dependencyType = DependencyBlockType.forRuleKey(activeRuleDefinition.ruleKey());

        if (this.dependencyType == null) {
            throw new IllegalArgumentException("Unsupported rule key: " + activeRuleDefinition.ruleKey());
        }

        /* Configure the allowed dependency names */
        final String deps = activeRuleDefinition.param(NpmRulesDefinition.DEPS_PARAM_KEY);
        final PredicateFactory predicateFactory = new PredicateFactory();
        this.allowedDependenciesPredicate = predicateFactory.createPredicateForDependencyListString(deps);
    }

    /**
     * Create a list of strings for the lines in the file. Any whitespace is removed.
     * @param packageJsonFile the file to parse
     * @return list of strings
     */
    List<String> readFileLines(final InputFile packageJsonFile) {

        List<String> lines = new ArrayList<>();
        try (InputStream in = packageJsonFile.inputStream();
                InputStreamReader ir = new InputStreamReader(in);
                BufferedReader r = new BufferedReader(ir)) {

            String line;
            while ((line=r.readLine()) != null) {
                lines.add(line.replaceAll("\\s", ""));
            }
        } catch (IOException e) {
            LOG.error("Error reading package.json for lines", e);
        }

        return lines;

    }

    /**
     * Parse out the dependencies held in the given input file.
     *
     * @param packageJsonFile the input file
     * @return set containing the dependency names. This will always return a
     *         non-null value.
     */
    Map<String, Integer> parseDependencies(final InputFile packageJsonFile) {

        final String jsonObjectName = this.dependencyType.getJsonObjectName();
        final Map<String, Integer> dependencies = new TreeMap<>();

        /* Read in the file first as a list of strings so we can do line number detection later on */
        final List<String> lines = readFileLines(packageJsonFile);

        /* Find the expected start range for the area we are looking for */
        int startLine = lines.indexOf("\"" + jsonObjectName + "\":{");

        if (startLine >= 0) {
            /* If the JSON document contains the field we are looking for */

            /* Iterate through the lines from this start line, until we hit a closing curly brace, and pick out the dependencies */
            final Pattern pattern = Pattern.compile("^[\"](.*?)[\"][:](.*)");

            boolean foundClose = false;
            for (int index = startLine + 1; (index < lines.size() && !foundClose); index++) {

                if (lines.get(index).startsWith("}")){
                    foundClose = true;
                } else {
                    /* Add another dependency to the map */
                    Matcher m = pattern.matcher(lines.get(index));

                    if (m.matches()) {
                        dependencies.put(m.group(1), index + 1);
                    }
                }
            }
        }

        return dependencies;
    }

    /**
     * Creates a new issue for our rule violation.
     *
     * @param inputFile     the file being scanned
     * @param dependency    the name of the dependency which was found
     * @param lineNumber    the line number the dependency was found on
     * @param sensorContext the sensor context
     */
    private void createIssue(final InputFile inputFile, final String dependency, final Integer lineNumber, final SensorContext sensorContext) {

        LOG.info("Dependency " + dependency + " is not on the allowed list");

        NewIssue issue = sensorContext.newIssue();
        issue
            .forRule(ruleKey)
            .at(
                issue.newLocation()
                    .on(inputFile)
                    .at(inputFile.selectLine(lineNumber))
                    .message(String.format(ISSUE_MESSAGE, dependency)))
            .save();
    }

    /**
     * Scan the supplied file for issues.
     * @param inputFile the file to scan
     * @param sensorContext the sensor context
     */
    public void scanFile(final InputFile inputFile, final SensorContext sensorContext) {

        /* Need to read the file and extract the dependencies */
        final Map<String, Integer> dependencies = parseDependencies(inputFile);

        /*
         * Iterate through the dependencies and create issues for any not on the allow
         * list
         */
        dependencies.entrySet().forEach(dep -> {
            if (!allowedDependenciesPredicate.test(dep.getKey())) {
                createIssue(inputFile, dep.getKey(), dep.getValue(), sensorContext);
            }
        });
    }


    /**
     * Enum holding the types of dependency blocks that can exist in a package.json file.
     */
    private enum DependencyBlockType {

        DEPENDENCY("dependencies", NpmRulesDefinition.RULE_NPM_ALLOWED),
        DEV_DEPENDENCY("devDependencies", NpmRulesDefinition.RULE_NPM_ALLOWED_DEV),
        PEER_DEPENDENCY("peerDependencies", NpmRulesDefinition.RULE_NPM_ALLOWED_PEER)

        ;

        private final String jsonObjectName;
        private final RuleKey ruleKey;

        private DependencyBlockType(final String jsonObjectName, final RuleKey ruleKey) {
            this.jsonObjectName = jsonObjectName;
            this.ruleKey = ruleKey;
        }

        public String getJsonObjectName() {
            return jsonObjectName;
        }

        /**
         * Get the {@link DependencyBlockType} value for the supplied rule key.
         * @param ruleKey the rule key to find.
         * @return the enum value for the rule key, or null if a matching value is not found.
         */
        static DependencyBlockType forRuleKey(final RuleKey ruleKey) {

            for (DependencyBlockType value : DependencyBlockType.values()) {
                if (value.ruleKey.equals(ruleKey)) {
                    return value;
                }
            }

            /* If we exit the loop without returning then there was no match */
            return null;
        }

    }
}
