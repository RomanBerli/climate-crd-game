package ch.bbw.climategame.service;

import ch.bbw.climategame.dto.request.SubmitSurveyRequest;
import ch.bbw.climategame.dto.response.ContractStateDto;
import ch.bbw.climategame.dto.response.EndgameResultDto;
import ch.bbw.climategame.dto.response.GameStateResponse;
import ch.bbw.climategame.dto.response.JoinLobbyResponse;
import ch.bbw.climategame.dto.response.PlayerSlotStatusDto;
import ch.bbw.climategame.dto.response.PlayerStateDto;
import ch.bbw.climategame.dto.response.RoundHistoryDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GameEngineService {

    public static final long INACTIVE_TIMEOUT_SECONDS = 30L;

    private final GameSessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final RoundInvestmentRepository investmentRepository;
    private final PlayerSurveyRepository surveyRepository;
    private final PayoutService payoutService;

    private final double startAmountChf;
    private final int groupSize;
    private final int totalRounds;
    private final BigDecimal riskLevel;

    public GameEngineService(
            GameSessionRepository sessionRepository,
            PlayerRepository playerRepository,
            RoundInvestmentRepository investmentRepository,
            PlayerSurveyRepository surveyRepository,
            PayoutService payoutService,
            @Value("${app.game.start-amount-chf}") double startAmountChf,
            @Value("${app.game.group-size}") int groupSize,
            @Value("${app.game.total-rounds}") int totalRounds,
            @Value("${app.game.risk-level}") BigDecimal riskLevel
    ) {
        this.sessionRepository = sessionRepository;
        this.playerRepository = playerRepository;
        this.investmentRepository = investmentRepository;
        this.surveyRepository = surveyRepository;
        this.payoutService = payoutService;
        this.startAmountChf = startAmountChf;
        this.groupSize = groupSize;
        this.totalRounds = totalRounds;
        this.riskLevel = riskLevel;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public BigDecimal getRiskLevel() {
        return riskLevel;
    }

    public int getStartAmountRappen() {
        return (int) Math.round(startAmountChf * 100.0);
    }

    public int getTargetSumRappen() {
        return (getStartAmountRappen() / 2) * groupSize;
    }

    public List<Integer> getAllowedInvestmentOptions() {
        int startRappen = getStartAmountRappen();
        return List.of(
                0,
                (int) Math.round(startRappen * 0.05),
                (int) Math.round(startRappen * 0.10)
        );
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void kickInactivePlayers() {
        Instant cutoff = Instant.now().minusSeconds(INACTIVE_TIMEOUT_SECONDS);
        playerRepository.deleteInactiveLobbyPlayers(cutoff);
    }

    @Transactional
    public void leaveLobby(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        Optional<Player> existingPlayer = playerRepository.findBySessionToken(sessionToken.trim());
        if (existingPlayer.isEmpty()) {
            return;
        }
        Player player = existingPlayer.get();
        GameSession session = player.getSession();
        if (session.getStatus() == SessionStatus.LOBBY) {
            playerRepository.deletePlayerById(player.getId());
        } else {
            // Nach Spielstart/Spielende bleiben die Spielerdaten für den CSV-Export erhalten,
            // aber das Token wird invalidiert, damit der Browser ausgeloggt ist.
            player.setSessionToken("left-" + UUID.randomUUID());
            playerRepository.save(player);
        }
    }

    @Transactional
    public JoinLobbyResponse joinLobby(String rawCode, String existingToken) {
        kickInactivePlayers();

        String cleanCode = rawCode == null ? "" : rawCode.trim().toUpperCase();
        GameSession session = sessionRepository.findByCodeForUpdate(cleanCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Raum mit Code '" + cleanCode + "' wurde nicht gefunden."
                ));

        // Falls der Browser bereits ein gültiges Token für genau diese Session besitzt -> Reconnect erlauben
        if (existingToken != null && !existingToken.isBlank()) {
            Optional<Player> existingPlayer = playerRepository.findBySessionToken(existingToken);
            if (existingPlayer.isPresent() && existingPlayer.get().getSession().getId().equals(session.getId())) {
                Player p = existingPlayer.get();
                p.setLastSeenAt(Instant.now());
                playerRepository.save(p);
                return new JoinLobbyResponse(session.getId(), session.getCode(), p.getPlayerNumber(), p.getSessionToken());
            }
        }

        if (session.getStatus() != SessionStatus.LOBBY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diesem Raum kann nicht mehr beigetreten werden (Status: " + session.getStatus() + ")."
            );
        }

        List<Player> currentPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        if (currentPlayers.size() >= groupSize) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Die Lobby ist bereits voll (" + groupSize + "/" + groupSize + " Spieler)."
            );
        }

        Set<Integer> takenNumbers = currentPlayers.stream()
                .map(Player::getPlayerNumber)
                .collect(Collectors.toSet());

        int nextPlayerNumber = 1;
        while (takenNumbers.contains(nextPlayerNumber) && nextPlayerNumber <= groupSize) {
            nextPlayerNumber++;
        }

        int initialBalanceRappen = getStartAmountRappen();
        Player newPlayer = new Player();
        newPlayer.setSession(session);
        newPlayer.setPlayerNumber(nextPlayerNumber);
        newPlayer.setSessionToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString().substring(0, 8));
        newPlayer.setInitialBalanceRappen(initialBalanceRappen);
        newPlayer.setCurrentBalanceRappen(initialBalanceRappen);
        newPlayer.setLastSeenAt(Instant.now());

        playerRepository.save(newPlayer);

        return new JoinLobbyResponse(
                session.getId(),
                session.getCode(),
                newPlayer.getPlayerNumber(),
                newPlayer.getSessionToken()
        );
    }

    @Transactional
    public GameStateResponse getPlayerGameState(String sessionToken) {
        kickInactivePlayers();

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player player = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Spieler-Sitzung nicht gefunden oder abgelaufen."
                ));

        player.setLastSeenAt(Instant.now());
        playerRepository.save(player);

        GameSession session = player.getSession();
        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        return buildGameStateResponse(session, player, allPlayers, allInvestments);
    }

    @Transactional
    public GameStateResponse submitContractJoin(String sessionToken, boolean join) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player tokenPlayer = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spieler-Sitzung ungültig."));

        GameSession session = sessionRepository.findByIdForUpdate(tokenPlayer.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.RUNNING || session.getRoundPhase() != RoundPhase.CONTRACT_JOIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Die Beitrittsphase zum Abkommen ist nicht aktiv.");
        }

        if (tokenPlayer.isContractJoinDecided()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sie haben bereits über den Beitritt zum Abkommen entschieden.");
        }

        tokenPlayer.setContractJoinDecided(true);
        tokenPlayer.setInContract(join);
        tokenPlayer.setEverJoinedContract(join);
        tokenPlayer.setLastSeenAt(Instant.now());
        playerRepository.save(tokenPlayer);

        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        long decidedCount = allPlayers.stream().filter(Player::isContractJoinDecided).count();

        if (decidedCount >= groupSize) {
            long memberCount = allPlayers.stream().filter(Player::isInContract).count();
            if (memberCount > 0) {
                session.setContractActive(true);
                session.setLastContractEvent(memberCount + " von " + groupSize + " Spielern sind dem Abkommen beigetreten.");
            } else {
                session.setContractActive(false);
                session.setLastContractEvent("Kein Spieler ist dem Abkommen beigetreten. Es gilt kein Mindestbeitrag.");
            }
            session.setRoundPhase(RoundPhase.INVESTMENT);
            session.setRoundStartTime(Instant.now());
            sessionRepository.save(session);
        }

        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
        return buildGameStateResponse(session, tokenPlayer, allPlayers, allInvestments);
    }

    @Transactional
    public GameStateResponse submitContractReviewAction(String sessionToken, String rawAction) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player tokenPlayer = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spieler-Sitzung ungültig."));

        GameSession session = sessionRepository.findByIdForUpdate(tokenPlayer.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.RUNNING || session.getRoundPhase() != RoundPhase.CONTRACT_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aktuell befindet sich das Spiel nicht in der Zwischenphase des Abkommens.");
        }

        if (!tokenPlayer.isInContract()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nur aktive Mitglieder des Abkommens können diese Aktion ausführen.");
        }

        int currentRound = session.getCurrentRound();
        String action = rawAction == null ? "" : rawAction.trim().toUpperCase();
        tokenPlayer.setLastSeenAt(Instant.now());

        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());

        switch (action) {
            case "STAY" -> {
                tokenPlayer.setContractReviewDecidedRound(currentRound);
                playerRepository.save(tokenPlayer);

                boolean allMembersDecided = allPlayers.stream()
                        .filter(Player::isInContract)
                        .allMatch(p -> Integer.valueOf(currentRound).equals(p.getContractReviewDecidedRound()));
                if (allMembersDecided) {
                    session.setRoundPhase(RoundPhase.INVESTMENT);
                    session.setRoundStartTime(Instant.now());
                    sessionRepository.save(session);
                }
            }
            case "LEAVE" -> {
                List<Player> remainingMembers = allPlayers.stream()
                        .filter(p -> p.isInContract() && p.getPlayerNumber() != tokenPlayer.getPlayerNumber())
                        .toList();
                int feePerMember = getFivePercentRappen(tokenPlayer.getInitialBalanceRappen());
                int totalFeeRappen = feePerMember * remainingMembers.size();

                if (tokenPlayer.getCurrentBalanceRappen() < totalFeeRappen) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Nicht genügend Guthaben für die Austrittsgebühr."
                    );
                }

                tokenPlayer.setCurrentBalanceRappen(tokenPlayer.getCurrentBalanceRappen() - totalFeeRappen);
                tokenPlayer.setInContract(false);
                tokenPlayer.setLeftContract(true);
                tokenPlayer.setContractReviewDecidedRound(currentRound);

                for (Player member : remainingMembers) {
                    member.setCurrentBalanceRappen(member.getCurrentBalanceRappen() + feePerMember);
                }
                playerRepository.saveAll(allPlayers);

                session.setLastContractEvent("Spieler " + tokenPlayer.getPlayerNumber() + " ist aus dem Abkommen ausgetreten.");

                if (remainingMembers.isEmpty()) {
                    session.setContractActive(false);
                    session.setRoundPhase(RoundPhase.INVESTMENT);
                    session.setRoundStartTime(Instant.now());
                } else {
                    boolean allRemainingDecided = remainingMembers.stream()
                            .allMatch(p -> Integer.valueOf(currentRound).equals(p.getContractReviewDecidedRound()));
                    if (allRemainingDecided) {
                        session.setRoundPhase(RoundPhase.INVESTMENT);
                        session.setRoundStartTime(Instant.now());
                    }
                }
                sessionRepository.save(session);
            }
            case "START_VOTE" -> {
                if (Integer.valueOf(currentRound).equals(session.getVoteInitiatedRound())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "In dieser Zwischenrunde fand bereits eine Abstimmung über die Abschaffung statt."
                    );
                }
                session.setVoteInitiatedRound(currentRound);
                session.setVoteInitiatorPlayerNumber(tokenPlayer.getPlayerNumber());
                session.setRoundPhase(RoundPhase.CONTRACT_VOTE);
                session.setLastContractEvent("Spieler " + tokenPlayer.getPlayerNumber() + " hat eine Abstimmung zur Abschaffung des Abkommens gestartet.");

                tokenPlayer.setContractVoteRound(currentRound);
                tokenPlayer.setContractVoteAbolish(true);
                playerRepository.save(tokenPlayer);

                resolveContractVoteIfComplete(session, allPlayers, currentRound);
                sessionRepository.save(session);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Abkommens-Aktion: " + action);
        }

        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
        Player updatedPlayer = allPlayers.stream()
                .filter(p -> p.getPlayerNumber() == tokenPlayer.getPlayerNumber())
                .findFirst()
                .orElse(tokenPlayer);

        return buildGameStateResponse(session, updatedPlayer, allPlayers, allInvestments);
    }

    @Transactional
    public GameStateResponse submitContractVote(String sessionToken, boolean abolish) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player tokenPlayer = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spieler-Sitzung ungültig."));

        GameSession session = sessionRepository.findByIdForUpdate(tokenPlayer.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.RUNNING || session.getRoundPhase() != RoundPhase.CONTRACT_VOTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aktuell läuft keine Abstimmung über das Abkommen.");
        }

        if (!tokenPlayer.isInContract()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nur Mitglieder des Abkommens sind stimmberechtigt.");
        }

        int currentRound = session.getCurrentRound();
        if (Integer.valueOf(currentRound).equals(tokenPlayer.getContractVoteRound())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sie haben in dieser Abstimmung bereits abgestimmt.");
        }

        tokenPlayer.setContractVoteRound(currentRound);
        tokenPlayer.setContractVoteAbolish(abolish);
        tokenPlayer.setLastSeenAt(Instant.now());
        playerRepository.save(tokenPlayer);

        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        resolveContractVoteIfComplete(session, allPlayers, currentRound);
        sessionRepository.save(session);

        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
        Player updatedPlayer = allPlayers.stream()
                .filter(p -> p.getPlayerNumber() == tokenPlayer.getPlayerNumber())
                .findFirst()
                .orElse(tokenPlayer);

        return buildGameStateResponse(session, updatedPlayer, allPlayers, allInvestments);
    }

    private void resolveContractVoteIfComplete(GameSession session, List<Player> allPlayers, int currentRound) {
        List<Player> members = allPlayers.stream()
                .filter(Player::isInContract)
                .toList();

        boolean allVoted = members.stream()
                .allMatch(p -> Integer.valueOf(currentRound).equals(p.getContractVoteRound()));

        if (!allVoted) {
            return;
        }

        long yesVotes = members.stream()
                .filter(p -> Boolean.TRUE.equals(p.getContractVoteAbolish()))
                .count();
        int totalMembers = members.size();

        // Einfache Mehrheit (> 50% der Mitglieder)
        if (yesVotes * 2 > totalMembers) {
            session.setContractActive(false);
            session.setContractAbolished(true);
            for (Player member : members) {
                member.setInContract(false);
            }
            playerRepository.saveAll(members);
            session.setLastContractEvent(
                    "Abstimmung (" + yesVotes + "/" + totalMembers + " für Abschaffung): Das Abkommen wurde per Mehrheit kostenlos aufgelöst."
            );
            session.setRoundPhase(RoundPhase.INVESTMENT);
            session.setRoundStartTime(Instant.now());
        } else {
            // Keine Mehrheit -> Abkommen bleibt aktiv, Rückkehr zu CONTRACT_REVIEW
            for (Player member : members) {
                if (Boolean.TRUE.equals(member.getContractVoteAbolish())) {
                    member.setContractReviewDecidedRound(null);
                }
            }
            playerRepository.saveAll(members);
            session.setLastContractEvent(
                    "Abstimmung (" + yesVotes + "/" + totalMembers + " für Abschaffung): Keine Mehrheit – das Abkommen bleibt bestehen."
            );
            boolean allReviewed = members.stream()
                    .allMatch(p -> Integer.valueOf(currentRound).equals(p.getContractReviewDecidedRound()));
            session.setRoundPhase(allReviewed ? RoundPhase.INVESTMENT : RoundPhase.CONTRACT_REVIEW);
        }
    }

    @Transactional
    public GameStateResponse submitInvestment(String sessionToken, int amountRappen) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player tokenPlayer = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spieler-Sitzung ungültig."));

        GameSession session = sessionRepository.findByIdForUpdate(tokenPlayer.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.RUNNING || session.getRoundPhase() != RoundPhase.INVESTMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Investitionen sind erst möglich, sobald die Einzahlungsphase der Runde aktiv ist.");
        }

        int currentRound = session.getCurrentRound();
        if (investmentRepository.existsBySessionIdAndRoundNumberAndPlayerNumber(session.getId(), currentRound, tokenPlayer.getPlayerNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sie haben in Runde " + currentRound + " bereits investiert.");
        }

        List<Integer> allowedOptions = getAllowedInvestmentOptions();
        if (!allowedOptions.contains(amountRappen)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ungültiger Investitionsbetrag."
            );
        }

        int minRequiredRappen = getMinimumRequiredInvestmentRappen(session, tokenPlayer);
        if (amountRappen < minRequiredRappen) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Als Mitglied des Abkommens müssen Sie mindestens den vorgeschriebenen Mindestbeitrag pro Runde investieren."
            );
        }

        if (tokenPlayer.getCurrentBalanceRappen() < amountRappen) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nicht genügend Guthaben verfügbar."
            );
        }

        recordInvestmentInternal(session, tokenPlayer, currentRound, amountRappen);

        // Kein automatischer Rundenwechsel: Der Admin klickt die Runde im Dashboard weiter, sobald alle 6 Spieler gewählt haben.
        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
        Player updatedPlayer = allPlayers.stream()
                .filter(p -> p.getPlayerNumber() == tokenPlayer.getPlayerNumber())
                .findFirst()
                .orElse(tokenPlayer);

        return buildGameStateResponse(session, updatedPlayer, allPlayers, allInvestments);
    }

    @Transactional
    public GameStateResponse submitSurvey(String sessionToken, SubmitSurveyRequest request) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player tokenPlayer = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spieler-Sitzung ungültig."));

        GameSession session = sessionRepository.findByIdForUpdate(tokenPlayer.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() != SessionStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Der Fragebogen kann erst nach Abschluss der 10 Runden ausgefüllt werden.");
        }

        if (surveyRepository.existsBySessionIdAndPlayerNumber(session.getId(), tokenPlayer.getPlayerNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sie haben den Fragebogen bereits abgeschickt.");
        }

        String formattedRanking = formatRankingList(request.motivationRanking());

        PlayerSurvey survey = new PlayerSurvey();
        survey.setSession(session);
        survey.setPlayer(tokenPlayer);
        survey.setPlayerNumber(tokenPlayer.getPlayerNumber());
        survey.setExpectedProbabilityPct(request.expectedProbabilityPct());
        survey.setReactionToUnfairness(request.reactionToUnfairness().trim());
        survey.setMotivationRanking(formattedRanking);
        survey.setContractJoinedReason(cleanNullable(request.contractJoinedReason()));
        survey.setContractNotJoinedReason(cleanNullable(request.contractNotJoinedReason()));
        survey.setContractLeftReason(cleanNullable(request.contractLeftReason()));
        survey.setContractStayedReason(cleanNullable(request.contractStayedReason()));
        survey.setAge(request.age());
        survey.setGender(request.gender().trim());
        survey.setTwintPhone(request.twintPhone().trim());

        surveyRepository.save(survey);

        tokenPlayer.setLastSeenAt(Instant.now());
        playerRepository.save(tokenPlayer);

        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());

        return buildGameStateResponse(session, tokenPlayer, allPlayers, allInvestments);
    }

    @Transactional
    public void advanceRoundOrFinish(GameSession session) {
        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());

        if (session.getCurrentRound() < totalRounds) {
            int nextRound = session.getCurrentRound() + 1;
            session.setCurrentRound(nextRound);
            session.setRoundStartTime(Instant.now());

            long activeContractMembers = allPlayers.stream().filter(Player::isInContract).count();
            if (session.getGameMode() == GameMode.WITH_CONTRACTS && session.isContractActive() && activeContractMembers > 0) {
                session.setRoundPhase(RoundPhase.CONTRACT_REVIEW);
            } else {
                session.setRoundPhase(RoundPhase.INVESTMENT);
            }
            sessionRepository.save(session);
        } else {
            // Nach Runde 10 -> Endabrechnung berechnen & Status FINISHED setzen.
            // Spieler sehen zuerst den Fragebogen und sofort nach dem Absenden automatisch ihr Resultat!
            List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
            payoutService.resolveEndgame(session, allPlayers, allInvestments);
            playerRepository.saveAll(allPlayers);
            sessionRepository.save(session);
        }
    }

    public int getFivePercentRappen(int initialBalanceRappen) {
        return (int) Math.round(initialBalanceRappen * 0.05);
    }

    private int getMinimumRequiredInvestmentRappen(GameSession session, Player player) {
        if (session.getGameMode() == GameMode.WITH_CONTRACTS && session.isContractActive() && player.isInContract()) {
            int fivePercent = getFivePercentRappen(player.getInitialBalanceRappen());
            if (player.getCurrentBalanceRappen() >= fivePercent) {
                return fivePercent;
            }
        }
        return 0;
    }

    private String formatRankingList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(i + 1).append(". ").append(items.get(i).trim());
        }
        return sb.toString();
    }

    private String cleanNullable(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        return val.trim();
    }

    private void recordInvestmentInternal(
            GameSession session,
            Player player,
            int roundNumber,
            int amountRappen
    ) {
        RoundInvestment investment = new RoundInvestment();
        investment.setSession(session);
        investment.setPlayer(player);
        investment.setRoundNumber(roundNumber);
        investment.setPlayerNumber(player.getPlayerNumber());
        investment.setAmountRappen(amountRappen);
        investment.setAutoBet(false);
        investmentRepository.save(investment);

        player.setCurrentBalanceRappen(player.getCurrentBalanceRappen() - amountRappen);
        player.setLastSeenAt(Instant.now());
        playerRepository.save(player);

        session.setTotalPotRappen(session.getTotalPotRappen() + amountRappen);
        sessionRepository.save(session);
    }

    public List<RoundHistoryDto> buildCompletedRoundHistory(
            GameSession session,
            List<RoundInvestment> allInvestments,
            int groupSize
    ) {
        if (session.getStatus() != SessionStatus.RUNNING || session.getCurrentRound() <= 1) {
            return List.of();
        }

        int lastCompletedRound = session.getCurrentRound() - 1;
        Map<Integer, RoundInvestment> roundMap = allInvestments.stream()
                .filter(inv -> inv.getRoundNumber() == lastCompletedRound)
                .collect(Collectors.toMap(
                        RoundInvestment::getPlayerNumber,
                        inv -> inv,
                        (a, b) -> a
                ));

        List<RoundHistoryDto.PlayerInvestmentDto> row = new ArrayList<>();
        int roundTotalRappen = 0;
        for (int pNum = 1; pNum <= groupSize; pNum++) {
            RoundInvestment inv = roundMap.get(pNum);
            int amount = inv != null ? inv.getAmountRappen() : 0;
            boolean autoBet = inv != null && inv.isAutoBet();
            roundTotalRappen += amount;
            row.add(new RoundHistoryDto.PlayerInvestmentDto(pNum, amount, autoBet));
        }

        return List.of(new RoundHistoryDto(lastCompletedRound, roundTotalRappen, row));
    }

    private GameStateResponse buildGameStateResponse(
            GameSession session,
            Player player,
            List<Player> allPlayers,
            List<RoundInvestment> allInvestments
    ) {
        int currentRound = session.getCurrentRound();
        RoundPhase roundPhase = session.getRoundPhase() != null ? session.getRoundPhase() : RoundPhase.INVESTMENT;

        Map<Integer, Player> playerByNumber = allPlayers.stream()
                .collect(Collectors.toMap(Player::getPlayerNumber, p -> p));

        Map<Integer, RoundInvestment> currentRoundInvestments = allInvestments.stream()
                .filter(inv -> inv.getRoundNumber() == currentRound)
                .collect(Collectors.toMap(RoundInvestment::getPlayerNumber, inv -> inv, (a, b) -> a));

        Set<Integer> surveyedPlayerNumbers = surveyRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId())
                .stream()
                .map(PlayerSurvey::getPlayerNumber)
                .collect(Collectors.toSet());

        RoundInvestment myCurrentRoundInvestment = currentRoundInvestments.get(player.getPlayerNumber());
        boolean hasSubmittedRound = myCurrentRoundInvestment != null;
        boolean hasSubmittedSurvey = surveyedPlayerNumbers.contains(player.getPlayerNumber());
        Integer myCurrentAmount = myCurrentRoundInvestment != null ? myCurrentRoundInvestment.getAmountRappen() : null;

        int minRequiredRappen = getMinimumRequiredInvestmentRappen(session, player);
        List<Integer> allowedOptions = getAllowedInvestmentOptions().stream()
                .filter(opt -> opt >= minRequiredRappen && opt <= player.getCurrentBalanceRappen())
                .toList();

        int ownTotalInvested = allInvestments.stream()
                .filter(inv -> inv.getPlayerNumber() == player.getPlayerNumber())
                .mapToInt(RoundInvestment::getAmountRappen)
                .sum();

        PlayerStateDto playerDto = new PlayerStateDto(
                player.getPlayerNumber(),
                player.getInitialBalanceRappen(),
                player.getCurrentBalanceRappen(),
                ownTotalInvested,
                hasSubmittedRound,
                hasSubmittedSurvey,
                player.isInContract(),
                player.isEverJoinedContract(),
                player.isLeftContract(),
                myCurrentAmount,
                allowedOptions,
                hasSubmittedSurvey ? player.getFinalPayoutRappen() : null
        );

        List<PlayerSlotStatusDto> slots = new ArrayList<>();
        for (int pNum = 1; pNum <= groupSize; pNum++) {
            Player slotPlayer = playerByNumber.get(pNum);
            boolean isConnected = slotPlayer != null;
            boolean inContract = slotPlayer != null && slotPlayer.isInContract();
            boolean leftContract = slotPlayer != null && slotPlayer.isLeftContract();
            boolean isReady;
            if (session.getStatus() == SessionStatus.LOBBY) {
                isReady = isConnected;
            } else if (session.getStatus() == SessionStatus.FINISHED) {
                isReady = surveyedPlayerNumbers.contains(pNum);
            } else if (roundPhase == RoundPhase.CONTRACT_JOIN) {
                isReady = slotPlayer != null && slotPlayer.isContractJoinDecided();
            } else if (roundPhase == RoundPhase.CONTRACT_REVIEW) {
                isReady = slotPlayer != null && (!slotPlayer.isInContract() || Integer.valueOf(currentRound).equals(slotPlayer.getContractReviewDecidedRound()));
            } else if (roundPhase == RoundPhase.CONTRACT_VOTE) {
                isReady = slotPlayer != null && (!slotPlayer.isInContract() || Integer.valueOf(currentRound).equals(slotPlayer.getContractVoteRound()));
            } else {
                isReady = currentRoundInvestments.containsKey(pNum);
            }
            slots.add(new PlayerSlotStatusDto(pNum, isConnected, isReady, inContract, leftContract));
        }

        int memberCount = (int) allPlayers.stream().filter(Player::isInContract).count();
        int fivePercentRappen = getFivePercentRappen(player.getInitialBalanceRappen());
        int otherMembersCount = player.isInContract() ? Math.max(0, memberCount - 1) : memberCount;
        int totalExitFeeForPlayerRappen = player.isInContract() ? fivePercentRappen * otherMembersCount : 0;
        boolean canAffordExit = player.isInContract() && player.getCurrentBalanceRappen() >= totalExitFeeForPlayerRappen;
        boolean voteAlreadyHeldThisRound = Integer.valueOf(currentRound).equals(session.getVoteInitiatedRound());

        boolean hasDecidedCurrentContractPhase = switch (roundPhase) {
            case CONTRACT_JOIN -> player.isContractJoinDecided();
            case CONTRACT_REVIEW -> !player.isInContract() || Integer.valueOf(currentRound).equals(player.getContractReviewDecidedRound());
            case CONTRACT_VOTE -> !player.isInContract() || Integer.valueOf(currentRound).equals(player.getContractVoteRound());
            case INVESTMENT -> hasSubmittedRound;
        };

        ContractStateDto contractStateDto = new ContractStateDto(
                roundPhase,
                session.isContractActive(),
                session.isContractAbolished(),
                memberCount,
                fivePercentRappen,
                fivePercentRappen,
                totalExitFeeForPlayerRappen,
                canAffordExit,
                voteAlreadyHeldThisRound,
                session.getVoteInitiatorPlayerNumber(),
                hasDecidedCurrentContractPhase,
                session.getLastContractEvent()
        );

        List<RoundHistoryDto> history = buildCompletedRoundHistory(session, allInvestments, groupSize);
        // Resultat erst mitliefern, sobald der Spieler seinen Fragebogen abgeschickt hat!
        EndgameResultDto endgameDto = (session.getStatus() == SessionStatus.FINISHED && hasSubmittedSurvey)
                ? payoutService.buildEndgameResultDto(session, player)
                : null;
        int riskPercent = (int) Math.round(session.getRiskLevel().doubleValue() * 100.0);

        int visibleTotalPotRappen = (session.getStatus() == SessionStatus.FINISHED && hasSubmittedSurvey)
                ? session.getTotalPotRappen()
                : 0;

        return new GameStateResponse(
                session.getId(),
                session.getCode(),
                session.getStatus(),
                session.getGameMode(),
                roundPhase,
                contractStateDto,
                session.getRiskLevel(),
                riskPercent,
                session.getTargetSumRappen(),
                visibleTotalPotRappen,
                session.getCurrentRound(),
                totalRounds,
                allPlayers.size(),
                groupSize,
                playerDto,
                slots,
                history,
                endgameDto
        );
    }
}
