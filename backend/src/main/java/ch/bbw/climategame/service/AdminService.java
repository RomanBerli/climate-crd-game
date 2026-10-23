package ch.bbw.climategame.service;

import ch.bbw.climategame.dto.response.AdminSessionResponse;
import ch.bbw.climategame.entity.GameMode;
import ch.bbw.climategame.entity.GameSession;
import ch.bbw.climategame.entity.Player;
import ch.bbw.climategame.entity.PlayerSurvey;
import ch.bbw.climategame.entity.RoundInvestment;
import ch.bbw.climategame.entity.RoundPhase;
import ch.bbw.climategame.entity.SessionStatus;
import ch.bbw.climategame.repository.GameSessionRepository;
import ch.bbw.climategame.repository.PlayerRepository;
import ch.bbw.climategame.repository.PlayerSurveyRepository;
import ch.bbw.climategame.repository.RoundInvestmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final GameSessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final RoundInvestmentRepository investmentRepository;
    private final PlayerSurveyRepository surveyRepository;
    private final GameEngineService gameEngineService;

    public AdminService(
            GameSessionRepository sessionRepository,
            PlayerRepository playerRepository,
            RoundInvestmentRepository investmentRepository,
            PlayerSurveyRepository surveyRepository,
            GameEngineService gameEngineService
    ) {
        this.sessionRepository = sessionRepository;
        this.playerRepository = playerRepository;
        this.investmentRepository = investmentRepository;
        this.surveyRepository = surveyRepository;
        this.gameEngineService = gameEngineService;
    }

    @Transactional
    public AdminSessionResponse createSession(GameMode requestedMode) {
        GameMode mode = requestedMode != null ? requestedMode : GameMode.NORMAL;

        BigDecimal configuredRisk = gameEngineService.getRiskLevel();
        if (configuredRisk == null || configuredRisk.compareTo(BigDecimal.ZERO) < 0 || configuredRisk.compareTo(BigDecimal.ONE) > 0) {
            configuredRisk = new BigDecimal("0.90");
        }

        GameSession session = new GameSession();
        session.setCode(generateUniqueRoomCode());
        session.setStatus(SessionStatus.LOBBY);
        session.setGameMode(mode);
        session.setRoundPhase(RoundPhase.INVESTMENT);
        session.setContractActive(false);
        session.setContractAbolished(false);
        session.setRiskLevel(configuredRisk.setScale(2, RoundingMode.HALF_UP));
        session.setCurrentRound(0);
        session.setTargetSumRappen(gameEngineService.getTargetSumRappen());
        session.setTotalPotRappen(0);

        GameSession saved = sessionRepository.save(session);
        return buildAdminSessionResponse(saved);
    }

    @Transactional
    public List<AdminSessionResponse> getAllSessions() {
        gameEngineService.kickInactivePlayers();
        return sessionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::buildAdminSessionResponse)
                .toList();
    }

    @Transactional
    public AdminSessionResponse getSession(UUID sessionId) {
        gameEngineService.kickInactivePlayers();
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));
        return buildAdminSessionResponse(session);
    }

    @Transactional
    public AdminSessionResponse startSession(UUID sessionId) {
        gameEngineService.kickInactivePlayers();
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.LOBBY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session befindet sich nicht mehr im Lobby-Status.");
        }

        int connectedCount = playerRepository.countBySessionId(session.getId());
        int requiredGroupSize = gameEngineService.getGroupSize();
        if (connectedCount < requiredGroupSize) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Spielstart erst möglich, wenn alle " + requiredGroupSize + " Spieler verbunden sind (aktuell: " + connectedCount + "/" + requiredGroupSize + ")."
            );
        }

        session.setStatus(SessionStatus.RUNNING);
        session.setCurrentRound(1);
        session.setRoundPhase(session.getGameMode() == GameMode.WITH_CONTRACTS ? RoundPhase.CONTRACT_JOIN : RoundPhase.INVESTMENT);
        session.setRoundStartTime(Instant.now());

        GameSession saved = sessionRepository.save(session);
        return buildAdminSessionResponse(saved);
    }

    @Transactional
    public AdminSessionResponse advanceRound(UUID sessionId) {
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.RUNNING || session.getRoundPhase() != RoundPhase.INVESTMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Die Runde kann erst weitergeschaltet werden, wenn die Einzahlungsphase abgeschlossen ist."
            );
        }

        int currentRound = session.getCurrentRound();
        int requiredGroupSize = gameEngineService.getGroupSize();
        int submittedCount = investmentRepository.countBySessionIdAndRoundNumber(session.getId(), currentRound);

        if (submittedCount < requiredGroupSize) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Runde " + currentRound + " kann erst weitergeklickt werden, wenn alle " + requiredGroupSize
                            + " Spieler gewählt haben (aktuell: " + submittedCount + "/" + requiredGroupSize + ")."
            );
        }

        gameEngineService.advanceRoundOrFinish(session);
        return buildAdminSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public String exportSessionCsv(UUID sessionId) {
        return exportSessionCsv(sessionId, "all");
    }

    @Transactional(readOnly = true)
    public String exportSessionCsv(UUID sessionId, String rawType) {
        String type = rawType == null ? "all" : rawType.trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "round_info", "roundinfo" -> exportRoundInfoCsv(sessionId);
            case "invest" -> exportInvestCsv(sessionId);
            case "survey" -> exportSurveyCsv(sessionId);
            case "payout", "auszahlung" -> exportPayoutCsv(sessionId);
            case "all" -> exportAllCombinedCsv(sessionId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannter CSV-Export-Typ: " + rawType);
        };
    }

    @Transactional(readOnly = true)
    public String exportRoundInfoCsv(UUID sessionId) {
        GameSession session = findSessionOrThrow(sessionId);
        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        List<RoundInvestment> investments = investmentRepository
                .findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        Map<Integer, Integer> totalInvestedByPlayer = investments.stream()
                .collect(Collectors.groupingBy(
                        RoundInvestment::getPlayerNumber,
                        Collectors.summingInt(RoundInvestment::getAmountRappen)
                ));

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("session_code,game_mode,target_sum_rappen,total_invested_rappen,target_reached,risk_occurred,player_number,player_total_invested_rappen,balance_before_risk_rappen,final_payout_rappen,final_payout_chf,ever_joined_contract,left_contract,in_contract_at_end,left_contract_at_round,contract_abolished,contract_abolished_at_round\n");

        for (Player p : players) {
            int playerTotalInvested = totalInvestedByPlayer.getOrDefault(p.getPlayerNumber(), 0);
            int balanceBeforeRisk = p.getCurrentBalanceRappen();
            int finalPayoutRappen = p.getFinalPayoutRappen() != null ? p.getFinalPayoutRappen() : balanceBeforeRisk;
            String finalPayoutChf = String.format(Locale.US, "%.2f", finalPayoutRappen / 100.0);

            sb.append(csvEscape(session.getCode())).append(',')
                    .append(csvEscape(session.getGameMode().name())).append(',')
                    .append(session.getTargetSumRappen()).append(',')
                    .append(session.getTotalPotRappen()).append(',')
                    .append(session.getIsGoalReached() != null ? session.getIsGoalReached() : false).append(',')
                    .append(session.getRiskOccurred() != null ? session.getRiskOccurred() : false).append(',')
                    .append(p.getPlayerNumber()).append(',')
                    .append(playerTotalInvested).append(',')
                    .append(balanceBeforeRisk).append(',')
                    .append(finalPayoutRappen).append(',')
                    .append(finalPayoutChf).append(',')
                    .append(p.isEverJoinedContract()).append(',')
                    .append(p.isLeftContract()).append(',')
                    .append(p.isInContract()).append(',')
                    .append(p.getLeftContractAtRound() != null ? p.getLeftContractAtRound() : "").append(',')
                    .append(session.isContractAbolished()).append(',')
                    .append(session.getContractAbolishedAtRound() != null ? session.getContractAbolishedAtRound() : "")
                    .append('\n');
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportInvestCsv(UUID sessionId) {
        GameSession session = findSessionOrThrow(sessionId);
        List<RoundInvestment> investments = investmentRepository
                .findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("session_code,round,player_number,invested_rappen,invested_chf,submitted_at,currently_in_contract\n");

        for (RoundInvestment inv : investments) {
            String investedChf = String.format(Locale.US, "%.2f", inv.getAmountRappen() / 100.0);
            sb.append(csvEscape(session.getCode())).append(',')
                    .append(inv.getRoundNumber()).append(',')
                    .append(inv.getPlayerNumber()).append(',')
                    .append(inv.getAmountRappen()).append(',')
                    .append(investedChf).append(',')
                    .append(csvEscape(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "")).append(',')
                    .append(inv.isCurrentlyInContract())
                    .append('\n');
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportSurveyCsv(UUID sessionId) {
        GameSession session = findSessionOrThrow(sessionId);
        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        Map<Integer, PlayerSurvey> surveyByNumber = surveyRepository
                .findBySessionIdOrderByPlayerNumberAsc(session.getId())
                .stream()
                .collect(Collectors.toMap(PlayerSurvey::getPlayerNumber, s -> s, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("session_code,player_number,survey_expected_probability_pct,survey_reaction_unfairness,survey_motivation_ranking,survey_contract_joined_reason,survey_contract_not_joined_reason,survey_contract_left_reason,survey_contract_stayed_reason,survey_age,survey_gender\n");

        for (Player p : players) {
            PlayerSurvey survey = surveyByNumber.get(p.getPlayerNumber());
            sb.append(csvEscape(session.getCode())).append(',')
                    .append(p.getPlayerNumber()).append(',')
                    .append(survey != null ? survey.getExpectedProbabilityPct() : "").append(',')
                    .append(csvEscape(survey != null ? survey.getReactionToUnfairness() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getMotivationRanking() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractJoinedReason() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractNotJoinedReason() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractLeftReason() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractStayedReason() : "")).append(',')
                    .append(survey != null ? survey.getAge() : "").append(',')
                    .append(csvEscape(survey != null ? survey.getGender() : ""))
                    .append('\n');
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportPayoutCsv(UUID sessionId) {
        GameSession session = findSessionOrThrow(sessionId);
        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        Map<Integer, PlayerSurvey> surveyByNumber = surveyRepository
                .findBySessionIdOrderByPlayerNumberAsc(session.getId())
                .stream()
                .collect(Collectors.toMap(PlayerSurvey::getPlayerNumber, s -> s, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("session_code,player_number,final_payout_rappen,final_payout_chf,survey_twint_number\n");

        for (Player p : players) {
            PlayerSurvey survey = surveyByNumber.get(p.getPlayerNumber());
            int finalPayoutRappen = p.getFinalPayoutRappen() != null ? p.getFinalPayoutRappen() : p.getCurrentBalanceRappen();
            String finalPayoutChf = String.format(Locale.US, "%.2f", finalPayoutRappen / 100.0);

            sb.append(csvEscape(session.getCode())).append(',')
                    .append(p.getPlayerNumber()).append(',')
                    .append(finalPayoutRappen).append(',')
                    .append(finalPayoutChf).append(',')
                    .append(csvEscape(survey != null ? survey.getTwintPhone() : ""))
                    .append('\n');
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportAllCombinedCsv(UUID sessionId) {
        GameSession session = findSessionOrThrow(sessionId);

        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        Map<Integer, Player> playerByNumber = players.stream()
                .collect(Collectors.toMap(Player::getPlayerNumber, p -> p));

        Map<Integer, PlayerSurvey> surveyByNumber = surveyRepository
                .findBySessionIdOrderByPlayerNumberAsc(session.getId())
                .stream()
                .collect(Collectors.toMap(PlayerSurvey::getPlayerNumber, s -> s, (a, b) -> a));

        List<RoundInvestment> investments = investmentRepository
                .findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        Map<Integer, Integer> totalInvestedByPlayer = investments.stream()
                .collect(Collectors.groupingBy(
                        RoundInvestment::getPlayerNumber,
                        Collectors.summingInt(RoundInvestment::getAmountRappen)
                ));

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM für korrekte Umlaute in Microsoft Excel
        sb.append('\uFEFF');
        sb.append("session_code,game_mode,target_sum_rappen,total_invested_rappen,target_reached,risk_occurred,round,player_number,invested_rappen,invested_chf,submitted_at,currently_in_contract,player_total_invested_rappen,balance_before_risk_rappen,final_payout_rappen,final_payout_chf,ever_joined_contract,left_contract,in_contract_at_end,left_contract_at_round,contract_abolished,contract_abolished_at_round,survey_expected_probability_pct,survey_reaction_unfairness,survey_motivation_ranking,survey_contract_joined_reason,survey_contract_not_joined_reason,survey_contract_left_reason,survey_contract_stayed_reason,survey_age,survey_gender\n");

        for (RoundInvestment inv : investments) {
            Player p = playerByNumber.get(inv.getPlayerNumber());
            PlayerSurvey survey = surveyByNumber.get(inv.getPlayerNumber());
            int playerTotalInvested = totalInvestedByPlayer.getOrDefault(inv.getPlayerNumber(), 0);
            int balanceBeforeRisk = p != null ? p.getCurrentBalanceRappen() : 0;
            int finalPayoutRappen = (p != null && p.getFinalPayoutRappen() != null)
                    ? p.getFinalPayoutRappen()
                    : balanceBeforeRisk;

            String investedChf = String.format(Locale.US, "%.2f", inv.getAmountRappen() / 100.0);
            String finalPayoutChf = String.format(Locale.US, "%.2f", finalPayoutRappen / 100.0);

            sb.append(csvEscape(session.getCode())).append(',')
                    .append(csvEscape(session.getGameMode().name())).append(',')
                    .append(session.getTargetSumRappen()).append(',')
                    .append(session.getTotalPotRappen()).append(',')
                    .append(session.getIsGoalReached() != null ? session.getIsGoalReached() : false).append(',')
                    .append(session.getRiskOccurred() != null ? session.getRiskOccurred() : false).append(',')
                    .append(inv.getRoundNumber()).append(',')
                    .append(inv.getPlayerNumber()).append(',')
                    .append(inv.getAmountRappen()).append(',')
                    .append(investedChf).append(',')
                    .append(csvEscape(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "")).append(',')
                    .append(inv.isCurrentlyInContract()).append(',')
                    .append(playerTotalInvested).append(',')
                    .append(balanceBeforeRisk).append(',')
                    .append(finalPayoutRappen).append(',')
                    .append(finalPayoutChf).append(',')
                    .append(p != null && p.isEverJoinedContract()).append(',')
                    .append(p != null && p.isLeftContract()).append(',')
                    .append(p != null && p.isInContract()).append(',')
                    .append(p != null && p.getLeftContractAtRound() != null ? p.getLeftContractAtRound() : "").append(',')
                    .append(session.isContractAbolished()).append(',')
                    .append(session.getContractAbolishedAtRound() != null ? session.getContractAbolishedAtRound() : "").append(',')
                    .append(survey != null ? survey.getExpectedProbabilityPct() : "").append(',')
                    .append(csvEscape(survey != null ? survey.getReactionToUnfairness() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getMotivationRanking() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractJoinedReason() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractNotJoinedReason() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractLeftReason() : "")).append(',')
                    .append(csvEscape(survey != null ? survey.getContractStayedReason() : "")).append(',')
                    .append(survey != null ? survey.getAge() : "").append(',')
                    .append(csvEscape(survey != null ? survey.getGender() : ""))
                    .append('\n');
        }

        return sb.toString();
    }

    private GameSession findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private AdminSessionResponse buildAdminSessionResponse(GameSession session) {
        List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        int currentRound = session.getCurrentRound();
        RoundPhase roundPhase = session.getRoundPhase() != null ? session.getRoundPhase() : RoundPhase.INVESTMENT;

        List<RoundInvestment> currentRoundInvestmentsList = currentRound > 0
                ? investmentRepository.findBySessionIdAndRoundNumberOrderByPlayerNumberAsc(session.getId(), currentRound)
                : List.of();

        Set<Integer> surveyedPlayerNumbers = surveyRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId())
                .stream()
                .map(PlayerSurvey::getPlayerNumber)
                .collect(Collectors.toSet());

        int groupSize = gameEngineService.getGroupSize();
        int totalRounds = gameEngineService.getTotalRounds();

        Map<Integer, Player> playerByNumber = players.stream()
                .collect(Collectors.toMap(Player::getPlayerNumber, p -> p));

        Map<Integer, RoundInvestment> currentRoundInvestments = currentRoundInvestmentsList.stream()
                .collect(Collectors.toMap(RoundInvestment::getPlayerNumber, inv -> inv, (a, b) -> a));

        List<AdminSessionResponse.AdminPlayerDto> adminPlayers = new ArrayList<>();
        int readyPlayersCount = 0;

        for (int pNum = 1; pNum <= groupSize; pNum++) {
            Player p = playerByNumber.get(pNum);
            RoundInvestment currentInv = currentRoundInvestments.get(pNum);
            if (p != null) {
                boolean isReady;
                if (session.getStatus() == SessionStatus.LOBBY) {
                    isReady = true;
                } else if (session.getStatus() == SessionStatus.FINISHED) {
                    isReady = surveyedPlayerNumbers.contains(pNum);
                } else if (roundPhase == RoundPhase.CONTRACT_JOIN) {
                    isReady = p.isContractJoinDecided();
                } else if (roundPhase == RoundPhase.CONTRACT_REVIEW) {
                    isReady = !p.isInContract() || Integer.valueOf(currentRound).equals(p.getContractReviewDecidedRound());
                } else if (roundPhase == RoundPhase.CONTRACT_VOTE) {
                    isReady = !p.isInContract() || Integer.valueOf(currentRound).equals(p.getContractVoteRound());
                } else {
                    isReady = currentInv != null;
                }
                if (isReady) {
                    readyPlayersCount++;
                }
                adminPlayers.add(new AdminSessionResponse.AdminPlayerDto(
                        pNum,
                        true,
                        isReady,
                        p.isInContract(),
                        p.isLeftContract(),
                        p.getFinalPayoutRappen(),
                        p.getLastSeenAt()
                ));
            } else {
                adminPlayers.add(new AdminSessionResponse.AdminPlayerDto(
                        pNum,
                        false,
                        false,
                        false,
                        false,
                        null,
                        null
                ));
            }
        }

        int contractMemberCount = (int) players.stream().filter(Player::isInContract).count();
        boolean canAdvanceRound = session.getStatus() == SessionStatus.RUNNING
                && roundPhase == RoundPhase.INVESTMENT
                && readyPlayersCount >= groupSize;
        int riskPercent = (int) Math.round(session.getRiskLevel().doubleValue() * 100.0);

        return new AdminSessionResponse(
                session.getId(),
                session.getCode(),
                session.getStatus(),
                session.getGameMode(),
                roundPhase,
                session.isContractActive(),
                contractMemberCount,
                session.getRiskLevel(),
                riskPercent,
                session.getCurrentRound(),
                totalRounds,
                session.getTargetSumRappen(),
                session.getTotalPotRappen(),
                players.size(),
                readyPlayersCount,
                groupSize,
                canAdvanceRound,
                session.getIsGoalReached(),
                session.getRiskOccurred(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                adminPlayers
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
