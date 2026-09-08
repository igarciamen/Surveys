package com.igarciamen.surveys.model;

public enum SurveyAccess {
    OPEN,        // anyone can respond
    PASSWORD,    // requires the survey password
    RESTRICTED   // requires an authenticated account
}
