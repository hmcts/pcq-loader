package uk.gov.hmcts.reform.pcqloader.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang.ArrayUtils;
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

import static uk.gov.hmcts.reform.pcq.commons.utils.PcqUtils.nullIfEmpty;

@Component
@Slf4j
public class PayloadMappingHelper extends PayloadMappingHelperBase {

    private static final String EMPTY_STRING = "";

    private final PayloadValidationHelper payloadValidationHelper;

    @Autowired
    public PayloadMappingHelper(PayloadValidationHelper payloadValidationHelper) {
        super();
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
            if (scannedItems != null && scannedItems.length == 1
                && StringUtils.isNotEmpty(scannedItems[0].getOcrData())) {

                // Step 3. Retrieve and decode the Ocr Data.
                String ocrData = new String(Base64Utils.decodeFromString(scannedItems[0].getOcrData()));

                //Step 4. Get the payload object for the Json ocrData
                PcqPayLoad pcqPayLoad = new ObjectMapper().readValue(ocrData, PcqPayLoad.class);

                //Step 5. Perform the mapping and get the PcqAnswers object.
                PcqAnswerRequest answerRequest = performMapping(pcqMetaData, pcqPayLoad);
                log.info("Successfully completed mapping from payload to Answer Definition");
                return answerRequest;

            } else {
                log.error("No scanned items with ocr_data found in the meta-data file.");
            }

        } catch (JsonProcessingException jpe) {
            log.error("JsonProcessingException during payload parsing - " + jpe.getMessage());
        } catch (NumberFormatException nfe) {
            log.error("NumberFormatException during payload parsing - " + nfe.getMessage());
        }

        return null;
    }

    private PcqAnswerRequest performMapping(PcqMetaData metaData, PcqPayLoad payLoad) {
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

        mapLanguageFields(payloadContents,answers);
        mapGenderFields(payloadContents,answers);
        mapGeneralFields(payloadContents,answers);
        mapDisabilityFields(payloadContents,answers);
        mapDisabilityOtherFields(payloadContents,answers);
        mapDisabilityOther2Fields(payloadContents,answers);
        mapDisabilityOther3Fields(payloadContents,answers);
        mapDisabilityNoneAndPregnancyFields(payloadContents,answers);

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
