/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.auth.xacml;

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Gregory Jansen
 *
 */
public class PolicyUtil {

    private PolicyUtil() {
        //not called
    }

    /**
     * Extract a policy set or policy ID for the document.
     *
     * @param policyStream the policy input
     * @return an identifier
     */
    public static String getID(final InputStream policyStream) {
        try {
            // create the factory
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);

            DocumentBuilder db = null;
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            db = factory.newDocumentBuilder();

            // Parse the policy content
            final Document doc = db.parse(policyStream);

            final String result = getID(doc);
            if (result == null) {
                throw new Error("Cannot find policy ID");
            }
            return result;
        } catch (final Exception e) {
            throw new Error("Unable to parse policy", e);
        }
    }

    /**
     * Get the ID of the XACML policy document.
     *
     * @param doc the DOM
     * @return the ID
     */
    public static String getID(final Document doc) {
        // handle the policy, if it's a known type
        final Element root = doc.getDocumentElement();
        final String name = root.getTagName();
        if (name.equals("Policy")) {
            return root.getAttribute("PolicyId");
        } else if (name.equals("PolicySet")) {
            return root.getAttribute("PolicySetId");
        } else {
            return null;
        }
    }

    /**
     * Gets the repository path for a policy ID.
     *
     * @param id the policy id
     * @return the repository path
     */
    public static String getPathForId(final String id) {
        return id.substring(URIConstants.POLICY_URI_PREFIX.length());
    }

    /**
     * Find the nearest real Modeshape node for a given Modeshape path.
     *
     * @param modepath the path in ModeShape
     * @param session a session
     * @return a Node in session
     */
    public static Node getFirstRealNode(final String modepath, final Session session) {
        Node node = null;
        for (String path = modepath; path.indexOf("/{") >= 0; path = path.substring(0, path.lastIndexOf("/{"))) {
            try {
                node = session.getNode(path);
                break;
            } catch (final PathNotFoundException expected) {
            } catch (final RepositoryException e) {
                throw new Error("Cannot reach repository", e);
            }
        }
        if (node == null) {
            try {
                node = session.getRootNode();
            } catch (final RepositoryException e) {
                throw new Error("Cannot reach repository", e);
            }
        }
        return node;
    }

    /**
     * Get the action ids.
     *
     * @param context the evaluation context
     * @return a set of actions
     */
    public static Set<String> getActions(final EvaluationCtx context) {
        final Set<String> result = new HashSet<String>();
        final EvaluationResult eval =
                context.getActionAttribute(URI.create("http://www.w3.org/2001/XMLSchema#string"),
                        URIConstants.ATTRIBUTEID_ACTION_ID, null);
        if (eval == null) {
            return null;
        }
        if (eval.getStatus() == null) {
            final AttributeValue val = eval.getAttributeValue();
            if (val != null && val.getValue() != null) {
                result.add(val.getValue().toString());
            }
        }
        return result;
    }
}
