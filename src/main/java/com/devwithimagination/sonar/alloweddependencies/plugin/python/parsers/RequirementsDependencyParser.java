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
            int logicalLineNumber = 0;
            final StringBuilder logicalLine = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (logicalLine.length() == 0) {
                    logicalLineNumber = lineNumber;
                }

                final boolean continued = hasLineContinuation(line);
                logicalLine.append(continued ? line.substring(0, line.length() - 1) : line);
                if (!continued) {
                    addDependenciesForLogicalLine(dependencies, inputFile, groupType, visitedFiles,
                        logicalLine.toString(), logicalLineNumber);
                    logicalLine.setLength(0);
                }
            }

            if (logicalLine.length() > 0) {
                addDependenciesForLogicalLine(dependencies, inputFile, groupType, visitedFiles,
                    logicalLine.toString(), logicalLineNumber);
            }
        } catch (IOException e) {
            LOG.warn("Unable to read requirements file '{}'.", inputFile, e);
        }

        visitedFiles.remove(relativePath);
        return dependencies;
    }

    private void addDependenciesForLogicalLine(final List<DependencyOccurrence> dependencies, final InputFile inputFile,
            final PythonDependencyGroupType groupType, final Set<String> visitedFiles, final String logicalLine,
            final int lineNumber) {

        final String trimmedLine = PythonRequirementNameParser.stripInlineComment(logicalLine).trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
            return;
        }

        final Optional<String> includePath = parseIncludePath(trimmedLine);
        if (includePath.isPresent()) {
            addIncludedFileDependencies(dependencies, inputFile, groupType, visitedFiles, includePath.get());
            return;
        }

        addRequirementDependency(dependencies, inputFile, trimmedLine, lineNumber);
    }

    private void addIncludedFileDependencies(final List<DependencyOccurrence> dependencies,
            final InputFile inputFile, final PythonDependencyGroupType groupType, final Set<String> visitedFiles,
            final String includePath) {

        if (PythonDependencyGroupType.DEV.equals(groupType)
                && MAIN_REQUIREMENTS_FILE.equals(Path.of(includePath).getFileName().toString())) {
            LOG.debug("Skipping dev requirements include '{}' because it is handled by the main rule.", includePath);
            return;
        }

        final InputFile includedFile = resolveIncludedFile(inputFile, includePath);
        if (includedFile != null) {
            dependencies.addAll(parseFile(includedFile, groupType, visitedFiles));
        } else {
            LOG.warn("Skipped requirements include '{}' from '{}' because it is not indexed.", includePath, inputFile);
        }
    }

    private static Optional<String> parseIncludePath(final String line) {
        final Optional<String> shortOptionPath = parseShortIncludePath(line);
        if (shortOptionPath.isPresent()) {
            return shortOptionPath;
        }

        final Optional<String> requirementPath = parseLongIncludePath(line, "--requirement");
        if (requirementPath.isPresent()) {
            return requirementPath;
        }

        return parseLongIncludePath(line, "--constraint");
    }

    private static Optional<String> parseShortIncludePath(final String line) {
        if (line.length() > 2 && ("-r".equals(line.substring(0, 2)) || "-c".equals(line.substring(0, 2)))
                && Character.isWhitespace(line.charAt(2))) {
            return Optional.of(stripQuotes(line.substring(3).trim()));
        }
        return Optional.empty();
    }

    private static Optional<String> parseLongIncludePath(final String line, final String optionName) {
        if (!line.startsWith(optionName) || line.length() == optionName.length()) {
            return Optional.empty();
        }

        final char separator = line.charAt(optionName.length());
        if (separator == '=') {
            return nonEmptyPath(line.substring(optionName.length() + 1));
        } else if (Character.isWhitespace(separator)) {
            return nonEmptyPath(line.substring(optionName.length() + 1));
        }
        return Optional.empty();
    }

    private static Optional<String> nonEmptyPath(final String value) {
        final String path = stripQuotes(value.trim());
        if (path.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(path);
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

    private static void addRequirementDependency(final List<DependencyOccurrence> dependencies,
            final InputFile inputFile, final String requirement, final int lineNumber) {

        final Optional<String> dependencyName = PythonRequirementNameParser.parseName(requirement);
        if (dependencyName.isPresent()) {
            dependencies.add(new DependencyOccurrence(dependencyName.get(), inputFile, lineNumber));
        }
    }

    private static boolean hasLineContinuation(final String line) {
        int trailingBackslashes = 0;
        for (int index = line.length() - 1; index >= 0 && line.charAt(index) == '\\'; index--) {
            trailingBackslashes++;
        }
        return trailingBackslashes % 2 == 1;
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
