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
package org.jahia.modules.external.users.admin;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.users.*;
import org.jahia.services.content.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.webflow.core.collection.ParameterMap;

import java.io.Serializable;
import java.util.*;

public class UserGroupProviderAdminFlow implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UserGroupProviderAdminFlow.class);

    @Autowired
    private transient ExternalUserGroupServiceImpl externalUserGroupServiceImpl;

    private transient JCRStoreService jcrStoreService;

    public List<UserGroupProviderInfo> getUserGroupProviders() {
        ArrayList<UserGroupProviderInfo> infos = new ArrayList<UserGroupProviderInfo>();
        Map<String, JCRStoreProvider> providers = jcrStoreService.getSessionFactory().getProviders();
        for (Map.Entry<String, UserGroupProviderRegistration> entry : externalUserGroupServiceImpl.getRegisteredProviders().entrySet()) {
            UserGroupProviderInfo providerInfo = new UserGroupProviderInfo();
            providerInfo.setKey(entry.getKey());
            UsersDataSource dataSource = (UsersDataSource) entry.getValue().getUserProvider().getDataSource();
            UserGroupProvider userGroupProvider = dataSource.getUserGroupProvider();
            String userGroupProviderClass = userGroupProvider.getClass().getName();
            providerInfo.setProviderClass(userGroupProviderClass);
            providerInfo.setGroupSupported(userGroupProvider.supportsGroups());
            providerInfo.setRunning(providers.containsKey(entry.getKey() + ".users") && providers.get(entry.getKey() + ".users").isAvailable());
            Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
            if (configurations.containsKey(userGroupProviderClass)) {
                UserGroupProviderConfiguration configuration = configurations.get(userGroupProviderClass);
                providerInfo.setEditSupported(configuration.isEditSupported());
                providerInfo.setEditJSP(configuration.getEditJSP());
                providerInfo.setDeleteSupported(configuration.isDeleteSupported());
            }
            infos.add(providerInfo);
        }
        return infos;
    }

    public Map<String, String> getCreateConfigurations() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, UserGroupProviderConfiguration> entry : externalUserGroupServiceImpl.getProviderConfigurations().entrySet()) {
            if (entry.getValue().isCreateSupported()) {
                map.put(entry.getKey(), entry.getValue().getCreateJSP());
            }
        }
        return map;
    }

    public void suspendProvider(String providerKey) {
        UserGroupProviderRegistration registration = externalUserGroupServiceImpl.getRegisteredProviders().get(providerKey);
        JCRStoreProvider userProvider = registration.getUserProvider();
        if (userProvider != null) {
            userProvider.stop();
        }
        JCRStoreProvider groupProvider = registration.getGroupProvider();
        if (groupProvider != null) {
            groupProvider.stop();
        }
    }

    public void resumeProvider(String providerKey) throws JahiaInitializationException {
        UserGroupProviderRegistration registration = externalUserGroupServiceImpl.getRegisteredProviders().get(providerKey);
        JCRStoreProvider userProvider = registration.getUserProvider();
        if (userProvider != null) {
            userProvider.start();
        }
        JCRStoreProvider groupProvider = registration.getGroupProvider();
        if (groupProvider != null) {
            groupProvider.start();
        }
    }

    public void createProvider(ParameterMap parameters) {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
        String providerClass = parameters.get("providerClass");
        configurations.get(providerClass).create(parameters);
    }

    public void editProvider(ParameterMap parameters) {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
        String providerKey = parameters.get("providerKey");
        String providerClass = parameters.get("providerClass");
        configurations.get(providerClass).edit(providerKey, parameters);
    }

    public void deleteProvider(String providerKey, String providerClass) {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
        configurations.get(providerClass).delete(providerKey);
    }

    @Autowired
    public void setJcrStoreService(@Value("#{JCRStoreService}") JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }
}
