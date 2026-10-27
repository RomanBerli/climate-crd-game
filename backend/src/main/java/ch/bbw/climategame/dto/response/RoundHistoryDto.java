package ch.bbw.climategame.dto.response;

import java.util.List;

public record RoundHistoryDto(
        int round,
        int roundTotalRappen,
        List<PlayerInvestmentDto> investments
) {
    public record PlayerInvestmentDto(
            int playerNumber,
            int amountRappen,
            boolean isAutoBet
    ) {
    }
}
