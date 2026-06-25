package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.devwithimagination.sonar.alloweddependencies.plugin.common.DependencyOccurrence;
import com.devwithimagination.sonar.alloweddependencies.plugin.python.checks.PythonDependencyGroupType;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

class TestPyprojectTomlDependencyParser {

    @Test
    void parsesMainDependenciesFromProjectAndPoetryTables() throws IOException {
        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(createInputFile("src/test/resources/python/pyproject"), PythonDependencyGroupType.MAIN,
                Arrays.asList("main"));

        final List<String> names = dependencyNames(dependencies);
        assertEquals(Arrays.asList("FastAPI", "Requests", "external-main", "external-poetry", "urllib3"), names);
        assertLineNumber(dependencies, "Requests", 2);
        assertLineNumber(dependencies, "urllib3", 3);
        assertLineNumber(dependencies, "FastAPI", 10);
    }

    @Test
    void parsesDevDependenciesFromPoetryAndPep735Tables() throws IOException {
        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(createInputFile("src/test/resources/python/pyproject"), PythonDependencyGroupType.DEV,
                Arrays.asList("dev"));

        final List<String> names = dependencyNames(dependencies);
        assertEquals(Arrays.asList(
            "flake8",
            "legacy-dev",
            "modern-dev",
            "mypy",
            "pep735-dev-extra",
            "pep735_lint_extra",
            "pytest",
            "ruff"), names);
    }

    @Test
    void parsesTemplateDependenciesFromPoetryAndPep735Groups() throws IOException {
        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(createInputFile("src/test/resources/python/pyproject"), PythonDependencyGroupType.CUSTOM,
                Arrays.asList("docs"));

        final List<String> names = dependencyNames(dependencies);
        assertEquals(Arrays.asList(
            "external-docs",
            "flake8",
            "mkdocs-material",
            "pep735_lint_extra",
            "sphinx",
            "sphinx"), names);
    }

    @Test
    void skipsInvalidTomlWithoutThrowing() throws IOException {
        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(createInputFile("src/test/resources/python/invalid"), PythonDependencyGroupType.MAIN,
                Arrays.asList("main"));

        assertTrue(dependencies.isEmpty());
    }

    @Test
    void fallsBackToFirstLineWhenTomlPositionIsMissing() {
        assertEquals(1, PyprojectTomlDependencyParser.lineNumber(null));
    }

    @Test
    void skipsUnreadableTomlWithoutThrowing() throws IOException {
        final InputFile inputFile = mock(InputFile.class);
        when(inputFile.inputStream()).thenThrow(new IOException("unreadable"));

        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(inputFile, PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertTrue(dependencies.isEmpty());
    }

    @Test
    void skipsMissingAndUnsupportedDependencyTables() {
        final InputFile inputFile = createGeneratedInputFile(
            "[project]\n" +
            "dependencies = \"not-an-array\"\n" +
            "\n" +
            "[dependency-groups]\n" +
            "docs = \"not-an-array\"\n");

        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(inputFile, PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertTrue(dependencies.isEmpty());
    }

    @Test
    void skipsUnsupportedPep735ItemsAndInvalidRequirementStrings() {
        final InputFile inputFile = createGeneratedInputFile(
            "[dependency-groups]\n" +
            "docs = [\n" +
            "    \"sphinx\",\n" +
            "    42,\n" +
            "    \"--index-url https://example.com/simple\",\n" +
            "    {not-include-group = \"ignored\"},\n" +
            "]\n");

        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(inputFile, PythonDependencyGroupType.CUSTOM, Arrays.asList("docs", "missing"));

        assertEquals(Arrays.asList("sphinx"), dependencyNames(dependencies));
    }

    @Test
    void skipsUnsupportedProjectDependencyArrayItemsAndInvalidRequirements() {
        final InputFile inputFile = createGeneratedInputFile(
            "[project]\n" +
            "dependencies = [\n" +
            "    \"requests\",\n" +
            "    42,\n" +
            "    \"--index-url https://example.com/simple\",\n" +
            "]\n");

        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(inputFile, PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList("requests"), dependencyNames(dependencies));
    }

    @Test
    void stopsPep735IncludeCycles() {
        final InputFile inputFile = createGeneratedInputFile(
            "[dependency-groups]\n" +
            "first = [\n" +
            "    \"first-package\",\n" +
            "    {include-group = \"second\"},\n" +
            "]\n" +
            "second = [\n" +
            "    \"second-package\",\n" +
            "    {include-group = \"first\"},\n" +
            "]\n");

        final List<DependencyOccurrence> dependencies = new PyprojectTomlDependencyParser()
            .parse(inputFile, PythonDependencyGroupType.CUSTOM, Arrays.asList("first"));

        assertEquals(Arrays.asList("first-package", "second-package"), dependencyNames(dependencies));
        assertLineNumber(dependencies, "first-package", 2);
        assertLineNumber(dependencies, "second-package", 6);
    }

    private static InputFile createInputFile(final String path) throws IOException {
        final File moduleBaseDir = new File(path);
        final File basePath = new File(moduleBaseDir, "pyproject.toml");
        final String fileContents = String.join(System.lineSeparator(), Files.readAllLines(basePath.toPath()));

        return createInputFile(moduleBaseDir, basePath, fileContents);
    }

    private static InputFile createGeneratedInputFile(final String fileContents) {
        final File moduleBaseDir = new File("src/test/resources/python/generated");
        final File basePath = new File(moduleBaseDir, "pyproject.toml");
        return createInputFile(moduleBaseDir, basePath, fileContents);
    }

    private static InputFile createInputFile(final File moduleBaseDir, final File basePath, final String fileContents) {
        return new TestInputFileBuilder("python-test-project", moduleBaseDir, basePath)
            .setCharset(Charset.forName("UTF-8"))
            .setContents(fileContents)
            .build();
    }

    private static List<String> dependencyNames(final List<DependencyOccurrence> dependencies) {
        return dependencies.stream()
            .map(DependencyOccurrence::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    private static void assertLineNumber(final List<DependencyOccurrence> dependencies, final String name,
            final int lineNumber) {

        final DependencyOccurrence occurrence = dependencies.stream()
            .filter(dependency -> name.equals(dependency.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Missing dependency " + name));

        assertEquals(lineNumber, occurrence.getLineNumber());
    }
}
