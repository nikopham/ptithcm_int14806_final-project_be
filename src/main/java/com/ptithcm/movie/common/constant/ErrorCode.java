package com.ptithcm.movie.common.constant;

public final class ErrorCode {

    /* ===== thành công ===== */
    public static final int SUCCESS              = 0;

    /* ===== lỗi xác thực ===== */
    public static final int UNAUTHORIZED         = 1001;
    public static final int FORBIDDEN            = 1003;
    public static final int TOKEN_EXPIRED        = 1004;

    /* ===== lỗi người dùng ===== */
    public static final int EMAIL_EXISTS         = 2001;
    public static final int EMAIL_NOT_VERIFIED   = 2002;

    public static final int BAD_CREDENTIALS      = 2003;
    public static final int TOO_MANY_REQUESTS    = 2004;
    public static final int MAIL_SEND_ERROR      = 5002;


    /* ===== lỗi hệ thống ===== */
    public static final int INTERNAL_SERVER      = 5000;
    public static final int DATABASE_ERROR       = 5001;

    private ErrorCode() {}
}