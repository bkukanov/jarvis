package org.safehaus.service.rest;


import java.util.List;

import javax.jws.WebService;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.safehaus.dao.entities.jira.JarvisIssue;
import org.safehaus.exceptions.JiraClientException;
import org.safehaus.model.JarvisProject;
import org.safehaus.model.Views;

import com.fasterxml.jackson.annotation.JsonView;

import net.rcarz.jiraclient.Status;
import net.rcarz.jiraclient.Transition;


@WebService
public interface ProjectService
{
    @GET
    @Path( "projects" )
    List<JarvisProject> getProjects();

    @GET
    @Path( "projects/{projectId}" )
    @JsonView( Views.JarvisProjectLong.class )
    JarvisProject getProject( @PathParam( "projectId" ) String projectId ) throws JiraClientException;

    @GET
    @Path( "projects/{projectId}/issues" )
    @JsonView( Views.JarvisIssueShort.class )
    List<JarvisIssue> getIssues( @PathParam( "projectId" ) String projectId );

    @GET
    @Path( "issues/{issueId}" )
    @JsonView( Views.JarvisIssueLong.class )
    JarvisIssue getIssue( @PathParam( "issueId" ) String issueId );

    @POST
    @Path( "issues" )
    @Consumes( MediaType.APPLICATION_JSON )
    JarvisIssue createIssue( JarvisIssue issue );

    @GET
    @Path( "transitions/{issueIdOrKey}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @JsonView( Views.CompleteView.class )
    List<Transition> getTransitions( @PathParam( "issueIdOrKey" ) String issueIdOrKey ) throws JiraClientException;

    @PUT
    @Path( "issue/{issueIdOrKey}/transition/{transitionId}" )
    Status toTransition( @PathParam( "issueIdOrKey" ) String issueIdOrKey,
                         @PathParam( "transitionId" ) String transitionId ) throws JiraClientException;

    @PUT
    @Path( "issue/{issueIdOrKey}/status/start" )
    Status start( @PathParam( "issueIdOrKey" ) String issueIdOrKey ) throws JiraClientException;

    @PUT
    @Path( "issue/{issueIdOrKey}/status/resolve" )
    Status resolve( @PathParam( "issueIdOrKey" ) String issueIdOrKey ) throws JiraClientException;

    @PUT
    @Path( "issue/{issueIdOrKey}/status/request-approval" )
    Status requestApproval( @PathParam( "issueIdOrKey" ) String issueIdOrKey ) throws JiraClientException;

    @PUT
    @Path( "issue/{issueIdOrKey}/status/approve" )
    Status approve( @PathParam( "issueIdOrKey" ) String issueIdOrKey ) throws JiraClientException;

    @PUT
    @Path( "issue/{issueIdOrKey}/status/reject" )
    Status reject( @PathParam( "issueIdOrKey" ) String issueIdOrKey ) throws JiraClientException;
}
