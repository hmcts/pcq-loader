package uk.gov.hmcts.reform.pcqloader.exceptions;

import java.io.Serial;

public class ZipProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 4L;

    public ZipProcessingException(String message) {
        super(message);
    }

    public ZipProcessingException(String message, Exception exp) {
        super(message, exp);
    }
}
