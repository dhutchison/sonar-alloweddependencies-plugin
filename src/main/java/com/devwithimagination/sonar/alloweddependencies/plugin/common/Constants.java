package com.devwithimagination.sonar.alloweddependencies.plugin.common;

/**
 * Class containing common constants which apply to all implementations.
 */
public final class Constants {

    private Constants() {

    }

    /**
     * The message to use for created issues. This contains a string placeholder for
     * inserting the name of the dependency which was not on the allowed list.
     */
    public static final String ISSUE_MESSAGE = "Remove this forbidden dependency: %s.";

}
