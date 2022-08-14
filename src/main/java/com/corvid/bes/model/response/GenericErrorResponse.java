package com.corvid.bes.model.response;

public class GenericErrorResponse extends ErrorResponse{
    public GenericErrorResponse(int httpStatus, int status, String message){
        super(httpStatus, status, message);
    }

    public GenericErrorResponse(int status, String message){
        super(500, status, message);
    }
}
