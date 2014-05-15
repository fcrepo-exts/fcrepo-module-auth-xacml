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

import static org.jboss.security.xacml.sunxacml.attr.AttributeDesignator.RESOURCE_TARGET;
import static org.jboss.security.xacml.sunxacml.attr.AttributeDesignator.SUBJECT_TARGET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.NodeService;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.BagAttribute;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.ctx.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;

/**
 * @author Andrew Woods
 * @author Scott Prater
 */
public class TripleAttributeFinderModuleTest {

    private TripleAttributeFinderModule finder;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private Session mockSession;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private FedoraResource mockFedoraResource;

    @Mock
    private IdentifierTranslator mockIdentifierTranslator;

    @Mock
    private Dataset mockDataset;

    @Mock
    private DatasetGraph mockDatasetGraph;

    @Mock
    private Iterator<Quad> mockQuads;

    @Mock
    private Quad mockQuad;

    @Mock
    private Node mockNode;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        finder = new TripleAttributeFinderModule();
        finder.sessionFactory = mockSessionFactory;
        finder.nodeService = mockNodeService;

        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testIsDesignatorSupported() throws Exception {
        assertTrue("Designator should be supported!", finder.isDesignatorSupported());
    }

    @Test
    public void testIsSelectorSupported() throws Exception {
        assertFalse("Selector should not be supported!", finder.isSelectorSupported());
    }

    @Test
    public void testGetSupportedDesignatorTypes() throws Exception {
        final Set<Integer> designatorTypes = finder.getSupportedDesignatorTypes();
        assertNotNull("Designator Types should not be null!", designatorTypes);

        assertEquals("Should be 1 designator!", 1, designatorTypes.size());
        assertTrue("Designator type should be: " + RESOURCE_TARGET, designatorTypes.contains(RESOURCE_TARGET));
    }

    @Test
    public void testGetSupportedIds() throws Exception {
        assertNull("All IDs supported, should be null!", finder.getSupportedIds());
    }

    @Test
    public void testFindAttributeSelector() throws Exception {
        final String contextPath = "contextPath";
        final org.w3c.dom.Node namespaceNode = null;
        final URI attributeType = URI.create("uri:att-type");
        final EvaluationCtx context = evaluationCtx("/path/to/resource");
        final String xpathVersion = "xpathVersion";

        final EvaluationResult result = finder.findAttribute(contextPath,
                namespaceNode,
                                                             attributeType,
                                                             context,
                                                             xpathVersion);
        assertIsEmptyResult(result);
    }

    @Test
    public void testFindAttributeWrongDesignator() throws Exception {
        assertIsEmptyResult(doFindAttribute(SUBJECT_TARGET, "/path/to/resource"));
    }

    @Test
    public void testFindAttribute() throws RepositoryException {

        final Quad quad = new Quad(mockNode, mockNode, mockNode, mockNode);

        final String resourceId = "/path/to/resource";

        when(mockNodeService.getObject(mockSession, resourceId)).thenReturn(mockFedoraResource);
        when(mockFedoraResource.getPropertiesDataset(any(IdentifierTranslator.class))).thenReturn(mockDataset);
        when(mockDataset.asDatasetGraph()).thenReturn(mockDatasetGraph);
        when(mockDatasetGraph.find(eq(Node.ANY),
                eq(Node.ANY),
                any(Node.class),
                eq(Node.ANY))).thenReturn(mockQuads);
        when(mockQuads.hasNext()).thenReturn(true).thenReturn(false);
        when(mockQuads.next()).thenReturn(quad);
        // when(mockQuad.getObject()).thenReturn(mockNode);
        when(mockNode.getURI()).thenReturn("SamIAm");

        final EvaluationResult result = doFindAttribute(resourceId);

        final AttributeValue attributeValue = result.getAttributeValue();
        assertNotNull("Evaluation.attributeValue shoud not be null!", attributeValue);
        assertTrue("Evaluation.attributeValue should be a bag!", attributeValue.isBag());

        final URI value = (URI) attributeValue.getValue();
        assertNotNull("EvaluationResult value should not be null!", value);
        assertEquals(value.toString(), "SamIAm");
    }

    @Test
    public void testFindAttributeBySelector() {
        final URI attributeType = URI.create("uri:att-type");
        final EvaluationCtx context = evaluationCtx("/path/to/resource");
        final EvaluationResult result = finder.findAttribute("/", null, attributeType, context, "2.0");
        final BagAttribute bag = (BagAttribute) result.getAttributeValue();
        assertTrue("EvaluationResult should be a bag!", bag.isBag());
        assertTrue("Attribute bag should be empty!", bag.isEmpty());
    }

