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
package org.jahia.modules.external.users.admin;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.users.*;
import org.jahia.modules.external.users.impl.ExternalUserGroupServiceImpl;
import org.jahia.modules.external.users.impl.UserDataSource;
import org.jahia.modules.external.users.impl.UserGroupProviderRegistration;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.ParameterMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow controller for the user/group providers.
 */
public class UserGroupProviderAdminFlow implements Serializable {

    private static final long AVAILABILITY_TIMEOUT = 60 * 1000L;

    private static final long serialVersionUID = 4171622809934546645L;

    private static final int WAIT_SLEEP = 2000;

    @Autowired
    private transient ExternalUserGroupServiceImpl externalUserGroupServiceImpl;

    private transient JCRStoreService jcrStoreService;

    /**
     * Performs the creation of the provider.
     * 
     * @param parameters
     *            flow parameter map
     * @param flashScope
     *            flow attribute map
     * @throws Exception
     *             in case of a creation error
     */
    public void createProvider(ParameterMap parameters, MutableAttributeMap flashScope) throws Exception {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
        String providerClass = parameters.get("providerClass");
        @SuppressWarnings("unchecked")
        String providerKey = configurations.get(providerClass).create(parameters.asMap(), flashScope.asMap()) + ".users";
        wait(providerKey, true);
    }

    /**
     * Performs deletion of the provider
     * 
     * @param providerKey
     *            the key of the provider
     * @param providerClass
     *            provider class name
     * @param flashScope
     *            the flow attribute map
     * @throws Exception
     *             in case of an error during deletion
     */
    @SuppressWarnings("unchecked")
    public void deleteProvider(String providerKey, String providerClass, MutableAttributeMap flashScope) throws Exception {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
        configurations.get(providerClass).delete(providerKey, flashScope.asMap());
        providerKey += ".users";
        wait(providerKey, false);
    }

    /**
     * Performs the edition of the provider configuration.
     * 
     * @param parameters
     *            flow parameter map
     * @param flashScope
     *            flow attribute map
     * @throws Exception
     *             in case of an error during edition
     */
    @SuppressWarnings("unchecked")
    public void editProvider(ParameterMap parameters, MutableAttributeMap flashScope) throws Exception {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
        String providerKey = parameters.get("providerKey");
        String providerClass = parameters.get("providerClass");
        configurations.get(providerClass).edit(providerKey, parameters.asMap(), flashScope.asMap());
        providerKey += ".users";
        wait(providerKey, true);
    }

    /**
     * Returns the provider create configuration map.
     * 
     * @return the provider create configuration map
     */
    public Map<String, UserGroupProviderConfiguration> getCreateConfigurations() {
        HashMap<String, UserGroupProviderConfiguration> map = new HashMap<String, UserGroupProviderConfiguration>();
        for (Map.Entry<String, UserGroupProviderConfiguration> entry : externalUserGroupServiceImpl.getProviderConfigurations().entrySet()) {
            if (entry.getValue().isCreateSupported()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Returns a list of registered user/group providers.
     * 
     * @return a list of registered user/group providers
     */
    public List<UserGroupProviderInfo> getUserGroupProviders() {
        ArrayList<UserGroupProviderInfo> infos = new ArrayList<UserGroupProviderInfo>();
        Map<String, JCRStoreProvider> providers = jcrStoreService.getSessionFactory().getProviders();
        for (Map.Entry<String, UserGroupProviderRegistration> entry : externalUserGroupServiceImpl.getRegisteredProviders().entrySet()) {
            UserGroupProviderInfo providerInfo = new UserGroupProviderInfo();
            providerInfo.setKey(entry.getKey());
            UserDataSource dataSource = (UserDataSource) entry.getValue().getUserProvider().getDataSource();
            UserGroupProvider userGroupProvider = dataSource.getUserGroupProvider();
            String userGroupProviderClass = userGroupProvider.getClass().getName();
            providerInfo.setProviderClass(userGroupProviderClass);
            providerInfo.setGroupSupported(userGroupProvider.supportsGroups());
            JCRStoreProvider prov = providers.get(entry.getKey() + ".users");
            providerInfo.setRunning(prov != null && prov.isAvailable());
            Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupServiceImpl.getProviderConfigurations();
            UserGroupProviderConfiguration configuration = configurations.get(userGroupProviderClass);
            if (configuration != null) {
                providerInfo.setEditSupported(configuration.isEditSupported());
                providerInfo.setEditJSP(configuration.getEditJSP());
                providerInfo.setDeleteSupported(configuration.isDeleteSupported());
            }
            providerInfo.setSiteKey(entry.getValue().getSiteKey());
            infos.add(providerInfo);
        }
        return infos;
    }

    /**
     * Resumes the specified provider.
     * 
     * @param providerKey
     *            the key of the provider to be resumed
     * @throws JahiaInitializationException
     *             in case of a provider initialization error
     */
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

    @Autowired
    public void setJcrStoreService(@Value("#{JCRStoreService}") JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    /**
     * Resumes the provider.
     * 
     * @param providerKey
     *            the key of the provider to be resumed
     */
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

    private void wait(String providerKey, boolean shouldBeAvailable) {
        JCRSessionFactory sessionFactory = jcrStoreService.getSessionFactory();
        
        long endTime = System.currentTimeMillis() + AVAILABILITY_TIMEOUT;
        while (System.currentTimeMillis() < endTime
                && (shouldBeAvailable
                        && (!sessionFactory.getProviders().containsKey(providerKey) || !sessionFactory.getProviders()
                                .get(providerKey).isAvailable()) || (!shouldBeAvailable && sessionFactory
                        .getProviders().containsKey(providerKey)))) {
            // wait for provider availability / unavilability if it's asynchronous
            try {
                Thread.sleep(WAIT_SLEEP);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
