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

        @NotBlank(message = "Bitte wählen Sie eine Antwort zur Reaktion auf unfaire Beiträge.")
        String reactionToUnfairness,

        @NotEmpty(message = "Bitte sortieren Sie die Prioritäten.")
        List<String> motivationRanking,

        String contractJoinedReason,
        String contractNotJoinedReason,
        String contractLeftReason,
        String contractStayedReason,

        @NotNull(message = "Bitte geben Sie Ihr Alter an.")
        @Min(value = 5, message = "Bitte ein gültiges Alter eingeben.")
        @Max(value = 120, message = "Bitte ein gültiges Alter eingeben.")
        Integer age,

        @NotBlank(message = "Bitte wählen Sie Ihre Identifikation aus.")
        String gender,

        @NotBlank(message = "Bitte geben Sie eine Twint-Telefonnummer für die Auszahlung an.")
        String twintPhone
) {
}
