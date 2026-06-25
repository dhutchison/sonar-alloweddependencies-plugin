package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TestPythonRequirementNameParser {

    @Test
    void parsesCommonRequirementShapes() {
        assertEquals("requests", PythonRequirementNameParser.parseName("requests==2").get());
        assertEquals("urllib3", PythonRequirementNameParser.parseName("urllib3[secure]>=2 ; python_version > '3'").get());
        assertEquals("uvicorn",
            PythonRequirementNameParser.parseName("uvicorn[standard,watchfiles]>=0.29").get());
        assertEquals("broken",
            PythonRequirementNameParser.parseName("broken[extra>=1.0").get());
        assertEquals("package", PythonRequirementNameParser.parseName("package @ https://example.com/package.tar.gz").get());
        assertEquals("editable-package",
            PythonRequirementNameParser.parseName("-e git+https://example.com/repo.git#egg=editable-package").get());
    }

    @Test
    void skipsOptionsAndUrlRequirementsWithoutEggName() {
        assertTrue(PythonRequirementNameParser.parseName("--index-url https://example.com/simple").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("git+https://example.com/repo.git").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("# comment").isEmpty());
    }
}
