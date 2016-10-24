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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;

import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.finder.ResourceFinderResult;


/**
 * @author Andrew Woods
 *         Date: 5/9/14
 * @author Esme Cowles
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraResourceFinderModuleTest {
    FedoraResourceFinderModule resourceFinder;

    @Mock SessionFactory mockSessionFactory;
    @Mock FedoraSessionImpl mockSession;
    @Mock Session mockJcrSession;
    @Mock AttributeValue mockParent;
    @Mock Node mockParentNode, mockChildNode, mockGrandchildNode;
    @Mock NodeType mockNodeType;
    @Mock NodeIterator mockParentIterator, mockChildIterator, mockGrandchildIterator;

    @Before
    public void setUp() throws RepositoryException {
        resourceFinder = new FedoraResourceFinderModule();
        resourceFinder.sessionFactory = mockSessionFactory;

        when( mockSessionFactory.getInternalSession() ).thenReturn(mockSession);
        when( mockSession.getJcrSession() ).thenReturn(mockJcrSession);
        when( mockParent.getValue() ).thenReturn("/foo");
        when( mockJcrSession.getNode("/foo") ).thenReturn(mockParentNode);

        when( mockParentNode.getNodes() ).thenReturn(mockParentIterator);
        when( mockParentIterator.hasNext() ).thenReturn(true,false);
        when( mockParentIterator.nextNode() ).thenReturn(mockChildNode);

        when( mockChildNode.getPath() ).thenReturn("/foo/bar");
        when( mockChildNode.getName() ).thenReturn("bar");
        when( mockChildNode.getPrimaryNodeType() ).thenReturn(mockNodeType);
        when( mockNodeType.isNodeType(anyString()) ).thenReturn(false);
        when( mockChildNode.getNodes() ).thenReturn(mockChildIterator);
        when( mockChildIterator.hasNext() ).thenReturn(true,false);
        when( mockChildIterator.nextNode() ).thenReturn(mockGrandchildNode);

        when( mockGrandchildNode.getPath() ).thenReturn("/foo/bar/baz");
        when( mockGrandchildNode.getName() ).thenReturn("baz");
        when( mockGrandchildNode.getPrimaryNodeType() ).thenReturn(mockNodeType);
        when( mockGrandchildNode.getNodes() ).thenReturn(mockGrandchildIterator);
        when( mockGrandchildIterator.hasNext() ).thenReturn(false);
        when( mockGrandchildIterator.nextNode() ).thenReturn(null);

    }

    @Test
    public void testIsChildSupported() {
        assertTrue( resourceFinder.isChildSupported() );
    }

    @Test
    public void testIsDescendantSupported() {
        assertTrue( resourceFinder.isDescendantSupported() );
    }

    @Test
    public void testFindChildResources() {
        final ResourceFinderResult result = resourceFinder.findChildResources( mockParent, null );
        final Set<?> resources = result.getResources();
        assertTrue( "Child not found", resources.contains("/foo/bar") );
        assertFalse( "Grandchild should not be found", resources.contains("/foo/bar/baz") );
    }

    @Test
    public void testFindDescendantResources() {
        final ResourceFinderResult result = resourceFinder.findDescendantResources( mockParent, null );
        final Set<?> resources = result.getResources();
        assertTrue( "Child not found", resources.contains("/foo/bar") );
        assertTrue( "Grandchild not found", resources.contains("/foo/bar/baz") );
    }
}
