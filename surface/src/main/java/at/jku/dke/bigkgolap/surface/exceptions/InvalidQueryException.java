package at.jku.dke.bigkgolap.surface.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidQueryException extends ResponseStatusException {

    public InvalidQueryException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public InvalidQueryException(String message, Exception cause) {
        super(HttpStatus.BAD_REQUEST, message + " - Cause: " + cause.getMessage());
    }
}
