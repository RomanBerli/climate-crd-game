package ch.bbw.climategame.dto.request;

import jakarta.validation.constraints.NotNull;

public record InvestRequest(
        @NotNull(message = "Investitionsbetrag darf nicht leer sein.")
        Integer amountRappen
) {
}
