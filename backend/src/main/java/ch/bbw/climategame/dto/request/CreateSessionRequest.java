package ch.bbw.climategame.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSessionRequest(
        @NotNull(message = "Risiko-Treatment (0.90, 0.50 oder 0.10) ist erforderlich.")
        BigDecimal riskLevel
) {
}
