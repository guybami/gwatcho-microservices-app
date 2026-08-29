package com.gwatcho.userservice.exception;

public class BadEmailException extends RuntimeException{

    public BadEmailException(String email) {

        super("Invalid email " + email + " ! Please check");
    }
}
