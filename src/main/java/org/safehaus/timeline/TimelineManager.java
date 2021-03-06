package org.safehaus.timeline;


import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.safehaus.dao.entities.jira.IssueWorkLog;
import org.safehaus.dao.entities.jira.JarvisLink;
import org.safehaus.dao.entities.jira.JiraIssueChangelog;
import org.safehaus.dao.entities.jira.JiraMetricIssue;
import org.safehaus.dao.entities.jira.JiraProject;
import org.safehaus.dao.entities.jira.JiraUser;
import org.safehaus.dao.entities.sonar.SonarMetricIssue;
import org.safehaus.dao.entities.stash.StashMetricIssue;
import org.safehaus.model.Capture;
import org.safehaus.service.api.IssueChangelogDao;
import org.safehaus.service.api.JiraMetricDao;
import org.safehaus.service.api.ServicePackDao;
import org.safehaus.service.api.SonarMetricService;
import org.safehaus.service.api.StashMetricService;
import org.safehaus.timeline.dao.TimelineDao;
import org.safehaus.timeline.model.IssueProgress;
import org.safehaus.timeline.model.ProgressStatus;
import org.safehaus.timeline.model.ProjectStats;
import org.safehaus.timeline.model.StoryTimeline;
import org.safehaus.timeline.model.Structure;
import org.safehaus.timeline.model.StructuredIssue;
import org.safehaus.timeline.model.StructuredProject;
import org.safehaus.timeline.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * Created by talas on 9/29/15.
 */
//@Service
public class TimelineManager
{
    private static final Logger logger = LoggerFactory.getLogger( TimelineManager.class );

    private JiraMetricDao jiraMetricDao;

    private TimelineDao timelineDaoImpl;

    @Autowired
    private SonarMetricService sonarMetricService;

    @Autowired
    private StashMetricService stashMetricService;

    @Autowired
    private ServicePackDao servicePackDao;

    @Autowired
    private IssueChangelogDao issueChangelogDao;

    private Map<String, StructuredProject> structuredProjects = Maps.newHashMap();


    public TimelineManager( final JiraMetricDao jiraMetricDao, final TimelineDao timelineDaoImpl )
    {
        logger.info( "Timeline manager initialized" );
        this.jiraMetricDao = jiraMetricDao;
        this.timelineDaoImpl = timelineDaoImpl;

        //        researchKeys.add( "SS-3389" );
        //        researchKeys.add( "SS-3376" );
        //        researchKeys.add( "KURJUN-38" );
        //        researchKeys.add( "KMS-259" );
        //        researchKeys.add( "KMS-198" );
        //        researchKeys.add( "KMS-152" );
    }


    @PostConstruct
    public void init()
    {
        //        ServiceIdentity jiraIdentity = new ServiceIdentity(  )
        //        ServicePack servicePack = new ServicePack( "Keshig",  )
        logger.info( "Timeline service initialized." );

        try
        {
            List<JiraProject> jiraProjects = jiraMetricDao.getProjects();
            for ( final JiraProject jiraProject : jiraProjects )
            {
                buildProjectStructure( jiraProject );
            }
        }
        catch ( Exception ex )
        {
            logger.error( "Error initializing timelineManager", ex );
        }
    }


    public void rebuildProjectStructure( String projectKey )
    {
        buildProjectStructure( jiraMetricDao.getProject( projectKey ) );
    }


