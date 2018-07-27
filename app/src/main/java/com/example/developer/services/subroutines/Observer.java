package com.example.developer.services.subroutines;

public abstract class Observer {
    protected Subject subject;
    public abstract void update(int location);
}
