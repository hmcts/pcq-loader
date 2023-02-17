package uk.gov.hmcts.reform.pcqloader.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcq.commons.model.PcqMetaData;
import uk.gov.hmcts.reform.pcq.commons.model.PcqScannableItems;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.pcq.commons.tests.utils.TestUtils.jsonStringFromFile;

@ExtendWith(SpringExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
  class PayloadMappingHelperIntegTest {

    private static final String FAIL_ASSERT_MSG = "Method call failed.";
    private static final String FORM_ID_VALIDATION_MSG = "Form Id is not correct.";
    private static final String SERVICE_ID_VALIDATION_MSG = "Service Id is not correct.";
    private static final String EXPECTED_DOB = "2000-01-01T00:00:00.000Z";
    private static final String DOB_VALIDATION_MSG = "Dob is not correct";
    private static final String ETHNICITY_MSG = "Ethnicity not correct";
    private static final String ETHNICITY_OTHER_MSG = "Ethnicity Other is not correct.";
    private static final String ETHNICITY_STRING = "Ethnicity";
    private static final String DCN_VALIDATION_MSG = "DCN Number is not correct";

    private PayloadMappingHelper payloadMappingHelper;

    private final PayloadValidationHelper payloadValidationHelper = new PayloadValidationHelper();

    @BeforeEach
    public void setUp() {
        payloadMappingHelper = new PayloadMappingHelper(payloadValidationHelper);
    }

    @Test
    @DisplayName("Payload cannot be decrypted.")
    void testBadDecryption() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/decryptErrorMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertNull(mappedAnswers, "No mapping should be done.");
    }

    @Test
    @DisplayName("Payload data had invalid numerical values.")
    void testInvalidNumericData() throws IOException {

        String metaDataPayLoad =
            jsonStringFromFile("testPayloadFiles/invalidNumericValuesPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertNull(mappedAnswers, "Data values returned from form are not numeric.");
    }

    @Test
    @DisplayName("Multiple choices test.")
    void testMultipleChoices() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/multipleChoiceMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertInvalid(mappedAnswers.getPcqAnswers().getLanguageMain(), "Language_Main");
        assertInvalid(mappedAnswers.getPcqAnswers().getEnglishLanguageLevel(), "English Language Level");
        assertInvalid(mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_STRING);
        assertInvalid(mappedAnswers.getPcqAnswers().getDisabilityConditions(), "Disability Conditions");
        assertInvalid(mappedAnswers.getPcqAnswers().getDisabilityImpact(), "Disability Impact");
        assertInvalid(mappedAnswers.getPcqAnswers().getPregnancy(), "Pregnancy");
        assertInvalid(mappedAnswers.getPcqAnswers().getSexuality(), "Sexuality");
        assertInvalid(mappedAnswers.getPcqAnswers().getSex(), "Sex");
        assertInvalid(mappedAnswers.getPcqAnswers().getGenderDifferent(), "Gender Different");
        assertInvalid(mappedAnswers.getPcqAnswers().getMarriage(), "Marriage");
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("Empty payload - User did not mark anything in the form.")
    void testEmptyPayload() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/emptyPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertNullMapping(mappedAnswers.getPcqAnswers());
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("Missing Elements from the Payload - Exela/BulkScan error case")
    void testMissingElementsFromPayload() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/nullPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertNullMapping(mappedAnswers.getPcqAnswers());
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);
    }

    @Test
    @DisplayName("Other Text Field entered but user has not ticked the Other checkbox.")
    void testInvalidOtherTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/invalidOtherPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertInvalid(mappedAnswers.getPcqAnswers().getLanguageMain(), "Language_Main");
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("OtherWhite and OtherMixed Text Field entered but user has not ticked Only OtherWhite checkbox.")
    void testInvalidOtherWhiteTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/invalidOtherWhitePayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertInvalid(mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_STRING);
        assertNull(mappedAnswers.getPcqAnswers().getEthnicityOther(), ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("OtherWhite and OtherMixed Text Field entered but user has not ticked Only OtherMixed checkbox.")
    void testInvalidOtherMixedTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/invalidOtherMixedPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertInvalid(mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_STRING);
        assertNull(mappedAnswers.getPcqAnswers().getEthnicityOther(), ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("OtherWhite and OtherAsian Text Field entered but user has not ticked Only OtherAsian checkbox.")
    void testInvalidOtherAsianTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/invalidOtherAsianPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertInvalid(mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_STRING);
        assertNull(mappedAnswers.getPcqAnswers().getEthnicityOther(), ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("OtherWhite and OtherCarib Text Field entered but user has not ticked Only OtherCarib checkbox.")
    void testInvalidOtherCaribTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/invalidOtherCaribPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertInvalid(mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_STRING);
        assertNull(mappedAnswers.getPcqAnswers().getEthnicityOther(), ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("Valid other white ethnicity test")
    void testValidOtherWhiteTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/validOtherWhitePayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertEquals(4, mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_MSG);
        assertEquals("OtherWhite", mappedAnswers.getPcqAnswers().getEthnicityOther(),
                     ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);
    }

    @Test
    @DisplayName("Valid other mixed ethnicity test")
    void testValidOtherMixedTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/validOtherMixedPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertEquals(8, mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_MSG);
        assertEquals("Other Mixed", mappedAnswers.getPcqAnswers().getEthnicityOther(),
                     ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("Valid other Asian ethnicity test")
    void testValidOtherAsianTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/validOtherAsianPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertEquals(13, mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_MSG);
        assertEquals("Other Asian", mappedAnswers.getPcqAnswers().getEthnicityOther(),
                     ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("Valid other Carib ethnicity test")
    void testValidOtherCaribTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/validOtherCaribPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertEquals(16, mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_MSG);
        assertEquals("Other Carib", mappedAnswers.getPcqAnswers().getEthnicityOther(),
                     ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("Valid other ethnicity test")
    void testValidOtherEthnicityTextInput() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/validOtherEthnicityPayloadMetaFile.json");

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);
        assertEquals(18, mappedAnswers.getPcqAnswers().getEthnicity(), ETHNICITY_MSG);
        assertEquals("Other Ethnicity", mappedAnswers.getPcqAnswers().getEthnicityOther(),
                     ETHNICITY_OTHER_MSG);
        assertNotNull(mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

    }

    @Test
    @DisplayName("A valid scenario test where user has supplied a valid form.")
    void testValidFormSubmission() throws IOException {

        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/validPayloadMetaFile.json");
        PcqMetaData metaData = jsonMetaDataObjectFromString(metaDataPayLoad);

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertSuccessMapping(mappedAnswers, metaData);
    }

    @Test
    @DisplayName("A valid scenario test where valid form is supplied but with unKnown fields.")
    void testValidFormWithUnknownFields() throws IOException {
        String metaDataPayLoad = jsonStringFromFile("testPayloadFiles/unknownFieldsPayloadMetaFile.json");
        PcqMetaData metaData = jsonMetaDataObjectFromString(metaDataPayLoad);

        PcqAnswerRequest mappedAnswers = invokeMappingHelper(metaDataPayLoad);

        assertUnknownFieldMapping(mappedAnswers, metaData);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private PcqAnswerRequest invokeMappingHelper(String metaDataPayLoad) {
        PcqAnswerRequest mappedAnswers = null;
        try {
            mappedAnswers = payloadMappingHelper.mapPayLoadToPcqAnswers(metaDataPayLoad);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(FAIL_ASSERT_MSG, e);
        }
        return mappedAnswers;
    }

    private void assertInvalid(Integer actualValue, String fieldName) {
        assertEquals(-1, actualValue, fieldName + " not correct.");
    }

    private void assertNullMapping(PcqAnswers answers) {
        assertNull(answers.getLanguageMain(), "Language_Main is not correct.");
        assertNull(answers.getMarriage(), "Marriage is not correct.");
        assertNull(answers.getGenderOther(), "Gender_other is not correct.");
        assertNull(answers.getGenderDifferent(), "Gender_different is not correct.");
        assertNull(answers.getSex(), "Sex is not correct.");
        assertNull(answers.getSexualityOther(), "Sexuality_Other is not correct.");
        assertNull(answers.getSexuality(), "Sexuality is not correct.");
        assertNull(answers.getDisabilityConditionOther(), "Disability_Condition_other is not correct.");
        assertNull(answers.getDisabilityOther(), "Disability_Other is not correct.");
        assertNull(answers.getDisabilitySocial(), "Disability_Social is not correct.");
        assertNull(answers.getDisabilityStamina(), "Disability_Stamina is not correct.");
        assertNull(answers.getDisabilityMentalHealth(), "Disability_Mental_Health is not correct.");
        assertNull(answers.getDisabilityDexterity(), "Disability_Dexterity is not correct.");
        assertNull(answers.getDisabilityMobility(), "Disability_Mobility is not correct.");
        assertNull(answers.getDisabilityVision(), "Disability_Vision is not correct.");
        assertNull(answers.getDisabilityImpact(), "Disability_Impact is not correct.");
        assertNull(answers.getDisabilityConditions(), "Disability_Conditions is not correct.");
        assertNull(answers.getEthnicityOther(), "Ethnicity_Other is not correct.");
        assertNull(answers.getEthnicity(), "Ethnicity is not correct.");
        assertNull(answers.getDobProvided(), "Dob_Provided is not correct.");
        assertNull(answers.getDob(), "Dob is not correct.");
        assertNull(answers.getReligionOther(), "Religion_Other is not correct.");
        assertNull(answers.getReligion(), "Religion is not correct.");
        assertNull(answers.getLanguageOther(), "Language_Other is not correct.");
        assertNull(answers.getEnglishLanguageLevel(), "English_Language_level is not correct.");
        assertNull(answers.getDisabilityHearing(), "Disability_Hearing is not correct.");
        assertNull(answers.getDisabilityLearning(), "Disability_Learning is not correct.");
        assertNull(answers.getDisabilityMemory(), "Disability_Memory is not correct.");
        assertNull(answers.getDisabilityNone(), "Disability_None is not correct.");
        assertNull(answers.getPregnancy(), "Pregnancy is not correct.");
    }

    public void assertSuccessMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertEquals(1, answers.getLanguageMain(), "Language_Main is not correct.");
        assertNull(answers.getLanguageOther(), "Language_Other is not correct.");
        assertNull(answers.getEnglishLanguageLevel(), "English_Language_level is not correct.");
        assertEquals(3, answers.getReligion(), "Religion is not correct.");
        assertNull(answers.getReligionOther(), "Religion_Other is not correct.");
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), "Dob_Provided is not correct.");
        assertEquals(5, answers.getEthnicity(), "Ethnicity is not correct.");
        assertNull(answers.getEthnicityOther(), "Ethnicity_Other is not correct.");
        assertEquals(1, answers.getDisabilityConditions(), "Disability_Conditions is not correct.");
        assertEquals(2, answers.getDisabilityImpact(), "Disability_Impact is not correct.");
        assertEquals(1, answers.getDisabilityVision(), "Disability_Vision is not correct.");
        assertEquals(0, answers.getDisabilityHearing(), "Disability_Hearing is not correct.");
        assertEquals(1, answers.getDisabilityMobility(), "Disability_Mobility is not correct.");
        assertEquals(0, answers.getDisabilityDexterity(), "Disability_Dexterity is not correct.");
        assertEquals(1, answers.getDisabilityLearning(), "Disability_Learning is not correct.");
        assertEquals(0, answers.getDisabilityMemory(), "Disability_Memory is not correct.");
        assertEquals(1, answers.getDisabilityMentalHealth(), "Disability_Mental is not correct.");
        assertEquals(0, answers.getDisabilityStamina(), "Disability_Stamina is not correct.");
        assertEquals(1, answers.getDisabilitySocial(), "Disability_Social is not correct.");
        assertEquals(1, answers.getDisabilityOther(), "Disability_Other is not correct.");
        assertEquals("Other disability", answers.getDisabilityConditionOther(),
                     "Disability_Condition_Other is not correct.");
        assertEquals(0, answers.getDisabilityNone(), "Disability_None is not correct.");
        assertEquals(2, answers.getPregnancy(), "Pregnancy is not correct.");
        assertEquals(1, answers.getSexuality(), "Sexuality is not correct.");
        assertNull(answers.getSexualityOther(), "Sexuality_Other is not correct.");
        assertEquals(1, answers.getSex(), "Sex is not correct.");
        assertEquals(1, answers.getGenderDifferent(), "Gender_Different is not correct.");
        assertNull(answers.getGenderOther(), "Gender_other is not correct.");
        assertEquals(2, answers.getMarriage(), "Marriage is not correct.");
    }

    public void assertUnknownFieldMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertEquals(1, answers.getLanguageMain(), "Language_Main is not correct.");
        assertNull(answers.getLanguageOther(), "Language_Other is not correct.");
        assertNull(answers.getEnglishLanguageLevel(), "English_Language_level is not correct.");
        assertEquals(1, answers.getReligion(), "Religion is not correct.");
        assertNull(answers.getReligionOther(), "Religion_Other is not correct.");
        assertEquals("1987-01-05T00:00:00.000Z", answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), "Dob_Provided is not correct.");
        assertEquals(1, answers.getEthnicity(), "Ethnicity is not correct.");
        assertNull(answers.getEthnicityOther(), "Ethnicity_Other is not correct.");
        assertEquals(1, answers.getDisabilityConditions(), "Disability_Conditions is not correct.");
        assertEquals(1, answers.getDisabilityImpact(), "Disability_Impact is not correct.");
        assertEquals(1, answers.getDisabilityVision(), "Disability_Vision is not correct.");
        assertEquals(1, answers.getDisabilityHearing(), "Disability_Hearing is not correct.");
        assertEquals(1, answers.getDisabilityMobility(), "Disability_Mobility is not correct.");
        assertEquals(1, answers.getDisabilityDexterity(), "Disability_Dexterity is not correct.");
        assertNull(answers.getDisabilityLearning(), "Disability_Learning is not correct.");
        assertNull(answers.getDisabilityMemory(), "Disability_Memory is not correct.");
        assertEquals(1, answers.getDisabilityMentalHealth(), "Disability_Mental is not correct.");
        assertEquals(1, answers.getDisabilityStamina(), "Disability_Stamina is not correct.");
        assertNull(answers.getDisabilitySocial(), "Disability_Social is not correct.");
        assertNull(answers.getDisabilityOther(), "Disability_Other is not correct.");
        assertNull(answers.getDisabilityConditionOther(), "Disability_Condition_Other is not correct.");
        assertNull(answers.getDisabilityNone(), "Disability_None is not correct.");
        assertEquals(1, answers.getPregnancy(), "Pregnancy is not correct.");
        assertEquals(1, answers.getSexuality(), "Sexuality is not correct.");
        assertNull(answers.getSexualityOther(), "Sexuality_Other is not correct.");
        assertEquals(1, answers.getSex(), "Sex is not correct.");
        assertEquals(1, answers.getGenderDifferent(), "Gender_Different is not correct.");
        assertNull(answers.getGenderOther(), "Gender_other is not correct.");
        assertEquals(1, answers.getMarriage(), "Marriage is not correct.");
    }

    public void assertDefaultAndGeneratedFields(PcqAnswerRequest mappedAnswers) {
        //Check the primary key generated field is not missing.
        assertNotNull(mappedAnswers.getPcqId(), "PCQ Id is Null");

        //Check the default values are set correctly for paper channel
        assertNull(mappedAnswers.getCaseId(), "Case Id is not correct.");
        String expectedPartyId = "PaperForm";
        assertEquals(expectedPartyId, mappedAnswers.getPartyId(), "Party Id is not correct.");
        int expectedChannel = 2;
        assertEquals(expectedChannel, mappedAnswers.getChannel(), "Channel is not correct.");
        assertNotNull(mappedAnswers.getCompletedDate(), "Completed Date is empty");
        String expectedActor = "UNKNOWN";
        assertEquals(expectedActor, mappedAnswers.getActor(), "Actor is not correct.");
        int expectedVersion = 1;
        assertEquals(expectedVersion, mappedAnswers.getVersionNo(), "Version Number is not correct.");
        assertNull(mappedAnswers.getOptOut(), "Opt Out is not correct.");
    }


    public static PcqMetaData jsonMetaDataObjectFromString(String jsonString) throws IOException {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(jsonString, PcqMetaData.class);
    }

}
