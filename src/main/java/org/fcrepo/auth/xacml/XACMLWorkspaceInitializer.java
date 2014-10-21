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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeTemplate;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.services.BinaryService;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sets up node types and default policies for the XACML Authorization Delegate.
 *
 * @author Gregory Jansen
 */
public class XACMLWorkspaceInitializer {

    /**
     * Class-level logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XACMLWorkspaceInitializer.class);

    /**
     * Fedora's ModeShape session factory.
     */
    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private BinaryService binaryService;

    private File initialPoliciesDirectory;

    private File initialRootPolicyFile;

    /**
     * Constructor
     *
     * @param initialPoliciesDirectory of default policies
     * @param initialRootPolicyFile    defining root policy
     */
    public XACMLWorkspaceInitializer(final File initialPoliciesDirectory, final File initialRootPolicyFile) {
        if (null == initialPoliciesDirectory) {
            throw new IllegalArgumentException("InitialPolicyDirectory is null!");
        }
        if (null == initialPoliciesDirectory.list() || 0 == initialPoliciesDirectory.list().length) {
            throw new IllegalArgumentException("InitialPolicyDirectory does not exist or is empty! " +
                                                       initialPoliciesDirectory.getAbsolutePath());
        }
        if (null == initialRootPolicyFile || !initialRootPolicyFile.exists()) {
            throw new IllegalArgumentException("InitialRootPolicyFile is null or does not exist!");
        }

        this.initialPoliciesDirectory = initialPoliciesDirectory;
        this.initialRootPolicyFile = initialRootPolicyFile;
    }

    /**
     * Initializes default policies.
     */
    public void init() {
        doInit(false);
    }

    /**
     * Initializes node types and default policies - for Integration Tests!
     */
    public void initTest() {
        doInit(true);
    }

    private void doInit(final boolean test) {
        // Do not "registerNodeTypes" because the xacml-policy.cnd introduces a cyclical dependency with the main
        //  fedora-node-types.cnd that causes an exception on repository restart.
        if (test) {
            registerNodeTypes();
        }
        loadInitialPolicies();
        linkRootToPolicy();
    }

    private void registerNodeTypes() {
        Session session = null;
        try {
            session = sessionFactory.getInternalSession();
            final NodeTypeManager mgr = (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
            final URL cnd = XACMLWorkspaceInitializer.class.getResource("/cnd/xacml-policy.cnd");
            final NodeTypeIterator nti = mgr.registerNodeTypes(cnd, true);
            while (nti.hasNext()) {
                final NodeType nt = nti.nextNodeType();
                LOGGER.debug("registered node type: {}", nt.getName());
            }

            // Add "authz:xacmlAssignable" mixin to "fedora:resource" type
            final NodeType nodeType = mgr.getNodeType("fedora:resource");
            final NodeTypeTemplate nodeTypeTemplate = mgr.createNodeTypeTemplate(nodeType);
            final String[] superTypes = nodeType.getDeclaredSupertypeNames();
            final ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
            listBuilder.add(superTypes);
            listBuilder.add("authz:xacmlAssignable");
            final ImmutableList<String> newSuperTypes = listBuilder.build();
            nodeTypeTemplate.setDeclaredSuperTypeNames(newSuperTypes.toArray(new String[newSuperTypes.size()]));

            mgr.registerNodeType(nodeTypeTemplate, true);

            session.save();
            LOGGER.debug("Registered XACML policy node types");
        } catch (final RepositoryException | IOException e) {
            throw new Error("Cannot register XACML policy node types", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Create nodes for the default XACML policy set. Policies are created at paths according to their IDs.
     */
    private void loadInitialPolicies() {
        Session session = null;
        try {
            session = sessionFactory.getInternalSession();
            for (final File p : initialPoliciesDirectory.listFiles()) {
                final String id = PolicyUtil.getID(FileUtils.openInputStream(p));
                final String repoPath = PolicyUtil.getPathForId(id);
                final FedoraBinary binary = binaryService.findOrCreateBinary(session, repoPath);
                binary.setContent(new FileInputStream(p),
                                  "application/xml",
                                  null,
                                  p.getName(),
                                  null);

                LOGGER.info("Add initial policy {} at {}", p.getAbsolutePath(), binary.getPath());
            }
            session.save();
        } catch (final RepositoryException | InvalidChecksumException | IOException e) {
            throw new Error("Cannot create default root policies", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Set the policy that is effective at the root node.
     */
    private void linkRootToPolicy() {
        Session session = null;
        try {
            session = sessionFactory.getInternalSession();
            session.getRootNode().addMixin("authz:xacmlAssignable");
            final String id = PolicyUtil.getID(FileUtils.openInputStream(initialRootPolicyFile));
            final String repoPath = PolicyUtil.getPathForId(id);
            final Node globalPolicy = session.getNode(repoPath);
            session.getRootNode().setProperty("authz:policy", globalPolicy);
            session.save();
        } catch (final RepositoryException | IOException e) {
            throw new Error("Cannot configure root mix-in or policy", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
}
