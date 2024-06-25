package com.project.andrew.interfaces;

import com.project.andrew.AbstractIslandOrganism;

public interface Eater<T extends AbstractIslandOrganism> {
    boolean eat(T food);
}