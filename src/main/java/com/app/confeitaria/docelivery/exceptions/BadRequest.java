package com.app.confeitaria.docelivery.exceptions;

public class BadRequest  extends RuntimeException{
    public BadRequest(String message){
        super(message);
    }
}
