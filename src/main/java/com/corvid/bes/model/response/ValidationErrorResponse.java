package com.corvid.bes.model.response;

import java.util.Map;

public class ValidationErrorResponse extends ErrorResponse{

    Map<String, String> violations;

    public ValidationErrorResponse(int httpStatus, int responseCode, String message, Map<String, String> violations){
        super(httpStatus, responseCode, message);
        this.violations = violations;
    }

    public ValidationErrorResponse(int responseCode, String message, Map<String, String> violations){
        super(412, responseCode, message);
        this.violations = violations;
    }

    public ValidationErrorResponse(String message, Map<String, String> violations){
        super(412, 2001, message);
        this.violations = violations;
    }
}
