package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.xpath.XPathExpression;

import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SimpleXPathBasedCheck;

/** Reports Maven build and core extensions which are absent from the configured allow list. */
public class AllowedMavenExtensionsCheck extends SimpleXPathBasedCheck {

    private static final List<String> ALLOWED_FILE_NAMES =
        Arrays.asList("pom.xml", ".flattened-pom.xml", "extensions.xml");
    private static final String ISSUE_MESSAGE = "Remove this forbidden Maven extension: %s.";

    private final XPathExpression extensionExpression =
        getXPathExpression("/project/build/extensions/extension | /extensions/extension");
    private final AllowedMavenExtensionsCheckConfig config;

    public AllowedMavenExtensionsCheck(@Nonnull final AllowedMavenExtensionsCheckConfig config) {
        this.config = config;
    }

    @Override
    public void scanFile(final XmlFile xmlFile) {
        if (!ALLOWED_FILE_NAMES.contains(xmlFile.getInputFile().filename().toLowerCase())) {
            return;
        }

        evaluateAsList(extensionExpression, xmlFile.getNamespaceUnawareDocument()).forEach(extension -> {
            final String coordinate = MavenXmlCoordinates.extensionCoordinate(extension);
            if (!config.getAllowedExtensionsPredicate().test(coordinate)) {
                reportIssue(extension, String.format(ISSUE_MESSAGE, coordinate));
            }
        });
    }

    public AllowedMavenExtensionsCheckConfig getConfig() {
        return config;
    }
}
