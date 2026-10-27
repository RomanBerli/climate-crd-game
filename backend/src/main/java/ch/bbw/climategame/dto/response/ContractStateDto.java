package ch.bbw.climategame.dto.response;

import ch.bbw.climategame.entity.RoundPhase;

public record ContractStateDto(
        RoundPhase roundPhase,
        boolean contractActive,
        boolean contractAbolished,
        int memberCount,
        int minInvestmentRappen,
        boolean canLeaveContractNow,
        boolean hasDecidedCurrentContractPhase,
        String lastContractEvent
) {
}
