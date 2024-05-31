package com.devwithimagination.sonar.alloweddependencies.plugin.npm.sensor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.npm.checks.AllowedNpmDependenciesCheck;
import com.devwithimagination.sonar.alloweddependencies.plugin.npm.rules.NpmRulesDefinition;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
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

    /**
     * The rules this sensor supports.
     */
    private static final List<RuleKey> SUPPORTED_KEYS = Arrays.asList(
            NpmRulesDefinition.RULE_NPM_ALLOWED,
            NpmRulesDefinition.RULE_NPM_ALLOWED_DEV);

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

        /* Create our rule checkers, one per active template rule */
        final List<AllowedNpmDependenciesCheck> checks = context.activeRules()
                .findByRepository(NpmRulesDefinition.REPOSITORY_NPM)
                .stream()
                .filter(rule -> SUPPORTED_KEYS.contains(rule.ruleKey()))
                .map(AllowedNpmDependenciesCheck::new)
                .collect(Collectors.toList());

        /* Only scan files if we have an enabled rule */
        if (!checks.isEmpty()) {

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

                LOG.info("NPM Dependency input file {}", inputFile);

                /* Scan using our checks */
                checks.forEach(check -> check.scanFile(inputFile, context));
            }
        }
    }
}
