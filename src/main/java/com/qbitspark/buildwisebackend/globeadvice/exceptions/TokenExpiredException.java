package com.qbitspark.buildwisebackend.globeadvice.exceptions;

public class TokenExpiredException extends Exception{
    public TokenExpiredException(String message){
        super(message);
    }
}
