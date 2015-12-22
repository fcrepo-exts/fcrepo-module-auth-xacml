/*
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.api.Session;

/**
 * Test the behavior of the XACML eval context builder.
 *
 * @author Gregory Jansen
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraEvaluationCtxBuilderTest {

    @Mock
    private Session session;

    /**
     * Test a builder of evaluation context.
     */
    @Test
    public void test() {
        // use builder to create context.
        final FedoraEvaluationCtxBuilder builder = new FedoraEvaluationCtxBuilder();
        final Set<String> roles = new HashSet<>();
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
