package at.jku.dke.bigkgolap.shared.exceptions;

public class GraphNotAvailableException extends RuntimeException {
    public GraphNotAvailableException(String message) {
        super(message);
    }

    public GraphNotAvailableException(String message, Throwable exception) {
        super(message, exception);
    }
}
