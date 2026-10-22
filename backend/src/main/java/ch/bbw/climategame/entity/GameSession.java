package ch.bbw.climategame.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "game_session")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", length = 4, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SessionStatus status = SessionStatus.LOBBY;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", length = 30, nullable = false)
    private GameMode gameMode = GameMode.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_phase", length = 30, nullable = false)
    private RoundPhase roundPhase = RoundPhase.INVESTMENT;

    @Column(name = "contract_active", nullable = false)
    private boolean contractActive = false;

    @Column(name = "contract_abolished", nullable = false)
    private boolean contractAbolished = false;

    @Column(name = "vote_initiated_round")
    private Integer voteInitiatedRound;

    @Column(name = "vote_initiator_player_number")
    private Integer voteInitiatorPlayerNumber;

    @Column(name = "last_contract_event", length = 255)
    private String lastContractEvent;

    @Column(name = "risk_level", precision = 4, scale = 2, nullable = false)
    private BigDecimal riskLevel;

    @Column(name = "current_round", nullable = false)
    private int currentRound = 0;

    @Column(name = "round_start_time")
    private Instant roundStartTime;

    @Column(name = "target_sum_rappen", nullable = false)
    private int targetSumRappen;

    @Column(name = "total_pot_rappen", nullable = false)
    private int totalPotRappen = 0;

    @Column(name = "is_goal_reached")
    private Boolean isGoalReached;

    @Column(name = "risk_occurred")
    private Boolean riskOccurred;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("playerNumber ASC")
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNumber ASC, playerNumber ASC")
    private List<RoundInvestment> investments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public RoundPhase getRoundPhase() {
        return roundPhase;
    }

    public void setRoundPhase(RoundPhase roundPhase) {
        this.roundPhase = roundPhase;
    }

    public boolean isContractActive() {
        return contractActive;
    }

    public void setContractActive(boolean contractActive) {
        this.contractActive = contractActive;
    }

    public boolean isContractAbolished() {
        return contractAbolished;
    }

    public void setContractAbolished(boolean contractAbolished) {
        this.contractAbolished = contractAbolished;
    }

    public Integer getVoteInitiatedRound() {
        return voteInitiatedRound;
    }

    public void setVoteInitiatedRound(Integer voteInitiatedRound) {
        this.voteInitiatedRound = voteInitiatedRound;
    }

    public Integer getVoteInitiatorPlayerNumber() {
        return voteInitiatorPlayerNumber;
    }

    public void setVoteInitiatorPlayerNumber(Integer voteInitiatorPlayerNumber) {
        this.voteInitiatorPlayerNumber = voteInitiatorPlayerNumber;
    }

    public String getLastContractEvent() {
        return lastContractEvent;
    }

    public void setLastContractEvent(String lastContractEvent) {
        this.lastContractEvent = lastContractEvent;
    }

    public BigDecimal getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(BigDecimal riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public Instant getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(Instant roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public int getTargetSumRappen() {
        return targetSumRappen;
    }

    public void setTargetSumRappen(int targetSumRappen) {
        this.targetSumRappen = targetSumRappen;
    }

    public int getTotalPotRappen() {
        return totalPotRappen;
    }

    public void setTotalPotRappen(int totalPotRappen) {
        this.totalPotRappen = totalPotRappen;
    }

    public Boolean getIsGoalReached() {
        return isGoalReached;
    }

    public void setIsGoalReached(Boolean isGoalReached) {
        this.isGoalReached = isGoalReached;
    }

    public Boolean getRiskOccurred() {
        return riskOccurred;
    }

    public void setRiskOccurred(Boolean riskOccurred) {
        this.riskOccurred = riskOccurred;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<RoundInvestment> getInvestments() {
        return investments;
    }

    public void setInvestments(List<RoundInvestment> investments) {
        this.investments = investments;
    }
}
