package uk.gov.hmcts.reform.pcqloader.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcq.commons.model.PcqAnswers;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@SuppressWarnings({"PMD.GodClass","PMD.TooManyMethods"})
public class PayloadValidationHelper {

    private final String[] submitAnswerOtherFields = {"languageOther", "genderOther", "sexualityOther",
        "ethnicityOther", "religionOther"};

    private final String[] submitAnswerOtherIntFieldValues = {"2", "2", "4", "4,8,13,16,18", "8"};

    public void validateDisabilityNone(PcqAnswers answers) {
        // If the disability_none has value of "1" then all other disabilities should be set to null.
        if (answers.getDisabilityNone() != null && answers.getDisabilityNone() > 0) {
            setDisabilityValuesToNull(answers);
            log.info("Setting disability values to null because disability None value is 1");
        }
    }

    public void validateDisabilityConditions(PcqAnswers answers) {
        //If the disability conditions has value of "2" or "0" (No or prefer not to say) then
        //Disability impact should be set to null along with the disabilities.
        if (answers.getDisabilityConditions() != null && (answers.getDisabilityConditions() == 2
            || answers.getDisabilityConditions() == 0)) {
            answers.setDisabilityImpact(null);
            answers.setDisabilityNone(null);
            setDisabilityValuesToNull(answers);
            log.info("Setting disability values to null because disability Condition value is not 1");
        }
    }

    public void validateDisabilityImpact(PcqAnswers answers) {
        //If the disability impact has value of "3" or "0" (No or prefer not to say) then
        //Disabilities should be set to null.
        if (answers.getDisabilityImpact() != null && (answers.getDisabilityImpact() == 3
            || answers.getDisabilityImpact() == 0)) {
            setDisabilityValuesToNull(answers);
            answers.setDisabilityNone(null);
            log.info("Setting disability values to null because disability impact value is not 1 or 2");
        }
    }

    public void validateLanguageLevel(PcqAnswers answers) {
        //If user has selected the main language as English/Welsh or Prefer Not to Say then
        //set the language level to null.
        if (answers.getEnglishLanguageLevel() != null && answers.getLanguageMain() != null
            && (answers.getLanguageMain() == 1 || answers.getLanguageMain() == 0)) {
            answers.setEnglishLanguageLevel(null);
            log.info("Setting english language level to null because main language value is not 2");
        }
    }

    public void validateAndCorrectOtherFields(PcqAnswers answers) {
        for (int i = 0; i < submitAnswerOtherFields.length; i++) {
            switch (submitAnswerOtherFields[i]) {
                case "languageOther" :
                    validateOtherLanguage(answers, i);
                    break;
                case "genderOther" :
                    validateOtherGender(answers, i);
                    break;
                case "sexualityOther" :
                    validateOtherSexuality(answers, i);
                    break;
                case "ethnicityOther" :
                    validateOtherEthnicity(answers, i);
                    break;
                case "religionOther" :
                    validateOtherReligion(answers, i);
                    break;
                default :
                    break;
            }
        }
    }

    public void validateOtherLanguage(PcqAnswers answers, int index) {
        if (answers.getLanguageOther() != null
            && answers.getLanguageMain() != null && !submitAnswerOtherIntFieldValues[index]
            .contains(String.valueOf(answers.getLanguageMain()))) {
            answers.setLanguageOther(null);
            if (answers.getLanguageMain() != -1) {
                answers.setEnglishLanguageLevel(null);
            }
        }
    }

    public void validateOtherGender(PcqAnswers answers, int index) {
        if (answers.getGenderOther() != null
            && answers.getGenderDifferent() != null && !submitAnswerOtherIntFieldValues[index]
            .contains(String.valueOf(answers.getGenderDifferent()))) {
            answers.setGenderOther(null);
        }
    }

    public void validateOtherSexuality(PcqAnswers answers, int index) {
        if (answers.getSexualityOther() != null
            && answers.getSexuality() != null && !submitAnswerOtherIntFieldValues[index]
            .contains(String.valueOf(answers.getSexuality()))) {
            answers.setSexualityOther(null);
        }
    }

    public void validateOtherEthnicity(PcqAnswers answers, int index) {
        if (answers.getEthnicityOther() != null && answers.getEthnicity() != null) {
            List<String> splitList = Arrays.asList(submitAnswerOtherIntFieldValues[index].split(","));
            if (! splitList.contains(String.valueOf(answers.getEthnicity()))) {
                answers.setEthnicityOther(null);
            }
        }
    }

    public void validateOtherReligion(PcqAnswers answers, int index) {
        if (answers.getReligionOther() != null
            && answers.getReligion() != null && !submitAnswerOtherIntFieldValues[index]
            .contains(String.valueOf(answers.getReligion()))) {
            answers.setReligionOther(null);
        }
    }

    public boolean isDobProvided(PcqAnswers answers) {
        if (answers.getDobProvided() != null && answers.getDobProvided() == 0) {
            answers.setDob(null);
            return false;
        }

        return true;
    }

    private void setDisabilityValuesToNull(PcqAnswers pcqAnswers) {
        pcqAnswers.setDisabilityVision(null);
        pcqAnswers.setDisabilityHearing(null);
        pcqAnswers.setDisabilityMobility(null);
        pcqAnswers.setDisabilityDexterity(null);
        pcqAnswers.setDisabilityLearning(null);
        pcqAnswers.setDisabilityMemory(null);
        pcqAnswers.setDisabilityMentalHealth(null);
        pcqAnswers.setDisabilityStamina(null);
        pcqAnswers.setDisabilitySocial(null);
        pcqAnswers.setDisabilityOther(null);
        pcqAnswers.setDisabilityConditionOther(null);
    }

}
