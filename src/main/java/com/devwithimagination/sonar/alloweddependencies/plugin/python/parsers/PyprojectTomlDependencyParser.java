package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.checks.PythonDependencyGroupType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlPosition;
import org.tomlj.TomlTable;

/**
 * Parses Python dependencies from pyproject.toml.
 */
public class PyprojectTomlDependencyParser {

    private static final Logger LOG = LoggerFactory.getLogger(PyprojectTomlDependencyParser.class);

    private static final String DEPENDENCIES = "dependencies";

    private static final String POETRY = "poetry";

    public List<DependencyOccurrence> parse(final InputFile inputFile,
            final PythonDependencyGroupType groupType, final List<String> groups) {

        final List<DependencyOccurrence> dependencies = new ArrayList<>();

        final TomlParseResult toml;
        try {
            toml = Toml.parse(inputFile.inputStream());
        } catch (IOException e) {
            LOG.warn("Unable to read pyproject.toml '{}'.", inputFile, e);
            return dependencies;
        }

        if (toml.hasErrors()) {
            LOG.warn("Skipped '{}' due to TOML parsing error: {}", inputFile, toml.errors().get(0));
            return dependencies;
        }

        if (PythonDependencyGroupType.MAIN.equals(groupType)) {
            dependencies.addAll(parseProjectDependencies(toml, inputFile));
            dependencies.addAll(parsePoetryDependencyTable(toml, inputFile,
                Arrays.asList("tool", POETRY, DEPENDENCIES)));
        } else if (PythonDependencyGroupType.DEV.equals(groupType)) {
            final Map<String, String> pep735GroupNames = pep735GroupNames(toml);
            dependencies.addAll(parsePep735Group(toml, inputFile, "dev", pep735GroupNames, new HashSet<>()));
            dependencies.addAll(parsePoetryDependencyTable(toml, inputFile,
                Arrays.asList("tool", POETRY, "dev-dependencies")));
            dependencies.addAll(parsePoetryDependencyTable(toml, inputFile,
                Arrays.asList("tool", POETRY, "group", "dev", DEPENDENCIES)));
        } else {
            final Map<String, String> pep735GroupNames = pep735GroupNames(toml);
            for (String group : groups) {
                dependencies.addAll(parsePep735Group(toml, inputFile, group, pep735GroupNames, new HashSet<>()));
                dependencies.addAll(parsePoetryDependencyTable(toml, inputFile,
                    Arrays.asList("tool", POETRY, "group", group, DEPENDENCIES)));
            }
        }

        return dependencies;
    }

    private static List<DependencyOccurrence> parseProjectDependencies(final TomlParseResult toml,
            final InputFile inputFile) {

        final Object value = toml.get(Arrays.asList("project", DEPENDENCIES));
        if (!(value instanceof TomlArray)) {
            return new ArrayList<>();
        }

        return parseRequirementArray((TomlArray) value, inputFile);
    }

    private static List<DependencyOccurrence> parsePoetryDependencyTable(final TomlParseResult toml,
            final InputFile inputFile, final List<String> path) {

        final List<DependencyOccurrence> dependencies = new ArrayList<>();
        final TomlTable table = toml.getTable(path);
        if (table == null) {
            return dependencies;
        }

        for (String dependencyName : table.keySet()) {
            if ("python".equalsIgnoreCase(dependencyName)) {
                continue;
            }

            final List<String> dependencyPath = new ArrayList<>(path);
            dependencyPath.add(dependencyName);
            dependencies.add(new DependencyOccurrence(
                dependencyName,
                inputFile,
                lineNumber(toml.inputPositionOf(dependencyPath))));
        }

        return dependencies;
    }

    private static List<DependencyOccurrence> parsePep735Group(final TomlParseResult toml, final InputFile inputFile,
            final String groupName, final Map<String, String> groupNames, final Set<String> groupStack) {

        final String normalizedGroupName = normalizeGroupName(groupName);
        if (!groupStack.add(normalizedGroupName)) {
            LOG.warn("Skipped recursive PEP 735 dependency group include '{}'.", groupName);
            return new ArrayList<>();
        }

        final List<DependencyOccurrence> dependencies = new ArrayList<>();
        final String declaredGroupName = groupNames.get(normalizedGroupName);
        if (declaredGroupName == null) {
            groupStack.remove(normalizedGroupName);
            return dependencies;
        }

        final List<String> groupPath = Arrays.asList("dependency-groups", declaredGroupName);
        final Object value = toml.get(groupPath);
        if (!(value instanceof TomlArray)) {
            groupStack.remove(normalizedGroupName);
            return dependencies;
        }

        final TomlArray groupArray = (TomlArray) value;
        for (int index = 0; index < groupArray.size(); index++) {
            final Object item = groupArray.get(index);
            if (item instanceof String) {
                addRequirementStringDependency(dependencies, inputFile, (String) item,
                    lineNumber(groupArray.inputPositionOf(index)));
            } else if (item instanceof TomlTable) {
                final String includeGroup = ((TomlTable) item).getString("include-group");
                if (includeGroup != null) {
                    dependencies.addAll(parsePep735Group(toml, inputFile, includeGroup, groupNames, groupStack));
                }
            }
        }

        groupStack.remove(normalizedGroupName);
        return dependencies;
    }

    private static Map<String, String> pep735GroupNames(final TomlParseResult toml) {
        final Map<String, String> groupNames = new LinkedHashMap<>();
        final TomlTable dependencyGroups = toml.getTable("dependency-groups");
        if (dependencyGroups == null) {
            return groupNames;
        }

        for (String groupName : dependencyGroups.keySet()) {
            final String existingName = groupNames.putIfAbsent(normalizeGroupName(groupName), groupName);
            if (existingName != null) {
                LOG.warn("PEP 735 dependency groups '{}' and '{}' have the same normalized name.",
                    existingName, groupName);
            }
        }
        return groupNames;
    }

    private static String normalizeGroupName(final String groupName) {
        return groupName.trim().toLowerCase(Locale.ROOT).replaceAll("[-_.]+", "-");
    }

    private static List<DependencyOccurrence> parseRequirementArray(final TomlArray array, final InputFile inputFile) {
        final List<DependencyOccurrence> dependencies = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            final Object value = array.get(index);
            if (value instanceof String) {
                addRequirementStringDependency(dependencies, inputFile, (String) value,
                    lineNumber(array.inputPositionOf(index)));
            }
        }
        return dependencies;
    }

    private static void addRequirementStringDependency(final List<DependencyOccurrence> dependencies,
            final InputFile inputFile, final String requirement, final int lineNumber) {

        final Optional<String> dependencyName = PythonRequirementNameParser.parseName(requirement);
        if (dependencyName.isPresent()) {
            dependencies.add(new DependencyOccurrence(dependencyName.get(), inputFile, lineNumber));
        }
    }

    static int lineNumber(final TomlPosition position) {
        if (position == null) {
            return 1;
        }
        return position.line();
    }
}
