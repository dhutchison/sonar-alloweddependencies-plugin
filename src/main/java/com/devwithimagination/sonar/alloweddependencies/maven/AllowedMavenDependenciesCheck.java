package com.devwithimagination.sonar.alloweddependencies.maven;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpression;
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
     * The XPath Expression used to find maven dependency nodes in a pom file.
     */
    private XPathExpression dependencyExpression = getXPathExpression("//dependencies/dependency");

    /**
     * List containing the "groupId:artifactId" pairs for dependencies which are
     * allowed.
     */
    private final List<String> allowedDependencies;

    /**
     * Create a new {@link AllowedMavenDependenciesCheck}.
     *
     * @param allowedDependencies list containing the "groupId:artifactId" pairs for
     *                            dependencies which are allowed
     */
    public AllowedMavenDependenciesCheck(final List<String> allowedDependencies) {

        /* Null check first, then add */
        this.allowedDependencies = new ArrayList<>();

        if (allowedDependencies != null) {
            this.allowedDependencies.addAll(allowedDependencies);
        }

    }

    @Override
    public void scanFile(XmlFile xmlFile) {

        /* Only scan pom.xml files */
        if ("pom.xml".equalsIgnoreCase(xmlFile.getInputFile().filename())) {

            /*
             * Iterate through matches for the XPath expression and compare with the list of
             * approved dependencies
             */
            evaluateAsList(dependencyExpression, xmlFile.getNamespaceUnawareDocument()).forEach(dependency -> {

                final String groupId = getChildElementText("groupId", dependency);
                final String artifactId = getChildElementText("artifactId", dependency);

                final String listKey = groupId + ":" + artifactId;

                if (!allowedDependencies.contains(listKey)) {
                    reportIssue(dependency, "Remove this forbidden dependency.");
                }
            });
        }
    }

    /**
     * Get the text node content from a child element of a parent.
     *
     * @param childElementName the name of the element to find
     * @param parent           the node to look for children of
     */
    private static String getChildElementText(final String childElementName, final Node parent) {

        for (Node node : XmlFile.children(parent)) {
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(childElementName)) {
                return node.getTextContent();
            }
        }

        return "";
    }
}
