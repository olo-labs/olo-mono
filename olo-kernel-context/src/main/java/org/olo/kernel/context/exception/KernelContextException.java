package org.olo.kernel.context.exception;

public class KernelContextException extends RuntimeException {

    public KernelContextException(String message) {
        super(message);
    }

    public KernelContextException(String message, Throwable cause) {
        super(message, cause);
    }
}
