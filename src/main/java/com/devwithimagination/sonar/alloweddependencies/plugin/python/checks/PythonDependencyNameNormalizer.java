package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import java.util.Locale;

/**
 * Normalizes Python package names using the PEP 503 normalization shape.
 */
public final class PythonDependencyNameNormalizer {

    private PythonDependencyNameNormalizer() {

    }

    public static String normalize(final String dependencyName) {
        if (dependencyName == null) {
            return "";
        }
        return dependencyName.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[-_.]+", "-");
    }
}

