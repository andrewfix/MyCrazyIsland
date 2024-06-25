package com.project.andrew.exceptions;

public class OrganismlNotInCellException extends RuntimeException {
    public OrganismlNotInCellException() {
        super("Местоположение особи не задано (field currentCell == null)");
    }
}
