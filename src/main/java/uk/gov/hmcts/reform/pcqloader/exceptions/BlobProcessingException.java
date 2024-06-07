package uk.gov.hmcts.reform.pcqloader.exceptions;

import java.io.Serial;

public class BlobProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 4L;

    public BlobProcessingException(String message) {
        super(message);
    }

    public BlobProcessingException(String message, Exception exp) {
        super(message, exp);
    }
}
