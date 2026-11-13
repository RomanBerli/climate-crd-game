package ch.bbw.climategame.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitSurveyRequest(
        @Min(value = 0, message = "Wahrscheinlichkeit muss mindestens 0% sein.")
        @Max(value = 100, message = "Wahrscheinlichkeit darf maximal 100% sein.")
        Integer expectedProbabilityPct,

        @NotBlank(message = "Bitte wähle eine Antwort zur Reaktion auf tiefe Beiträge.")
        @Size(max = 250, message = "Die Antwort zur Reaktion auf tiefe Beiträge ist zu lang.")
        String reactionToUnfairness,

        @NotEmpty(message = "Bitte wähle deine Strategie aus.")
        @Size(max = 10, message = "Zu viele Antworten bei der Strategie.")
        List<@NotBlank(message = "Bitte wähle deine Strategie aus.") @Size(max = 250, message = "Die Antwort zur Strategie ist zu lang.") String> motivationRanking,

        @Size(max = 500, message = "Der Grund für den Beitritt ist zu lang (maximal 500 Zeichen).")
        String contractJoinedReason,
        @Size(max = 500, message = "Der Grund für den Nicht-Beitritt ist zu lang (maximal 500 Zeichen).")
        String contractNotJoinedReason,
        @Size(max = 500, message = "Der Grund für den Austritt ist zu lang (maximal 500 Zeichen).")
        String contractLeftReason,
        @Size(max = 500, message = "Die Antwort ist zu lang (maximal 500 Zeichen).")
        String contractStayedReason,

        @NotNull(message = "Bitte gib dein Alter an.")
        @Min(value = 5, message = "Bitte ein gültiges Alter eingeben.")
        @Max(value = 120, message = "Bitte ein gültiges Alter eingeben.")
        Integer age,

        @NotBlank(message = "Bitte wähle dein Geschlecht aus.")
        @Size(max = 50, message = "Die Angabe zum Geschlecht ist zu lang.")
        String gender,

        @NotBlank(message = "Bitte wähle deine gewünschte Auszahlungsart.")
        @Size(max = 100, message = "Die Angabe zur Auszahlung ist zu lang.")
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
