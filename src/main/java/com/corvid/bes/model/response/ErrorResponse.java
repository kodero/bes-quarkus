package com.corvid.bes.model.response;

public class ErrorResponse extends BaseResponse{
    public ErrorResponse(int httpStatus, int responseCode, String message){
        this.httpStatus = httpStatus;
        this.responseCode = responseCode;
        this.message = message;
        this.responseType = ResponseType.ERROR;
    }

    public ErrorResponse(int responseCode, String message){
        this.httpStatus = 500;
        this.responseCode = responseCode;
        this.message = message;
        this.responseType = ResponseType.ERROR;
    }

    /* Default generic error msg */
    public ErrorResponse(String message){
        this.httpStatus = 500;
        this.responseCode = 2000;
        this.message = message;
        this.responseType = ResponseType.ERROR;
    }
}
