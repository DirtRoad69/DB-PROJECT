package com.example.developer.objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Site {

    private static Site siteInstance = null;
    SitePoints sitePointsInstance = null;
    private String siteId, siteName, area;
    private HashMap<String, Integer> patrolInfoTimes;
    private HashMap<String, String> patrolClock;


    private Site(){
        this.patrolInfoTimes.put("minTime", 0);
        this.patrolInfoTimes.put("maxTime", 0);
        this.patrolInfoTimes.put("intervalTimer", 0);
        this.patrolInfoTimes.put("startDelay", 0);
        this.patrolClock.put("startPatrolTime", "07:00");
        this.patrolClock.put("startEndPoint", "");
        this.patrolClock.put("endPatrolTime", "16:00");


    }
    public void init(String siteId,String siteName,String area){
        this.siteId = siteId;
        this.siteName = siteName;
        this.area = area;
    }
    public HashMap<String, Integer> getPatrolInfoTimes() {
        return patrolInfoTimes;
    }

    public void setPatrolClock(HashMap<String, String> patrolClock) {
        this.patrolClock = patrolClock;
    }

    public void setPatrolInfoTimes(HashMap<String, Integer> patrolInfoTimes) {
        this.patrolInfoTimes = patrolInfoTimes;
    }

    public Map<String, String> getPatrolClock() {
        return patrolClock;
    }

    public static Site getSiteInstance(){
        if(siteInstance == null)
            siteInstance = new Site();
        return siteInstance;
    }
    public SitePoints getsitePointsInstance(){
        if (this.sitePointsInstance == null){
            this.sitePointsInstance = new SitePoints();
        }
        return this.sitePointsInstance;
    }
    public void setPatrolCollection(List<PatrolPointConfig> pointsCol){
        getsitePointsInstance().setPointCollection(pointsCol);
    }
    public  List<PatrolPointConfig> getPatrolCollection(){

        return getsitePointsInstance().getPointCollection();
    }

    private class SitePoints {

        private List<PatrolPointConfig> pointCollection;

        private SitePoints(){

        }


        public void init(List<PatrolPointConfig> pointCollection){
            this.pointCollection.addAll(pointCollection) ;
        }

        public List<PatrolPointConfig> getPointCollection() {
            return pointCollection;
        }

        public void setPointCollection(List<PatrolPointConfig> pointCollection) {
            if(!this.pointCollection.isEmpty())
                this.pointCollection.clear();
            this.pointCollection.addAll(pointCollection);
        }


    }

}
