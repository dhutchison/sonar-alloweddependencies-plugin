package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Builds allow-list predicates for canonical Maven coordinates. */
final class MavenCoordinatePredicateFactory {

    private static final String REGEX_PREFIX = "regex:";

    Predicate<String> create(final String configuredCoordinates, final UnaryOperator<String> exactNormalizer) {
        if (configuredCoordinates == null) {
            return coordinate -> false;
        }

        return Arrays.stream(configuredCoordinates.split("\\r?\\n"))
            .map(String::trim)
            .filter(row -> !row.isEmpty())
            .filter(row -> !row.startsWith("#"))
            .map(row -> createRowPredicate(row, exactNormalizer))
            .reduce(coordinate -> false, Predicate::or);
    }

    private Predicate<String> createRowPredicate(final String row, final UnaryOperator<String> exactNormalizer) {
        if (row.startsWith(REGEX_PREFIX)) {
            final String expression = row.substring(REGEX_PREFIX.length());
            try {
                return Pattern.compile(expression, Pattern.CASE_INSENSITIVE).asPredicate();
            } catch (PatternSyntaxException exception) {
                throw new IllegalArgumentException("Invalid Maven coordinate allow-list regex: " + expression, exception);
            }
        }

        final String normalized = exactNormalizer.apply(row);
        return normalized::equalsIgnoreCase;
    }
}
