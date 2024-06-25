package com.project.andrew.exceptions;

public class ReproductionException extends RuntimeException {
    public ReproductionException(String message) {
        super(message + " Ошибка при размножении!");
    }
}