    private void buildProjectStructure( JiraProject jiraProject )
    {
        logger.info( "Building project structure." );
        StructuredProject project =
                new StructuredProject( jiraProject.getProjectId(), jiraProject.getName(), jiraProject.getKey(),
                        jiraProject.getDescription(), jiraProject.getProjectVersions() );

        project.setDoneStatus( new ProgressStatus() );
        project.setInProgressStatus( new ProgressStatus() );
        project.setOpenStatus( new ProgressStatus() );

        //TODO replace with more precise project services association
        if ( "SS".equals( jiraProject.getKey() ) )
        {
            SonarMetricIssue sonarMetricIssue = sonarMetricService.findSonarMetricIssueByProjectId( "5855" );
            if ( sonarMetricIssue != null )
            {
                ProjectStats projectStats = new ProjectStats( sonarMetricIssue );
                project.setProjectStats( projectStats );
            }
        }
        else if ( "GFIG".equals( jiraProject.getKey() ) )
        {
            SonarMetricIssue sonarMetricIssue = sonarMetricService.findSonarMetricIssueByProjectId( "2999" );
            if ( sonarMetricIssue != null )
            {
                ProjectStats projectStats = new ProjectStats( sonarMetricIssue );
                project.setProjectStats( projectStats );
            }
        }
        else if ( "JETTYJAM".equals( jiraProject.getKey() ) )
        {
            SonarMetricIssue sonarMetricIssue = sonarMetricService.findSonarMetricIssueByProjectId( "3040" );
            if ( sonarMetricIssue != null )
            {
                ProjectStats projectStats = new ProjectStats( sonarMetricIssue );
                project.setProjectStats( projectStats );
            }
        }

        Set<StructuredIssue> structuredEpics = getProjectEpics( jiraProject.getKey() );
        project.setEpicsCount( structuredEpics.size() );

        for ( final StructuredIssue structuredEpic : structuredEpics )
        {
            sumUpEstimates( structuredEpic, project );
            String statusKey;
            switch ( structuredEpic.getStatus() )
            {
                case "Open":
                    statusKey = "Open";
                    break;
                case "Closed":
                case "Resolved":
                case "Done":
                    statusKey = "Done";
                    break;
                default:
                    statusKey = "In Progress";
                    break;
            }
            Long count = project.getEpicCompletion().get( statusKey );
            if ( count == null )
            {
                count = 0L;
            }
            count++;
            project.getEpicCompletion().put( statusKey, count );
        }

        project.setIssues( structuredEpics );
        structuredProjects.put( jiraProject.getKey(), project );

        timelineDaoImpl.updateProject( project );
    }


    /**
     * Constructs project dependency tree
     *
     * @param projectKey - target project key to view dependency
     *
     * @return - project
     */
    public StructuredProject getProject( final String projectKey )
    {
        StructuredProject structuredProject = timelineDaoImpl.getProjectByKey( projectKey );
        if ( structuredProject != null )
        {
            for ( final String issueKey : structuredProject.getIssuesKeys() )
            {
                StructuredIssue issue = timelineDaoImpl.getStructuredIssueByKey( issueKey );
                structuredProject.addIssue( issue );
            }
        }
        return structuredProject;
    }


    /**
     * returns list of projects
     */
    public List<StructuredProject> getProjects()
    {
        return timelineDaoImpl.getAllProjects();
    }


    /**
     * Gets all jira issues for project by putting each into map to make it accessible via issue key
     *
     * @param projectKey - target project to pull issues for
     *
     * @return - issues map
     */
    private Map<String, JiraMetricIssue> getJiraProjectIssues( String projectKey )
    {
        Map<String, JiraMetricIssue> jiraMetricIssues = Maps.newHashMap();

        List<JiraMetricIssue> issues = jiraMetricDao.getIssuesByTypeForProject( projectKey, "Epic" );
        for ( final JiraMetricIssue issue : issues )
        {
            jiraMetricIssues.put( issue.getIssueKey(), issue );
        }
        return jiraMetricIssues;
    }


