package uk.gov.hmcts.reform.pcqloader.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqPayloadContents;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class PayloadMappingHelperBase {

    private final Set<String> seenFields = new HashSet<>();
    private final Set<String> duplicates = new HashSet<>();

    private static final Set<String> NUMERIC_FIELDS = new HashSet<>(Arrays.asList(
        "dob_provided",
        "language_main",
        "english_language_level",
        "sex",
        "gender_different",
        "sexuality",
        "marriage",
        "ethnicity",
        "religion",
        "disability_condition",
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
        "disability_none",
        "pregnancy"
    ));

    protected void checkForDuplicates(PcqPayloadContents... payloadContents) {
        seenFields.clear();
        duplicates.clear();

        for (PcqPayloadContents payloadContent : payloadContents) {
            String fieldName = payloadContent.getFieldName();
            if (NUMERIC_FIELDS.contains(fieldName) && seenFields.contains(fieldName)) {
                duplicates.add(fieldName);
                log.error("Invalid answer for {}, found more than one value in payload.", fieldName);
            }
            seenFields.add(fieldName);
        }
    }

    protected void mapFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            mapField(payloadContent, answers);
        }
    }

    private void mapField(PcqPayloadContents payloadContent, PcqAnswers answers) {
        String fieldValue = payloadContent.getFieldValue();
        String fieldName = payloadContent.getFieldName();
        if (!NUMERIC_FIELDS.contains(fieldName) || StringUtils.isEmpty(fieldValue)) {
            return;
        }

        Integer value = duplicates.contains(fieldName) ? -1 : Integer.parseInt(fieldValue);
        setFieldValue(fieldName, value, answers);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NcssCount"})
    private void setFieldValue(String fieldName, Integer value, PcqAnswers answers) {
        switch (fieldName) {
            case "dob_provided":
                answers.setDobProvided(value);
                break;
            case "language_main":
                answers.setLanguageMain(value);
                break;
            case "english_language_level":
                answers.setEnglishLanguageLevel(value);
                break;
            case "sex":
                answers.setSex(value);
                break;
            case "gender_different":
                answers.setGenderDifferent(value);
                break;
            case "sexuality":
                answers.setSexuality(value);
                break;
            case "marriage":
                answers.setMarriage(value);
                break;
            case "ethnicity":
                answers.setEthnicity(value);
                break;
            case "religion":
                answers.setReligion(value);
                break;
            case "disability_condition":
                answers.setDisabilityConditions(value);
                break;
            case "disability_impact":
                answers.setDisabilityImpact(value);
                break;
            case "disability_vision":
                answers.setDisabilityVision(value);
                break;
            case "disability_hearing":
                answers.setDisabilityHearing(value);
                break;
            case "disability_mobility":
                answers.setDisabilityMobility(value);
                break;
            case "disability_dexterity":
                answers.setDisabilityDexterity(value);
                break;
            case "disability_learning":
                answers.setDisabilityLearning(value);
                break;
            case "disability_memory":
                answers.setDisabilityMemory(value);
                break;
            case "disability_mental_health":
                answers.setDisabilityMentalHealth(value);
                break;
            case "disability_stamina":
                answers.setDisabilityStamina(value);
                break;
            case "disability_social":
                answers.setDisabilitySocial(value);
                break;
            case "disability_other":
                answers.setDisabilityOther(value);
                break;
            case "disability_none":
                answers.setDisabilityNone(value);
                break;
            case "pregnancy":
                answers.setPregnancy(value);
                break;
            default:
                break;
        }
    }
}
