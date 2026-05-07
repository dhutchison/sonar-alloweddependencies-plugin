package com.devwithimagination.sonar.alloweddependencies.plugin.common;

import static com.devwithimagination.sonar.alloweddependencies.plugin.common.Constants.ISSUE_MESSAGE;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

/**
 * Shared helper for reporting dependency allow-list violations.
 */
public final class DependencyIssueReporter {

    private DependencyIssueReporter() {

    }

    public static void reportIssue(final SensorContext sensorContext, final RuleKey ruleKey,
            final InputFile inputFile, final int lineNumber, final String dependency) {

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
}

