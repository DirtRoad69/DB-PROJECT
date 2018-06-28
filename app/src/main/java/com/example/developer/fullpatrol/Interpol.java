package com.example.developer.fullpatrol;

public class Interpol {
    private static Interpol instance;
    private static final long THRESHOLD = 1000;
    private long nextTime;
    private boolean isExecuting, isOutOfMainActivity;

    private Interpol(){ this. isExecuting = false; this.isOutOfMainActivity = false;}

    public boolean execute(long time){
        return (Math.abs(time - nextTime) <= THRESHOLD);
    }

    public void setNextTime(long nextTime) {
        this.nextTime = nextTime;
    }

    public static long getNextTimePatrol(long pastTime, long interval){
        long current = System.currentTimeMillis();
        long diff = current - pastTime;
        if(diff < 0)
            return  -1;

        long  div = diff % interval;
        long nextTime = (current - div) + interval;
        return  nextTime;
    }

    public void setExecuting(boolean pIsExecuting){
        this.isExecuting = pIsExecuting;
    }

    public boolean isExecuting() {
        return isExecuting;
    }

    public void setOutOfMainActivity(boolean outOfMainActivity) {
        isOutOfMainActivity = outOfMainActivity;
    }

    public boolean isOutOfMainActivity() {
        return isOutOfMainActivity;
    }

    public static Interpol getInstance(){
        if(instance == null){
            instance = new Interpol();
        }
        return instance;
    }

}
