package by.sirius.task.tracker.api.exceptions;

import org.springframework.http.HttpStatus;

public class BadRequestException extends CustomBaseException {
    public BadRequestException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
