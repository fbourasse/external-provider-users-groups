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
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
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
package org.jahia.modules.external.users;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUserManagerService;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

public class GroupsDataSource implements ExternalDataSource, ExternalDataSource.Searchable {

    private JahiaUserManagerService jahiaUserManagerService;

    private String providerKey;

    private UsersDataSource usersDataSource;

    private UserGroupProvider userGroupProvider;

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        return Collections.emptyList();
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
        String groupName = StringUtils.substringAfterLast(path, "/");
        if (!userGroupProvider.hasGroup(groupName)) {
            throw new PathNotFoundException("Cannot find group " + path);
        }
        if (!path.equals("/" + groupName)) {
            throw new PathNotFoundException("Cannot find group " + path);
        }
        return getGroupData(groupName);
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return new HashSet(Arrays.asList("jnt:group", "jnt:members", "jnt:member"));
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
        String groupName = StringUtils.substringAfterLast(path, "/");
        if (!userGroupProvider.hasGroup(groupName)) {
            return false;
        }
        if (!path.equals("/" + groupName)) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> search(ExternalQuery externalQuery) throws RepositoryException {
        Properties searchCriterias = new Properties();
        UsersDataSource.getCriteriasFromConstraints(externalQuery.getConstraint(), searchCriterias);
        if (searchCriterias.isEmpty()) {
            searchCriterias.put("*", "*");
        }
        List<String> result = new ArrayList<String>();
        for (JahiaGroup group : userGroupProvider.searchGroups(searchCriterias)) {
            result.add("/" + group.getName());
        }
        return result;
    }

    private ExternalData getGroupData(String groupName) {
        String path = "/" + groupName;
        Map<String,String[]> properties = new HashMap<String, String[]>();
        properties.put("j:external", new String[] {"true"});
        properties.put("j:externalSource", new String[] {providerKey});
        return new ExternalData(path, path, "jnt:group", properties);
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public void setUsersDataSource(UsersDataSource usersDataSource) {
        this.usersDataSource = usersDataSource;
    }

    public void setUserGroupProvider(UserGroupProvider userGroupProvider) {
        this.userGroupProvider = userGroupProvider;
    }
}
