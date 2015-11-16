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
package org.jahia.test.external.users;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.users.*;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupImpl;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserImpl;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.regex.Pattern;

public class TxtxUserGroupProvider extends BaseUserGroupProvider {

    private List<String> users = Arrays.asList("tata", "tete", "titi", "yaya");

    private List<String> groups = Arrays.asList("toto", "tutu", "tyty");

    @Override
    public JahiaUser getUser(String name) throws UserNotFoundException {
        if (users.contains(name)) {
            Properties properties = new Properties();
            properties.put("j:email", "mail@tx.tx");
            return new JahiaUserImpl(name, name, properties, getKey());
        }
        throw new UserNotFoundException("Cannot find user " + name);
    }

    @Override
    public JahiaGroup getGroup(String name) throws GroupNotFoundException {
        if (groups.contains(name)) {
            Properties properties = new Properties();
            return new JahiaGroupImpl(name, name, null, properties);
        }
        throw new GroupNotFoundException("Cannot find group " + name);
    }

    @Override
    public List<Member> getGroupMembers(String groupName) {
        ArrayList<Member> members = new ArrayList<Member>();
        if ("toto".equals(groupName)) {
            members.add(new Member("tata", Member.MemberType.USER));
            members.add(new Member("tete", Member.MemberType.USER));
            members.add(new Member("tutu", Member.MemberType.GROUP));
        }
        if ("tutu".equals(groupName)) {
            members.add(new Member("titi", Member.MemberType.USER));
        }
        if ("tyty".equals(groupName)) {
            members.add(new Member("tete", Member.MemberType.USER));
            members.add(new Member("titi", Member.MemberType.USER));
        }
        return members;
    }

    @Override
    public List<String> getMembership(Member member) {
        if ("tata".equals(member.getName())) {
            return Arrays.asList("toto");
        } else if ("tete".equals(member.getName())) {
            return Arrays.asList("toto", "tyty");
        } else if ("titi".equals(member.getName())) {
            return Arrays.asList("tutu", "tyty");
        } else if ("toto".equals(member.getName())) {
            return Collections.emptyList();
        } else if ("tutu".equals(member.getName())) {
            return Arrays.asList("toto");
        } else if ("tyty".equals(member.getName())) {
            return Collections.emptyList();
        }
        return null;
    }

    @Override
    public List<String> searchUsers(Properties searchCriterias, long offset, long limit) {
        if (searchCriterias.get("cookieauth") != null) {
            return Collections.emptyList();
        }
        String filter = (String) searchCriterias.get("username");
        if (filter == null) {
            filter = (String) searchCriterias.get("*");
        }
        ArrayList<String> l;
        if (filter != null) {
            l = new ArrayList<String>(Collections2.filter(users, Predicates.contains(Pattern.compile("^" + StringUtils.replace(filter, "*", ".*") + "$"))));
        } else {
            l = new ArrayList<String>(users);
        }
        return l.subList(Math.min((int) offset, l.size()), limit < 0 ? l.size() : Math.min((int) (offset + limit), l.size()));
    }

    @Override
    public List<String> searchGroups(Properties searchCriterias, long offset, long limit) {
        String filter = (String) searchCriterias.get("groupname");
        if (filter == null) {
            filter = (String) searchCriterias.get("*");
        }
        ArrayList<String> l;
        if (filter != null) {
            l = new ArrayList<String>(Collections2.filter(groups, Predicates.contains(Pattern.compile("^" + StringUtils.replace(filter, "*", ".*") + "$"))));
        } else {
            l = new ArrayList<String>(groups);
        }
        return l.subList(Math.min((int) offset, l.size()), limit < 0 ? l.size() : Math.min((int) (offset + limit), l.size()));
    }

    @Override
    public boolean verifyPassword(String userName, String userPassword) {
        return "password".equals(userPassword);
    }

    @Override
    public boolean supportsGroups() {
        return true;
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        return true;
    }
}
