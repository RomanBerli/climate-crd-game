package ch.bbw.climategame.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinLobbyRequest(
        @NotBlank(message = "Bitte einen gültigen Raumcode eingeben.")
        @Size(min = 4, max = 4, message = "Der Raumcode muss genau 4 Zeichen lang sein.")
        String code
) {
}
