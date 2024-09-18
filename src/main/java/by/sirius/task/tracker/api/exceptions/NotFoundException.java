package by.sirius.task.tracker.api.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CustomBaseException {
    public NotFoundException(String message, HttpStatus status) {
        super(message, status);
    }
}
