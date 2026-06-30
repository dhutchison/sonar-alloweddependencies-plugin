package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.xpath.XPathExpression;

import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SimpleXPathBasedCheck;

/** Reports activated Maven plugins which are absent from the configured allow list. */
public class AllowedMavenPluginsCheck extends SimpleXPathBasedCheck {

    private static final List<String> POM_FILE_NAMES = Arrays.asList("pom.xml", ".flattened-pom.xml");
    private static final String ISSUE_MESSAGE = "Remove this forbidden Maven plugin: %s.";

    private final XPathExpression pluginExpression = getXPathExpression(
        "/project/build/plugins/plugin"
            + " | /project/profiles/profile/build/plugins/plugin"
            + " | /project/reporting/plugins/plugin"
            + " | /project/profiles/profile/reporting/plugins/plugin");
    private final AllowedMavenPluginsCheckConfig config;

    public AllowedMavenPluginsCheck(@Nonnull final AllowedMavenPluginsCheckConfig config) {
        this.config = config;
    }

    @Override
    public void scanFile(final XmlFile xmlFile) {
        if (!POM_FILE_NAMES.contains(xmlFile.getInputFile().filename().toLowerCase())) {
            return;
        }

        evaluateAsList(pluginExpression, xmlFile.getNamespaceUnawareDocument()).forEach(plugin -> {
            final String coordinate = MavenXmlCoordinates.pluginCoordinate(plugin);
            if (!config.getAllowedPluginsPredicate().test(coordinate)) {
                reportIssue(plugin, String.format(ISSUE_MESSAGE, coordinate));
            }
        });
    }

    public AllowedMavenPluginsCheckConfig getConfig() {
        return config;
    }
}
