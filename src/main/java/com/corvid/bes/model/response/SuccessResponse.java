package com.corvid.bes.model.response;

public class SuccessResponse extends BaseResponse{
    public SuccessResponse(int httpStatus, int responseCode, String message){
        this.httpStatus = httpStatus;
        this.responseCode = responseCode;
        this.message = message;
        this.responseType = ResponseType.SUCCESS;
    }

    public SuccessResponse(int responseCode, String message){
        this.httpStatus = 200;
        this.responseCode = responseCode;
        this.message = message;
        this.responseType = ResponseType.SUCCESS;
    }

    public SuccessResponse(String message){
        this.httpStatus = 200;
        this.responseCode = 1000;
        this.message = message;
        this.responseType = ResponseType.SUCCESS;
    }

    public SuccessResponse(){
        this.httpStatus = 200;
        this.responseCode = 1000;
        this.message = "Done!";
        this.responseType = ResponseType.SUCCESS;
    }
}
