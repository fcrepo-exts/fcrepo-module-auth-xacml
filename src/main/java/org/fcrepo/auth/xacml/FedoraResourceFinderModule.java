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

import static org.fcrepo.kernel.utils.FedoraTypesUtils.isInternalNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.jboss.security.xacml.sunxacml.ctx.Status.STATUS_PROCESSING_ERROR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.fcrepo.http.commons.session.SessionFactory;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.finder.ResourceFinderModule;
import org.jboss.security.xacml.sunxacml.finder.ResourceFinderResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Locates resources that are subordinate to a Fedora resource.
 * @author Gregory Jansen
 * @author Esme Cowles
 */
@Component("fedoraResourceFinderModule")
public class FedoraResourceFinderModule extends ResourceFinderModule {

    /**
     * Fedora's ModeShape session factory.
     */
    @Autowired
    protected SessionFactory sessionFactory;

    /*
     * Does find child resources.
     * @see org.jboss.security.xacml.sunxacml.finder.ResourceFinderModule#
     * isChildSupported()
     */
    @Override
    public final boolean isChildSupported() {
        return true;
    }

    /*
     * Does find descendant resources.
     * @see org.jboss.security.xacml.sunxacml.finder.ResourceFinderModule#
     * isDescendantSupported()
     */
    @Override
    public final boolean isDescendantSupported() {
        return true;
    }

    /*
     * Finds ModeShape child resources based on parent ID and evaluation
     * context.
     * @see org.jboss.security.xacml.sunxacml.finder.ResourceFinderModule#
     * findChildResources(org.jboss.security.xacml.sunxacml.attr.AttributeValue,
     * org.jboss.security.xacml.sunxacml.EvaluationCtx)
     */
    @Override
    public final ResourceFinderResult findChildResources(
            final AttributeValue parentResourceId,
            final EvaluationCtx context) {
        return findChildren( parentResourceId, false );
    }

    /*
     * Finds ModeShape descendant resources based on parent ID and evaluation
     * context.
     * @see org.jboss.security.xacml.sunxacml.finder.ResourceFinderModule#
     * findDescendantResources
     * (org.jboss.security.xacml.sunxacml.attr.AttributeValue,
     * org.jboss.security.xacml.sunxacml.EvaluationCtx)
     */
    @Override
    public final ResourceFinderResult findDescendantResources(
            final AttributeValue parentResourceId,
            final EvaluationCtx context) {
        return findChildren( parentResourceId, true );
    }

    /**
     * Find the child resources (or all descendant resources) of a path.
     * @param parent Repository path to find children of.
     * @param recurse If true, find all descenant resources, not just direct children.
    **/
    private ResourceFinderResult findChildren( final AttributeValue parent, final boolean recurse ) {
        try {
            final Session session = sessionFactory.getInternalSession();
            final Node node = session.getNode( parent.getValue().toString() );
            final Set<String> children = new HashSet<String>();
            findChildren( node, children, recurse );
            return new ResourceFinderResult( children );
        } catch ( RepositoryException ex ) {
            final HashMap errors = new HashMap();
            errors.put( parent, STATUS_PROCESSING_ERROR );
            return new ResourceFinderResult( errors );
        }
    }

    /**
     * Find children of a node.
     * @param node Repository node to find children of
     * @param children Set to add child paths to
     * @param If true, find all descendant paths, not just direct child paths
    **/
    private void findChildren( final Node node, final Set<String> children, final boolean recurse )
        throws RepositoryException {
        for ( final NodeIterator nodes = node.getNodes(); nodes.hasNext(); ) {
            final Node child = nodes.nextNode();
            if ( !isInternalNode.apply(child) && !child.getName().equals(JCR_CONTENT) ) {

                children.add( child.getPath() );

                if ( recurse ) {
                    findChildren( child, children, recurse );
                }
            }
        }
    }
}
