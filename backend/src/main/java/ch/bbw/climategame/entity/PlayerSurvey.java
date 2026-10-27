package ch.bbw.climategame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "player_survey",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_survey_session_player", columnNames = {"session_id", "player_number"})
        }
)
public class PlayerSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private Player player;

    @Column(name = "player_number", nullable = false)
    private int playerNumber;

    @Column(name = "expected_probability_pct", nullable = false)
    private int expectedProbabilityPct;

    @Column(name = "reaction_to_unfairness", length = 255, nullable = false)
    private String reactionToUnfairness;

    @Column(name = "motivation_ranking", length = 1000, nullable = false)
    private String motivationRanking;

    @Column(name = "contract_joined_reason", length = 500)
    private String contractJoinedReason;

    @Column(name = "contract_not_joined_reason", length = 500)
    private String contractNotJoinedReason;

    @Column(name = "contract_left_reason", length = 500)
    private String contractLeftReason;

    @Column(name = "contract_stayed_reason", length = 500)
    private String contractStayedReason;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "gender", length = 50, nullable = false)
    private String gender;

    @Column(name = "twint_phone", length = 100, nullable = false)
    private String twintPhone;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = Instant.now();
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

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    public int getExpectedProbabilityPct() {
        return expectedProbabilityPct;
    }

    public void setExpectedProbabilityPct(int expectedProbabilityPct) {
        this.expectedProbabilityPct = expectedProbabilityPct;
    }

    public String getReactionToUnfairness() {
        return reactionToUnfairness;
    }

    public void setReactionToUnfairness(String reactionToUnfairness) {
        this.reactionToUnfairness = reactionToUnfairness;
    }

    public String getMotivationRanking() {
        return motivationRanking;
    }

    public void setMotivationRanking(String motivationRanking) {
        this.motivationRanking = motivationRanking;
    }

    public String getContractJoinedReason() {
        return contractJoinedReason;
    }

    public void setContractJoinedReason(String contractJoinedReason) {
        this.contractJoinedReason = contractJoinedReason;
    }

    public String getContractNotJoinedReason() {
        return contractNotJoinedReason;
    }

    public void setContractNotJoinedReason(String contractNotJoinedReason) {
        this.contractNotJoinedReason = contractNotJoinedReason;
    }

    public String getContractLeftReason() {
        return contractLeftReason;
    }

    public void setContractLeftReason(String contractLeftReason) {
        this.contractLeftReason = contractLeftReason;
    }

    public String getContractStayedReason() {
        return contractStayedReason;
    }

    public void setContractStayedReason(String contractStayedReason) {
        this.contractStayedReason = contractStayedReason;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTwintPhone() {
        return twintPhone;
    }

    public void setTwintPhone(String twintPhone) {
        this.twintPhone = twintPhone;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