    @Test
    public void testFindAttributeInvalidSession() throws RepositoryException {
        when(mockSessionFactory.getInternalSession()).thenThrow(new RepositoryException());
        final EvaluationResult result = doFindAttribute("/path/to/resource");
        final String status = (String) result.getStatus().getCode().get(0);
        assertEquals("Evaluation status should be STATUS_PROCESSING_ERROR!", status,
                Status.STATUS_PROCESSING_ERROR);
    }

    @Test
    public void testFindAttributeNullResourceId() throws RepositoryException {
        final String resourceId = "{{";

        when(mockNodeService.getObject(mockSession, resourceId)).thenReturn(null);

        final EvaluationResult result = doFindAttribute(resourceId);
        final BagAttribute bag = (BagAttribute) result.getAttributeValue();
        assertTrue("EvaluationResult should be a bag!", bag.isBag());
        assertTrue("Attribute bag should be empty!", bag.isEmpty());
    }

    @Test
    public void testFindAttributeNewResourceId() throws RepositoryException {
        final String resourceId = "/no/such/path";

        when(mockNodeService.getObject(mockSession, resourceId)).thenReturn(mockFedoraResource);
        when(mockFedoraResource.getPath()).thenThrow(new PathNotFoundException());

        final EvaluationResult result = doFindAttribute(resourceId);
        final BagAttribute bag = (BagAttribute) result.getAttributeValue();
        assertTrue("EvaluationResult should be a bag!", bag.isBag());
        assertTrue("Attribute bag should be empty!", bag.isEmpty());
    }

    @Test
    public void testFindAttributeBadProperties() throws RepositoryException {
        final String resourceId = "/no/such/path";

        when(mockNodeService.getObject(mockSession, resourceId)).thenReturn(mockFedoraResource);
        when(mockFedoraResource.getPropertiesDataset(any(IdentifierTranslator.class))).thenThrow(
                new RepositoryException());

        final EvaluationResult result = doFindAttribute(resourceId);
        final String status = (String) result.getStatus().getCode().get(0);
        assertEquals("Evaluation status should be STATUS_PROCESSING_ERROR!", status,
                Status.STATUS_PROCESSING_ERROR);
    }

    @Test
    public void testFindAttributeNoAttr() throws RepositoryException {
        final String resourceId = "/no/such/path";

        when(mockQuads.hasNext()).thenReturn(false);

        final EvaluationResult result = doFindAttribute(resourceId);
        final BagAttribute bag = (BagAttribute) result.getAttributeValue();
        assertTrue("EvaluationResult should be a bag!", bag.isBag());
        assertTrue("Attribute bag should be empty!", bag.isEmpty());
    }

    // Helper methods

    private void assertIsEmptyResult(final EvaluationResult result) {
        final BagAttribute attributeValue = (BagAttribute) result.getAttributeValue();
        assertNotNull("Evaluation.attributeValue shoud not be null!", attributeValue);
        assertTrue("Evaluation.attributeValue should be a bag!", attributeValue.isBag());

        assertTrue("Attribute bag should be empty!", attributeValue.isEmpty());
        final Object value = attributeValue.getValue();
        assertNull("EvaluationResult value should be null!", value);
    }

    private EvaluationResult doFindAttribute(final String resourceId) {
        return doFindAttribute(-1, resourceId);
    }

    private EvaluationResult doFindAttribute(final int argDesignatorType, final String resourceId) {
        final URI attributeType = URI.create("http://www.w3.org/2001/XMLSchema#anyURI");
        final URI attributeId = URI.create("uri:att-id");
        final URI issuer = null;
        final URI subjectCategory = null;
        final EvaluationCtx context = evaluationCtx(resourceId);
        final int designatorType = argDesignatorType == -1 ? RESOURCE_TARGET : argDesignatorType;

        final EvaluationResult result = finder.findAttribute(attributeType,
                                                             attributeId,
                                                             issuer,
                                                             subjectCategory,
                                                             context,
                                                             designatorType);

        assertNotNull("EvaluationResult should not be null!", result);
        return result;
    }

    private EvaluationCtx evaluationCtx(final String resourceId) {
        final FedoraEvaluationCtxBuilder builder = new FedoraEvaluationCtxBuilder();
        if (resourceId != null) {
            builder.addResourceID(resourceId);
        }
        builder.addSubject("user", null);

        return builder.build();
    }

}
