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

import static org.fcrepo.auth.xacml.URIConstants.ATTRIBUTEID_ACTION_ID;
import static org.fcrepo.auth.xacml.URIConstants.ATTRIBUTEID_ENVIRONMENT_ORIGINAL_IP_ADDRESS;
import static org.fcrepo.auth.xacml.URIConstants.ATTRIBUTEID_RESOURCE_ID;
import static org.fcrepo.auth.xacml.URIConstants.ATTRIBUTEID_RESOURCE_WORKSPACE;
import static org.fcrepo.auth.xacml.URIConstants.ATTRIBUTEID_SUBJECT_ID;
import static org.fcrepo.auth.xacml.URIConstants.FCREPO_SUBJECT_GROUP;
import static org.fcrepo.auth.xacml.URIConstants.FCREPO_SUBJECT_ROLE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.security.xacml.sunxacml.BasicEvaluationCtx;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.ctx.Attribute;
import org.jboss.security.xacml.sunxacml.ctx.RequestCtx;
import org.jboss.security.xacml.sunxacml.ctx.Subject;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinder;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Gregory Jansen
 *
 */
public class FedoraEvaluationCtxBuilder {

    /**
     * Class-level logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraEvaluationCtxBuilder.class);

    /**
     * The list of other subjects.
     */
    private final List<Subject> subjectList = new ArrayList<Subject>();

    /**
     * This list of resource attributes.
     */
    private final List<Attribute> resourceList = new ArrayList<Attribute>();

    /**
     * The ID of the resource.
     */
    private AttributeValue resourceId;

    /**
     * This list of action attributes.
     */
    private final List<Attribute> actionList = new ArrayList<Attribute>();

    /**
     * This list of environment attributes.
     */
    private final List<Attribute> environmentList = new ArrayList<Attribute>();

    /**
     * The list of attribute finder modules.
     */
    private final List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();

    /**
     * Build the evaluation context.
     *
     * @return the evaluation context
     */
    public final EvaluationCtx build() {
        final RequestCtx rc = new RequestCtx(subjectList, resourceList, actionList, environmentList);
        if (LOGGER.isDebugEnabled()) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                rc.encode(baos);
                LOGGER.debug("RequestCtx dump:\n{}", baos.toString("utf-8"));
            } catch (final IOException e) {
                LOGGER.info("Cannot print request context", e);
            }
        }

        final AttributeFinder af = new AttributeFinder();
        af.setModules(attributeFinderModules);
        try {
            final BasicEvaluationCtx result = new BasicEvaluationCtx(rc, af);
            // result.setResourceId(resourceId);
            return result;
        } catch (final ParsingException e) {
            throw new Error(e);
        }
    }

    /**
     * Add a finder module to context.
     *
     * @param module module to add
     * @return the builder
     */
    public final FedoraEvaluationCtxBuilder addFinderModule(final AttributeFinderModule module) {
        this.attributeFinderModules.add(module);
        return this;
    }

    /**
     * Adds a basic Fedora subject to the context.
     *
     * @param username the user principal name, or null
     * @param roles the effective roles for user, or null
     * @return the builder
     */
    public final FedoraEvaluationCtxBuilder addSubject(final String username, final Set<String> roles) {
        final List<Attribute> subjectAttrs = new ArrayList<>();
        if (username != null) {
            final StringAttribute v = new StringAttribute(username);
            final Attribute sid = new Attribute(ATTRIBUTEID_SUBJECT_ID, null, null, v);
            subjectAttrs.add(sid);
        }

        if (roles != null) {
            for (final String role : roles) {
                final StringAttribute roleAttr = new StringAttribute(role);
                final Attribute roleId = new Attribute(FCREPO_SUBJECT_ROLE, null, null, roleAttr);
                subjectAttrs.add(roleId);
            }
        }

        this.subjectList.add(new Subject(subjectAttrs));
        return this;
    }

    /**
     * Add the node or property path as resource ID.
     *
     * @param rawModeShapePath the path to the node or property
     * @return the builder
     */
    public final FedoraEvaluationCtxBuilder addResourceID(final String rawModeShapePath) {
        final Attribute rid = new Attribute(ATTRIBUTEID_RESOURCE_ID, null, null, new StringAttribute(rawModeShapePath));
        resourceList.add(rid);
        return this;
    }

    /**
     * Add the workspace name.
     *
     * @param name name of workspace
     * @return the builder
     */
    public final FedoraEvaluationCtxBuilder addWorkspace(final String name) {
        final Attribute wid = new Attribute(ATTRIBUTEID_RESOURCE_WORKSPACE, null, null, new StringAttribute(name));
        resourceList.add(wid);
        return this;
    }

    /**
     * Adds actions as action ID and modify resource scope to handle remove.
     *
     * @param actions the requested actions
     * @return the builder
     */
    public final FedoraEvaluationCtxBuilder addActions(final String[] actions) {
        if (actions != null) {
            for (final String action : actions) {
                final Attribute a = new Attribute(ATTRIBUTEID_ACTION_ID, null, null, new StringAttribute(action));
                actionList.add(a);
                // if ("remove".equals(action)) {
                // final Attribute scope =
                // new Attribute(ATTRIBUTEID_RESOURCE_SCOPE, null, null,
                // new StringAttribute("Descendants"));
                // resourceList.add(scope);
                // }
            }
        }
        return this;
    }

    /**
     * @param remoteAddr the remote address
     */
    public void addOriginalRequestIP(final String remoteAddr) {
        final Attribute a = new Attribute(ATTRIBUTEID_ENVIRONMENT_ORIGINAL_IP_ADDRESS,
                null,
                null,
                new StringAttribute(remoteAddr));
        actionList.add(a);
    }

    /**
     * This method adds group attributes to the subject-set.
     *
     * @param user      for arg groups
     * @param allGroups to be added to the subject-set
     * @return this object
     */
    public FedoraEvaluationCtxBuilder addGroups(final Principal user, final Set<Principal> allGroups) {
        LOGGER.trace("For user, {}, adding groups {}", user.getName(), allGroups);

        if (null == user || null == allGroups || allGroups.isEmpty()) {
            LOGGER.trace("Not adding any groups!");
            return this;
        }

        final List<Attribute> subjectAttrs = new ArrayList<>();
        for (final Principal group : allGroups) {
            // Do not include the user principal in the group attributes.
            if (!group.equals(user)) {
                final StringAttribute groupAttr = new StringAttribute(group.getName());
                final Attribute groupId = new Attribute(FCREPO_SUBJECT_GROUP, null, null, groupAttr);
                subjectAttrs.add(groupId);
            }
        }
        this.subjectList.add(new Subject(subjectAttrs));
        return this;
    }

}
