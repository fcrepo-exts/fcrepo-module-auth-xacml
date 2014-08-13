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

import static org.fcrepo.auth.xacml.URIConstants.POLICY_URI_PREFIX;
import static org.fcrepo.auth.xacml.URIConstants.XACML_POLICY_PROPERTY;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.services.DatastreamService;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.PolicyReference;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.combine.PolicyCombinerElement;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinder;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinderResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author Andrew Woods
 *         Date: 5/9/14
 */
public class FedoraPolicyFinderModuleTest {

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockParentNode;

    @Mock
    private Property mockPolicyProperty;

    @Mock
    private Node mockPolicyNode;

    @Mock
    private Datastream mockPolicyDs;

    @Mock
    private DatastreamService mockDsService;

    @Mock
    private PolicyFinder mockFinder;

    @Mock
    private EvaluationCtx context;

    @Mock
    private AttributeValue mockResourceId;

    @Mock
    private AttributeFinderModule mockAttributeFinder;

    private FedoraPolicyFinderModule finderModule;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(mockResourceId.getValue()).thenReturn("path");
        when(context.getResourceId()).thenReturn(mockResourceId);

        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockSession.getNode(anyString())).thenReturn(mockNode);

        when(mockNode.getParent()).thenReturn(mockParentNode);

        when(mockPolicyProperty.getNode()).thenReturn(mockPolicyNode);

        when(mockDsService.asDatastream(mockPolicyNode)).thenReturn(mockPolicyDs);

        finderModule = new FedoraPolicyFinderModule();
        setField(finderModule, "sessionFactory", mockSessionFactory);
        setField(finderModule, "datastreamService", mockDsService);
        finderModule.init(mockFinder);
    }

    @Test
    public void testIsRequestSupported() throws Exception {
        assertTrue(finderModule.isRequestSupported());
    }

    @Test
    public void testIsIdReferenceSupported() throws Exception {
        assertTrue(finderModule.isIdReferenceSupported());
    }

    @Test
    public void testFindPolicyOnTargetNode() throws Exception {

        when(mockNode.hasProperty(eq(XACML_POLICY_PROPERTY))).thenReturn(true);
        when(mockNode.getProperty(eq(XACML_POLICY_PROPERTY))).thenReturn(mockPolicyProperty);

        when(mockPolicyDs.getContent()).thenReturn(this.getClass().getResourceAsStream("/xacml/testPolicy.xml"));

        final FedoraEvaluationCtxBuilder ctxBuilder = new FedoraEvaluationCtxBuilder();
        ctxBuilder.addResourceID("/{}myPath");
        final Set<String> subjectSet = new HashSet<>();
        ctxBuilder.addSubject("test", subjectSet);

        final EvaluationCtx ctx = ctxBuilder.build();

        final PolicyFinderResult result = finderModule.findPolicy(ctx);

        assertFalse(result.notApplicable());
        assertFalse(result.indeterminate());
        assertNotNull(result.getPolicy());
    }

    @Test
    public void testFindPolicyByIdReference() throws Exception {
        final String policyPath = "/path/to/policy";
        final String idPath = POLICY_URI_PREFIX + policyPath;
        final URI idReference = new URI(idPath);

        when(mockPolicyDs.getContent()).thenReturn(this.getClass().getResourceAsStream("/xacml/testPolicy.xml"));
        when(mockDsService.getDatastream(any(Session.class), eq(policyPath))).thenReturn(mockPolicyDs);

        final PolicyFinderResult result = finderModule.findPolicy(idReference, 0, null, null);

        assertFalse(result.notApplicable());
        assertFalse(result.indeterminate());
        assertNotNull(result.getPolicy());
    }

    @Test
    public void testFindPolicySet() throws Exception {

        when(mockNode.hasProperty(eq(XACML_POLICY_PROPERTY))).thenReturn(true);
        when(mockNode.getProperty(eq(XACML_POLICY_PROPERTY))).thenReturn(mockPolicyProperty);

        when(mockPolicyDs.getContent())
        .thenReturn(this.getClass().getResourceAsStream("/xacml/adminRolePolicySet.xml"));

        final String referencedId = "fcrepo:policies/AdminPermissionPolicySet";
        final Datastream referencedPolicyDs = mock(Datastream.class);
        when(referencedPolicyDs.getContent()).thenReturn(
                this.getClass().getResourceAsStream("/xacml/adminPermissionPolicySet.xml"));
        when(mockDsService.getDatastream(any(Session.class), eq(referencedId))).thenReturn(referencedPolicyDs);

        final FedoraEvaluationCtxBuilder ctxBuilder = new FedoraEvaluationCtxBuilder();
        ctxBuilder.addResourceID("/{}myPath");
        final Set<String> subjectSet = new HashSet<>();
        subjectSet.add("admin");
        ctxBuilder.addSubject("username", subjectSet);

        final EvaluationCtx ctx = ctxBuilder.build();

        final PolicyFinderResult result = finderModule.findPolicy(ctx);

        assertFalse(result.notApplicable());
        assertFalse(result.indeterminate());
        assertNotNull(result.getPolicy());

        final List<?> policyChildren = result.getPolicy().getChildElements();
        assertEquals("Combiner algorithm child not found", 1, policyChildren.size());
        final PolicyCombinerElement combiner = (PolicyCombinerElement) policyChildren.get(0);
        final PolicyReference policyRef = (PolicyReference) combiner.getElement();
        assertEquals("URI for policy reference was not the expected value", "fcrepo:policies/AdminPermissionPolicySet",
                policyRef.getReference().toString());
    }
}

