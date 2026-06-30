package uk.gov.hmcts.reform.pcqloader.services;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;

import java.util.Map;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PcqBackendService {

    ResponseEntity<Map<String, String>> submitAnswers(PcqAnswerRequest answerRequest);

}
