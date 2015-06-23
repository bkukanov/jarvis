package org.safehaus.upsource.client;


import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.upsource.model.Project;
import org.safehaus.upsource.util.TestUtil;
import org.safehaus.util.RestUtil;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;


@RunWith( MockitoJUnitRunner.class )
public class UpsourceManagerImplTest
{

    @Mock
    RestUtil restUtil;

    UpsourceManagerImpl upsourceManager;


    @Before
    public void setUp() throws Exception
    {
        upsourceManager = new UpsourceManagerImpl( "http://upsource.subutai.io", "upsource-bot", "Uy/4eN]7+~}h8tUG" );
    }


    private void setResponse( String response ) throws RestUtil.RestException
    {
        upsourceManager.restUtil = restUtil;
        doReturn( response ).when( restUtil ).get( anyString(), anyMap() );
    }


    @Test
    public void testGetAllProjects() throws Exception
    {
        setResponse( TestUtil.PROJECTS_JSON );

        Set<Project> projects = upsourceManager.getAllProjects();

        assertFalse( projects.isEmpty() );

        for ( Project project : projects )
        {
            System.out.println( project );
        }
    }
}
