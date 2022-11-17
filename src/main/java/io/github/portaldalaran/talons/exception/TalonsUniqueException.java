package io.github.portaldalaran.talons.exception;

/**
 * @author aohee@163.com
 */
public class TalonsUniqueException extends RuntimeException {
    private String value;
    private String message;

    public TalonsUniqueException(String message, String value) {
        this.message = message;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
