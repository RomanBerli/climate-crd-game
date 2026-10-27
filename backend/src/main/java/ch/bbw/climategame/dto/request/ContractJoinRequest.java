package ch.bbw.climategame.dto.request;

public record ContractJoinRequest(
        String choice,
        Boolean join
) {
    public String resolvedChoice() {
        if (choice != null && !choice.isBlank()) {
            return choice.trim().toUpperCase();
        }
        if (join != null) {
            return join ? "YES" : "NO";
        }
        return "";
    }
}
