package com.echo.flaginfo.minirpc.exception;
public class MiniRpcException extends RuntimeException {
    public MiniRpcException() {
    }

    public MiniRpcException(String message) {
        super(message);
    }

    public MiniRpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public MiniRpcException(Throwable cause) {
        super(cause);
    }

    public MiniRpcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}