package ch.bbw.climategame.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitSurveyRequest(
        @NotNull(message = "Bitte schätzen Sie die Wahrscheinlichkeit (0–100%).")
        @Min(value = 0, message = "Wahrscheinlichkeit muss mindestens 0% sein.")
        @Max(value = 100, message = "Wahrscheinlichkeit darf maximal 100% sein.")
        Integer expectedProbabilityPct,

        @NotBlank(message = "Bitte wählen Sie eine Antwort zur Reaktion auf tiefe Beiträge.")
        String reactionToUnfairness,

        @NotEmpty(message = "Bitte wählen Sie Ihre Strategie aus.")
        List<String> motivationRanking,

        String contractJoinedReason,
        String contractNotJoinedReason,
        String contractLeftReason,
        String contractStayedReason,

        @NotNull(message = "Bitte geben Sie Ihr Alter an.")
        @Min(value = 5, message = "Bitte ein gültiges Alter eingeben.")
        @Max(value = 120, message = "Bitte ein gültiges Alter eingeben.")
        Integer age,

        @NotBlank(message = "Bitte wählen Sie Ihr Geschlecht aus.")
        String gender,

        @NotBlank(message = "Bitte wählen Sie Ihre gewünschte Auszahlungsart.")
        String twintPhone,

        Integer lowRiskInvestRappen
) {
    public SubmitSurveyRequest(
            Integer expectedProbabilityPct,
            String reactionToUnfairness,
            List<String> motivationRanking,
            String contractJoinedReason,
            String contractNotJoinedReason,
            String contractLeftReason,
            String contractStayedReason,
            Integer age,
            String gender,
            String twintPhone
    ) {
        this(
                expectedProbabilityPct,
                reactionToUnfairness,
                motivationRanking,
                contractJoinedReason,
                contractNotJoinedReason,
                contractLeftReason,
                contractStayedReason,
                age,
                gender,
                twintPhone,
                null
        );
    }
}
