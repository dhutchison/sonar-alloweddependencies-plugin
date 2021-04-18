package com.devwithimagination.sonar.alloweddependencies.plugin.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for the PredicateFactory.
 */
class TestPredicateFactory {

    /**
     * Test if a predicate created by the {@link PredicateFactory} matches a
     * supplied dependency item.
     *
     * @param allowedDeps   the newline seperated string of dependencies which are
     *                      allowed.
     * @param testDep       the dependency item to compare with the predicate
     * @param expectedMatch the expected result - if the created predicate should
     *                      find a match or not.
     */
    @ParameterizedTest
    @MethodSource("provideTestArguments")
    void testPredicates(final String allowedDeps, final String testDep, final boolean expectedMatch) {

        final PredicateFactory factory = new PredicateFactory();

        final Predicate<String> predicate = factory.createPredicateForDependencyListString(allowedDeps);

        final boolean matches = predicate.test(testDep);
        assertEquals(expectedMatch, matches);

    }

    private static Stream<Arguments> provideTestArguments() {
        return Stream.of(
            /* Regex tests */
            Arguments.of(
                "regex:org\\.junit\\.*",
                "org.junit.dep:the-dep",
                true
            ),
            Arguments.of(
                "regex:org\\.junit\\.jupiter\\.*",
                "org.junit.dep:the-dep",
                false
            ),
            Arguments.of(
                "regex:org\\.junit\\.*\n" +
                "a-dep",
                "org.junit.dep:the-dep",
                true
            ),
            /* String tests */
            Arguments.of(
                "a-dep",
                "a-dep",
                true
            ),
            Arguments.of(
                "a-dep",
                "b-dep",
                false
            ),
            Arguments.of(
                "regex:org\\.junit\\.*\n" +
                "a-dep",
                "a-dep",
                true
            ),

            /* Testing comments */
            Arguments.of(
                "#a-dep\n" +
                "a-dep",
                "#a-dep",
                false
            ),
            Arguments.of(
                "#a-dep\n" +
                "a-dep",
                "a-dep",
                true
            )
        );
    }

}
