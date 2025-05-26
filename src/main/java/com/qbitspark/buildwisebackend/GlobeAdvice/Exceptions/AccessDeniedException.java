package com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions;

public class AccessDeniedException extends Exception{
    public AccessDeniedException(String message){
        super(message);
    }
}
