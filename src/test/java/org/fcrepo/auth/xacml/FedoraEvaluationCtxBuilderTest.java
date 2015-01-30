/**
 * Copyright 2015 DuraSpace, Inc.
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

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.jboss.security.xacml.interfaces.XMLSchemaConstants;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.jcr.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the behavior of the XACML eval context builder.
 *
 * @author Gregory Jansen
 */
public class FedoraEvaluationCtxBuilderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraEvaluationCtxBuilderTest.class);

    @Mock
    private Session session;

    /**
     * Setup the test case.
     */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test a builder of evaluation context.
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        // use builder to create context.
        final FedoraEvaluationCtxBuilder builder = new FedoraEvaluationCtxBuilder();
        final Set<String> roles = new HashSet<String>();
        roles.add("reader");
        builder.addSubject("testuser", roles);
        builder.addResourceID("/testobject/testdatastream/myproperty1");
        builder.addWorkspace("default");
        builder.addActions(new String[] {"read"});
        final EvaluationCtx ctx = builder.build();

        // TODO verify contents of the resulting context.
        final URI string = URI.create(XMLSchemaConstants.DATATYPE_STRING);
        final EvaluationResult evAction = ctx.getActionAttribute(string, URIConstants.ATTRIBUTEID_ACTION_ID, null);
        Assert.assertNull(evAction.getStatus());
        Assert.assertEquals("read", evAction.getAttributeValue().getValue());
    }
}
