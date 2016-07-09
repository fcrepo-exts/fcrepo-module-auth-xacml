/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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
import java.util.Collections;
import java.util.Set;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeDesignator;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;


/**
 * Finds resource attributes via a configured set of SPARQL queries.
 * @author Gregory Jansen
 */
@Component
public class SparqlResourceAttributeFinderModule extends AttributeFinderModule {

    /**
     * Supported designator types.
     */
    private static final Set<Integer> DESIGNATOR_TYPES = Collections
            .unmodifiableSet(Collections
                    .singleton(AttributeDesignator.RESOURCE_TARGET));

    /*
     * (non-Javadoc)
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#
     * isDesignatorSupported()
     */
    @Override
    public final boolean isDesignatorSupported() {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#
     * isSelectorSupported()
     */
    @Override
    public final boolean isSelectorSupported() {
        return true;
    }

    /*
     * Supports resource designator types.
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#
     * getSupportedDesignatorTypes()
     */
    @Override
    public final Set<Integer> getSupportedDesignatorTypes() {
        return SparqlResourceAttributeFinderModule.DESIGNATOR_TYPES;
    }

    /*
     * The list of attribute IDs which are mapped to SPARQL queries.
     * @see org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#
     * getSupportedIds()
     */
    @Override
    public final Set<URI> getSupportedIds() {
        // TODO Auto-generated method stub
        return super.getSupportedIds();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#findAttribute
     * (java.net.URI, java.net.URI, java.net.URI, java.net.URI,
     * org.jboss.security.xacml.sunxacml.EvaluationCtx, int)
     */
    @Override
    public final EvaluationResult findAttribute(final URI attributeType,
            final URI attributeId, final URI issuer, final URI subjectCategory,
            final EvaluationCtx context, final int designatorType) {
        // TODO Auto-generated method stub
        return super.findAttribute(attributeType, attributeId, issuer,
                subjectCategory, context, designatorType);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule#findAttribute
     * (java.lang.String, org.w3c.dom.Node, java.net.URI,
     * org.jboss.security.xacml.sunxacml.EvaluationCtx, java.lang.String)
     */
    @Override
    public final EvaluationResult findAttribute(final String contextPath,
            final Node namespaceNode, final URI attributeType,
            final EvaluationCtx context, final String xpathVersion) {
        // TODO Auto-generated method stub
        return super.findAttribute(contextPath, namespaceNode, attributeType,
                context, xpathVersion);
    }

}
