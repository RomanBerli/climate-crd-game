package ch.bbw.climategame.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ContractReviewRequest(@NotBlank String action) {
}