    /**
     * Gets all epics for project
     */
    private Set<StructuredIssue> getProjectEpics( String projectKey )
    {
        Set<StructuredIssue> epics = Sets.newHashSet();
        List<JiraMetricIssue> epicIssues = jiraMetricDao.getIssuesByTypeForProject( projectKey, "Epic" );

        for ( final JiraMetricIssue epicIssue : epicIssues )
        {
            //TODO remove temporal "requirements_december" condition
            if ( "Epic".equals( epicIssue.getType().getName() ) && projectKey.equals( epicIssue.getProjectKey() ) )
            {
                if ( "SS".equals( projectKey ) && !epicIssue.getLabels().contains( "requirements_december" ) )
                {
                    continue;
                }
                StructuredIssue epic = new StructuredIssue( epicIssue.getIssueKey(), epicIssue.getIssueId(),
                        epicIssue.getType().getName(), epicIssue.getSummary(), epicIssue.getReporterName(),
                        epicIssue.getReporterName(), epicIssue.getAssigneeName(), epicIssue.getUpdateDate().getTime(),
                        epicIssue.getCreationDate().getTime(), epicIssue.getStatus(), epicIssue.getProjectKey(),
                        epicIssue.getDueDate().toString(), epicIssue.getRemoteLinks(), epicIssue.getComponents(),
                        epicIssue.getLabels(), epicIssue.getDescription(), epicIssue.getOriginalEstimateMinutes(),
                        epicIssue.getIssueWorkLogs() );

                assignIssueEstimate( epic, epicIssue );

                List<String> epicStories = getChildIssues( epicIssue );
                Set<String> issueKeys = Sets.newHashSet();
                for ( final String story : epicStories )
                {
                    buildStructureIssue( story, epic, issueKeys );
                }

                epics.add( epic );
            }
        }
        return epics;
    }


    private void assignIssueEstimate( StructuredIssue structuredIssue, JiraMetricIssue issue )
    {
        structuredIssue.setOpenStatus( new ProgressStatus() );
        structuredIssue.setInProgressStatus( new ProgressStatus() );
        structuredIssue.setDoneStatus( new ProgressStatus() );

        if ( issue.getAssigneeName() != null )
        {
            structuredIssue.getUsers().add( issue.getAssigneeName() );
        }
        if ( issue.getReporterName() != null )
        {
            structuredIssue.getUsers().add( issue.getReporterName() );
        }

        ProgressStatus progressStatus = null;
        //TODO Simplify value assigment by switch cases
        String status = "Open";
        switch ( issue.getStatus() )
        {
            case "Open":
                status = "Open";
                progressStatus = structuredIssue.getOpenStatus();
                break;
            case "Done":
                status = "Done";
                progressStatus = structuredIssue.getDoneStatus();
                break;
            default:
                status = "In Progress";
                progressStatus = structuredIssue.getInProgressStatus();
                break;
        }

        if ( "Story".equals( structuredIssue.getIssueType() ) )
        {
            IssueProgress storyPoints = new IssueProgress();
            IssueProgress storyProgress = new IssueProgress();
            Random random = new Random();
            long val = ( random.nextInt( 4 ) + 1 ) * 2;
            switch ( issue.getStatus() )
            {
                case "Open":
                    storyPoints.setOpen( val );
                    storyProgress.setOpen( 1 );
                    break;
                case "Done":
                    storyPoints.setDone( val );
                    storyProgress.setDone( 1 );
                    break;
                default:
                    storyPoints.setInProgress( val );
                    storyProgress.setInProgress( 1 );
                    break;
            }
            structuredIssue.setStoryPoints( storyPoints );
        }

        if ( "Requirement".equals( structuredIssue.getIssueType() ) )
        {
            IssueProgress requirementProgress = new IssueProgress();
            switch ( issue.getStatus() )
            {
                case "Open":
                    requirementProgress.setOpen( 1 );
                    break;
                case "Done":
                    requirementProgress.setDone( 1 );
                    break;
                default:
                    requirementProgress.setInProgress( 1 );
                    break;
            }
            structuredIssue.setRequirementProgress( requirementProgress );
        }

        if ( progressStatus != null )
        {
            progressStatus
                    .setOriginalEstimate( progressStatus.getOriginalEstimate() + issue.getOriginalEstimateMinutes() );
            progressStatus.setRemainingRestimate(
                    progressStatus.getRemainingRestimate() + issue.getRemainingEstimateMinutes() );
            progressStatus.setTimeSpent( progressStatus.getTimeSpent() + issue.getTimeSpentMinutes() );
        }
        if ( "Resolved".equals( issue.getStatus() ) || "Closed".equals( issue.getStatus() ) ||
                "Done".equals( issue.getStatus() ) )
        {
            String type = issue.getType().getName();
            Long totalSolved = structuredIssue.getTotalIssuesSolved().get( type );
            if ( totalSolved == null )
            {
                totalSolved = 0L;
            }
            totalSolved += issue.getTimeSpentMinutes();
            structuredIssue.getTotalIssuesSolved().put( type, totalSolved );
        }
    }


