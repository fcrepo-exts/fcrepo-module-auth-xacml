package org.fcrepo.auth.xacml;

import static org.fcrepo.kernel.utils.TestHelpers.setField;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;

import javax.jcr.Node;
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

        final File initialPoliciesDirectory = new File(this.getClass().getResource("/xacml").getPath());
        final File initialRootPolicyFile = new File(this.getClass().getResource("/xacml/testPolicy.xml").getPath());
        xacmlWI = new XACMLWorkspaceInitializer(initialPoliciesDirectory, initialRootPolicyFile);

        setField(xacmlWI, "sessionFactory", mockSessionFactory);
        setField(xacmlWI, "datastreamService", mockDsService);
    }

    @Test
    public void testConstructor() {
        assertNotNull(xacmlWI);
    }

    @Test
    public void testInit() {
        xacmlWI.init();
    }
}
