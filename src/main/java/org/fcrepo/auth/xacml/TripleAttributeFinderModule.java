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

import java.net.URI;
import java.util.Set;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.services.NodeService;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.ctx.Status;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static org.jboss.security.xacml.sunxacml.attr.AttributeDesignator.RESOURCE_TARGET;
import static org.jboss.security.xacml.sunxacml.attr.BagAttribute.createEmptyBag;
import static org.jboss.security.xacml.sunxacml.ctx.Status.STATUS_PROCESSING_ERROR;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Finds resource attributes based on triples in the Fedora graph. Retrieves
 * values where the attribute URI matches the triple predicate and the triple
 * object can be supplied as the requested data type.
 *
 * @author Gregory Jansen
 * @author Andrew Woods
 */
@Component("tripleAttributeFinderModule")
public class TripleAttributeFinderModule extends AttributeFinderModule {

    private static final Logger LOGGER = getLogger(TripleAttributeFinderModule.class);


    /**
     * Fedora's ModeShape session factory.
     */
    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected NodeService nodeService;


    /**
     * Supported designator types.
     */
    private static final Set<Integer> DESIGNATOR_TYPES = unmodifiableSet(singleton(RESOURCE_TARGET));

    /**
     * Supports designators.
     *
     * @return if designator is supported.
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#
     * isDesignatorSupported()
     */
    @Override
    public final boolean isDesignatorSupported() {
        return true;
    }

    /**
     * Supports resource attributes.
     *
     * @return the supported designator types.
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#getSupportedDesignatorTypes()
     */
    @Override
    public final Set<Integer> getSupportedDesignatorTypes() {
        return DESIGNATOR_TYPES;
    }

    /**
     * Finds the matching triples values.
     *
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#findAttribute
     * (java.net.URI, java.net.URI, java.net.URI, java.net.URI,
     * org.jboss.security.xacml.sunxacml.EvaluationCtx, int)
     */
    @Override
    public final EvaluationResult findAttribute(final URI attributeType,
                                                final URI attributeId,
                                                final URI issuer,
                                                final URI subjectCategory,
                                                final EvaluationCtx context,
                                                final int designatorType) {
        LOGGER.debug("findAttribute({}, {}, {}, {}, {}, {})",
                     attributeType, attributeId, issuer, subjectCategory, context, designatorType);


        // Make sure this is a Resource attribute
        if (designatorType != RESOURCE_TARGET) {
            return new EvaluationResult(createEmptyBag(attributeType));
        }

        final Session session;
        try {
            session = sessionFactory.getInternalSession();

        } catch (RepositoryException e) {
            LOGGER.debug("Error getting session!");
            final Status status = new Status(singletonList(STATUS_PROCESSING_ERROR), "Error getting session");
            return new EvaluationResult(status);
        }

        // The resourceId is the path of the object be acted on
        final AttributeValue resourceIdAttValue = context.getResourceId();
        if (null == resourceIdAttValue) {
            LOGGER.debug("Context should have a resource-id attribute!");
            final Status status = new Status(singletonList(STATUS_PROCESSING_ERROR), "Resource Id not found!");
            return new EvaluationResult(status);
        }

        final String resourceId = (String) resourceIdAttValue.getValue();

        // Get the resource be acted on
        final FedoraResource resource;
        try {
            resource = nodeService.getObject(session, resourceId);

        } catch (RepositoryException e) {
            // If the object does not exist, it may be due to the action being "create"
            return new EvaluationResult(createEmptyBag(attributeType));
        }

        // Get the properties of the resource
        // TODO: resource.getTriples()

        // TODO: The result should have goodies
        return new EvaluationResult(createEmptyBag(attributeType));
    }

}