    private void sumUpEstimates( Structure structuredIssue, Structure parent )
    {
        // Assign open statuses
        sumUpProgressByStatus( structuredIssue.getOpenStatus(), parent.getOpenStatus() );

        // Assign in progress statuses
        sumUpProgressByStatus( structuredIssue.getInProgressStatus(), parent.getInProgressStatus() );

        // Assign done statuses
        sumUpProgressByStatus( structuredIssue.getDoneStatus(), parent.getDoneStatus() );

        // Sum up stories according to statuses
        sumUpProgressByType( structuredIssue.getStoryProgress(), parent.getStoryProgress() );

        // Sum up Story points according to status
        sumUpProgressByType( structuredIssue.getStoryPoints(), parent.getStoryPoints() );

        // Sum up Requirements according to status
        sumUpProgressByType( structuredIssue.getRequirementProgress(), parent.getRequirementProgress() );

        parent.getUsers().addAll( structuredIssue.getUsers() );

        for ( final Map.Entry<String, Long> entry : structuredIssue.getTotalIssuesSolved().entrySet() )
        {
            String key = entry.getKey();
            Long value = entry.getValue();

            Long parentValue = parent.getTotalIssuesSolved().get( key );
            if ( parentValue == null )
            {
                parentValue = 0L;
            }
            parentValue += value;

            parent.getTotalIssuesSolved().put( key, parentValue );
        }
    }


    private void sumUpProgressByType( IssueProgress child, IssueProgress parent )
    {
        if ( child != null && parent != null )
        {
            parent.setDone( parent.getDone() + child.getDone() );
            parent.setInProgress( parent.getInProgress() + child.getInProgress() );
            parent.setOpen( parent.getOpen() + child.getOpen() );
        }
    }


    private void sumUpProgressByStatus( ProgressStatus progressStatus, ProgressStatus parentProgress )
    {
        if ( progressStatus != null && parentProgress != null )
        {
            parentProgress
                    .setOriginalEstimate( progressStatus.getOriginalEstimate() + parentProgress.getOriginalEstimate() );
            parentProgress.setRemainingRestimate(
                    progressStatus.getRemainingRestimate() + parentProgress.getRemainingRestimate() );
            parentProgress.setTimeSpent( progressStatus.getTimeSpent() + parentProgress.getTimeSpent() );
        }
    }


    /**
     * constructs story timeline according to dependency in issues
     */
    public StoryTimeline getStoryTimeline( final String storyKey, final String fromDate, final String toDate )
    {
        StoryTimeline storyTimeline = new StoryTimeline();
        try
        {
            if ( storyKey != null )
            {
                Set<String> issues = Sets.newHashSet();

                JiraMetricIssue storyIssue = jiraMetricDao.getJiraMetricIssueByKey( storyKey );

                storyTimeline = new StoryTimeline( storyIssue );

                Long from = Long.valueOf( fromDate );
                Long to = Long.valueOf( toDate );

                for ( final String gitCommit : storyTimeline.getGitCommits() )
                {
                    StashMetricIssue stashMetricIssue = stashMetricService.findStashMetricIssueById( gitCommit );
                    if ( stashMetricIssue != null )
                    {
                        storyTimeline.getCommits().add( stashMetricIssue );
                    }
                }
                populateEvents( storyTimeline, storyTimeline, new Date( from ), new Date( to ), issues );

                //            story.getIssues().remove( (JiraMetricIssue)story );
                storyTimeline.getIssues().remove( storyTimeline );
            }
        }
        catch ( Exception e )
        {
            logger.error( "Error generating story timeline..." );
        }
        return storyTimeline;
    }


