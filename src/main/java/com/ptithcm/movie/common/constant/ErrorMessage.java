package com.ptithcm.movie.common.constant;

public final class ErrorMessage {

    public static final String SUCCESS            = "Success";

    public static final String UNAUTHORIZED       = "Unauthorized";
    public static final String FORBIDDEN          = "Forbidden";
    public static final String TOKEN_EXPIRED      = "Token expired";

    public static final String EMAIL_EXISTS       = "Email already registered";
    public static final String EMAIL_NOT_VERIFIED = "Email has not been verified";
    public static final String BANNED_ACCOUNT     = "Account was banned";
    public static final String BAD_CREDENTIALS    = "Wrong email or password";

    public static final String INTERNAL_SERVER    = "Internal server error";
    public static final String DATABASE_ERROR     = "Database error";

    private ErrorMessage() {}
}
