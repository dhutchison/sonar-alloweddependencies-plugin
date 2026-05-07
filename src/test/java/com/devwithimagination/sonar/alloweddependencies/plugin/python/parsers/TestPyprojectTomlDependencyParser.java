package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(dependencies.stream().allMatch(dependency -> dependency.getLineNumber() > 0));
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

    private static InputFile createInputFile(final String path) throws IOException {
        final File moduleBaseDir = new File(path);
        final File basePath = new File(moduleBaseDir, "pyproject.toml");
        final String fileContents = String.join(System.lineSeparator(), Files.readAllLines(basePath.toPath()));

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
}

