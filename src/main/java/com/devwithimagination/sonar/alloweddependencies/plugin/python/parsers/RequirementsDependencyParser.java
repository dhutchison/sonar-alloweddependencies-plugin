package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.checks.PythonDependencyGroupType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;

/**
 * Parses pip requirements files, following include and constraint files.
 */
public class RequirementsDependencyParser {

    private static final Logger LOG = LoggerFactory.getLogger(RequirementsDependencyParser.class);

    private static final Pattern INCLUDE_PATTERN =
        Pattern.compile("^(?:-r|--requirement|-c|--constraint)\\s+(.+)$");

    private static final String MAIN_REQUIREMENTS_FILE = "requirements.txt";

    private static final String REQUIREMENTS_DEV_FILE = "requirements-dev.txt";

    private static final String DEV_REQUIREMENTS_FILE = "dev-requirements.txt";

    private final Map<String, InputFile> inputFilesByRelativePath;

    private final List<InputFile> inputFiles;

    public RequirementsDependencyParser(final Iterable<InputFile> inputFiles) {
        this.inputFilesByRelativePath = new LinkedHashMap<>();
        this.inputFiles = new ArrayList<>();
        for (InputFile inputFile : inputFiles) {
            inputFilesByRelativePath.put(normalizePath(inputFile.relativePath()), inputFile);
            this.inputFiles.add(inputFile);
        }
    }

    public List<DependencyOccurrence> parse(final PythonDependencyGroupType groupType, final List<String> groups) {
        final List<DependencyOccurrence> dependencies = new ArrayList<>();
        for (String fileName : fileNamesForGroup(groupType, groups)) {
            for (InputFile inputFile : findInputFiles(fileName)) {
                dependencies.addAll(parseFile(inputFile, groupType, new HashSet<>()));
            }
        }
        return dependencies;
    }

    private Set<InputFile> findInputFiles(final String configuredFileName) {
        final Set<InputFile> matches = new LinkedHashSet<>();
        final String normalizedFileName = normalizePath(configuredFileName);
        final InputFile exactMatch = inputFilesByRelativePath.get(normalizedFileName);
        if (exactMatch != null) {
            matches.add(exactMatch);
        }

        for (InputFile inputFile : inputFiles) {
            if (normalizedFileName.equals(inputFile.filename())) {
                matches.add(inputFile);
            }
        }

        return matches;
    }

    private List<String> fileNamesForGroup(final PythonDependencyGroupType groupType, final List<String> groups) {
        final List<String> fileNames = new ArrayList<>();
        if (PythonDependencyGroupType.MAIN.equals(groupType)) {
            fileNames.add(MAIN_REQUIREMENTS_FILE);
        } else if (PythonDependencyGroupType.DEV.equals(groupType)) {
            fileNames.add(REQUIREMENTS_DEV_FILE);
            fileNames.add(DEV_REQUIREMENTS_FILE);
        } else {
            for (String group : groups) {
                if ("main".equalsIgnoreCase(group)) {
                    fileNames.add(MAIN_REQUIREMENTS_FILE);
                } else if ("dev".equalsIgnoreCase(group)) {
                    fileNames.add(REQUIREMENTS_DEV_FILE);
                    fileNames.add(DEV_REQUIREMENTS_FILE);
                } else {
                    fileNames.add(group);
                }
            }
        }
        return fileNames;
    }

    private List<DependencyOccurrence> parseFile(final InputFile inputFile, final PythonDependencyGroupType groupType,
            final Set<String> visitedFiles) {

        final List<DependencyOccurrence> dependencies = new ArrayList<>();
        final String relativePath = normalizePath(inputFile.relativePath());
        if (!visitedFiles.add(relativePath)) {
            LOG.warn("Skipped recursive requirements include '{}'.", relativePath);
            return dependencies;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile.inputStream()))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                final String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }

                final Matcher includeMatcher = INCLUDE_PATTERN.matcher(trimmedLine);
                if (includeMatcher.matches()) {
                    final String includePath = stripQuotes(includeMatcher.group(1).trim());
                    if (PythonDependencyGroupType.DEV.equals(groupType)
                            && MAIN_REQUIREMENTS_FILE.equals(Path.of(includePath).getFileName().toString())) {
                        LOG.debug("Skipping dev requirements include '{}' because it is handled by the main rule.",
                            includePath);
                        continue;
                    }

                    final InputFile includedFile = resolveIncludedFile(inputFile, includePath);
                    if (includedFile != null) {
                        dependencies.addAll(parseFile(includedFile, groupType, visitedFiles));
                    } else {
                        LOG.warn("Skipped requirements include '{}' from '{}' because it is not indexed.",
                            includePath, inputFile);
                    }
                    continue;
                }

                final Optional<String> dependencyName = PythonRequirementNameParser.parseName(line);
                if (dependencyName.isPresent()) {
                    dependencies.add(new DependencyOccurrence(dependencyName.get(), inputFile, lineNumber));
                }
            }
        } catch (IOException e) {
            LOG.warn("Unable to read requirements file '{}'.", inputFile, e);
        }

        visitedFiles.remove(relativePath);
        return dependencies;
    }

    private InputFile resolveIncludedFile(final InputFile includingFile, final String includePath) {
        final Path parentPath = Path.of(includingFile.relativePath()).getParent();
        final Path resolvedPath;
        if (parentPath == null) {
            resolvedPath = Path.of(includePath).normalize();
        } else {
            resolvedPath = parentPath.resolve(includePath).normalize();
        }
        return inputFilesByRelativePath.get(normalizePath(resolvedPath.toString()));
    }

    private static String stripQuotes(final String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalizePath(final String path) {
        return path.replace('\\', '/');
    }
}
