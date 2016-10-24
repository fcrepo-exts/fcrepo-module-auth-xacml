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

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <p>
 * XACMLWorkspaceInitializerTest class.
 * </p>
 *
 * @author mohideen
 */
@RunWith(MockitoJUnitRunner.class)
public class XACMLWorkspaceInitializerTest {

    private XACMLWorkspaceInitializer xacmlWI;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private Session mockJcrSession;

    @Mock
    private FedoraSessionImpl mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    NonRdfSourceDescription mockNonRdfSourceDescription;

    @Mock
    FedoraBinary mockBinary;

    @Before
    public void setUp() throws Exception {
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockSession.getJcrSession()).thenReturn(mockJcrSession);
        when(mockJcrSession.getRootNode()).thenReturn(mockNode);
        when(mockBinaryService.findOrCreate(eq(mockSession), anyString())).thenReturn(mockBinary);
        when(mockNonRdfSourceDescription.getPath()).thenReturn("/dummy/test/path");

        final File initialPoliciesDirectory = policiesDirectory();
        final File initialRootPolicyFile = rootPolicyFile();
        xacmlWI = new XACMLWorkspaceInitializer(initialPoliciesDirectory, initialRootPolicyFile);

        setField(xacmlWI, "sessionFactory", mockSessionFactory);
        setField(xacmlWI, "binaryService", mockBinaryService);
    }

    private File policiesDirectory() {
        return new File(this.getClass().getResource("/xacml").getPath());
    }

    private File rootPolicyFile() {
        return new File(this.getClass().getResource("/xacml/testPolicy.xml").getPath());
    }

    @Test
    public void testConstructor() {
        assertNotNull(xacmlWI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorIllegalArg0() {
        xacmlWI = new XACMLWorkspaceInitializer(null, rootPolicyFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorIllegalArg1() {
        xacmlWI = new XACMLWorkspaceInitializer(policiesDirectory(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyDir() {
        final File emptyPoliciesDirectory = new File(this.getClass().getResource("/web.xml").getPath());
        xacmlWI = new XACMLWorkspaceInitializer(emptyPoliciesDirectory, rootPolicyFile());
    }

    @Test
    public void testInit() throws Exception {
        when(mockBinaryService.findOrCreate(eq(mockSession), anyString())).thenReturn(mockBinary);

        xacmlWI.init();

        final int expectedFiles = policiesDirectory().list().length;
        verify(mockBinaryService, times(expectedFiles)).findOrCreate(eq(mockSession), anyString());

        verify(mockNode).addMixin("authz:xacmlAssignable");
        verify(mockNode).setProperty(eq("authz:policy"), any(Node.class));
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testInitInitialPoliciesException() {
        when(mockSessionFactory.getInternalSession()).thenThrow(new RepositoryRuntimeException("expected"));

        xacmlWI.init();
    }

    @Test(expected = Error.class)
    public void testInitLinkRootToPolicyException() throws Exception {
        when(mockBinaryService.findOrCreate(eq(mockSession), anyString())).thenReturn(mockBinary);
        when(mockJcrSession.getRootNode()).thenThrow(new RepositoryException("expected"));

        xacmlWI.init();
    }

}
