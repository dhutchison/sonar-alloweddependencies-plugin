package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import static com.devwithimagination.sonar.alloweddependencies.plugin.common.Constants.ISSUE_MESSAGE;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.xpath.XPathExpression;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SimpleXPathBasedCheck;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implementation of {@link SimpleXPathBasedCheck} which compares declared Maven
 * dependencies in a pom.xml file against a list of approved dependencies,
 * raising issues for any which are not found.
 */

public class AllowedMavenDependenciesCheck extends SimpleXPathBasedCheck {

    /**
     * The default scope to assign to a dependency if it is not defined in the pom.
     */
    private static final String DEFAULT_MAVEN_SCOPE = "compile";

    /**
     * Logger
     */
    private static final Logger LOG = Loggers.get(AllowedMavenDependenciesCheck.class);

    private static final List<String> ALLOWED_FILE_NAMES = Arrays.asList(
        "pom.xml",
        ".flattened-pom.xml"
    );

    /**
     * The XPath Expression used to find maven dependency nodes in a pom file.
     */
    private final XPathExpression dependencyExpression = getXPathExpression("//dependencies/dependency");

    /**
     * The configuration for this check.
     */
    private final AllowedMavenDependenciesCheckConfig config;

    /**
     * Create a new {@link AllowedMavenDependenciesCheck} based on the supplied configuration.
     *
     * @param config the configuration for this check
     */
    public AllowedMavenDependenciesCheck(@Nonnull final AllowedMavenDependenciesCheckConfig config) {

        LOG.info("Creating AllowedMavenDependenciesCheck for {}", config.getRule().ruleKey());
        this.config = config;

    }

    @Override
    public void scanFile(final XmlFile xmlFile) {

        /* Only scan pom.xml files */
        if (ALLOWED_FILE_NAMES.contains(xmlFile.getInputFile().filename().toLowerCase())) {

            /*
             * Iterate through matches for the XPath expression and compare with the list of
             * approved dependencies
             */
            evaluateAsList(dependencyExpression, xmlFile.getNamespaceUnawareDocument()).forEach(dependency -> {

                final String groupId = getChildElementText("groupId", dependency, null);
                final String artifactId = getChildElementText("artifactId", dependency, null);
                final String scope = getChildElementText("scope", dependency, DEFAULT_MAVEN_SCOPE);

                final String listKey = groupId + ":" + artifactId;

                if ((config.getScopes().isEmpty() || config.getScopes().contains(scope))
                        && !config.getAllowedDependenciesPredicate().test(listKey)) {

                    LOG.info("Forbidden dependency: {}", listKey);

                    reportIssue(dependency, String.format(ISSUE_MESSAGE, listKey));
                }
            });
        }
    }

    /**
     * Get the config this check was created with.
     *
     * @return the config object.
     */
    public AllowedMavenDependenciesCheckConfig getConfig() {
        return config;
    }

    /**
     * Get the text node content from a child element of a parent.
     *
     * @param childElementName the name of the element to find
     * @param parent           the node to look for children of
     * @param defaultValue     the default value to return if a match is not found.
     */
    private static String getChildElementText(final String childElementName, final Node parent,
            final String defaultValue) {

        for (Node node : XmlFile.children(parent)) {
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(childElementName)) {
                return node.getTextContent();
            }
        }

        return defaultValue;
    }
}
