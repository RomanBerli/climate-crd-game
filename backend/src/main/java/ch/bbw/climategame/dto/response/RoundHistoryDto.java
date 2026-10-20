package ch.bbw.climategame.dto.response;

import java.util.List;

public record RoundHistoryDto(
        int round,
        List<PlayerInvestmentDto> investments
) {
    public record PlayerInvestmentDto(
            int playerNumber,
            int amountRappen,
            boolean isAutoBet
    ) {
    }
}
