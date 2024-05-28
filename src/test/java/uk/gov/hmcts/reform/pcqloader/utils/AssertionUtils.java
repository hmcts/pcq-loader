package uk.gov.hmcts.reform.pcqloader.utils;

import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswerRequest;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqMetaData;
import uk.gov.hmcts.reform.pcq.commons.model.PcqPayLoad;
import uk.gov.hmcts.reform.pcq.commons.model.PcqPayloadContents;
import uk.gov.hmcts.reform.pcq.commons.model.PcqScannableItems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("PMD.TooManyMethods")
public class AssertionUtils {

    private static final String FORM_ID_VALIDATION_MSG = "Form Id is not correct.";
    private static final String SERVICE_ID_VALIDATION_MSG = "Service Id is not correct.";
    private static final String RELIGION_VALIDATION_MSG = "Religion is not correct";
    private static final String ETHNICITY_VALIDATION_MSG = "Ethnicity value is not correct";
    private static final String EXPECTED_DOB = "2000-01-01T00:00:00.000Z";
    private static final String DOB_VALIDATION_MSG = "Dob is not correct";
    private static final String DOB_PROVIDED_MSG =  "Dob provided is not correct";
    private static final String LANG_MAIN_VALIDATION_MSG = "Language_Main is not correct";
    private static final String SEXUALITY_VALIDATION_MSG = "Sexuality is not correct";
    private static final String GENDER_DIFFERENT_MSG = "Gender Different is not correct";
    private static final String DCN_NUMBER_VALIDATION_MSG = "DCN Number is not correct.";
    private static final String ENGLISH_LANGUAGE_LEVEL_MSG = "English Language Level is not correct";
    private static final String OTHER_LANGUAGE_MSG = "Other Language is not correct";
    private static final String OTHER_RELIGION_MSG = "Other Religion is not correct";
    private static final String ETHNICITY_OTHER_NULL_MSG = "Ethnicity Other should have been null";
    private static final String DISABILITY_CONDITIONS_MSG = "Disability Conditions is not correct";
    private static final String DISABILITY_IMPACT_MSG = "Disability impact is not correct";
    private static final String PREGNANCY_MSG = "Pregnancy is not correct";
    private static final String SEXUALITY_OTHER_MSG = "Sexuality Other is not correct";
    private static final String SEX_MSG = "Sex is not correct";
    private static final String OTHER_GENDER_MSG = "Other Gender is not correct";
    private static final String MARRIAGE_MSG = "Marriage is not correct";

