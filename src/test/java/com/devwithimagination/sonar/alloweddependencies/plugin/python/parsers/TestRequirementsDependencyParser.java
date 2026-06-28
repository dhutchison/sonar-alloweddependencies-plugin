package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class TestRequirementsDependencyParser {

    @Test
    void parsesMainRequirementsWithIncludesAndIgnoresConstraints() throws IOException {
        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(createInputFiles())
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList(
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

    @Test
    void parsesExplicitTemplateRequirementFiles() {
        final List<InputFile> inputFiles = Arrays.asList(
            createInputFile("requirements.txt", "requests==2.32.0\n"),
            createInputFile("requirements-dev.txt", "pytest==8.2.0\n"),
            createInputFile("dev-requirements.txt", "ruff==0.5.0\n"),
            createInputFile("docs.txt", "sphinx==7.3.7\n"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.CUSTOM, Arrays.asList(
                "requirements.txt", "requirements-dev.txt", "dev-requirements.txt", "docs.txt"));

        assertEquals(Arrays.asList("pytest", "requests", "ruff", "sphinx"), dependencyNames(dependencies));
    }

    @Test
    void parsesLogicalLinesAndIgnoresConstraints() {
        final List<InputFile> inputFiles = Arrays.asList(
            createInputFile("requirements.txt",
                "requests==2.32.0 \\\n" +
                "    --hash=sha256:abc123 \\\n" +
                "    --hash=sha256:def456\n" +
                "-r shared.txt # shared dependencies\n" +
                "--constraint=constraints.txt\n"),
            createInputFile("shared.txt", "urllib3==2.2.0\n"),
            createInputFile("constraints.txt", "idna==3.7\n"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList("requests", "urllib3"), dependencyNames(dependencies));
        assertOccurrence(dependencies, "requests", "requirements.txt", 1);
        assertOccurrence(dependencies, "urllib3", "shared.txt", 1);
    }

    @Test
    void parsesRequirementIncludeFormsAndIgnoresConstraintForms() {
        final List<InputFile> inputFiles = Arrays.asList(
            createInputFile("requirements.txt",
                "-r shared.txt\n" +
                "-c constraints.txt\n" +
                "--requirement nested/requirements-extra.txt\n" +
                "--constraint='nested/constraints-extra.txt'\n" +
                "--requirement=\"nested/requirements-quoted.txt\"\n"),
            createInputFile("shared.txt", "urllib3==2.2.0\n"),
            createInputFile("constraints.txt", "idna==3.7\n"),
            createInputFile("nested/requirements-extra.txt", "certifi==2024.7.4\n"),
            createInputFile("nested/constraints-extra.txt", "charset-normalizer==3.3.2\n"),
            createInputFile("nested/requirements-quoted.txt", "h11==0.14.0\n"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList("certifi", "h11", "urllib3"),
            dependencyNames(dependencies));
    }

    @Test
    void ignoresCommentsBlankLinesAndMalformedIncludes() {
        final List<InputFile> inputFiles = Arrays.asList(
            createInputFile("requirements.txt",
                "\n" +
                "   # comment\n" +
                "-r   \n" +
                "-r=shared.txt\n" +
                "--requirement\n" +
                "--requirement:\n" +
                "--constraint=   \n" +
                "--requirement=\"missing.txt\n" +
                "--constraint='missing.txt\n" +
                "requests==2.32.0\n"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList("requests"), dependencyNames(dependencies));
    }

    @Test
    void parsesUnterminatedLineContinuationAtEndOfFile() {
        final List<InputFile> inputFiles = Arrays.asList(
            createInputFile("requirements.txt", "requests==2.32.0 \\"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList("requests"), dependencyNames(dependencies));
        assertOccurrence(dependencies, "requests", "requirements.txt", 1);
    }

    @Test
    void resolvesNestedRelativeIncludes() {
        final List<InputFile> inputFiles = Arrays.asList(
            createInputFile("config/requirements-tools.txt", "-r ../shared/tools.txt\n"),
            createInputFile("shared/tools.txt", "build==1.2.1\n"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.CUSTOM, Arrays.asList("config/requirements-tools.txt"));

        assertEquals(Arrays.asList("build"), dependencyNames(dependencies));
        assertOccurrence(dependencies, "build", "shared/tools.txt", 1);
    }

    @Test
    void skipsMissingAndUnreadableFilesWithoutAbortingOtherFiles() throws IOException {
        final InputFile unreadableRequirements = mock(InputFile.class);
        when(unreadableRequirements.relativePath()).thenReturn("broken/requirements.txt");
        when(unreadableRequirements.filename()).thenReturn("requirements.txt");
        when(unreadableRequirements.inputStream()).thenThrow(new IOException("unreadable"));

        final List<InputFile> inputFiles = Arrays.asList(
            unreadableRequirements,
            createInputFile("working/requirements.txt",
                "-r missing.txt\n" +
                "requests==2.32.0\n"));

        final List<DependencyOccurrence> dependencies = new RequirementsDependencyParser(inputFiles)
            .parse(PythonDependencyGroupType.MAIN, Arrays.asList("main"));

        assertEquals(Arrays.asList("requests"), dependencyNames(dependencies));
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

        return createInputFile(fileName, fileContents);
    }

    private static InputFile createInputFile(final String fileName, final String fileContents) {
        final File moduleBaseDir = new File("src/test/resources/python/requirements");
        final File basePath = new File(moduleBaseDir, fileName);

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

    private static void assertOccurrence(final List<DependencyOccurrence> dependencies, final String name,
            final String relativePath, final int lineNumber) {

        final DependencyOccurrence occurrence = dependencies.stream()
            .filter(dependency -> name.equals(dependency.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Missing dependency " + name));

        assertEquals(relativePath, occurrence.getInputFile().relativePath());
        assertEquals(lineNumber, occurrence.getLineNumber());
    }
}
