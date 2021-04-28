package uk.gov.hmcts.reform.pcqloader.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcq.commons.model.PcqMetaData;
import uk.gov.hmcts.reform.pcq.commons.model.PcqPayLoad;
import uk.gov.hmcts.reform.pcq.commons.model.PcqPayloadContents;
import uk.gov.hmcts.reform.pcq.commons.model.PcqScannableItems;
import uk.gov.hmcts.reform.pcq.commons.utils.PcqUtils;

import java.lang.reflect.Field;

import static uk.gov.hmcts.reform.pcq.commons.utils.PcqUtils.nullIfEmpty;

@Component
@Slf4j
public class PayloadMappingHelper {

    private static final String EMPTY_STRING = "";

    private final String[] submitAnswerIntElements = {"dob_provided", "language_main", "english_language_level", "sex",
        "gender_different", "sexuality", "marriage", "ethnicity", "religion", "disability_condition",
        "disability_impact", "disability_vision", "disability_hearing", "disability_mobility", "disability_dexterity",
        "disability_learning", "disability_memory", "disability_mental_health", "disability_stamina",
        "disability_social", "disability_other", "disability_none", "pregnancy"};

    private final String[] submitAnswerIntFields = {"dobProvided", "languageMain", "englishLanguageLevel", "sex",
        "genderDifferent", "sexuality", "marriage", "ethnicity", "religion", "disabilityConditions",
        "disabilityImpact", "disabilityVision", "disabilityHearing", "disabilityMobility", "disabilityDexterity",
        "disabilityLearning", "disabilityMemory", "disabilityMentalHealth", "disabilityStamina",
        "disabilitySocial", "disabilityOther", "disabilityNone", "pregnancy"};

    private final PayloadValidationHelper payloadValidationHelper;

    @Autowired
    public PayloadMappingHelper(PayloadValidationHelper payloadValidationHelper) {
        this.payloadValidationHelper = payloadValidationHelper;
    }

    public PcqAnswerRequest mapPayLoadToPcqAnswers(String metaDataString) throws
        NoSuchFieldException, IllegalAccessException {

        try {
            // Step 1. Convert the JSon String into an Java Object
            PcqMetaData pcqMetaData = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(metaDataString, PcqMetaData.class);

            // Step 2. Retrieve the Scannable items
            PcqScannableItems[] scannedItems = pcqMetaData.getScannableItems();
            if (scannedItems != null && scannedItems.length > 0) {

                // Step 3. Retrieve and decode the Ocr Data.
                String ocrData = new String(Base64Utils.decodeFromString(scannedItems[0].getOcrData()));

                //Step 4. Get the payload object for the Json ocrData
                PcqPayLoad pcqPayLoad = new ObjectMapper().readValue(ocrData, PcqPayLoad.class);

                //Step 5. Perform the mapping and get the PcqAnswers object.
                PcqAnswerRequest answerRequest = performMapping(pcqMetaData, pcqPayLoad);
                log.info("Successfully completed mapping from payload to Answer Definition");
                return answerRequest;

            } else {
                log.error("No scanned items found in the meta-data file.");
            }

        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException during payload parsing - " + e.getMessage());
        }

        return null;
    }

