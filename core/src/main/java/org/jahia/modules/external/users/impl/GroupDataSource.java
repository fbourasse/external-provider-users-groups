/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.external.users.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.users.GroupNotFoundException;
import org.jahia.modules.external.users.Member;
import org.jahia.modules.external.users.UserGroupProvider;
import org.jahia.modules.external.users.Member.MemberType;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.services.usermanager.JahiaUserSplittingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import java.util.*;

/**
 * Data source implementation for retrieving groups.
 */
public class GroupDataSource implements ExternalDataSource, ExternalDataSource.Searchable, ExternalDataSource.Referenceable {

    private static final Logger logger = LoggerFactory.getLogger(GroupDataSource.class);

    private static final String MEMBER_PATHS_SESSION_VAR = "memberPathsByGroupName";

    private static final String MEMBER_REF_ATTR = "j:member";
    
    private static final String MEMBERS_ROOT_NAME = "j:members";
    
    public static final HashSet<String> SUPPORTED_NODE_TYPES = new HashSet<String>(Arrays.asList("jnt:group", "jnt:members", "jnt:member"));

    private ExternalContentStoreProvider contentStoreProvider;

    private JahiaUserManagerService jahiaUserManagerService;

    private UserGroupProvider userGroupProvider;

    private UserDataSource userDataSource;

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        if (path == null || path.indexOf('/') == -1) {
            throw new PathNotFoundException(path);
        }
        if ("/".equals(path)) {
            return Collections.emptyList();
        }
        String[] pathSegments = StringUtils.split(path, '/');
        if (pathSegments.length == 1) {
            return Arrays.asList(MEMBERS_ROOT_NAME);
        }
        if (!MEMBERS_ROOT_NAME.equals(pathSegments[1])) {
            throw new PathNotFoundException(path);
        }
        String memberPathBase = StringUtils.substringAfter(path, "/" + MEMBERS_ROOT_NAME);
        JahiaUserSplittingRule userSplittingRule = jahiaUserManagerService.getUserSplittingRule();
        String userPath = StringUtils.substringAfter(memberPathBase, userDataSource.getContentStoreProvider().getMountPoint());
        String groupPath = StringUtils.substringAfter(memberPathBase, contentStoreProvider.getMountPoint());
        if (StringUtils.isNotBlank(userPath) && StringUtils.split(userPath, '/').length >= userSplittingRule.getNumberOfSegments() + 1) { // split folders + user name
            // path is for member user node or subnode
            return Collections.emptyList();
        }
        if (StringUtils.isNotBlank(groupPath)) {
            // path is for member group node or subnode
            return Collections.emptyList();
        }
        HashSet<String> children = new HashSet<String>();
        for (String memberPath : getMembers(pathSegments[0])) {
            if (memberPath.startsWith(memberPathBase)) {
                memberPath = StringUtils.removeStart(memberPath, memberPathBase + "/");
                memberPath = StringUtils.substringBefore(memberPath, "/");
                children.add(memberPath);
            }
        }
        List<String> l = new ArrayList<String>();
        l.addAll(children);
        return l;
    }

    public ExternalContentStoreProvider getContentStoreProvider() {
        return contentStoreProvider;
    }

    private ExternalData getGroupData(JahiaGroup group) {
        String path = "/" + group.getName();
        Map<String,String[]> properties = new HashMap<String, String[]>();
        Properties groupProperties = group.getProperties();
        for (Object key : groupProperties.keySet()) {
            properties.put((String) key, new String[]{(String) groupProperties.get(key)});
        }
        properties.put("j:external", new String[]{"true"});
        properties.put("j:externalSource", new String[]{StringUtils.removeEnd(contentStoreProvider.getKey(), ".groups")});
        return new ExternalData(path, path, "jnt:group", properties);
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        if (identifier.startsWith("/")) {
            try {
                return getItemByPath(identifier);
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(identifier, e);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        if (path == null || path.indexOf('/') == -1) {
            throw new PathNotFoundException(path);
        }
        if ("/".equals(path)) {
            return new ExternalData(path, path, "jnt:groupsFolder", new HashMap<String, String[]>());
        }
        String[] pathSegments = StringUtils.split(path, '/');
        if (pathSegments.length == 1) {
            try {
                return getGroupData(userGroupProvider.getGroup(pathSegments[0]));
            } catch (GroupNotFoundException e) {
                throw new PathNotFoundException("Cannot find group " + path, e);
            }
        }
        if (!MEMBERS_ROOT_NAME.equals(pathSegments[1])) {
            throw new PathNotFoundException(path);
        }
        String memberPath = StringUtils.substringAfter(path, "/" + MEMBERS_ROOT_NAME);
        JahiaUserSplittingRule userSplittingRule = jahiaUserManagerService.getUserSplittingRule();
        String userPath = StringUtils.substringAfter(memberPath, userDataSource.getContentStoreProvider().getMountPoint());
        String groupPath = StringUtils.substringAfter(memberPath, contentStoreProvider.getMountPoint());
        if (StringUtils.isNotBlank(userPath)) {
            int nbrOfUserPathSegments = StringUtils.split(userPath, '/').length;
            if (nbrOfUserPathSegments == userSplittingRule.getNumberOfSegments() + 1) { // member user path
                return getMemberData(path, userPath, userDataSource.getContentStoreProvider());
            } else if (nbrOfUserPathSegments < userSplittingRule.getNumberOfSegments() + 1) { // split folder
                return new ExternalData(path, path, "jnt:members", new HashMap<String, String[]>());
            } else { // path too long
                throw new PathNotFoundException(path);
            }
        }
        if (StringUtils.isNotBlank(groupPath)) {
            if (StringUtils.split(groupPath, '/').length == 1) { // member group path
                return getMemberData(path, groupPath, contentStoreProvider);
            } else { // path too long
                throw new PathNotFoundException(path);
            }
        }
        return new ExternalData(path, path, "jnt:members", new HashMap<String, String[]>());
    }

    private ExternalData getMemberData(String path, String refPath, ExternalContentStoreProvider provider) {
        HashMap<String, String[]> properties = new HashMap<String, String[]>();
        try {
            properties.put(MEMBER_REF_ATTR, new String[]{provider.getOrCreateInternalIdentifier(refPath)});
        } catch (RepositoryException e) {
            logger.error("Failed to get UUID for member " + refPath, e);
        }
        return new ExternalData(path, path, "jnt:member", properties);
    }

    @SuppressWarnings("unchecked")
    private List<String> getMembers(String groupName) throws RepositoryException {
        Map<String, Object> sessionVariables = ExternalContentStoreProvider.getCurrentSession().getSessionVariables();
        Map<String, List<String>> memberPathsByGroupName;
        if (sessionVariables.containsKey(MEMBER_PATHS_SESSION_VAR)) {
            memberPathsByGroupName = (Map<String, List<String>>) sessionVariables.get(MEMBER_PATHS_SESSION_VAR);
        } else {
            memberPathsByGroupName = new HashMap<String, List<String>>();
            sessionVariables.put(MEMBER_PATHS_SESSION_VAR, memberPathsByGroupName);
        }
        if (memberPathsByGroupName.containsKey(groupName)) {
            return memberPathsByGroupName.get(groupName);
        } else {
            ArrayList<String> paths = new ArrayList<String>();
            JahiaUserSplittingRule userSplittingRule = jahiaUserManagerService.getUserSplittingRule();
            for (Member member : userGroupProvider.getGroupMembers(groupName)) {
                if (member.getType() == Member.MemberType.GROUP) {
                    paths.add(contentStoreProvider.getMountPoint() + "/" + member.getName());
                } else {
                    paths.add(userDataSource.getContentStoreProvider().getMountPoint() + userSplittingRule.getRelativePathForUsername(member.getName()));
                }
            }
            memberPathsByGroupName.put(groupName, paths);
            return paths;
        }
    }

    @Override
    public List<String> getReferringProperties(String identifier, String propertyName) {
        if (MEMBER_REF_ATTR.equals(propertyName)) {
            String principalId = identifier;
            List<String> groups = null;
            ExternalContentStoreProvider provider = null;

            if (principalId.startsWith("/")) {
                // already an externalId -> identifier of a group
                groups = userGroupProvider.getMembership(new Member(StringUtils.substringAfterLast(principalId, "/"), Member.MemberType.GROUP));
                provider = contentStoreProvider;
            } else {
                try {
                    provider = userDataSource.getContentStoreProvider();
                    if (identifier.startsWith(provider.getId())) {
                        principalId = provider.getExternalProviderInitializerService().getExternalIdentifier(identifier);
                        if (principalId != null && principalId.startsWith("/")) {
                            groups = userGroupProvider.getMembership(new Member(StringUtils.substringAfterLast(principalId, "/"), Member.MemberType.USER));
                        }
                    }
                } catch (RepositoryException e) {
                    logger.debug("Error while treating member id as an external user one", e);
                }
            }

            List<String> properties = new ArrayList<String>();
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    properties.add("/" + group + "/" + MEMBERS_ROOT_NAME + provider.getMountPoint() + principalId + "/" + MEMBER_REF_ATTR);
                }
            }
            return properties;
        }

        return null;
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return SUPPORTED_NODE_TYPES;
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String path) {
        try {
            getItemByPath(path);
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> search(ExternalQuery externalQuery) throws RepositoryException {
        Properties searchCriteria = new Properties();
        SearchCriteriaHelper.getCriteriaFromConstraints(externalQuery.getConstraint(), searchCriteria, "groupname");
        searchCriteria.remove("jcr:language");
        List<String> result = new ArrayList<String>();
        try {
            for (String groupName : userGroupProvider.searchGroups(searchCriteria, externalQuery.getOffset(), externalQuery.getLimit())) {
                result.add("/" + groupName);
            }
        } catch (Exception e) {
            logger.error("Error while executing query {} on provider {}, issue {}",new Object[]{externalQuery,userGroupProvider,e.getMessage()});
            if(logger.isDebugEnabled()){
                logger.debug(e.getMessage(),e);
            }
        }
        return result;
    }

    public void setContentStoreProvider(ExternalContentStoreProvider contentStoreProvider) {
        this.contentStoreProvider = contentStoreProvider;
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setUserGroupProvider(UserGroupProvider userGroupProvider) {
        this.userGroupProvider = userGroupProvider;
    }

    public void setUserDataSource(UserDataSource userDataSource) {
        this.userDataSource = userDataSource;
    }
}
