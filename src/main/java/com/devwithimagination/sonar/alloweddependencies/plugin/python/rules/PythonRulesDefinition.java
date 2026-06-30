package com.devwithimagination.sonar.alloweddependencies.plugin.python.rules;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Defines Python dependency allow-list rules.
 */
public class PythonRulesDefinition implements RulesDefinition {

    private static final String ALLOW_LIST_PARAM_DESCRIPTION =
        "Newline separated list of Python package names. Exact matches are normalized using Python package " +
        "name normalization and are case-insensitive. Prefix a row with regex: to allow dependencies matching " +
        "a regular expression. Blank lines and rows starting with # are ignored.";

    public static final String REPOSITORY_PYTHON = "allowed-dependencies-python";

    public static final String PYTHON_DEPENDENCY_LANGUAGE = "py";

    public static final RuleKey RULE_PYTHON_ALLOWED_MAIN =
        RuleKey.of(REPOSITORY_PYTHON, "python-allowed-dependencies-main");

    public static final RuleKey RULE_PYTHON_ALLOWED_DEV =
        RuleKey.of(REPOSITORY_PYTHON, "python-allowed-dependencies-dev");

    public static final RuleKey RULE_PYTHON_ALLOWED =
        RuleKey.of(REPOSITORY_PYTHON, "python-allowed-dependencies");

    public static final String DEPS_PARAM_KEY = "pythonDependencies";

    public static final String GROUPS_PARAM_KEY = "pythonDependencyGroups";

    public static final String REQUIREMENTS_FILES_PARAM_KEY = "pythonRequirementsFiles";

    @Override
    public void define(final Context context) {

        final NewRepository pythonRepository = context.createRepository(REPOSITORY_PYTHON, PYTHON_DEPENDENCY_LANGUAGE)
            .setName("Python Allowed Dependency Analyzer");

        createRule(pythonRepository, RULE_PYTHON_ALLOWED_MAIN,
            "Allowed Dependencies (Python Main)",
            "<p>This rule applies to main Python dependencies in <code>pyproject.toml</code> and " +
            "<code>requirements.txt</code>.</p>");

        createRule(pythonRepository, RULE_PYTHON_ALLOWED_DEV,
            "Allowed Development Dependencies (Python)",
            "<p>This rule applies to development Python dependencies in Poetry, PEP 735 dependency groups, " +
            "<code>requirements-dev.txt</code>, and <code>dev-requirements.txt</code>.</p>");

        final NewRule templateRule = createRule(pythonRepository, RULE_PYTHON_ALLOWED,
            "Allowed Dependencies (Python template)",
            "<p>This rule is a template for custom Poetry groups, PEP 735 dependency groups, and explicit " +
            "requirements files.</p>")
            .setTemplate(true);

        templateRule.createParam(GROUPS_PARAM_KEY)
            .setName("Python dependency groups")
            .setDescription("Comma separated list of Poetry group names or PEP 735 dependency group names.")
            .setType(RuleParamType.STRING);

        templateRule.createParam(REQUIREMENTS_FILES_PARAM_KEY)
            .setName("Python requirements files")
            .setDescription("Comma separated list of explicit requirements file paths.")
            .setType(RuleParamType.STRING);

        pythonRepository.done();
    }

    private NewRule createRule(final NewRepository repository, final RuleKey key, final String name,
            final String description) {

        final NewRule rule = repository.createRule(key.rule())
            .setName(name)
            .setHtmlDescription(
                "<p>Only approved Python dependencies should be used.</p>" +
                description +
                "<p>Generates an issue for every Python dependency which is not in the allowed list.</p>")
            .setTags("python", "dependency")
            .setStatus(RuleStatus.READY)
            .setSeverity(Severity.MINOR);

        rule.setDebtRemediationFunction(rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));

        rule.createParam(DEPS_PARAM_KEY)
            .setName("Allowed Python Dependencies")
            .setDescription(ALLOW_LIST_PARAM_DESCRIPTION)
            .setType(RuleParamType.TEXT);

        return rule;
    }
}
