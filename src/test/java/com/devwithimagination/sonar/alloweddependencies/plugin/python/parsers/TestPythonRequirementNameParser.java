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
        assertEquals("my.package_extra-more",
            PythonRequirementNameParser.parseName("my.package_extra-more==1.0").get());
        assertEquals("broken",
            PythonRequirementNameParser.parseName("broken[extra>=1.0").get());
        assertEquals("package", PythonRequirementNameParser.parseName("package @ https://example.com/package.tar.gz").get());
        assertEquals("editable-package",
            PythonRequirementNameParser.parseName("-e git+https://example.com/repo.git#egg=editable-package").get());
        assertEquals("editable-package",
            PythonRequirementNameParser.parseName("--editable git+https://example.com/repo.git#egg=editable-package").get());
    }

    @Test
    void skipsOptionsAndUrlRequirementsWithoutEggName() {
        assertTrue(PythonRequirementNameParser.parseName(null).isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("   ").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("--index-url https://example.com/simple").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("git+https://example.com/repo.git").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("http://example.com/package.tar.gz").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("https://example.com/package.tar.gz").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("./local-package").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("/opt/local-package").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("[invalid]>=1.0").isEmpty());
        assertTrue(PythonRequirementNameParser.parseName("# comment").isEmpty());
    }

    @Test
    void stripsInlineCommentsOnlyOutsideQuotes() {
        assertEquals("requests==2 ", PythonRequirementNameParser.stripInlineComment("requests==2 # comment"));
        assertEquals("requests==2#not-comment",
            PythonRequirementNameParser.stripInlineComment("requests==2#not-comment"));
        assertEquals("package; marker == '#not-comment'",
            PythonRequirementNameParser.stripInlineComment("package; marker == '#not-comment'"));
        assertEquals("package; marker == \"#not-comment\"",
            PythonRequirementNameParser.stripInlineComment("package; marker == \"#not-comment\""));
        assertEquals("package; marker == \"quoted ' value\"",
            PythonRequirementNameParser.stripInlineComment("package; marker == \"quoted ' value\""));
        assertEquals("package; marker == 'quoted \" value'",
            PythonRequirementNameParser.stripInlineComment("package; marker == 'quoted \" value'"));
    }
}
