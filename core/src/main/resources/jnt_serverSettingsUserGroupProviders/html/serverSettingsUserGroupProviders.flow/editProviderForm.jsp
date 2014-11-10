<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,admin-bootstrap.js,bootstrap-filestyle.min.js,jquery.metadata.js,jquery.tablesorter.js,jquery.tablecloth.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tablecloth.css"/>


<h2><fmt:message key="label.userGroupProvider.edit">
    <fmt:param value="${providerKey}"/>
</fmt:message></h2>

<form style="margin: 0;" action="${flowExecutionUrl}" method="post">
    <input type="hidden" name="providerKey" value="${providerKey}"/>
    <input type="hidden" name="providerClass" value="${providerClass}"/>

    <jsp:include page="${editJSP}"/>

    <div>
        <button class="btn btn-primary" type="submit" name="_eventId_save">
            <fmt:message key="label.save"/>
        </button>
        <button class="btn" type="button" onclick="$('#cancelForm${currentNode.identifier}').submit()">
            <fmt:message key="label.cancel"/>
        </button>
    </div>
</form>

<form id="cancelForm${currentNode.identifier}" style="display:none;" action="${flowExecutionUrl}" method="post">
    <input type="hidden" name="_eventId" value="cancel"/>
</form>
