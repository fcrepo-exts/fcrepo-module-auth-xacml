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

import static org.fcrepo.auth.common.FedoraAuthorizationDelegate.FEDORA_SERVLET_REQUEST;
import static org.fcrepo.auth.common.FedoraAuthorizationDelegate.FEDORA_USER_PRINCIPAL;
import static org.fcrepo.kernel.utils.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.PDP;
import org.jboss.security.xacml.sunxacml.ctx.ResponseCtx;
import org.jboss.security.xacml.sunxacml.ctx.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.Workspace;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Andrew Woods
 *         Date: 5/9/14
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PDPFactory.class})
public class XACMLAuthorizationDelegateTest {


    private XACMLAuthorizationDelegate xacmlAD;

    @Mock
    private FedoraPolicyFinderModule mockFedoraPFM;

    @Mock
    private FedoraResourceFinderModule mockFedoraRFM;

    private PDPFactory mockPdpFactory;

    @Mock
    private PDP mockPdp;

    @Mock
    private Session mockSession;

    @Mock
    private ResponseCtx mockResponseCtx;

    @Mock
    private Result mockResult;

    @Mock
    private Principal mockUser;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Mock
    private TripleAttributeFinderModule mockTripleAFM;

    @Mock
    private SparqlResourceAttributeFinderModule mockSparqlRAFM;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mockPdpFactory = PowerMockito.mock(PDPFactory.class);
        when(mockPdpFactory.makePDP()).thenReturn(mockPdp);
        when(mockPdp.evaluate(any(EvaluationCtx.class))).thenReturn(mockResponseCtx);
        when(mockResponseCtx.getResults()).thenReturn(getFakeResultSet());
        when(mockResult.getDecision()).thenReturn(mockResult.DECISION_PERMIT);
        when(mockSession.getAttribute(FEDORA_USER_PRINCIPAL)).thenReturn(mockUser);
        when(mockSession.getAttribute(FEDORA_SERVLET_REQUEST)).thenReturn(mockHttpServletRequest);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);

        xacmlAD = new XACMLAuthorizationDelegate();
        setField(xacmlAD, "pdpFactory", mockPdpFactory);
        setField(xacmlAD, "tripleResourceAttributeFinderModule", mockTripleAFM);
        setField(xacmlAD, "sparqlResourceAttributeFinderModule", mockSparqlRAFM);
    }

    private Set getFakeResultSet() {
        final Set fakeResults = new HashSet();
        fakeResults.add(mockResult);
        return fakeResults;
    }

    @Test(expected = Error.class)
    public void testInitPdpNull() throws Exception {
        when(mockPdpFactory.makePDP()).thenReturn(null);

        xacmlAD.init();
    }

    @Test
    public void testInit() throws Exception {
        xacmlAD.init();

        verify(mockPdpFactory).makePDP();
    }

    @Test
    public void testRolesHavePermission() throws Exception {
        xacmlAD.init();
        xacmlAD.rolesHavePermission(mockSession, "/fake/path", getFakeActions(), getFakeRoles());

        verify(mockPdp).evaluate(any(EvaluationCtx.class));
        verify(mockSession).getAttribute(FEDORA_USER_PRINCIPAL);
        verify(mockSession).getAttribute(FEDORA_SERVLET_REQUEST);
        verify(mockSession).getWorkspace();
        verify(mockResponseCtx).getResults();
        verify(mockResult).getDecision();
    }

    @Test
    public void testRolesHavePermissionTrue() throws Exception {
        xacmlAD.init();
        assertTrue(xacmlAD.rolesHavePermission(mockSession, "/fake/path", getFakeActions(), getFakeRoles()));
    }

    @Test
    public void testRolesHavePermissionFalse() throws Exception {
        xacmlAD.init();
        when(mockResult.getDecision()).thenReturn(mockResult.DECISION_DENY);
        assertFalse(xacmlAD.rolesHavePermission(mockSession, "/fake/path", getFakeActions(), getFakeRoles()));
    }

    private String[] getFakeActions() {
        final String[] fakeActions =  new String[2];
        fakeActions[0] = "fakeAction1";
        fakeActions[1] = "fakeAction2";
        return fakeActions;
    }

    private Set getFakeRoles() {
        final Set<String> fakeRoles = new HashSet();
        fakeRoles.add("fakeRole1");
        fakeRoles.add("fakeRole2");
        return fakeRoles;
    }
}
