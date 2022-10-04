package at.jku.dke.bigkgolap.surface.exceptions;

public class GraphQueryTimeoutException extends RuntimeException {
    public GraphQueryTimeoutException(String message) {
        super(message);
    }

    public GraphQueryTimeoutException(String message, Throwable exception) {
        super(message, exception);
    }
}
