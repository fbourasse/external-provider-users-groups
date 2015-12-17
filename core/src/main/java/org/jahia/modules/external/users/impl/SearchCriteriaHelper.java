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
 *     This program is free software; you can redistribute it and/or
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
package org.jahia.modules.external.users.impl;

import org.apache.jackrabbit.commons.query.qom.Operator;
import org.jahia.utils.Patterns;

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.*;
import java.util.Properties;

/**
 * Utility class for search criteria operations.
 */
final class SearchCriteriaHelper {

    /**
     * Populate String/String principal search criteria from search query constraint
     *
     * @param constraint
     * @param searchCriteria
     * @param principalNameProperty
     * @return true if the query contains OR constraints
     * @throws RepositoryException
     */
    static boolean fillCriteriaFromConstraint(Constraint constraint, Properties searchCriteria, String principalNameProperty) throws RepositoryException {
        if (constraint == null) {
            searchCriteria.put("*", "*");
            return false;
        } else if (constraint instanceof And) {
        	And andConstraint = (And) constraint;
        	boolean constraint1IsOr = fillCriteriaFromConstraint(andConstraint.getConstraint1(), searchCriteria, principalNameProperty);
        	boolean constraint2IsOr = fillCriteriaFromConstraint(andConstraint.getConstraint2(), searchCriteria, principalNameProperty);
        	return (constraint1IsOr || constraint2IsOr);
        } else if (constraint instanceof Or) {
            Constraint constraint1 = ((Or) constraint).getConstraint1();
            Constraint constraint2 = ((Or) constraint).getConstraint2();
            if (constraint1 instanceof FullTextSearch
                    && ((FullTextSearch) constraint1).getPropertyName() == null
                    && constraint2 instanceof Comparison
                    && Operator.LIKE.toString().equals(((Comparison) constraint2).getOperator())
                    && ((Comparison) constraint2).getOperand1() instanceof LowerCase
                    && ((LowerCase) ((Comparison) constraint2).getOperand1()).getOperand() instanceof PropertyValue
                    && "j:nodename".equals(((PropertyValue) ((LowerCase) ((Comparison) constraint2).getOperand1()).getOperand()).getPropertyName())
                    && ((Comparison) constraint2).getOperand2() instanceof Literal) {
                searchCriteria.put("*", getLikeComparisonValue(((Literal) ((Comparison) constraint2).getOperand2()).getLiteralValue().getString()));
                return false;
            } else {
                fillCriteriaFromConstraint(constraint1, searchCriteria, principalNameProperty);
                fillCriteriaFromConstraint(constraint2, searchCriteria, principalNameProperty);
                return true;
            }
        } else if (constraint instanceof Comparison) {
            String operator = ((Comparison) constraint).getOperator();
            DynamicOperand operand1 = ((Comparison) constraint).getOperand1();
            StaticOperand operand2 = ((Comparison) constraint).getOperand2();
            if (Operator.LIKE.toString().equals(operator)) {
                String key = getCriteriaKey(operand1, principalNameProperty);
                if (key != null && operand2 instanceof Literal) {
                    searchCriteria.put(key, getLikeComparisonValue(((Literal) operand2).getLiteralValue().getString()));
                }
            } else if (Operator.EQ.toString().equals(operator)) {
                String key = getCriteriaKey(operand1, principalNameProperty);
                if (key != null && operand2 instanceof Literal) {
                    searchCriteria.put(key, ((Literal) operand2).getLiteralValue().getString());
                }
            }
        }
        return false;
    }

    private static String getCriteriaKey(DynamicOperand operand1, String principalNameProperty) {
        String key = null;
        if (operand1 instanceof PropertyValue) {
            key = ((PropertyValue) operand1).getPropertyName();
        } else if (operand1 instanceof LowerCase
                && ((LowerCase) operand1).getOperand() instanceof PropertyValue) {
            key = ((PropertyValue) ((LowerCase) operand1).getOperand()).getPropertyName();
        } else if (operand1 instanceof NodeLocalName) {
            key = principalNameProperty;
        }
        if ("j:nodename".equals(key)) {
            key = principalNameProperty;
        }
        return key;
    }

    private static String getLikeComparisonValue(String comparisonValue) {
        if ("%".equals(comparisonValue)) {
            return "*";
        } else {
            return Patterns.PERCENT.matcher(comparisonValue).replaceAll("*");
        }
    }

}
