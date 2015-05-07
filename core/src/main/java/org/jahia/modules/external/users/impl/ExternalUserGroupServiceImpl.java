/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
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
import org.jahia.exceptions.JahiaException;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.modules.external.users.UserGroupProvider;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

import java.util.*;

/**
 * Implementation of the external user/group service.
 */
public class ExternalUserGroupServiceImpl implements ExternalUserGroupService {

    private static final List<String> EXTENDABLE_TYPES = Arrays.asList("nt:base");

    private static final List<String> OVERRIDABLE_ITEMS = Arrays.asList("jnt:user.*", "jnt:usersFolder.*",
            "mix:lastModified.*", "jmix:lastPublished.*");

    private static final Logger logger = LoggerFactory.getLogger(ExternalUserGroupServiceImpl.class);

    private static final String PROVIDERS_MOUNT_CONTAINER = "providers";
    private static final String USERS_FOLDER_NAME = "users";
    private static final String GROUPS_FOLDER_NAME = "groups";

    private JCRStoreService jcrStoreService;
    private JahiaSitesService jahiaSitesService;
    private String readOnlyUserProperties;
    private Map<String, UserGroupProviderRegistration> registeredProviders = new TreeMap<String, UserGroupProviderRegistration>();
    private Map<String, UserGroupProviderConfiguration> providerConfigurations = new HashMap<String, UserGroupProviderConfiguration>();

    @Override
    public void register(String providerKey, final UserGroupProvider userGroupProvider) {
        register(providerKey, null, userGroupProvider);
    }

    @Override
    public void register(String providerKey, final String siteKey, final UserGroupProvider userGroupProvider) {
        String userProviderKey = providerKey + ".users";
        String groupProviderKey = providerKey + ".groups";
        Map<String, JCRStoreProvider> providers = jcrStoreService.getSessionFactory().getProviders();
        if (providers.get(userProviderKey) == null && providers.get(groupProviderKey) == null) {
            try {
                UserDataSource userDataSource = (UserDataSource) SpringContextSingleton.getBeanInModulesContext("UserDataSourcePrototype");
                userDataSource.setUserGroupProvider(userGroupProvider);

                ExternalContentStoreProvider userProvider = (ExternalContentStoreProvider) SpringContextSingleton.getBeanInModulesContext("ExternalStoreProviderPrototype");
                userProvider.setKey(userProviderKey);
                String sitePath = "/sites/" + siteKey + "/";
                userProvider.setMountPoint((siteKey == null ? "/" : sitePath) + USERS_FOLDER_NAME + "/" + PROVIDERS_MOUNT_CONTAINER + "/" + providerKey);
                userProvider.setDataSource(userDataSource);
                userProvider.setExtendableTypes(EXTENDABLE_TYPES);
                userProvider.setOverridableItems(OVERRIDABLE_ITEMS);
                String readOnlyProps = (String) SettingsBean.getInstance().getPropertiesFile().get("external.users.properties.readonly." + providerKey);
                if (StringUtils.isBlank(readOnlyProps)) {
                    readOnlyProps = readOnlyUserProperties;
                }
                if (StringUtils.isNotBlank(readOnlyProps)) {
                    List<String> nonOverridableItems = new ArrayList<String>();
                    for (String p : StringUtils.split(readOnlyProps, ',')) {
                        if (StringUtils.isNotBlank(p)) {
                            nonOverridableItems.add("jnt:user." + p.trim());
                        }
                    }
                    if (!nonOverridableItems.isEmpty()) {
                        userProvider.setNonOverridableItems(nonOverridableItems);
                    }
                }

                userDataSource.setContentStoreProvider(userProvider);

                UserGroupProviderRegistration registration = new UserGroupProviderRegistration(siteKey, userProvider);
                registeredProviders.put(providerKey, registration);

                userProvider.start();


                if (userGroupProvider.supportsGroups()) {
                    GroupDataSource groupDataSource = (GroupDataSource) SpringContextSingleton.getBeanInModulesContext("GroupDataSourcePrototype");
                    groupDataSource.setUserDataSource(userDataSource);
                    groupDataSource.setUserGroupProvider(userGroupProvider);

                    ExternalContentStoreProvider groupProvider = (ExternalContentStoreProvider) SpringContextSingleton.getBeanInModulesContext("ExternalStoreProviderPrototype");
                    groupProvider.setKey(groupProviderKey);
                    groupProvider.setMountPoint((siteKey == null ? "/" : sitePath)+ GROUPS_FOLDER_NAME + "/" + PROVIDERS_MOUNT_CONTAINER + "/" + providerKey);
                    groupProvider.setDataSource(groupDataSource);

                    groupDataSource.setContentStoreProvider(groupProvider);

                    registration.setGroupProvider(groupProvider);

                    groupProvider.start();
                }
            } catch (JahiaInitializationException e) {
                logger.error(e.getMessage(), e);
            }
        }

        createMissingStructure(siteKey, userGroupProvider.supportsGroups());
    }

