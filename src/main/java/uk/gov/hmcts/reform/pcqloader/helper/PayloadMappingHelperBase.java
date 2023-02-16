package uk.gov.hmcts.reform.pcqloader.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;
import uk.gov.hmcts.reform.pcq.commons.model.PcqPayloadContents;

@Slf4j
@SuppressWarnings({"PMD.GodClass","PMD.CyclomaticComplexity","PMD.CognitiveComplexity"})
public class PayloadMappingHelperBase {

    private static final String DISABILITY_NONE = "disability_none";
    private static final String PREGNANCY = "pregnancy";

    protected void mapLanguageFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "dob_provided":
                    if (answers.getDobProvided() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDobProvided(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for dob_provided, found more than one value in payload.");
                        answers.setDobProvided(-1);
                    }
                    break;
                case "language_main":
                    if (answers.getLanguageMain() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setLanguageMain(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for language_main, found more than one value in payload.");
                        answers.setLanguageMain(-1);
                    }
                    break;
                case "english_language_level":
                    if (answers.getEnglishLanguageLevel() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setEnglishLanguageLevel(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for english_language_level, found more than one value in payload.");
                        answers.setEnglishLanguageLevel(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapGenderFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "sex":
                    if (answers.getSex() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setSex(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for sex, found more than one value in payload.");
                        answers.setSex(-1);
                    }
                    break;
                case "gender_different":
                    if (answers.getGenderDifferent() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setGenderDifferent(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for gender_different, found more than one value in payload.");
                        answers.setGenderDifferent(-1);
                    }
                    break;
                case "sexuality":
                    if (answers.getSexuality() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setSexuality(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for sexuality, found more than one value in payload.");
                        answers.setSexuality(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapGeneralFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "marriage":
                    if (answers.getMarriage() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setMarriage(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for marriage, found more than one value in payload.");
                        answers.setMarriage(-1);
                    }
                    break;
                case "ethnicity":
                    if (answers.getEthnicity() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setEthnicity(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for ethnicity, found more than one value in payload.");
                        answers.setEthnicity(-1);
                    }
                    break;
                case "religion":
                    if (answers.getReligion() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setReligion(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for religion, found more than one value in payload.");
                        answers.setReligion(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapDisabilityFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "disability_condition":
                    if (answers.getDisabilityConditions() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityConditions(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_condition, found more than one value in payload.");
                        answers.setDisabilityConditions(-1);
                    }
                    break;
                case "disability_impact":
                    if (answers.getDisabilityImpact() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityImpact(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_impact, found more than one value in payload.");
                        answers.setDisabilityImpact(-1);
                    }
                    break;
                case "disability_vision":
                    if (answers.getDisabilityVision() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityVision(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_vision, found more than one value in payload.");
                        answers.setDisabilityVision(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapDisabilityOtherFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "disability_hearing":
                    if (answers.getDisabilityHearing() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityHearing(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_hearing, found more than one value in payload.");
                        answers.setDisabilityHearing(-1);
                    }
                    break;
                case "disability_mobility":
                    if (answers.getDisabilityMobility() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityMobility(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_mobility, found more than one value in payload.");
                        answers.setDisabilityMobility(-1);
                    }
                    break;
                case "disability_dexterity":
                    if (answers.getDisabilityDexterity() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityDexterity(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_dexterity, found more than one value in payload.");
                        answers.setDisabilityDexterity(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapDisabilityOther2Fields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "disability_learning":
                    if (answers.getDisabilityLearning() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityLearning(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_learning, found more than one value in payload.");
                        answers.setDisabilityLearning(-1);
                    }
                    break;
                case "disability_memory":
                    if (answers.getDisabilityMemory() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityMemory(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_memory, found more than one value in payload.");
                        answers.setDisabilityMemory(-1);
                    }
                    break;
                case "disability_mental_health":
                    if (answers.getDisabilityMentalHealth() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityMentalHealth(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_mental_health, found more than one value in payload.");
                        answers.setDisabilityMentalHealth(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapDisabilityOther3Fields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            switch (payloadContent.getFieldName()) {
                case "disability_stamina":
                    if (answers.getDisabilityStamina() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityStamina(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_stamina, found more than one value in payload.");
                        answers.setDisabilityStamina(-1);
                    }
                    break;
                case "disability_social":
                    if (answers.getDisabilitySocial() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilitySocial(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_social, found more than one value in payload.");
                        answers.setDisabilitySocial(-1);
                    }
                    break;
                case "disability_other":
                    if (answers.getDisabilityOther() == null) {
                        if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                            answers.setDisabilityOther(Integer.valueOf(payloadContent.getFieldValue()));
                        }
                    } else {
                        log.error("Invalid answer for disability_other, found more than one value in payload.");
                        answers.setDisabilityOther(-1);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void mapDisabilityNoneAndPregnancyFields(PcqPayloadContents[] payloadContents, PcqAnswers answers) {
        for (PcqPayloadContents payloadContent : payloadContents) {
            if (DISABILITY_NONE.equals(payloadContent.getFieldName())) {
                if (answers.getDisabilityNone() == null) {
                    if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                        answers.setDisabilityNone(Integer.valueOf(payloadContent.getFieldValue()));
                    }
                } else {
                    log.error("Invalid answer for disability_none, found more than one value in payload.");
                    answers.setDisabilityNone(-1);
                }
            } else if (PREGNANCY.equals(payloadContent.getFieldName())) {
                if (answers.getPregnancy() == null) {
                    if (!StringUtils.isEmpty(payloadContent.getFieldValue())) {
                        answers.setPregnancy(Integer.valueOf(payloadContent.getFieldValue()));
                    }
                } else {
                    log.error("Invalid answer for pregnancy, found more than one value in payload.");
                    answers.setPregnancy(-1);
                }
            }
        }
    }
}
