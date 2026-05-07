package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyIssueReporter;
import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;

/**
 * Compares Python dependencies against the configured allow list.
 */
public class AllowedPythonDependenciesCheck {

    private static final Logger LOG = LoggerFactory.getLogger(AllowedPythonDependenciesCheck.class);

    private final AllowedPythonDependenciesCheckConfig config;

    public AllowedPythonDependenciesCheck(final AllowedPythonDependenciesCheckConfig config) {
        LOG.info("Creating AllowedPythonDependenciesCheck for {}", config.getRule().ruleKey());
        this.config = config;
    }

    public void scanDependency(final DependencyOccurrence dependency, final SensorContext sensorContext) {
        if (!config.getAllowedDependenciesPredicate().test(dependency.getName())) {
            LOG.info("Forbidden Python dependency: {}", dependency.getName());
            DependencyIssueReporter.reportIssue(sensorContext, config.getRule().ruleKey(),
                dependency.getInputFile(), dependency.getLineNumber(), dependency.getName());
        }
    }

    public AllowedPythonDependenciesCheckConfig getConfig() {
        return config;
    }
}

