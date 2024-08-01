package uk.gov.hmcts.reform.pcqloader.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {"PCQ_BACKEND_URL:http://127.0.0.1:4554"})
@Slf4j
public class PcqBackendIntegrationTest extends SpringBootIntegrationTest {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String MEDIA_TYPE = "application/json";
    private static final String CONNECTION_HEADER_VAL = "close";
    private static final String TEST_PCQ_ID = "UNIT_TEST_PCQ_1";
    private static final String TEST_DCN_NUMBER = "UNIT_TEST_DCN_1";
    private static final String HEADER_VALUE = "PCQ Loader Service";
    private static final String HEADER_CO_RELATION_KEY = "X-Correlation-Id";
    private static final String RESPONSE_ENTITY_NULL_MSG = "Response is Null";

    @Rule
    public WireMockRule pcqBackendService = new WireMockRule(WireMockConfiguration.options().port(4554));



    @Test
    public void testSubmitAnswersSuccess() {
        pcqSubmitAnswersWireMockSuccess();

        ResponseEntity responseEntity = pcqBackendServiceImpl.submitAnswers(generateTestRequest());
        assertNotNull(RESPONSE_ENTITY_NULL_MSG, responseEntity);

    }

    @Test
    public void testSubmitAnswersInvalidRequest() {
        pcqSubmitAnswersWireMockInvalid();

        ResponseEntity responseEntity = pcqBackendServiceImpl.submitAnswers(generateTestRequest());
        assertNotNull(RESPONSE_ENTITY_NULL_MSG, responseEntity);

    }

    @Test
    public void testSubmitAnswersUnknownError() {
        pcqSubmitAnswersWireMockUnknownError();

        ResponseEntity responseEntity = pcqBackendServiceImpl.submitAnswers(generateTestRequest());
        assertNotNull(RESPONSE_ENTITY_NULL_MSG, responseEntity);

    }


    private PcqAnswerRequest generateTestRequest() {
        PcqAnswerRequest pcqAnswerRequest = new PcqAnswerRequest();
        pcqAnswerRequest.setPcqId(TEST_PCQ_ID);
        pcqAnswerRequest.setDcnNumber(TEST_DCN_NUMBER);
        pcqAnswerRequest.setPartyId("PaperForm");
        PcqAnswers testAnswers = new PcqAnswers();
        testAnswers.setDobProvided(0);
        pcqAnswerRequest.setPcqAnswers(testAnswers);

        return pcqAnswerRequest;
    }

    private void pcqSubmitAnswersWireMockSuccess() {
        pcqBackendService.stubFor(post(urlPathMatching("/pcq/backend/submitAnswers"))
                                      .withRequestBody(equalToJson(jsonFromObject(generateTestRequest())))
                                      .withHeader(HEADER_CO_RELATION_KEY, containing(HEADER_VALUE))
                                      .willReturn(aResponse()
                                                      .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                                                      .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                                                      .withStatus(200)
                                                      .withBody(getResponseBody("200", "Successfully Created"))));
    }

    private void pcqSubmitAnswersWireMockInvalid() {
        pcqBackendService.stubFor(post(urlPathMatching("/pcq/backend/submitAnswers"))
                                      .withRequestBody(equalToJson(jsonFromObject(generateTestRequest())))
                                      .withHeader(HEADER_CO_RELATION_KEY, containing(HEADER_VALUE))
                                      .willReturn(aResponse()
                                                      .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                                                      .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                                                      .withStatus(400)
                                                      .withBody(getResponseBody("400", "Invalid Request"))));
    }

    private void pcqSubmitAnswersWireMockUnknownError() {
        pcqBackendService.stubFor(post(urlPathMatching("/pcq/backend/submitAnswers"))
                                      .withRequestBody(equalToJson(jsonFromObject(generateTestRequest())))
                                      .withHeader(HEADER_CO_RELATION_KEY, containing(HEADER_VALUE))
                                      .willReturn(aResponse()
                                                      .withHeader(CONTENT_TYPE, MEDIA_TYPE)
                                                      .withHeader(HttpHeaders.CONNECTION, CONNECTION_HEADER_VAL)
                                                      .withStatus(500)
                                                      .withBody(getResponseBody("500", "Unknown error occurred"))));
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private String jsonFromObject(PcqAnswerRequest pcqAnswerRequest) {
        String json = "{}";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            json = objectMapper.writeValueAsString(pcqAnswerRequest);
        } catch (JsonProcessingException e) {
            log.error("Json processing exception " + e.getMessage());
        }

        return json;
    }

    private String getResponseBody(String responseStatusCode, String responseStatus) {
        return "{\n"
            + "    \"pcqId\": \"" + TEST_PCQ_ID + "\",\n"
            + "    \"responseStatusCode\": \"" + responseStatusCode + "\",\n"
            + "    \"responseStatus\": \"" + responseStatus + "\"\n"
            + "}";
    }
}
