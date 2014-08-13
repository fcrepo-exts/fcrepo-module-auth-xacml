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

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.services.DatastreamService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

/**
 * <p>
 * XACMLWorkspaceInitializerTest class.
 * </p>
 * 
 * @author mohideen
 */
public class XACMLWorkspaceInitializerTest {

    private XACMLWorkspaceInitializer xacmlWI;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private DatastreamService mockDsService;

    @Mock
    Datastream mockDatastream;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockSession.getRootNode()).thenReturn(mockNode);
        when(
                mockDsService.createDatastream(eq(mockSession), anyString(), eq("application/xml"), anyString(),
                        Matchers.any(FileInputStream.class))).thenReturn(mockDatastream);
        when(mockDatastream.getPath()).thenReturn("/dummy/test/path");

        final File initialPoliciesDirectory = policiesDirectory();
        final File initialRootPolicyFile = rootPolicyFile();
        xacmlWI = new XACMLWorkspaceInitializer(initialPoliciesDirectory, initialRootPolicyFile);

        setField(xacmlWI, "sessionFactory", mockSessionFactory);
        setField(xacmlWI, "datastreamService", mockDsService);
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
        xacmlWI.init();

        final int expectedFiles = policiesDirectory().list().length;
        verify(mockDsService, times(expectedFiles)).createDatastream(eq(mockSession),
                                                                     anyString(),
                                                                     eq("application/xml"),
                                                                     anyString(),
                                                                     Matchers.any(FileInputStream.class));

        verify(mockNode).addMixin("authz:xacmlAssignable");
        verify(mockNode).setProperty(eq("authz:policy"), any(Node.class));
    }

    @Test(expected = Error.class)
    public void testInitInitialPoliciesException() throws Exception {
        when(mockSessionFactory.getInternalSession()).thenThrow(new RepositoryException("expected"));

        xacmlWI.init();
    }

    @Test(expected = Error.class)
    public void testInitLinkRootToPolicyException() throws Exception {
        when(mockSession.getRootNode()).thenThrow(new RepositoryException("expected"));

        xacmlWI.init();
    }

}
