package com.devwithimagination.sonar.alloweddependencies.plugin.python.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void regexMatchesUseNormalizedCandidate() {
        final Predicate<String> predicate = new PythonAllowedDependenciesPredicateFactory()
            .createPredicate("regex:^types-.*$");

        assertTrue(predicate.test("types_requests"));
        assertFalse(predicate.test("requests"));
    }
}