    private PcqAnswerRequest performMapping(PcqMetaData metaData, PcqPayLoad payLoad)
        throws IllegalAccessException, NoSuchFieldException {
        PcqAnswerRequest pcqAnswerRequest = new PcqAnswerRequest();

        //Set the default values
        pcqAnswerRequest.setActor("UNKNOWN");
        pcqAnswerRequest.setChannel(2);
        pcqAnswerRequest.setCompletedDate(PcqUtils.getCurrentCompletedDate());
        pcqAnswerRequest.setDcnNumber(metaData.getOriginatingDcnNumber());
        pcqAnswerRequest.setPartyId("PaperForm");
        pcqAnswerRequest.setServiceId(metaData.getJurisdiction());
        pcqAnswerRequest.setVersionNo(1);
        pcqAnswerRequest.setPcqId(PcqUtils.generateUuid());
        pcqAnswerRequest.setFormId(metaData.getScannableItems()[0].getDocumentType());

        PcqAnswers answers = new PcqAnswers();
        PcqPayloadContents[] payloadContents = payLoad.getMetaDataContents();

        //For all the integer schema elements, set the values in the schema object first.
        for (String schemaElement : submitAnswerIntElements) {
            mapIntegerElements(payloadContents, answers, schemaElement);
        }

        //<element>_other field check
        mapOtherFields(payloadContents, answers);

        //Dob check and mapping
        if (payloadValidationHelper.isDobProvided(answers)) {
            mapDateOfBirth(payloadContents, answers);
        }

        //Validate and check disability_conditions
        payloadValidationHelper.validateDisabilityConditions(answers);

        //validate and check disability_impact
        payloadValidationHelper.validateDisabilityImpact(answers);

        //Validate and check disability_none.
        payloadValidationHelper.validateDisabilityNone(answers);

        //Validate and correct other fields.
        payloadValidationHelper.validateAndCorrectOtherFields(answers);

        //Validate and correct english language level.
        payloadValidationHelper.validateLanguageLevel(answers);

        pcqAnswerRequest.setPcqAnswers(answers);

        return pcqAnswerRequest;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void mapIntegerElements(PcqPayloadContents[] payloadContents, PcqAnswers answers, String schemaElement)
        throws IllegalAccessException, NoSuchFieldException {
        for (PcqPayloadContents payloadContent : payloadContents) {
            if (payloadContent.getFieldName().equals(schemaElement)) {
                // Find the field name of the element in the SubmitAnswers object.
                int index = ArrayUtils.indexOf(submitAnswerIntElements, schemaElement);
                Field schemaField = PcqAnswers.class.getDeclaredField(submitAnswerIntFields[index]);
                // Make the field accessible
                PcqUtils.makeFieldAccessible(schemaField);
                // If the element value has been already set in answers object i.e. more than one check-box
                // has been ticked by the user, then set the answer to "-1".
                if (schemaField.get(answers) == null) {
                    if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                        schemaField.set(answers, Integer.valueOf(payloadContent.getFieldValue()));
                    }
                } else {
                    log.error("Invalid answer for " + schemaElement + ", found more than one value in payload.");
                    schemaField.set(answers, -1);
                }
            }
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.CyclomaticComplexity"})
    private void mapOtherFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "language_other" :
                    answers.setLanguageOther(nullIfEmpty(payloadContent.getFieldValue()));
                    break;
                case "other_religion_text" :
                    answers.setReligionOther(nullIfEmpty(payloadContent.getFieldValue()));
                    break;
                case "other_white_ethnicity_text" :
                case "other_mixed_ethnicity_text" :
                case "other_asian_ethnicity_text" :
                case "other_african_caribbean_ethnicity_text" :
                case "other_ethnicity_text" :
                    setEthnicity(payloadContent, answers);
                    break;
                case "other_disability_details" :
                    answers.setDisabilityConditionOther(nullIfEmpty(payloadContent.getFieldValue()));
                    break;
                case "other_sexuality_text" :
                    answers.setSexualityOther(nullIfEmpty(payloadContent.getFieldValue()));
                    break;
                case "gender_different_text" :
                    answers.setGenderOther(nullIfEmpty(payloadContent.getFieldValue()));
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void mapDateOfBirth(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        String dobDay = "";
        String dobMonth = "";
        String dobYear = "";
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "dob_day" :
                    dobDay = payloadContent.getFieldValue();
                    break;
                case "dob_month" :
                    dobMonth = payloadContent.getFieldValue();
                    break;
                case "dob_year" :
                    dobYear = payloadContent.getFieldValue();
                    break;
                default:
                    break;
            }
        }

        //Prefix the month and day with 0 if the length is 1 and numeric.
        dobMonth = PcqUtils.formatDobField(dobMonth);
        dobDay = PcqUtils.formatDobField(dobDay);

        String dob = dobYear + "-" + dobMonth + "-" + dobDay;

        if (PcqUtils.isDobValid(dob)) {
            answers.setDobProvided(1);
            answers.setDob(PcqUtils.generateCompleteDobString(dob));
        } else {
            answers.setDob(null);
            log.error("Invalid Dob provided - " + dob + ", setting the Dob to Null");
        }

    }


    private void setEthnicity(PcqPayloadContents payloadContent, PcqAnswers answers) {
        if (answers.getEthnicityOther() == null && answers.getEthnicity() != null && answers.getEthnicity() != -1) {
            // Only set the value if ethnicity is not already set.
            answers.setEthnicityOther(nullIfEmpty(payloadContent.getFieldValue()));
        } else {
            if (!EMPTY_STRING.equals(payloadContent.getFieldValue())) {
                // Invalid answer supplied as ethnicity is already set in the payload.
                log.error("Invalid answer for " + payloadContent.getFieldName() + ", found more than one "
                              + "other value in payload.");
                answers.setEthnicityOther(null);
                answers.setEthnicity(-1);
            }
        }
    }

}
