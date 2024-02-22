package io.github.liuziyuan.retrofit.core.exception;

/**
 * @author liuziyuan
 */
public class RetrofitStarterException extends RuntimeException {

    public RetrofitStarterException(String message) {
        super(message);
    }

    public RetrofitStarterException(String message, Throwable cause) {
        super(message, cause);
    }
}
