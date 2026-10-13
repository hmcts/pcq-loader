package uk.gov.hmcts.reform.pcqloader.serialization;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcqloader.helper.PayloadMappingHelper;
import uk.gov.hmcts.reform.pcqloader.helper.PayloadValidationHelper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PcqAnswerRequestJackson3RoundTripTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preservesAllPcqAnswerFieldsWhenSerializedForBackend() throws Exception {
        String json = """
            {
              "pcqId": "pcq-123",
              "dcnNumber": "dcn-123",
              "formId": "form-123",
              "ccdCaseId": "case-123",
              "partyId": "party-123",
              "channel": 2,
              "completedDate": "2026-09-29T12:34:56Z",
              "serviceId": "service-123",
              "actor": "actor-123",
              "versionNo": 1,
              "optOut": "N",
              "pcqAnswers": {
                "dob_provided": 1,
                "dob": "1990-01-02",
                "language_main": 1,
                "language_other": "Welsh",
                "english_language_level": 2,
                "sex": 3,
                "gender_different": 4,
                "gender_other": "Other gender",
                "sexuality": 5,
                "sexuality_other": "Other sexuality",
                "marriage": 6,
                "ethnicity": 7,
                "ethnicity_other": "Other ethnicity",
                "religion": 8,
                "religion_other": "Other religion",
                "disability_conditions": 9,
                "disability_impact": 10,
                "disability_vision": 11,
                "disability_hearing": 12,
                "disability_mobility": 13,
                "disability_dexterity": 14,
                "disability_learning": 15,
                "disability_memory": 16,
                "disability_mental_health": 17,
                "disability_stamina": 18,
                "disability_social": 19,
                "disability_other": 20,
                "disability_other_details": "Other disability details",
                "disability_none": 21,
                "pregnancy": 22,
                "opt_out": true
              }
            }
            """;

        JsonNode input = objectMapper.readTree(json);
        PcqAnswerRequest request = objectMapper.readValue(json, PcqAnswerRequest.class);
        JsonNode serializedRequest = objectMapper.valueToTree(request);

        assertThat(serializedRequest).isEqualTo(input);
    }

    @Test
    @SuppressWarnings("PMD.LawOfDemeter")
    void preservesMappedPayloadFieldsWhenSerializedForBackend() throws Exception {
        ClassPathResource metadataResource = new ClassPathResource("testPayloadFiles/successMetaFile.json");
        String metadata;
        try (InputStream inputStream = metadataResource.getInputStream()) {
            metadata = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        PayloadMappingHelper mappingHelper = new PayloadMappingHelper(new PayloadValidationHelper());

        PcqAnswerRequest request = mappingHelper.mapPayLoadToPcqAnswers(metadata);
        JsonNode serializedRequest = objectMapper.valueToTree(request);
        JsonNode answers = serializedRequest.get("pcqAnswers");

        assertThat(answers).isNotNull();
        List.of(
            "dob_provided",
            "dob",
            "language_main",
            "language_other",
            "english_language_level",
            "sex",
            "gender_different",
            "gender_other",
            "sexuality",
            "sexuality_other",
            "marriage",
            "ethnicity",
            "ethnicity_other",
            "religion",
            "religion_other",
            "disability_conditions",
            "disability_impact",
            "disability_vision",
            "disability_hearing",
            "disability_mobility",
            "disability_dexterity",
            "disability_learning",
            "disability_memory",
            "disability_mental_health",
            "disability_stamina",
            "disability_social",
            "disability_other",
            "disability_other_details",
            "disability_none",
            "pregnancy",
            "opt_out")
            .forEach(field -> assertThat(answers.has(field)).as(field).isTrue());
        assertThat(answers.has("languageMain")).isFalse();
        assertThat(answers.has("disabilityConditionOther")).isFalse();
    }
}
