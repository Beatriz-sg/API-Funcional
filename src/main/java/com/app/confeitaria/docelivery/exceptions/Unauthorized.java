package com.app.confeitaria.docelivery.exceptions;

public class Unauthorized extends RuntimeException{
    public Unauthorized(String message){
        super(message);
    }
}
