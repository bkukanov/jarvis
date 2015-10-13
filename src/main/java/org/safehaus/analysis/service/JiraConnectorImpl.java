package org.safehaus.analysis.service;


import org.safehaus.exceptions.JiraClientException;
import org.safehaus.jira.JiraRestClient;
import org.safehaus.model.JarvisContext;
import org.safehaus.util.JarvisContextHolder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;


/**
 * Created by kisik on 29.07.2015.
 */
public class JiraConnectorImpl implements JiraConnector
{
    private static final Log log = LogFactory.getLog( JiraConnectorImpl.class );
    private String jiraURL;
    private String jiraUserName;
    private String jiraPass;


    public JiraConnectorImpl( String jiraURL, String jiraUserName, String jiraPass )
    {
        this.jiraURL = jiraURL;
        this.jiraUserName = jiraUserName;
        this.jiraPass = jiraPass;
    }


    @Override
    public JiraRestClient jiraConnect() throws JiraClientException
    {
        log.info( "jiraConnect()" );
        JiraRestClient jiraRestClient = null;
        if ( JarvisContextHolder.getContext() != null && JarvisContextHolder.getContext().getJiraRestClient() != null )
        {
            jiraRestClient = JarvisContextHolder.getContext().getJiraRestClient();
        }
        else
        {
            JarvisContextHolder.setContext( new JarvisContext( jiraURL, jiraUserName, jiraPass ) );
            if ( JarvisContextHolder.getContext() != null
                    && JarvisContextHolder.getContext().getJiraRestClient() != null )
            {
                jiraRestClient = JarvisContextHolder.getContext().getJiraRestClient();
            }
            else
            {
                log.info( "JarvisContextHolder is null." );
                throw new JiraClientException( "Jira Client is null." );
            }
        }
        return jiraRestClient;
    }


    @Override
    public JiraClient getJiraClient() throws JiraClientException
    {
        ICredentials authenticationHandler = new BasicCredentials( jiraUserName, jiraPass );
        try
        {
            return new JiraClient( jiraURL, authenticationHandler );
        }
        catch ( JiraException e )
        {
            log.error( "Error connecting to JIRA", e );
        }
        return null;
    }
}
