package com.devwithimagination.sonar.alloweddependencies.plugin.python.parsers;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts package names from common Python requirement strings.
 */
public final class PythonRequirementNameParser {

    private static final Pattern REQUIREMENT_NAME_PATTERN =
        Pattern.compile("^\\s*([A-Za-z0-9][A-Za-z0-9._-]*)(?:\\s*\\[.*?\\])?.*$");

    private static final Pattern EGG_FRAGMENT_PATTERN =
        Pattern.compile("(?:^|[#&])egg=([A-Za-z0-9][A-Za-z0-9._-]*)");

    private PythonRequirementNameParser() {

    }

    public static Optional<String> parseName(final String requirement) {
        if (requirement == null) {
            return Optional.empty();
        }

        String line = stripInlineComment(requirement).trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return Optional.empty();
        }

        if (line.startsWith("-e ")) {
            line = line.substring(3).trim();
        } else if (line.startsWith("--editable ")) {
            line = line.substring("--editable ".length()).trim();
        }

        final Matcher eggMatcher = EGG_FRAGMENT_PATTERN.matcher(line);
        if (eggMatcher.find()) {
            return Optional.of(eggMatcher.group(1));
        }

        if (line.startsWith("-") || line.startsWith("git+") || line.startsWith("http://") || line.startsWith("https://")
                || line.startsWith(".") || line.startsWith("/")) {
            return Optional.empty();
        }

        final int directReferenceIndex = line.indexOf(" @ ");
        if (directReferenceIndex > 0) {
            line = line.substring(0, directReferenceIndex).trim();
        }

        final Matcher matcher = REQUIREMENT_NAME_PATTERN.matcher(line);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }

        return Optional.empty();
    }

    private static String stripInlineComment(final String requirement) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int index = 0; index < requirement.length(); index++) {
            final char current = requirement.charAt(index);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '#' && !inSingleQuote && !inDoubleQuote
                    && (index == 0 || Character.isWhitespace(requirement.charAt(index - 1)))) {
                return requirement.substring(0, index);
            }
        }
        return requirement;
    }
}

