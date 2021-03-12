package com.devwithimagination.sonar.alloweddependencies.plugin.maven.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPathExpression;

import com.devwithimagination.sonar.alloweddependencies.plugin.maven.rules.MavenRulesDefinition;

import org.sonar.api.batch.rule.ActiveRule;
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

    /**
     * The XPath Expression used to find maven dependency nodes in a pom file.
     */
    private final XPathExpression dependencyExpression = getXPathExpression("//dependencies/dependency");

    /**
     * List containing the "groupId:artifactId" pairs for dependencies which are
     * allowed.
     */
    private final List<String> allowedDependencies;

    /**
     * If a non-empty value is set for this, restrict to only dependencies with the given
     * scope.
     */
    private final List<String> restrictToScopes;

    /**
     * Create a new {@link AllowedMavenDependenciesCheck} based on an active rule.
     *
     * @param activeRuleDefinition the rule containing the parameter configuration.
     */
    public AllowedMavenDependenciesCheck(final ActiveRule activeRuleDefinition) {

        LOG.info("Creating AllowedMavenDependenciesCheck for {}", activeRuleDefinition.ruleKey());

        /* Configure the allowed dependency coordinates */
        final String deps = activeRuleDefinition.param(MavenRulesDefinition.DEPS_PARAM_KEY);

        if (deps != null) {
            /* Convert into a list based on lines */
            this.allowedDependencies = Arrays.asList(deps.split("\\r?\\n"));
        } else {
            this.allowedDependencies = Collections.emptyList();
        }

        LOG.info("Allowed dependencies: '{}'", this.allowedDependencies);

        /* Configure the check scope */
        final String checkScope = activeRuleDefinition.param(MavenRulesDefinition.SCOPES_PARAM_KEY);

        if (checkScope == null) {
            this.restrictToScopes = Collections.emptyList();
        } else {
            this.restrictToScopes = Arrays.asList(checkScope.split(","));
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

                final String groupId = getChildElementText("groupId", dependency, null);
                final String artifactId = getChildElementText("artifactId", dependency, null);
                final String scope = getChildElementText("scope", dependency, DEFAULT_MAVEN_SCOPE);

                final String listKey = groupId + ":" + artifactId;

                if ((restrictToScopes.isEmpty() || restrictToScopes.contains(scope))
                        && !allowedDependencies.contains(listKey)) {

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
