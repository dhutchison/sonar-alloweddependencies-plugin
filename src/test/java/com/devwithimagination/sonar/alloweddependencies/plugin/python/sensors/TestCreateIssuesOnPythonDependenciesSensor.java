package com.devwithimagination.sonar.alloweddependencies.plugin.python.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.Constants;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.rules.PythonRulesDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.rule.RuleKey;

class TestCreateIssuesOnPythonDependenciesSensor {

    private CreateIssuesOnPythonDependenciesSensor sensor;

    @BeforeEach
    void setup() {
        this.sensor = new CreateIssuesOnPythonDependenciesSensor(null);
    }

    @Test
    void testExecuteWithRulesAndFiles() throws IOException {
        final List<NewActiveRule> newActiveRules = Arrays.asList(
            new NewActiveRule.Builder()
                .setRuleKey(PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN)
                .setParam(PythonRulesDefinition.DEPS_PARAM_KEY, String.join("\n",
                    "requests",
                    "urllib3",
                    "fastapi",
                    "shared-package"))
                .build(),
            new NewActiveRule.Builder()
                .setRuleKey(PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV)
                .setParam(PythonRulesDefinition.DEPS_PARAM_KEY, String.join("\n",
                    "pytest",
                    "ruff",
                    "mypy",
                    "flake8",
                    "editable-package",
                    "tox"))
                .build()
        );

        final ActiveRules activeRules = new DefaultActiveRules(newActiveRules);
        final File baseDir = new File("src/test/resources/python");

        final SensorContextTester sensorContext = SensorContextTester.create(baseDir);
        sensorContext.setActiveRules(activeRules);
        addInputFile(sensorContext, baseDir, "pyproject/pyproject.toml");
        addInputFile(sensorContext, baseDir, "requirements/requirements.txt");
        addInputFile(sensorContext, baseDir, "requirements/shared.txt");
        addInputFile(sensorContext, baseDir, "requirements/cyclic.txt");
        addInputFile(sensorContext, baseDir, "requirements/constraints.txt");
        addInputFile(sensorContext, baseDir, "requirements/requirements-dev.txt");
        addInputFile(sensorContext, baseDir, "requirements/dev-requirements.txt");
        addInputFile(sensorContext, baseDir, "requirements/dev-shared.txt");

        sensor.execute(sensorContext);

        assertEquals(10, sensorContext.allIssues().size(), "Expecting Python dependency violations");
        assertIssue(sensorContext, "constrained-package", PythonRulesDefinition.RULE_PYTHON_ALLOWED_MAIN,
            "requirements/constraints.txt", 1);
        assertIssue(sensorContext, "dev_extra", PythonRulesDefinition.RULE_PYTHON_ALLOWED_DEV,
            "requirements/requirements-dev.txt", 3);
    }

    @Test
    void testExecuteWithTemplateRule() throws IOException {
        final RuleKey customRuleKey =
            RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "python-allowed-dependencies-docs");
        final NewActiveRule customRule = new NewActiveRule.Builder()
            .setRuleKey(customRuleKey)
            .setTemplateRuleKey(PythonRulesDefinition.RULE_PYTHON_ALLOWED.rule())
            .setParam(PythonRulesDefinition.DEPS_PARAM_KEY, String.join("\n",
                "sphinx",
                "flake8",
                "pep735_lint_extra"))
            .setParam(PythonRulesDefinition.GROUPS_PARAM_KEY, "docs")
            .build();

        final File baseDir = new File("src/test/resources/python");
        final SensorContextTester sensorContext = SensorContextTester.create(baseDir);
        sensorContext.setActiveRules(new DefaultActiveRules(Arrays.asList(customRule)));
        addInputFile(sensorContext, baseDir, "pyproject/pyproject.toml");

        sensor.execute(sensorContext);

        assertEquals(2, sensorContext.allIssues().size());
        assertIssue(sensorContext, "external-docs", customRuleKey, "pyproject/pyproject.toml", 37);
    }

    @Test
    void testExecuteIgnoresUnsupportedRules() throws IOException {
        final RuleKey unsupportedRuleKey =
            RuleKey.of(PythonRulesDefinition.REPOSITORY_PYTHON, "unsupported-python-rule");
        final NewActiveRule unsupportedRule = new NewActiveRule.Builder()
            .setRuleKey(unsupportedRuleKey)
            .setParam(PythonRulesDefinition.DEPS_PARAM_KEY, "")
            .build();

        final File baseDir = new File("src/test/resources/python");
        final SensorContextTester sensorContext = SensorContextTester.create(baseDir);
        sensorContext.setActiveRules(new DefaultActiveRules(Arrays.asList(unsupportedRule)));
        addInputFile(sensorContext, baseDir, "pyproject/pyproject.toml");

        sensor.execute(sensorContext);

        assertEquals(0, sensorContext.allIssues().size());
    }

    private static void addInputFile(final SensorContextTester sensorContext, final File baseDir,
            final String relativePath) throws IOException {

        final File testFile = new File(baseDir, relativePath);
        final String fileContents = String.join(System.lineSeparator(), Files.readAllLines(testFile.toPath()));
        sensorContext.fileSystem().add(
            TestInputFileBuilder.create("python-sensor-test-project", baseDir, testFile)
                .setCharset(Charset.forName("UTF-8"))
                .setContents(fileContents)
                .build());
    }

    private static void assertIssue(final SensorContextTester sensorContext, final String dependency,
            final RuleKey ruleKey, final String relativePath, final int lineNumber) {

        final String message = String.format(Constants.ISSUE_MESSAGE, dependency);
        final Issue issue = sensorContext.allIssues().stream()
            .filter(candidate -> message.equals(candidate.primaryLocation().message()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Missing issue for " + dependency));

        assertEquals(ruleKey, issue.ruleKey());
        assertEquals(relativePath, ((InputFile) issue.primaryLocation().inputComponent()).relativePath());
        assertEquals(lineNumber, issue.primaryLocation().textRange().start().line());
    }
}
