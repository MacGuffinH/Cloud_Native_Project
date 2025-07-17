package com.example.cloudnativeproject.controller.limit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 请求限流异常
 * 当请求频率超过限制时抛出此异常
 * 自动返回HTTP 429 Too Many Requests状态码
 */
@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS, reason = "Too many requests - Rate limit exceeded")
public class RequestLimitException extends RuntimeException {

    public RequestLimitException() {
        super("Request rate limit exceeded");
    }

    public RequestLimitException(String message) {
        super(message);
    }

    public RequestLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
