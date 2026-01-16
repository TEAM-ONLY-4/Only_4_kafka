package com.example.only4_kafka.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SuccessResponse<T> {

    private final boolean success = true;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(data);
    }

    public ResponseEntity<SuccessResponse<T>> asHttp(HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus).body(this);
    }

    public SuccessResponse(T data) {
        this.data = data;
    }
}
