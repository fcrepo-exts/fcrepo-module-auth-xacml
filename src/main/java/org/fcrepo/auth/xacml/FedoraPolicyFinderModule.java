/*
 * Copyright 2015 DuraSpace, Inc.
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

import static org.fcrepo.auth.xacml.URIConstants.POLICY_URI_PREFIX;
import static org.fcrepo.auth.xacml.URIConstants.XACML_POLICY_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.NodeService;
import org.jboss.security.xacml.sunxacml.AbstractPolicy;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.MatchResult;
import org.jboss.security.xacml.sunxacml.Policy;
import org.jboss.security.xacml.sunxacml.PolicyMetaData;
import org.jboss.security.xacml.sunxacml.PolicySet;
import org.jboss.security.xacml.sunxacml.VersionConstraints;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinder;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinderResult;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Locates a policy in ModeShape by evaluation context or by URI.
 *
 * @author Gregory Jansen
 * @author bbpennel
 */
@Component("fedoraPolicyFinderModule")
public class FedoraPolicyFinderModule extends PolicyFinderModule {

    private static final Logger LOGGER = getLogger(FedoraPolicyFinderModule.class);

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private BinaryService binaryService;

    @Autowired
    private NodeService nodeService;

    private PolicyFinder finder;

    /*
     * This policy finder can find by request context.
     * @see org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule#
     * isRequestSupported()
     */
    @Override
    public final boolean isRequestSupported() {
        return true;
    }

    /*
     * This policy finder can find by reference (URI)
     * @see org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule#
     * isIdReferenceSupported()
     */
    @Override
    public final boolean isIdReferenceSupported() {
        return true;
    }

    /**
     * Retrieves the policy from the given policy node
     *
     * @param policyBinary
     * @return
     */
    private AbstractPolicy getPolicy(final FedoraBinary policyBinary) {
        return loadPolicy(policyBinary);
    }

    /**
     * Creates a new policy or policy set object from the given policy node
     *
     * @param policyBinary
     * @return
     */
    private AbstractPolicy loadPolicy(final FedoraBinary policyBinary) {
        String policyName = "unparsed";
        try {
            // create the factory
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            final DocumentBuilder db = factory.newDocumentBuilder();

            // Parse the policy content
            final Document doc = db.parse(policyBinary.getContent());

            // handle the policy, if it's a known type
            final Element root = doc.getDocumentElement();
            final String name = root.getTagName();

            policyName = PolicyUtil.getID(doc);
            if (name.equals("Policy")) {
                return Policy.getInstance(root);
            } else if (name.equals("PolicySet")) {
                return PolicySet.getInstance(root, finder);
            } else {
                // this isn't a root type that we know how to handle
                throw new Exception("Unknown root document type: " + name);
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to parse policy from {}", policyName, e);
        }

        // a default fall-through in the case of an error
        return null;
    }

    /*
     * Find a policy in ModeShape that is appropriate for the evaluation
     * context.
     * @see
     * org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule#findPolicy
     * (org.jboss.security.xacml.sunxacml.EvaluationCtx)
     */
    @Override
    public final PolicyFinderResult findPolicy(final EvaluationCtx context) {
        final EvaluationResult ridEvalRes = context.getResourceAttribute(
                URI.create("http://www.w3.org/2001/XMLSchema#string"), URIConstants.ATTRIBUTEID_RESOURCE_ID, null);
        final AttributeValue resourceIdAttValue = ridEvalRes.getAttributeValue();
        String path = resourceIdAttValue.getValue().toString();

        LOGGER.debug("Finding policy for resource: {}", path);

        if ("".equals(path.trim())) {
            path = "/";
        }

        try {
            final Session internalSession = sessionFactory.getInternalSession();

            // Walk up the hierarchy to find the first node with a policy assigned
            Node nodeWithPolicy = PolicyUtil.getFirstRealNode(path, internalSession);
            while (nodeWithPolicy != null && !nodeWithPolicy.hasProperty(XACML_POLICY_PROPERTY)) {
                nodeWithPolicy = nodeWithPolicy.getParent();
            }

            // This should never happen, as PolicyUtil.getFirstRealNode() at least returns the root node.
            if (null == nodeWithPolicy) {
                LOGGER.warn("No policy found for: {}!", path);
                return new PolicyFinderResult();
            }

            final Property prop = nodeWithPolicy.getProperty(XACML_POLICY_PROPERTY);

            final FedoraBinary policyBinary;
            final FedoraResource resource = nodeService.find(internalSession, prop.getNode().getPath());
            if (resource.hasType(FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                policyBinary = binaryService.findOrCreate(internalSession, resource.getNode().getPath());

            } else {
                LOGGER.warn("Policy Binary not found for: {}", path);
                return new PolicyFinderResult();
            }

            if (policyBinary == null) {
                LOGGER.warn("Policy binary for path: {} was null!", nodeWithPolicy.getPath());
                return new PolicyFinderResult();
            }

            final AbstractPolicy policy = getPolicy(policyBinary);

            // Evaluate if the policy targets match the current context
            final MatchResult match = policy.match(context);
            final int result = match.getResult();

            if (result == MatchResult.INDETERMINATE) {
                return new PolicyFinderResult(match.getStatus());
            }

            // Found a good policy, return it
            if (result == MatchResult.MATCH) {
                return new PolicyFinderResult(policy);
            }

            return new PolicyFinderResult();
        } catch (final RepositoryException e) {
            LOGGER.warn("Failed to retrieve a policy for {}", e, path);
            return new PolicyFinderResult();
        }
    }

    /*
     * Find a policy in ModeShape by reference URI.
     * @see
     * org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule#findPolicy
     * (java.net.URI, int, org.jboss.security.xacml.sunxacml.VersionConstraints,
     * org.jboss.security.xacml.sunxacml.PolicyMetaData)
     */
    @Override
    public final PolicyFinderResult findPolicy(final URI idReference,
                                               final int type,
                                               final VersionConstraints constraints,
                                               final PolicyMetaData parentMetaData) {
        try {
            final String id = idReference.toString();
            if (!id.startsWith(POLICY_URI_PREFIX)) {
                LOGGER.warn("Policy reference must begin with {}, but was {}", POLICY_URI_PREFIX, id);
                return new PolicyFinderResult();
            }

            final String path = PolicyUtil.getPathForId(id);
            final Session internalSession = sessionFactory.getInternalSession();

            final FedoraBinary policyBinary;
            final FedoraResource resource = nodeService.find(internalSession, path);
            if (resource.hasType(FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                policyBinary = binaryService.findOrCreate(internalSession, resource.getNode().getPath());

            } else {
                LOGGER.warn("Policy Binary not found for: {}", path);
                return new PolicyFinderResult();
            }

            final AbstractPolicy policy = getPolicy(policyBinary);

            return new PolicyFinderResult(policy);

        } catch (final RepositoryRuntimeException | RepositoryException e) {
            LOGGER.warn("Failed to retrieve a policy for " + idReference.toString(), e);
            return new PolicyFinderResult();
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule#init(org.
     * jboss.security.xacml.sunxacml.finder.PolicyFinder)
     */
    @Override
    public void init(final PolicyFinder finder) {
        this.finder = finder;
    }

}
