package com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions;

public class TokenExpiredException extends Exception{
    public TokenExpiredException(String message){
        super(message);
    }
}
