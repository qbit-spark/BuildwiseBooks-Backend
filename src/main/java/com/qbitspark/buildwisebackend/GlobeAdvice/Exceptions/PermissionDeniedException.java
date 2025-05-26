package com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions;

public class PermissionDeniedException extends Exception{
    public PermissionDeniedException(String message){
        super(message);
    }
}
