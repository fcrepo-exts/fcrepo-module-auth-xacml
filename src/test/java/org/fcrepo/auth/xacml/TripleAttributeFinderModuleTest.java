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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.services.NodeService;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.BagAttribute;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.ctx.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.w3c.dom.Node;

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
        final Node namespaceNode = null;
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
    @Ignore("Until implemented")
    public void testFindAttribute() {
        final EvaluationResult result = doFindAttribute("/path/to/resource");

        final AttributeValue attributeValue = result.getAttributeValue();
        assertNotNull("Evaluation.attributeValue shoud not be null!", attributeValue);
        assertTrue("Evaluation.attributeValue should be a bag!", attributeValue.isBag());

        final Object value = attributeValue.getValue();
        assertNotNull("EvaluationResult value should not be null!", value);
        // assertEquals((String)value, "SamIAm");
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

    // @Test
    // public void testFindAttributeNoResourceId() {
    // final EvaluationResult result = doFindAttribute(null);
    // final String status = (String) result.getStatus().getCode().get(0);
    // assertEquals("Evaluation status should be STATUS_PROCESSING_ERROR!", status, Status.STATUS_PROCESSING_ERROR);
    // }

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
        final URI attributeType = URI.create("uri:att-type");
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
