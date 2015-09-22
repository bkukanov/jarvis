package org.safehaus.service;


import java.util.List;

import org.safehaus.analysis.StashUserMetricInfo;


/**
 * Created by kisik on 11.09.2015.
 */

public interface StashUserMetricService
{

    public List<StashUserMetricInfo> getStashUserMetricInfoListByDevId( String developerId );

    public StashUserMetricInfo getStashUserMetricInfo( String developerId, long timestamp );

    public List<StashUserMetricInfo> getStashUserMetricInfoListByMonth( long timestamp );
}
