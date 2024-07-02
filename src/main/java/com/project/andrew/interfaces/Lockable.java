package com.project.andrew.interfaces;

public interface Lockable {
    void unlock();
    boolean tryLock();
    boolean isLocked();
}