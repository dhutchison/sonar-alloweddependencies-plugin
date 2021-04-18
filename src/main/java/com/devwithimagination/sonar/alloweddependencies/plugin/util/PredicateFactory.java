package com.devwithimagination.sonar.alloweddependencies.plugin.util;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Class for creating Predicates for matching dependencies against.
 */
public class PredicateFactory {

    private static final String REGEX_PREFIX = "regex:";

    private static final String COMMENT_LINE_PREFIX = "#";

    /**
     * Logger
     */
    private static final Logger LOG = Loggers.get(PredicateFactory.class);

    /**
     * Create a predicate that will match any of the dependencies.
     *
     * @param deps the newline seperated list of dependency items which are valid.
     *             If a row is prefixed with {@value #REGEX_PREFIX} then it will be
     *             interpreted as a regular expression. Any line starting with '#'
     *             will be ignored.
     * @return the created {@link Predicate}.
     */
    public Predicate<String> createPredicateForDependencyListString(final String deps) {

        LOG.info("Allowed dependencies: '{}'", deps);

        if (deps == null) {
            return (t -> false);
        } else {

            return Arrays.asList(deps.split("\\r?\\n"))
                    .stream()
                    .map(String::trim)
                    .filter(v -> !v.startsWith(COMMENT_LINE_PREFIX))
                    .sorted()
                    .map(this::createPredicateForSingleDependencyRow)
                    .reduce(x -> false, Predicate::or);
        }
    }

    /**
     * Create a predicate for a single dependency item.
     *
     * @param deps the newline seperated list of dependency items which are valid.
     *             If a row is prefixed with {@value #REGEX_PREFIX} then it will be
     *             interpreted as a regular expression.
     * @return the created {@link Predicate}.
     */
    Predicate<String> createPredicateForSingleDependencyRow(final String dep) {

        if (dep.startsWith(REGEX_PREFIX)) {
            /*
             * Create a predicate for a regex expression, we always enable the case
             * insensitive flags
             */
            final String pattern = dep.substring(REGEX_PREFIX.length());

            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).asPredicate();
        } else {
            /* Exact string match */
            return dep::equalsIgnoreCase;
        }

    }

}
