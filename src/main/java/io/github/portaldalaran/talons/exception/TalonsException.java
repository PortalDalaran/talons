package io.github.portaldalaran.talons.exception;

/**
 * @author david
 */
public class TalonsException extends RuntimeException {
    public TalonsException() {
        super();
    }

    public TalonsException(String s) {
        super(s);
    }

    public TalonsException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public TalonsException(Throwable throwable) {
        super(throwable);
    }

    protected TalonsException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
