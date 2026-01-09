package com.ptithcm.movie.common.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ptithcm.movie.common.constant.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Kết quả chuẩn cho mọi API/service.
 *
 * @param <T> Kiểu dữ liệu payload.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceResult implements Serializable {
    @Serial
    private static final long serialVersionUID = -6513881169785677008L;

    private Boolean success;
    private Integer code;
    private String message;
    private Object data;

    public static ServiceResult Success() {
        return new ServiceResult(true);
    }

    public static ServiceResult Failure() {
        return new ServiceResult(false);
    }

    public ServiceResult(Boolean success) {
        this.success = success;
    }

    public ServiceResult code(int errorCode) {
        this.code = errorCode;
        return this;
    }

    public ServiceResult data(Object data) {
        this.data = data;
        return this;
    }

    public ServiceResult message(String message) {
        this.message = message;
        return this;
    }

    @JsonIgnore
    public boolean isSuccessCode() {
        return Integer.valueOf(ErrorCode.SUCCESS).equals(this.code);
    }

}
