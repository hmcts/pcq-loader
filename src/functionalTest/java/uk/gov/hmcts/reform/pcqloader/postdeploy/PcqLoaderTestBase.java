package uk.gov.hmcts.reform.pcqloader.postdeploy;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@Slf4j
public class PcqLoaderTestBase {


    protected PcqAnswerRequest getTestAnswerRecord(String pcqId, String apiUrl) throws IOException {
        return getResponseFromBackend(apiUrl, pcqId);
    }

    protected PcqAnswerRequest getResponseFromBackend(String apiUrl, String pcqId) {
        WebClient pcqWebClient = createPcqBackendWebClient(apiUrl);
        WebClient.RequestHeadersSpec requestBodySpec = pcqWebClient.get().uri(URI.create(
            apiUrl + "/pcq/backend/getAnswer/" + pcqId));
        PcqAnswerRequest response3 = requestBodySpec.retrieve().bodyToMono(PcqAnswerRequest.class).block();
        log.info("Returned response " + response3.toString());
        return response3;
    }

    private WebClient createPcqBackendWebClient(String apiUrl) {
        return WebClient
            .builder()
            .baseUrl(apiUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-Correlation-Id", "Pcq Loader Functional Test")
            .defaultUriVariables(Collections.singletonMap("url", apiUrl))
            .build();
    }

    protected int countBlobs(PagedIterable<BlobItem> blobs) {
        int blobCount = 0;
        for (BlobItem blobItem : blobs) {
            if (null != blobItem) {
                blobCount++;
            }
        }
        return blobCount;
    }

    @SuppressWarnings({"PMD.ConfusingTernary"})
    protected void checkAssertionsOnResponse(PcqAnswerRequest responseRecord,
                                             PcqAnswerRequest answerRequest) {
        assertEquals("PCQId not matching", responseRecord.getPcqId(), answerRequest.getPcqId());
        assertEquals("DCN Number not matching", responseRecord.getDcnNumber(), answerRequest.getDcnNumber());
        assertEquals("Form Id not matching", responseRecord.getFormId(), answerRequest.getFormId());
        assertEquals("PartyId not matching", responseRecord.getPartyId(),
                     answerRequest.getPartyId());
        assertEquals("Channel not matching", responseRecord.getChannel(),
                     answerRequest.getChannel());
        assertEquals("ServiceId not matching", responseRecord.getServiceId(),
                     answerRequest.getServiceId());
        assertEquals("Actor not matching", responseRecord.getActor(),
                     answerRequest.getActor());
        assertEquals("VersionNumber not matching", responseRecord.getVersionNo(),
                     answerRequest.getVersionNo());

        PcqAnswers answers = responseRecord.getPcqAnswers();

        assertEquals("DobProvided not matching", answers.getDobProvided(),
                     answerRequest.getPcqAnswers().getDobProvided());
        if (answers.getDob() != null) {
            assertEquals("Dob not matching", answers.getDob(), answerRequest.getPcqAnswers().getDob());
        } else {
            assertNull("Dob not matching", answerRequest.getPcqAnswers().getDob());
        }
        assertEquals("LanguageMain not matching", answers.getLanguageMain(),
                     answerRequest.getPcqAnswers().getLanguageMain());
        assertEquals("OtherLanguage not matching", answers.getLanguageOther(),
                     answerRequest.getPcqAnswers().getLanguageOther());
        assertEquals("EnglishLanguageLevel not matching", answers.getEnglishLanguageLevel(),
                     answerRequest.getPcqAnswers().getEnglishLanguageLevel());
        assertEquals("Sex not matching", answers.getSex(),
                     answerRequest.getPcqAnswers().getSex());
        assertEquals("Gender Different not matching", answers.getGenderDifferent(),
                     answerRequest.getPcqAnswers().getGenderDifferent());
        assertEquals("Other Gender not matching", answers.getGenderOther(),
                     answerRequest.getPcqAnswers().getGenderOther());
        assertEquals("Sexuality not matching", answers.getSexuality(),
                     answerRequest.getPcqAnswers().getSexuality());
        assertEquals("Sexuality Other not matching", answers.getSexualityOther(),
                     answerRequest.getPcqAnswers().getSexualityOther());
        assertEquals("Marriage not matching", answers.getMarriage(),
                     answerRequest.getPcqAnswers().getMarriage());
        assertEquals("Ethnicity not matching", answers.getEthnicity(),
                     answerRequest.getPcqAnswers().getEthnicity());
        assertEquals("Other Ethnicity not matching", answers.getEthnicityOther(),
                     answerRequest.getPcqAnswers().getEthnicityOther());
        assertEquals("Religion not matching", answers.getReligion(),
                     answerRequest.getPcqAnswers().getReligion());
        assertEquals("Religion Other not matching", answers.getReligionOther(),
                     answerRequest.getPcqAnswers().getReligionOther());
        assertEquals("Disability Conditions not matching", answers.getDisabilityConditions(),
                     answerRequest.getPcqAnswers().getDisabilityConditions());
        assertEquals("Disability Impact not matching", answers.getDisabilityImpact(),
                     answerRequest.getPcqAnswers().getDisabilityImpact());
        assertEquals("Disability Vision not matching", answers.getDisabilityVision(),
                     answerRequest.getPcqAnswers().getDisabilityVision());
        assertEquals("Disability Hearing not matching", answers.getDisabilityHearing(),
                     answerRequest.getPcqAnswers().getDisabilityHearing());
        assertEquals("Disability Mobility not matching", answers.getDisabilityMobility(),
                     answerRequest.getPcqAnswers().getDisabilityMobility());
        assertEquals("Disability Dexterity not matching", answers.getDisabilityDexterity(),
                     answerRequest.getPcqAnswers().getDisabilityDexterity());
        assertEquals("Disability Learning not matching", answers.getDisabilityLearning(),
                     answerRequest.getPcqAnswers().getDisabilityLearning());
        assertEquals("Disability Memory not matching", answers.getDisabilityMemory(),
                     answerRequest.getPcqAnswers().getDisabilityMemory());
        assertEquals("Disability Mental Health not matching", answers.getDisabilityMentalHealth(),
                     answerRequest.getPcqAnswers().getDisabilityMentalHealth());
        assertEquals("Disability Stamina not matching", answers.getDisabilityStamina(),
                     answerRequest.getPcqAnswers().getDisabilityStamina());
        assertEquals("Disability Social not matching", answers.getDisabilitySocial(),
                     answerRequest.getPcqAnswers().getDisabilitySocial());
        assertEquals("Disability Other not matching", answers.getDisabilityOther(),
                     answerRequest.getPcqAnswers().getDisabilityOther());
        assertEquals("Disability Other Details not matching", answers.getDisabilityConditionOther(),
                     answerRequest.getPcqAnswers().getDisabilityConditionOther());
        assertEquals("Disability None not matching", answers.getDisabilityNone(),
                     answerRequest.getPcqAnswers().getDisabilityNone());
        assertEquals("Pregnancy not matching", answers.getPregnancy(),
                     answerRequest.getPcqAnswers().getPregnancy());

    }

}
