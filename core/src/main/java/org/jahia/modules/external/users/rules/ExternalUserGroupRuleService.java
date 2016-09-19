/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.users.rules;

import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.sites.JahiaSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

public class ExternalUserGroupRuleService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalUserGroupRuleService.class);

    private ExternalUserGroupService externalUserGroupService;

    public void initSiteForPendingProviders(AddedNodeFact nodeFact) {
        JCRNodeWrapper node = nodeFact.getNode();
        try {
            if (node instanceof JahiaSite && !node.isNodeType("jnt:module")) {
                String siteKey = ((JahiaSite) node).getSiteKey();
                if(externalUserGroupService == null) {
                    throw new IllegalStateException("External user group service is not yet initialized, " +
                            "unable to check site for pending provider for site " + siteKey);
                }
                externalUserGroupService.initSiteForPendingProviders(siteKey);
            }
        } catch (RepositoryException e) {
            logger.error("Error during providers init check up for new site nodes", e);
        }
    }

    public void setExternalUserGroupService(ExternalUserGroupService externalUserGroupService) {
        this.externalUserGroupService = externalUserGroupService;
    }
}
