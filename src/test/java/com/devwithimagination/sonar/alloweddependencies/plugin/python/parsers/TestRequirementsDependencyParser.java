package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class TestRequirementsDependencyParser {

    @Test
    void parsesMainRequirementsWithIncludesAndConstraints() throws IOException {
        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(createInputFiles())
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList(
            "constrained-package",
            "cyclic-package",
            "package",
            "requests",
            "shared-package",
            "urllib3"), dependencyNames(dependencies));
    }

    @Test
    void parsesDevRequirementsAndSkipsMainRequirementsInclude() throws IOException {
        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(createInputFiles())
            .parse(PythonDependencyGroupType.DEV, Arrays.asList("dev"));

        assertEquals(Arrays.asList(
            "dev_extra",
            "editable-package",
            "pytest",
            "ruff",
            "tox"), dependencyNames(dependencies));
    }

    @Test
    void parsesExplicitTemplateRequirementFile() throws IOException {
        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(createInputFiles())
            .parse(PythonDependencyGroupType.CUSTOM, Arrays.asList("dev-shared.txt"));

        assertEquals(Arrays.asList("tox"), dependencyNames(dependencies));
    }

    private static List<InputFile> createInputFiles() throws IOException {
        return Arrays.asList(
            createInputFile("requirements.txt"),
            createInputFile("shared.txt"),
            createInputFile("cyclic.txt"),
            createInputFile("constraints.txt"),
            createInputFile("requirements-dev.txt"),
            createInputFile("dev-requirements.txt"),
            createInputFile("dev-shared.txt"));
    }

    private static InputFile createInputFile(final String fileName) throws IOException {
        final File moduleBaseDir = new File("src/test/resources/python/requirements");
        final File basePath = new File(moduleBaseDir, fileName);
        final String fileContents = String.join(System.lineSeparator(), Files.readAllLines(basePath.toPath()));

        return new TestInputFileBuilder("python-requirements-test-project", moduleBaseDir, basePath)
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

