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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Gregory Jansen
 *
 */
public class PolicyUtilTest {

    @Test
    public void test() throws Exception {
        final String id =
                PolicyUtil.getID(FileUtils.openInputStream(
                new File("src/main/resources/policies/GlobalRolesPolicySet.xml")));
        Assert.assertEquals("info:fedora/policies/GlobalRolesPolicySet", id);

        final String path = PolicyUtil.getPathForId(id);
        Assert.assertEquals("/policies/GlobalRolesPolicySet", path);
    }

}
