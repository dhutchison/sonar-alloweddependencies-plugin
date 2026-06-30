package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Maven XML coordinate extraction and normalization helpers. */
final class MavenXmlCoordinates {

    static final String DEFAULT_PLUGIN_GROUP = "org.apache.maven.plugins";

    private MavenXmlCoordinates() {
    }

    static String pluginCoordinate(final Node declaration) {
        return coordinate(declaration, DEFAULT_PLUGIN_GROUP);
    }

    static String extensionCoordinate(final Node declaration) {
        return coordinate(declaration, null);
    }

    static String normalizePluginAllowListRow(final String row) {
        return row.indexOf(':') < 0 ? DEFAULT_PLUGIN_GROUP + ":" + row : row;
    }

    private static String coordinate(final Node declaration, final String defaultGroup) {
        final String groupId = childText(declaration, "groupId", defaultGroup);
        final String artifactId = childText(declaration, "artifactId", null);
        return groupId + ":" + artifactId;
    }

    private static String childText(final Node parent, final String name, final String defaultValue) {
        for (Node child : XmlFile.children(parent)) {
            if (child.getNodeType() == Node.ELEMENT_NODE && ((Element) child).getTagName().equals(name)) {
                return child.getTextContent().trim();
            }
        }
        return defaultValue;
    }
}
