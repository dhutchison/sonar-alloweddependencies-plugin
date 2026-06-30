package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import java.util.Arrays;
import java.util.function.Predicate;

import com.devwithimagination.sonar.alloweddependencies.plugin.util.PredicateFactory;

/**
 * Creates predicates for Python package allow lists.
 */
public class PythonAllowedDependenciesPredicateFactory {

    private static final String REGEX_PREFIX = "regex:";

    public Predicate<String> createPredicate(final String deps) {
        final String normalizedAllowList = normalizeExactRows(deps);
        final Predicate<String> predicate = new PredicateFactory()
            .createPredicateForDependencyListString(normalizedAllowList);

        return dependency -> predicate.test(PythonDependencyNameNormalizer.normalize(dependency));
    }

    private static String normalizeExactRows(final String deps) {
        if (deps == null) {
            return null;
        }

        return String.join("\n",
            Arrays.asList(deps.split("\\r?\\n"))
                .stream()
                .map(PythonAllowedDependenciesPredicateFactory::normalizeExactRow)
                .toArray(String[]::new));
    }

    private static String normalizeExactRow(final String row) {
        final String trimmed = row.trim();
        if (trimmed.startsWith(REGEX_PREFIX) || trimmed.startsWith("#") || trimmed.isEmpty()) {
            return row;
        }
        return PythonDependencyNameNormalizer.normalize(trimmed);
    }
}