    /**
     * populating events for story which are pulled from child issues for selected story
     */
    private void populateEvents( StoryTimeline child, StoryTimeline parent, Date fromDate, Date toDate,
                                 Set<String> issues )
    {
        if ( child != null )
        {
            issues.add( child.getIssueKey() );

            attachAnnotations( child, fromDate, toDate );

            for ( final JarvisLink link : child.getIssueLinks() )
            {
                if ( link.getDirection() == JarvisLink.Direction.INWARD && link.getLinkDirection() != null )
                {
                    JiraMetricIssue childIssue =
                            jiraMetricDao.getJiraMetricIssueByKey( link.getLinkDirection().getIssueKey() );
                    if ( childIssue != null && !issues.contains( childIssue.getIssueKey() ) )
                    {
                        StoryTimeline childTimeline = new StoryTimeline( childIssue );
                        for ( final String gitCommit : childTimeline.getGitCommits() )
                        {
                            StashMetricIssue stashMetricIssue =
                                    stashMetricService.findStashMetricIssueById( gitCommit );
                            if ( stashMetricIssue != null )
                            {
                                childTimeline.getCommits().add( stashMetricIssue );
                            }
                        }

                        populateEvents( childTimeline, parent, fromDate, toDate, issues );

                        parent.getIssues().add( childTimeline );
                    }
                }
            }
        }
    }


    private void attachAnnotations( StoryTimeline child, Date fromDate, Date toDate )
    {
        for ( final JiraIssueChangelog changelog : child.getChangelogList() )
        {
            Date eventDate = new Date( changelog.getChangeKey().getCreated() );
            if ( fromDate.compareTo( eventDate ) == -1 && eventDate.compareTo( toDate ) == -1 )
            {
                try
                {
                    for ( final IssueWorkLog issueWorkLog : child.getIssueWorkLogs() )
                    {
                        String workLogComment = issueWorkLog.getComment();
                        String uri;
                        String quote;
                        String comment;
                        int uriStart = workLogComment.indexOf( "[" );
                        int uriEnd = workLogComment.indexOf( "]" );

                        uri = workLogComment.substring( uriStart + 1, uriEnd );

                        //needed to check url validity
                        URL url = new URL( uri );

                        int quoteStart = workLogComment.indexOf( "{quote}" );
                        int quoteEnd = workLogComment.indexOf( "{quote}", quoteStart + 1 );

                        if ( uriEnd + 1 == quoteStart )
                        {
                            quote = workLogComment.substring( quoteStart + 7, quoteEnd );
                            comment = workLogComment.substring( quoteEnd + 7 );

                            Long captureId = 1L;

                            if ( !Strings.isNullOrEmpty( quote ) )
                            {
                                captureId *= quote.hashCode();
                            }

                            if ( Strings.isNullOrEmpty( comment ) )
                            {
                                captureId *= comment.hashCode();
                            }

                            if ( !Strings.isNullOrEmpty( uri ) )
                            {
                                captureId *= uri.hashCode();
                            }

                            Capture capture = new Capture();
                            capture.setCreated( new Date( issueWorkLog.getCreateDate() ) );
                            capture.setResearchSession( child.getIssueKey() );
                            capture.setText( comment );
                            capture.setUri( uri );
                            capture.setQuote( quote );
                            capture.setId( captureId );

                            child.getAnnotations().add( capture );
                        }
                    }
                }
                catch ( MalformedURLException ex )
                {
                    logger.error( "Invalid url" );
                }
                catch ( StringIndexOutOfBoundsException ex )
                {
                    logger.error( "Invalid annotation format in work log" );
                }
                catch ( Exception e )
                {
                    logger.error( "Couldn't retrieve research session for key " + child.getIssueKey(), e );
                }
            }
        }
    }


