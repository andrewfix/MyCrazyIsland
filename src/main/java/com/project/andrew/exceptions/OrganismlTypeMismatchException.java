package com.project.andrew.exceptions;

public class OrganismlTypeMismatchException extends RuntimeException {
    public OrganismlTypeMismatchException() {
        super("Несовместимые типы организмов! (Ошибка репродукции)");
    }
}
