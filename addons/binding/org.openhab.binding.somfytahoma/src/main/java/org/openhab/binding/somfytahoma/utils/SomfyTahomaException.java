package org.openhab.binding.somfytahoma.utils;

/**
 * Created by Ondrej Pecta on 14.7.2017.
 */
public class SomfyTahomaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SomfyTahomaException(String message) {
        super(message);
    }

    public SomfyTahomaException(final Throwable cause) {
        super(cause);
    }

    public SomfyTahomaException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