    /**
     * Constructs dependency tree view for target issue
     */
    private void buildStructureIssue( String issueKey, StructuredIssue structuredParent, final Set<String> issueKeys )
    {
        JiraMetricIssue issue = jiraMetricDao.getJiraMetricIssueByKey( issueKey );
        if ( issue != null && !issueKeys.contains( issueKey ) )
        {
            issueKeys.add( issueKey );
            StructuredIssue structuredIssue =
                    new StructuredIssue( issue.getIssueKey(), issue.getIssueId(), issue.getType().getName(),
                            issue.getSummary(), issue.getReporterName(), issue.getReporterName(),
                            issue.getAssigneeName(), issue.getUpdateDate().getTime(), issue.getCreationDate().getTime(),
                            issue.getStatus(), issue.getProjectKey(), issue.getDueDate().toString(),
                            issue.getRemoteLinks(), issue.getComponents(), issue.getLabels(), issue.getDescription(),
                            issue.getOriginalEstimateMinutes(), issue.getIssueWorkLogs() );

            // Set values for current issue progress
            assignIssueEstimate( structuredIssue, issue );

            structuredParent.addIssue( structuredIssue );

            List<String> linkedIssues = getChildIssues( issue );
            for ( final String linkedIssue : linkedIssues )
            {
                buildStructureIssue( linkedIssue, structuredIssue, issueKeys );
            }

            // Sum up overall progress for parent issue overall progress
            sumUpEstimates( structuredIssue, structuredParent );
        }
    }


    /**
     * This method selectively chooses issues from its links and returns list of issue keys which are relevant for
     * structure
     */
    private List<String> getChildIssues( JiraMetricIssue issue )
    {
        List<String> linkedIssues = Lists.newArrayList();
        for ( final JarvisLink link : issue.getIssueLinks() )
        {
            if ( "Child".equals( link.getLinkType().getName() ) )
            {
                linkedIssues.add( link.getLinkDirection().getIssueKey() );
            }
        }
        return linkedIssues;
    }


    public UserInfo getUserInfo( String username )
    {
        Map<String, StructuredProject> projectMap = Maps.newHashMap();
        getProjectStatsByUser( projectMap, username );

        UserInfo userInfo = new UserInfo();
        JiraUser jiraUser = jiraMetricDao.getJiraUserByUsername( username );

        userInfo.setUserId( jiraUser.getUserId() );
        userInfo.setDisplayName( jiraUser.getDisplayName() );
        userInfo.setEmail( jiraUser.getEmail() );
        userInfo.setUsername( jiraUser.getUsername() );
        userInfo.getProjects().addAll( projectMap.values() );
        userInfo.setRecentActivity( issueChangelogDao.getChangelogByUsername( userInfo.getDisplayName(), 10 ) );
        userInfo.setWorkLogsByWeeks( getWorkLogsByWeeks( username ) );
        userInfo.setCommitsByWeeks( getCommitsByWeeks( username ) );

        for ( final StructuredProject structuredProject : projectMap.values() )
        {
            sumUpProgressByStatus( structuredProject.getInProgressStatus(), userInfo.getInProgressStatus() );
            sumUpProgressByStatus( structuredProject.getDoneStatus(), userInfo.getDoneStatus() );
            sumUpProgressByStatus( structuredProject.getOpenStatus(), userInfo.getOpenStatus() );

            for ( final Map.Entry<String, Long> entry : structuredProject.getTotalIssuesSolved().entrySet() )
            {
                Long val = userInfo.getTotalIssuesSolved().get( entry.getKey() );
                if ( val == null )
                {
                    val = 0L;
                }
                val += entry.getValue();
                userInfo.getTotalIssuesSolved().put( entry.getKey(), val );
            }

            IssueProgress projectPoints = structuredProject.getStoryPoints();
            IssueProgress userPoints = userInfo.getStoryPoints();

            userPoints.setDone( userPoints.getDone() + projectPoints.getDone() );
            userPoints.setInProgress( userPoints.getInProgress() + projectPoints.getInProgress() );
            userPoints.setDone( userPoints.getOpen() + projectPoints.getOpen() );
        }

        return userInfo;
    }


