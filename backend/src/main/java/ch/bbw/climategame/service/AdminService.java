package ch.bbw.climategame.service;

import ch.bbw.climategame.config.AppProperties;
import ch.bbw.climategame.dto.response.AdminSessionResponse;
import ch.bbw.climategame.dto.response.RoundHistoryDto;
import ch.bbw.climategame.entity.GameSession;
import ch.bbw.climategame.entity.Player;
import ch.bbw.climategame.entity.RoundInvestment;
import ch.bbw.climategame.entity.RoundPhase;
import ch.bbw.climategame.entity.SessionStatus;
import ch.bbw.climategame.repository.GameSessionRepository;
import ch.bbw.climategame.repository.PlayerRepository;
import ch.bbw.climategame.repository.RoundInvestmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final GameSessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final RoundInvestmentRepository investmentRepository;
    private final GameEngineService gameEngineService;
    private final AppProperties appProperties;

    public AdminService(
            GameSessionRepository sessionRepository,
            PlayerRepository playerRepository,
            RoundInvestmentRepository investmentRepository,
            GameEngineService gameEngineService,
            AppProperties appProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.playerRepository = playerRepository;
        this.investmentRepository = investmentRepository;
        this.gameEngineService = gameEngineService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AdminSessionResponse createSession(BigDecimal riskLevel) {
        if (riskLevel == null || riskLevel.compareTo(BigDecimal.ZERO) < 0 || riskLevel.compareTo(BigDecimal.ONE) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Risiko-Level muss zwischen 0.00 und 1.00 liegen (z.B. 0.90, 0.50, 0.10)."
            );
        }

        GameSession session = new GameSession();
        session.setCode(generateUniqueRoomCode());
        session.setStatus(SessionStatus.LOBBY);
        session.setRiskLevel(riskLevel.setScale(2, RoundingMode.HALF_UP));
        session.setCurrentRound(0);
        session.setRoundPhase(RoundPhase.REGULAR);
        session.setTargetSumRappen(appProperties.getGame().getTargetSumRappen());
        session.setTotalPotRappen(0);

        GameSession saved = sessionRepository.save(session);
        return buildAdminSessionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminSessionResponse> getAllSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::buildAdminSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSessionResponse getSession(UUID sessionId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));
        return buildAdminSessionResponse(session);
    }

    @Transactional
    public AdminSessionResponse startSession(UUID sessionId) {
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.LOBBY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session befindet sich nicht mehr im Lobby-Status.");
        }

        int connectedCount = playerRepository.countBySessionId(session.getId());
        int requiredGroupSize = appProperties.getGame().getGroupSize();
        if (connectedCount < requiredGroupSize) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Spielstart erst möglich, wenn alle " + requiredGroupSize + " Spieler verbunden sind (aktuell: " + connectedCount + "/" + requiredGroupSize + ")."
            );
        }

        Instant now = Instant.now();
        session.setStatus(SessionStatus.RUNNING);
        session.setCurrentRound(1);
        session.setRoundPhase(RoundPhase.REGULAR);
        session.setRoundStartTime(now);
        session.setRoundDeadline(now.plusSeconds(appProperties.getGame().getRegularPhaseSeconds()));
        session.setPausedAt(null);
        session.setPauseRemainingMs(null);

        GameSession saved = sessionRepository.save(session);
        return buildAdminSessionResponse(saved);
    }

    @Transactional
    public AdminSessionResponse pauseSession(UUID sessionId) {
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nur laufende Sessions können pausiert werden.");
        }

        Instant now = Instant.now();
        long remainingMs = 0L;
        if (session.getRoundDeadline() != null) {
            remainingMs = Math.max(0L, Duration.between(now, session.getRoundDeadline()).toMillis());
        }

        session.setPausedAt(now);
        session.setPauseRemainingMs(remainingMs);
        session.setStatus(SessionStatus.PAUSED);

        GameSession saved = sessionRepository.save(session);
        return buildAdminSessionResponse(saved);
    }

    @Transactional
    public AdminSessionResponse resumeSession(UUID sessionId) {
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session ist nicht pausiert.");
        }

        Instant now = Instant.now();
        long remainingMs = session.getPauseRemainingMs() != null
                ? session.getPauseRemainingMs()
                : appProperties.getGame().getRegularPhaseSeconds() * 1000L;

        session.setRoundDeadline(now.plusMillis(remainingMs));
        session.setPausedAt(null);
        session.setPauseRemainingMs(null);
        session.setStatus(SessionStatus.RUNNING);

        GameSession saved = sessionRepository.save(session);
        return buildAdminSessionResponse(saved);
    }

    @Transactional(readOnly = true)
    public String exportSessionCsv(UUID sessionId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        Map<Integer, Player> playerByNumber = players.stream()
                .collect(Collectors.toMap(Player::getPlayerNumber, p -> p));

        List<RoundInvestment> investments = investmentRepository
                .findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("session_id,session_code,risk_treatment,target_sum_rappen,total_invested_rappen,target_reached,risk_occurred,round,player_number,invested_rappen,is_auto_bet,submitted_at,final_payout_rappen\n");

        for (RoundInvestment inv : investments) {
            Player p = playerByNumber.get(inv.getPlayerNumber());
            int finalPayout = (p != null && p.getFinalPayoutRappen() != null)
                    ? p.getFinalPayoutRappen()
                    : (p != null ? p.getCurrentBalanceRappen() : 0);

            sb.append('"').append(session.getId()).append("\",")
                    .append('"').append(session.getCode()).append("\",")
                    .append(session.getRiskLevel().toPlainString()).append(',')
                    .append(session.getTargetSumRappen()).append(',')
                    .append(session.getTotalPotRappen()).append(',')
                    .append(session.getIsGoalReached() != null ? session.getIsGoalReached() : false).append(',')
                    .append(session.getRiskOccurred() != null ? session.getRiskOccurred() : false).append(',')
                    .append(inv.getRoundNumber()).append(',')
                    .append(inv.getPlayerNumber()).append(',')
                    .append(inv.getAmountRappen()).append(',')
                    .append(inv.isAutoBet()).append(',')
                    .append('"').append(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "").append("\",")
                    .append(finalPayout)
                    .append('\n');
        }

        return sb.toString();
    }

    private AdminSessionResponse buildAdminSessionResponse(GameSession session) {
        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        List<RoundInvestment> investments = investmentRepository
                .findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        int groupSize = appProperties.getGame().getGroupSize();
        int totalRounds = appProperties.getGame().getTotalRounds();
        int currentRound = session.getCurrentRound();

        Map<Integer, Player> playerByNumber = players.stream()
                .collect(Collectors.toMap(Player::getPlayerNumber, p -> p));

        Map<Integer, RoundInvestment> currentRoundInvestments = investments.stream()
                .filter(inv -> inv.getRoundNumber() == currentRound)
                .collect(Collectors.toMap(RoundInvestment::getPlayerNumber, inv -> inv, (a, b) -> a));

        List<AdminSessionResponse.AdminPlayerDto> adminPlayers = new ArrayList<>();
        int defaultInitialBalance = appProperties.getGame().getStartAmountRappen();

        for (int pNum = 1; pNum <= groupSize; pNum++) {
            Player p = playerByNumber.get(pNum);
            RoundInvestment currentInv = currentRoundInvestments.get(pNum);
            if (p != null) {
                boolean isReady = session.getStatus() == SessionStatus.LOBBY
                        || session.getStatus() == SessionStatus.FINISHED
                        || currentInv != null;
                int totalInvested = p.getInitialBalanceRappen() - p.getCurrentBalanceRappen();
                adminPlayers.add(new AdminSessionResponse.AdminPlayerDto(
                        pNum,
                        true,
                        isReady,
                        p.getInitialBalanceRappen(),
                        p.getCurrentBalanceRappen(),
                        totalInvested,
                        currentInv != null ? currentInv.getAmountRappen() : null,
                        p.getFinalPayoutRappen(),
                        p.getLastSeenAt()
                ));
            } else {
                adminPlayers.add(new AdminSessionResponse.AdminPlayerDto(
                        pNum,
                        false,
                        false,
                        defaultInitialBalance,
                        defaultInitialBalance,
                        0,
                        null,
                        null,
                        null
                ));
            }
        }

        List<RoundHistoryDto> history = gameEngineService.buildCompletedRoundHistory(session, investments, groupSize);
        int riskPercent = (int) Math.round(session.getRiskLevel().doubleValue() * 100.0);

        return new AdminSessionResponse(
                session.getId(),
                session.getCode(),
                session.getStatus(),
                session.getRiskLevel(),
                riskPercent,
                session.getCurrentRound(),
                totalRounds,
                session.getRoundPhase(),
                gameEngineService.calculateRemainingSeconds(session),
                session.getStatus() == SessionStatus.PAUSED,
                session.getTargetSumRappen(),
                session.getTotalPotRappen(),
                players.size(),
                groupSize,
                session.getIsGoalReached(),
                session.getRiskOccurred(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                adminPlayers,
                history
        );
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder sb = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (!sessionRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Konnte keinen eindeutigen 4-stelligen Raumcode generieren.");
    }
}
