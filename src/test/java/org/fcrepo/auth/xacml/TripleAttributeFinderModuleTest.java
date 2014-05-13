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

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.services.NodeService;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.Set;

import static org.jboss.security.xacml.sunxacml.attr.AttributeDesignator.RESOURCE_TARGET;
import static org.jboss.security.xacml.sunxacml.attr.AttributeDesignator.SUBJECT_TARGET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Andrew Woods
 *         Date: 5/9/14
 */
public class TripleAttributeFinderModuleTest {

    private TripleAttributeFinderModule finder;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private NodeService nodeService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        finder = new TripleAttributeFinderModule();
        finder.sessionFactory = sessionFactory;
        finder.nodeService = nodeService;
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
        final EvaluationCtx context = evaluationCtx();
        final String xpathVersion = "xpathVersion";

        final EvaluationResult result = finder.findAttribute(contextPath,
                                                             namespaceNode,
                                                             attributeType,
                                                             context,
                                                             xpathVersion);
        assertIsEmptyResult(result);
    }

    private void assertIsEmptyResult(EvaluationResult result) {
        final AttributeValue attributeValue = result.getAttributeValue();
        assertNotNull("Evaluation.attributeValue shoud not be null!", attributeValue);
        assertTrue("Evaluation.attributeValue should be a bag!", attributeValue.isBag());

        final Object value = attributeValue.getValue();
        assertNull("EvaluationResult value should be null!", value);
    }

    @Test
    public void testFindAttributeWrongDesignator() throws Exception {
        assertIsEmptyResult(doFindAttribute(SUBJECT_TARGET));
    }

    @Test
    @Ignore("Until implemented")
    public void testFindAttribute() {
        EvaluationResult result = doFindAttribute();

        final AttributeValue attributeValue = result.getAttributeValue();
        assertNotNull("Evaluation.attributeValue shoud not be null!", attributeValue);
        assertTrue("Evaluation.attributeValue should be a bag!", attributeValue.isBag());

        final Object value = attributeValue.getValue();
        assertNotNull("EvaluationResult value should not be null!", value);
    }

    private EvaluationResult doFindAttribute() {
        return doFindAttribute(-1);
    }

    private EvaluationResult doFindAttribute(final int argDesignatorType) {
        final URI attributeType = URI.create("uri:att-type");
        final URI attributeId = URI.create("uri:att-id");
        final URI issuer = null;
        final URI subjectCategory = null;
        final EvaluationCtx context = evaluationCtx();
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

    private EvaluationCtx evaluationCtx() {
        final FedoraEvaluationCtxBuilder builder = new FedoraEvaluationCtxBuilder();
        builder.addResourceID("/path/to/resource");
        builder.addSubject("user", null);

        return builder.build();
    }

}