    public void assertMultipleEthnicityMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(-1, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertSuccessMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertLanguageMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData, Integer expectedResult) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        if (expectedResult == null) {
            assertNull(answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        } else {
            assertEquals(expectedResult, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        }
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertOtherLanguageMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(2, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
        assertEquals(2, answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);

        assertEquals("Test", answers.getLanguageOther(), OTHER_LANGUAGE_MSG);
        assertNull(answers.getReligionOther(), OTHER_RELIGION_MSG);
        assertNull(answers.getEthnicityOther(), ETHNICITY_OTHER_NULL_MSG);
        assertEquals(1, answers.getDisabilityConditions(), DISABILITY_CONDITIONS_MSG);
        assertEquals(2, answers.getDisabilityImpact(), DISABILITY_IMPACT_MSG);
        assertEquals(2, answers.getPregnancy(), PREGNANCY_MSG);
        assertNull(answers.getSexualityOther(), SEXUALITY_OTHER_MSG);
        assertEquals(1, answers.getSex(), SEX_MSG);
        assertNull(answers.getGenderOther(), OTHER_GENDER_MSG);
        assertEquals(2, answers.getMarriage(), MARRIAGE_MSG);
    }

    public void assertInvalidOtherMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(3, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertMultipleInvalidOtherMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertEquals(2, answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(-1, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(-1, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(-1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(-1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(-1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertReligionInvalidMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(-1, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);

    }

    public void assertDobInvalidMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertNull(answers.getDob(),  DOB_VALIDATION_MSG);
        assertNull(answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);

    }

    public void assertDobNotProvidedMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(), SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesSuccess(answers);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertNull(answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(0, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);

    }

    public void assertDisabilityNoneMapping(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(),  SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertCommonAnswers(answers);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertDisabilitiesNoneFlag(answers, false);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);

    }

    public void assertCommonAnswers(PcqAnswers answers) {
        assertNull(answers.getLanguageOther(), OTHER_LANGUAGE_MSG);
        assertNull(answers.getReligionOther(), OTHER_RELIGION_MSG);
        assertNull(answers.getEthnicityOther(), ETHNICITY_OTHER_NULL_MSG);
        assertEquals(1, answers.getDisabilityConditions(), DISABILITY_CONDITIONS_MSG);
        assertEquals(2, answers.getDisabilityImpact(), DISABILITY_IMPACT_MSG);
        assertEquals(2, answers.getPregnancy(), PREGNANCY_MSG);
        assertNull(answers.getSexualityOther(), SEXUALITY_OTHER_MSG);
        assertEquals(1, answers.getSex(), SEX_MSG);
        assertNull(answers.getGenderOther(), OTHER_GENDER_MSG);
        assertEquals(2, answers.getMarriage(), MARRIAGE_MSG);
    }

    public void assertNoDisabilityCondition(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData,
                                            int expectedResult) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(),  SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertNull(answers.getLanguageOther(), OTHER_LANGUAGE_MSG);
        assertNull(answers.getEnglishLanguageLevel(), ENGLISH_LANGUAGE_LEVEL_MSG);
        assertNull(answers.getReligionOther(), OTHER_RELIGION_MSG);
        assertNull(answers.getEthnicityOther(), ETHNICITY_OTHER_NULL_MSG);
        assertEquals(expectedResult, answers.getDisabilityConditions(), DISABILITY_CONDITIONS_MSG);
        assertNull(answers.getDisabilityImpact(), DISABILITY_IMPACT_MSG);
        assertEquals(2, answers.getPregnancy(), PREGNANCY_MSG);
        assertNull(answers.getSexualityOther(), SEXUALITY_OTHER_MSG);
        assertEquals(1, answers.getSex(), SEX_MSG);
        assertNull(answers.getGenderOther(), OTHER_GENDER_MSG);
        assertEquals(2, answers.getMarriage(), MARRIAGE_MSG);
        assertDisabilitiesNoneFlag(answers, true);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertNoDisabilityImpact(PcqAnswerRequest mappedAnswers, PcqMetaData pcqMetaData,
                                            int expectedResult) {
        assertDefaultAndGeneratedFields(mappedAnswers);

        //Check the information that matches the meta-data information.
        PcqScannableItems[] pcqScannedItems = pcqMetaData.getScannableItems();
        assertEquals(pcqScannedItems[0].getDocumentType(), mappedAnswers.getFormId(), FORM_ID_VALIDATION_MSG);
        assertEquals(pcqMetaData.getJurisdiction(), mappedAnswers.getServiceId(),  SERVICE_ID_VALIDATION_MSG);
        //Check the correct DCN Number is populated.
        assertEquals(pcqMetaData.getOriginatingDcnNumber(), mappedAnswers.getDcnNumber(), DCN_NUMBER_VALIDATION_MSG);

        //Check the answers matches the payload supplied.
        PcqAnswers answers = mappedAnswers.getPcqAnswers();
        assertNull(answers.getLanguageOther(), OTHER_LANGUAGE_MSG);
        assertNull(answers.getEnglishLanguageLevel(), "English Language Level is not correct");
        assertNull(answers.getReligionOther(), OTHER_RELIGION_MSG);
        assertNull(answers.getEthnicityOther(), ETHNICITY_OTHER_NULL_MSG);
        assertEquals(1, answers.getDisabilityConditions(), DISABILITY_CONDITIONS_MSG);
        assertEquals(expectedResult, answers.getDisabilityImpact(), DISABILITY_IMPACT_MSG);
        assertEquals(2, answers.getPregnancy(), PREGNANCY_MSG);
        assertNull(answers.getSexualityOther(), SEXUALITY_OTHER_MSG);
        assertEquals(1, answers.getSex(), SEX_MSG);
        assertNull(answers.getGenderOther(), OTHER_GENDER_MSG);
        assertEquals(2, answers.getMarriage(), MARRIAGE_MSG);
        assertDisabilitiesNoneFlag(answers, true);
        assertEquals(5, answers.getEthnicity(), ETHNICITY_VALIDATION_MSG);
        assertEquals(3, answers.getReligion(), RELIGION_VALIDATION_MSG);
        assertEquals(EXPECTED_DOB, answers.getDob(),  DOB_VALIDATION_MSG);
        assertEquals(1, answers.getDobProvided(), DOB_PROVIDED_MSG);
        assertEquals(1, answers.getLanguageMain(), LANG_MAIN_VALIDATION_MSG);
        assertEquals(1, answers.getSexuality(), SEXUALITY_VALIDATION_MSG);
        assertEquals(1, answers.getGenderDifferent(), GENDER_DIFFERENT_MSG);
    }

    public void assertDisabilitiesSuccess(PcqAnswers answers) {
        assertEquals(1, answers.getDisabilityVision(), "Disability Vision is not correct");
        assertEquals(0, answers.getDisabilityHearing(), "Disability Hearing is not correct");
        assertEquals(1, answers.getDisabilityMobility(), "Disability Mobility is not correct");
        assertEquals(0, answers.getDisabilityDexterity(), "Disability Dexterity is not correct");
        assertEquals(1, answers.getDisabilityLearning(), "Disability Learning is not correct");
        assertEquals(0, answers.getDisabilityMemory(), "Disability Memory is not correct");
        assertEquals(1, answers.getDisabilityMentalHealth(), "Disability Mental Health is not correct");
        assertEquals(0, answers.getDisabilityStamina(), "Disability Stamina is not correct");
        assertEquals(1, answers.getDisabilitySocial(), "Disability Social is not correct");
        assertEquals(1, answers.getDisabilityOther(), "Disability Other is not correct");
        assertEquals("Other disability", answers.getDisabilityConditionOther(),
                     "Other Disability Details is not correct");
        assertEquals(0, answers.getDisabilityNone(), "Disability None is not correct");
    }

    public void assertDisabilitiesNoneFlag(PcqAnswers answers, boolean allNull) {
        assertNull(answers.getDisabilityVision(), "Disability Vision is not correct");
        assertNull(answers.getDisabilityHearing(), "Disability Hearing is not correct");
        assertNull(answers.getDisabilityMobility(), "Disability Mobility is not correct");
        assertNull(answers.getDisabilityDexterity(), "Disability Dexterity is not correct");
        assertNull(answers.getDisabilityLearning(), "Disability Learning is not correct");
        assertNull(answers.getDisabilityMemory(), "Disability Memory is not correct");
        assertNull(answers.getDisabilityMentalHealth(), "Disability Mental Health is not correct");
        assertNull(answers.getDisabilityStamina(), "Disability Stamina is not correct");
        assertNull(answers.getDisabilitySocial(), "Disability Social is not correct");
        assertNull(answers.getDisabilityOther(), "Disability Other is not correct");
        assertNull(answers.getDisabilityConditionOther(), "Other Disability Details is not correct");
        if (allNull) {
            assertNull(answers.getDisabilityNone(), "Disability None is not correct");
        } else {
            assertEquals(1, answers.getDisabilityNone(), "Disability None is not correct");
        }
    }

    public void assertPayLoads(PcqPayLoad expectedPayLoad, PcqPayLoad actualPayLoad) {
        PcqPayloadContents[] expectedPayLoadContents = expectedPayLoad.getMetaDataContents();
        PcqPayloadContents[] actualPayLoadContents = actualPayLoad.getMetaDataContents();

        assertEquals(expectedPayLoadContents.length, actualPayLoadContents.length,
                     "PayLoad lengths are different");
        for (int i = 0; i < expectedPayLoadContents.length; i++) {
            assertEquals(expectedPayLoadContents[i].getFieldName(), actualPayLoadContents[i].getFieldName(),
                         "Field names don't match in payload");
            assertEquals(expectedPayLoadContents[i].getFieldValue(), actualPayLoadContents[i].getFieldValue(),
                         "Field values don't match in payload");
        }
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
}
