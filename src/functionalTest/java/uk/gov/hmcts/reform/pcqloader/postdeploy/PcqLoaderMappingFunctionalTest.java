package uk.gov.hmcts.reform.pcqloader.postdeploy;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcqloader.config.TestApplicationConfiguration;
import uk.gov.hmcts.reform.pcqloader.helper.PayloadMappingHelper;
import uk.gov.hmcts.reform.pcqloader.services.PcqBackendService;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.pcq.commons.tests.utils.TestUtils.jsonStringFromFile;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplicationConfiguration.class)
@ActiveProfiles("functional")
@Slf4j
public class PcqLoaderMappingFunctionalTest extends PcqLoaderTestBase {

    private static final String FAIL_ASSERT_MSG = "Method call failed.";

    @Value("${pcqBackendUrl}")
    private String pcqBackendUrl;

    @Autowired
    private PayloadMappingHelper payloadMappingHelper;

    @Autowired
    private PcqBackendService pcqBackendService;

    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void testAllSuccessfulPcqInvocation() throws IOException {
        String metaDataPayLoad = jsonStringFromFile("JsonTestFiles/successMetaFile.json");
        PcqAnswerRequest mappedAnswers = null;
        try {
            mappedAnswers = payloadMappingHelper.mapPayLoadToPcqAnswers(metaDataPayLoad);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(FAIL_ASSERT_MSG, e);
        }

        Assertions.assertNotNull(mappedAnswers.getDcnNumber(), "DCN Number is invalid");
        if (mappedAnswers != null) {
            mappedAnswers.setPcqId(UUID.randomUUID().toString());
            mappedAnswers.setDcnNumber("DCN " + UUID.randomUUID().toString());

            ResponseEntity responseEntity = pcqBackendService.submitAnswers(mappedAnswers);
            Assertions.assertEquals(201, responseEntity.getStatusCode().value(),
                                    "Invalid Response");

            PcqAnswerRequest answerRecord = getTestAnswerRecord(mappedAnswers.getPcqId(), pcqBackendUrl);
            Assertions.assertNotNull(answerRecord, "Answer Record is null");
            checkAssertionsOnResponse(answerRecord, mappedAnswers);
            deleteTestAnswerRecord(answerRecord, pcqBackendUrl);
        }
    }

    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void testAllMultiplePcqInvocation() throws IOException {
        String metaDataPayLoad = jsonStringFromFile("JsonTestFiles/multipleReligionMetaFile.json");
        PcqAnswerRequest mappedAnswers = null;
        try {
            mappedAnswers = payloadMappingHelper.mapPayLoadToPcqAnswers(metaDataPayLoad);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(FAIL_ASSERT_MSG, e);
        }

        Assertions.assertNotNull(mappedAnswers.getDcnNumber(), "DCN Number is invalid");
        if (mappedAnswers != null) {
            mappedAnswers.setPcqId(UUID.randomUUID().toString());
            mappedAnswers.setDcnNumber("DCN " + UUID.randomUUID().toString());

            ResponseEntity responseEntity = pcqBackendService.submitAnswers(mappedAnswers);
            Assertions.assertEquals(201, responseEntity.getStatusCode().value(),
                                    "Invalid Response");

            PcqAnswerRequest answerRecord = getTestAnswerRecord(mappedAnswers.getPcqId(), pcqBackendUrl);
            Assertions.assertNotNull(answerRecord, "Answer Record is null");
            checkAssertionsOnResponse(answerRecord, mappedAnswers);
            deleteTestAnswerRecord(answerRecord, pcqBackendUrl);
        }
    }


}
