package com.devwithimagination.sonar.alloweddependencies.plugin.common;

import org.sonar.api.batch.fs.InputFile;

/**
 * A dependency found in a source descriptor file.
 */
public class DependencyOccurrence {

    private final String name;

    private final InputFile inputFile;

    private final int lineNumber;

    public DependencyOccurrence(final String name, final InputFile inputFile, final int lineNumber) {
        this.name = name;
        this.inputFile = inputFile;
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public InputFile getInputFile() {
        return inputFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}

