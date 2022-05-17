package uk.gov.hmcts.reform.pcqloader.services.impl;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.pcq.commons.controller.feign.PcqBackendFeignClient;
import uk.gov.hmcts.reform.pcq.commons.exception.ExternalApiException;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
class PcqBackendServiceImplTest {

    private static final String HEADER_VALUE = "Test_Loader";
    private static final String RESPONSE_INCORRECT = "Not the correct response";
    private static final String TEST_PCQ_ID = "UNIT_TEST_PCQ_1";
    private static final String TEST_DCN_NUMBER = "UNIT_TEST_DCN_1";
    private static final String EXPECTED_MSG_1 = "PcqIds don't match";
    private static final String EXPECTED_MSG_2 = "Status code not correct";
    private static final String EXPECTED_MSG_3 = "Status not correct";
    private static final String PCQ_ID_KEY = "pcqId";
    private static final String STATUS_CODE_KEY = "responseStatusCode";
    private static final String STATUS_KEY = "responseStatus";
    private static final String INVALID_REQUEST = "Invalid Request";

    @Mock
    private PcqBackendFeignClient mockPcqBackendFeignClient;

    @InjectMocks
    private PcqBackendServiceImpl pcqBackendService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(pcqBackendService, "jwtSecretKey", "TestKey");
        ReflectionTestUtils.setField(pcqBackendService, "coRelationHeader", HEADER_VALUE);
    }

    @Test
    void test201SuccessResponse() {
        PcqAnswerRequest testRequest = generateTestRequest();
        String responseBody = getResponseBody("201", "Successfully created");

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenReturn(Response.builder().request(mock(
            Request.class)).body(responseBody, Charset.defaultCharset()).status(201).build());

        ResponseEntity responseEntity = pcqBackendService.submitAnswers(testRequest);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof Map<?,?>);
        Map<?,?> responseBodyMap = (Map<?, ?>) responseEntity.getBody();
        assertEquals(EXPECTED_MSG_1, TEST_PCQ_ID, responseBodyMap.get(PCQ_ID_KEY));
        assertEquals(EXPECTED_MSG_2, "201", responseBodyMap.get(STATUS_CODE_KEY));
        assertEquals(EXPECTED_MSG_3, "Successfully created", responseBodyMap.get(STATUS_KEY));


        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));

    }

    @Test
    void testInvalidRequestResponse400() {
        PcqAnswerRequest testRequest = generateTestRequest();
        String responseBody = getResponseBody("400", INVALID_REQUEST);

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenReturn(Response.builder().request(mock(
                Request.class)).body(responseBody, Charset.defaultCharset()).status(400).build());

        ResponseEntity responseEntity = pcqBackendService.submitAnswers(testRequest);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof Map<?,?>);
        Map<?,?> responseBodyMap = (Map<?, ?>) responseEntity.getBody();
        assertEquals(EXPECTED_MSG_1, TEST_PCQ_ID, responseBodyMap.get(PCQ_ID_KEY));
        assertEquals(EXPECTED_MSG_2, "400", responseBodyMap.get(STATUS_CODE_KEY));
        assertEquals(EXPECTED_MSG_3, INVALID_REQUEST, responseBodyMap.get(STATUS_KEY));


        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));

    }

    @Test
    void testInvalidRequestResponse403() {
        PcqAnswerRequest testRequest = generateTestRequest();
        String responseBody = getResponseBody("403", INVALID_REQUEST);

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenReturn(Response.builder().request(mock(
                Request.class)).body(responseBody, Charset.defaultCharset()).status(403).build());

        ResponseEntity responseEntity = pcqBackendService.submitAnswers(testRequest);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof Map<?,?>);
        Map<?,?> responseBodyMap = (Map<?, ?>) responseEntity.getBody();
        assertEquals(EXPECTED_MSG_1, TEST_PCQ_ID, responseBodyMap.get(PCQ_ID_KEY));
        assertEquals(EXPECTED_MSG_2, "403", responseBodyMap.get(STATUS_CODE_KEY));
        assertEquals(EXPECTED_MSG_3, INVALID_REQUEST, responseBodyMap.get(STATUS_KEY));


        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));

    }

    @Test
    void testUnknownError() {
        PcqAnswerRequest testRequest = generateTestRequest();
        String responseBody = getResponseBody("500", "Unknown error occurred");

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenReturn(Response.builder().request(mock(
                Request.class)).body(responseBody, Charset.defaultCharset()).status(500).build());

        ResponseEntity responseEntity = pcqBackendService.submitAnswers(testRequest);

        assertTrue(RESPONSE_INCORRECT, responseEntity.getBody() instanceof Map<?,?>);
        Map<?,?> responseBodyMap = (Map<?, ?>) responseEntity.getBody();
        assertEquals(EXPECTED_MSG_1, TEST_PCQ_ID, responseBodyMap.get(PCQ_ID_KEY));
        assertEquals(EXPECTED_MSG_2, "500", responseBodyMap.get(STATUS_CODE_KEY));
        assertEquals(EXPECTED_MSG_3, "Unknown error occurred", responseBodyMap.get(STATUS_KEY));


        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));

    }

    @Test
    void executeFeignApiError() {
        PcqAnswerRequest testRequest = generateTestRequest();
        Map<String, Collection<String>> maps = new ConcurrentHashMap<>();
        FeignException feignException = new FeignException.BadGateway("Bade Gateway Error", mock(Request.class),
                                                                      "Test".getBytes(),maps);

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenThrow(feignException);

        assertThrows(ExternalApiException.class, () -> pcqBackendService.submitAnswers(testRequest));

        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));
    }

    @Test
    void executeFeignApiUnknownError() {
        PcqAnswerRequest testRequest = generateTestRequest();
        Map<String, Collection<String>> maps = new ConcurrentHashMap<>();
        FeignException feignException = new FeignException.FeignServerException(-1, "Bad error", mock(Request.class),
                                                                                "TestError".getBytes(),maps);

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenThrow(feignException);

        assertThrows(ExternalApiException.class, () -> pcqBackendService.submitAnswers(testRequest));

        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));
    }

    @Test
    void executeIllegalArgumentExceptionError() {
        PcqAnswerRequest testRequest = generateTestRequest();
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("test");

        when(mockPcqBackendFeignClient.submitAnswers(anyString(), anyString(), any(PcqAnswerRequest.class)))
            .thenThrow(illegalArgumentException);

        assertThrows(ExternalApiException.class, () -> pcqBackendService.submitAnswers(testRequest));

        verify(mockPcqBackendFeignClient, times(1)).submitAnswers(anyString(), anyString(),
                                                                  any(PcqAnswerRequest.class));
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

    private String getResponseBody(String responseStatusCode, String responseStatus) {
        return "{\n"
            + "    \"pcqId\": \"" + PcqBackendServiceImplTest.TEST_PCQ_ID + "\",\n"
            + "    \"responseStatusCode\": \"" + responseStatusCode + "\",\n"
            + "    \"responseStatus\": \"" + responseStatus + "\"\n"
            + "}";
    }
}
