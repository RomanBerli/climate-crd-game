package ch.bbw.climategame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_player_session_num", columnNames = {"session_id", "player_number"}),
                @UniqueConstraint(name = "uk_player_token", columnNames = {"session_token"})
        }
)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @Column(name = "player_number", nullable = false)
    private int playerNumber;

    @Column(name = "session_token", length = 64, nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "initial_balance_rappen", nullable = false)
    private int initialBalanceRappen;

    @Column(name = "current_balance_rappen", nullable = false)
    private int currentBalanceRappen;

    @Column(name = "final_payout_rappen")
    private Integer finalPayoutRappen;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @PrePersist
    protected void onCreate() {
        if (this.lastSeenAt == null) {
            this.lastSeenAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GameSession getSession() {
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public int getInitialBalanceRappen() {
        return initialBalanceRappen;
    }

    public void setInitialBalanceRappen(int initialBalanceRappen) {
        this.initialBalanceRappen = initialBalanceRappen;
    }

    public int getCurrentBalanceRappen() {
        return currentBalanceRappen;
    }

    public void setCurrentBalanceRappen(int currentBalanceRappen) {
        this.currentBalanceRappen = currentBalanceRappen;
    }

    public Integer getFinalPayoutRappen() {
        return finalPayoutRappen;
    }

    public void setFinalPayoutRappen(Integer finalPayoutRappen) {
        this.finalPayoutRappen = finalPayoutRappen;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