    private Map<String, Long> getCommitsByWeeks( String username )
    {
        List<StashMetricIssue> stashMetricIssues = stashMetricService.getStashMetricIssuesByUsername( username, 10000 );
        Map<String, Long> commitsByWeeks = Maps.newHashMap();
        for ( final StashMetricIssue metricIssue : stashMetricIssues )
        {
            String week = new SimpleDateFormat( "w" ).format( new java.util.Date( metricIssue.getAuthorTimestamp() ) );
            Long commitCount = commitsByWeeks.get( week );
            if ( commitCount == null )
            {
                commitCount = 0L;
            }
            commitCount++;
            commitsByWeeks.put( week, commitCount );
        }
        return commitsByWeeks;
    }


    private Map<String, Long> getWorkLogsByWeeks( String username )
    {
        List<IssueWorkLog> jiraMetricIssues = jiraMetricDao.getUserWorkLogs( username, 10000 );
        Map<String, Long> workLogsByWeeks = Maps.newHashMap();
        for ( final IssueWorkLog metricIssue : jiraMetricIssues )
        {
            String week = new SimpleDateFormat( "w" ).format( new java.util.Date( metricIssue.getCreateDate() ) );
            Long loggedHours = workLogsByWeeks.get( week );
            if ( loggedHours == null )
            {
                loggedHours = 0L;
            }
            loggedHours += metricIssue.getTimeSpentSeconds() / 60;
            workLogsByWeeks.put( week, loggedHours );
        }
        return workLogsByWeeks;
    }


    private void getProjectStatsByUser( Map<String, StructuredProject> projectMap, String username )
    {
        List<JiraMetricIssue> assigneeIssues = jiraMetricDao.findJiraMetricIssuesByAssigneeName( username );
        for ( final JiraMetricIssue jiraMetricIssue : assigneeIssues )
        {
            StructuredProject structuredProject = projectMap.get( jiraMetricIssue.getProjectKey() );
            if ( structuredProject == null )
            {
                JiraProject jiraProject = jiraMetricDao.getProject( jiraMetricIssue.getProjectKey() );
                structuredProject =
                        new StructuredProject( jiraProject.getProjectId(), jiraProject.getName(), jiraProject.getKey(),
                                jiraProject.getDescription(), jiraProject.getProjectVersions() );
            }


            ProgressStatus progressStatus = new ProgressStatus( jiraMetricIssue.getOriginalEstimateMinutes(),
                    jiraMetricIssue.getRemainingEstimateMinutes(), jiraMetricIssue.getTimeSpentMinutes() );

            switch ( jiraMetricIssue.getStatus() )
            {
                case "Open":
                    sumUpProgressByStatus( progressStatus, structuredProject.getOpenStatus() );
                    break;
                case "In Progress":
                    sumUpProgressByStatus( progressStatus, structuredProject.getInProgressStatus() );
                    break;
                case "Done":
                    sumUpProgressByStatus( progressStatus, structuredProject.getDoneStatus() );
                case "Closed":
                case "Resolved":
                    Long val = structuredProject.getTotalIssuesSolved().get( jiraMetricIssue.getType().getName() );
                    if ( val == null )
                    {
                        val = 0L;
                    }
                    val += jiraMetricIssue.getTimeSpentMinutes();
                    structuredProject.getTotalIssuesSolved().put( jiraMetricIssue.getType().getName(), val );
                    break;
            }

            if ( "Story".equals( jiraMetricIssue.getType().getName() ) )
            {
                IssueProgress storyPoints = structuredProject.getStoryPoints();
                Random random = new Random();
                long val = ( random.nextInt( 4 ) + 1 ) * 2;
                switch ( jiraMetricIssue.getStatus() )
                {
                    case "Open":
                        storyPoints.setOpen( val + storyPoints.getOpen() );
                        break;
                    case "In Progress":
                        storyPoints.setInProgress( val + storyPoints.getInProgress() );
                        break;
                    case "Done":
                        storyPoints.setDone( val + storyPoints.getDone() );
                        break;
                }
            }

            projectMap.put( jiraMetricIssue.getProjectKey(), structuredProject );
        }
    }
}
