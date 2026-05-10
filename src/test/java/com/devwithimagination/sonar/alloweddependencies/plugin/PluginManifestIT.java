package com.devwithimagination.sonar.alloweddependencies.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

/**
 * Integration-level packaging checks for the generated SonarQube plugin jar.
 */
class PluginManifestIT {

    @Test
    void generatedPluginManifestContainsModernRuntimeMetadata() throws IOException {
        final Path pluginJar = findPluginJar();

        try (JarFile jarFile = new JarFile(pluginJar.toFile())) {
            final Manifest manifest = jarFile.getManifest();
            final Attributes attributes = manifest.getMainAttributes();

            assertEquals("com.devwithimagination.sonar.alloweddependencies.AllowedDependenciesPlugin",
                attributes.getValue("Plugin-Class"));
            assertEquals("json,xml", attributes.getValue("Plugin-RequiredForLanguages"));
            assertTrue(attributes.getValue("Sonar-Version").startsWith("13.5.0."),
                "Expected the manifest to declare the modern Sonar plugin API minimum version");
        }
    }

    private static Path findPluginJar() throws IOException {
        Optional<Path> newestJar = Optional.empty();
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(
                Path.of("target"), "sonar-alloweddependencies-plugin-*.jar")) {
            for (Path jar : jars) {
                if (!jar.getFileName().toString().contains("-sources")) {
                    if (newestJar.isEmpty()
                            || Comparator.comparingLong(PluginManifestIT::lastModified)
                                .compare(jar, newestJar.get()) > 0) {
                        newestJar = Optional.of(jar);
                    }
                }
            }
        }
        return newestJar.orElseThrow(() -> new IOException("Could not find generated plugin jar in target/"));
    }

    private static long lastModified(final Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
