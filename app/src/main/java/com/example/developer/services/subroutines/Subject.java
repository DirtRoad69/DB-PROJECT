package com.example.developer.services.subroutines;


public abstract class Subject {


    private String name;
    public Subject(String name){
        this.name = name;
    }

    abstract public void stateChanged(int state);

    public abstract void attach(Observer observer);

    public abstract void notifyAllObservers();
    public String getName(){
        return name;
    }

}
