package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class TestPythonAllowedDependenciesPredicateFactory {

    @Test
    void exactMatchesUsePythonNameNormalization() {
        final Predicate<String> predicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate("Requests-Extra");

        assertTrue(predicate.test("requests_extra"));
        assertTrue(predicate.test("requests.extra"));
    }

    @Test
    void emptyAndNullNamesNormalizeToEmptyString() {
        assertEquals("", PythonDependencyNameNormalizer.normalize(null));
        assertEquals("", PythonDependencyNameNormalizer.normalize("   "));
    }

    @Test
    void nullAllowListRejectsAllDependencies() {
        final Predicate<String> predicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate(null);

        assertFalse(predicate.test("requests"));
    }

    @Test
    void commentsAndBlankRowsArePreservedWhileExactRowsAreNormalized() {
        final Predicate<String> predicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate("# comment\n\nRequests_Extra");

        assertTrue(predicate.test("requests-extra"));
        assertFalse(predicate.test("other"));
    }

    @Test
    void regexMatchesUseNormalizedCandidate() {
        final Predicate<String> predicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate("regex:^types-.*$");

        assertTrue(predicate.test("types_requests"));
        assertFalse(predicate.test("requests"));
    }
}