    private void createMissingStructure(final String siteKey, final boolean supportsGroups) {
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper rootNode = null;
                    if (siteKey == null) {
                        rootNode = session.getNode("/");
                    } else {
                        String path = "/sites/" + siteKey;
                        if (session.nodeExists(path)) {
                            rootNode = session.getNode(path);
                        }
                    }
                    if (rootNode == null) {
                        return null;
                    }

                    boolean saveNeeded = false;
                    JCRNodeWrapper node = rootNode;
                    if (node.hasNode(USERS_FOLDER_NAME)) {
                        node = node.getNode(USERS_FOLDER_NAME);
                    } else {
                        node = node.addNode(USERS_FOLDER_NAME, "jnt:usersFolder");
                        saveNeeded = true;
                    }
                    if (!node.hasNode(PROVIDERS_MOUNT_CONTAINER)) {
                        node.addNode(PROVIDERS_MOUNT_CONTAINER, "jnt:usersFolder");
                        saveNeeded = true;
                    }
                    if (supportsGroups) {
                        node = rootNode;
                        if (node.hasNode(GROUPS_FOLDER_NAME)) {
                            node = node.getNode(GROUPS_FOLDER_NAME);
                        } else {
                            node = node.addNode(GROUPS_FOLDER_NAME, "jnt:groupsFolder");
                            saveNeeded = true;
                        }
                        if (!node.hasNode(PROVIDERS_MOUNT_CONTAINER)) {
                            node.addNode(PROVIDERS_MOUNT_CONTAINER, "jnt:groupsFolder");
                            saveNeeded = true;
                        }
                    }
                    if (saveNeeded) {
                        session.save();
                    }
                    return null;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Failed to create providers mount containers", e);
            return;
        }
    }

    public void checkUserProvidersWaitingForSite(String newSiteKey) {
        for (Map.Entry<String, UserGroupProviderRegistration> entry : getRegisteredProviders().entrySet()) {
            String siteKey = entry.getValue().getSiteKey();
            if (siteKey == null || !siteKey.equals(newSiteKey)) {
                continue;
            }
            JahiaSite targetSite = null;
            if (siteKey != null) {
                try {
                    targetSite = jahiaSitesService.getSiteByKey(siteKey);
                } catch (JahiaException e) {
                    logger.debug("Cannot get site " + siteKey, e);
                }
            }
            if (targetSite != null) {
                createMissingStructure(siteKey, entry.getValue().getGroupProvider() != null);
            }
        }
    }

    @Override
    public void unregister(String providerKey) {
        Map<String, JCRStoreProvider> providers = jcrStoreService.getSessionFactory().getProviders();
        JCRStoreProvider provider = providers.get(providerKey + ".users");
        if (provider != null) {
            provider.stop();
        }
        provider = providers.get(providerKey + ".groups");
        if (provider != null) {
            provider.stop();
        }
        registeredProviders.remove(providerKey);
    }

    @Override
    public void setConfiguration(String providerClass, UserGroupProviderConfiguration userGroupProviderConfig) {
        providerConfigurations.put(providerClass, userGroupProviderConfig);
    }

    @Override
    public void setMountStatus(String providerKey, JCRMountPointNode.MountStatus status) {
        Map<String, JCRStoreProvider> providers = jcrStoreService.getSessionFactory().getProviders();
        JCRStoreProvider provider = providers.get(providerKey + ".users");
        if (provider != null) {
            provider.setMountStatus(status);
        }
        provider = providers.get(providerKey + ".groups");
        if (provider != null) {
            provider.setMountStatus(status);
        }
    }

    public Map<String, UserGroupProviderConfiguration> getProviderConfigurations() {
        return providerConfigurations;
    }

    public Map<String, UserGroupProviderRegistration> getRegisteredProviders() {
        return registeredProviders;
    }

    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    public void setReadOnlyUserProperties(String readOnlyUserProperties) {
        this.readOnlyUserProperties = readOnlyUserProperties;
    }

    public JCRStoreService getJcrStoreService() {
        return jcrStoreService;
    }

    public void setJahiaSitesService(JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }
}
