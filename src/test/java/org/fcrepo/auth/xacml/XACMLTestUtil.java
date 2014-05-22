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

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.BasicHttpEntity;
import org.fcrepo.auth.roles.common.integration.RolesFadTestObjectBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some XACML loading and linking utilities for tests.
 *
 * @author Gregory Jansen
 */
public class XACMLTestUtil {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XACMLTestUtil.class);

    private XACMLTestUtil () {
        // not called
    }

    public static void addPolicy(final RolesFadTestObjectBean policies, final File path) throws Exception {
        final String xacml = FileUtils.readFileToString(path);
        policies.addDatastream(FilenameUtils.getBaseName(path.getName()), xacml);
    }

    /**
     * Removes the policy link from an object.
     *
     * @param client
     * @param serverAddress
     * @param objectPath
     * @throws Exception
     */
    public static void unlinkPolicies(final HttpClient client, final String serverAddress, final String objectPath)
            throws Exception {
        final String subjectURI = serverAddress + objectPath;
        final HttpPatch patch = new HttpPatch(subjectURI);
        // setAuth(patch, "fedoraAdmin");
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("DELETE { <" + subjectURI + "> <http://fedora.info/definitions/v4/authorization#policy> ?o } " +
                        "WHERE { <" + subjectURI + "> <http://fedora.info/definitions/v4/authorization#policy> ?o }")
                        .getBytes()));
        patch.setEntity(e);
        LOGGER.debug("PATCH: {}", patch.getURI());
        final HttpResponse response = client.execute(patch);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    /**
     * Links an object to a policy(set).
     *
     * @param client
     * @param serverAddress
     * @param objectPath
     * @param policyPath
     * @throws Exception
     */
    public static void linkPolicy(final HttpClient client, final String serverAddress, final String objectPath,
            final String policyPath) throws Exception {
        final String subjectURI = serverAddress + objectPath;
        final String policyURI = policyPath;
        final HttpPatch patch = new HttpPatch(subjectURI);
        // setAuth(patch, "fedoraAdmin");
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        final StringBuilder sb = new StringBuilder();
        sb.append("INSERT { ")
                .append(MessageFormat
                        .format("<{0}> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " +
                                "<http://fedora.info/definitions/v4/authorization#xacmlAssignable> . ",
                                subjectURI)).append(
                        MessageFormat.format("<{0}> <http://fedora.info/definitions/v4/authorization#policy> <{1}> . ",
                                subjectURI, policyURI)).append("} WHERE { }");
        e.setContent(new ByteArrayInputStream(sb.toString().getBytes()));
        patch.setEntity(e);
        LOGGER.debug("PATCH: {}", patch.getURI());
        final HttpResponse response = client.execute(patch);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }
}
