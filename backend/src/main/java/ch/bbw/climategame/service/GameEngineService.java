package ch.bbw.climategame.service;

import ch.bbw.climategame.config.AppProperties;
import ch.bbw.climategame.dto.response.EndgameResultDto;
import ch.bbw.climategame.dto.response.GameStateResponse;
import ch.bbw.climategame.dto.response.JoinLobbyResponse;
import ch.bbw.climategame.dto.response.PlayerSlotStatusDto;
import ch.bbw.climategame.dto.response.PlayerStateDto;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GameEngineService {

    private final GameSessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final RoundInvestmentRepository investmentRepository;
    private final PayoutService payoutService;
    private final AppProperties appProperties;

    public GameEngineService(
            GameSessionRepository sessionRepository,
            PlayerRepository playerRepository,
            RoundInvestmentRepository investmentRepository,
            PayoutService payoutService,
            AppProperties appProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.playerRepository = playerRepository;
        this.investmentRepository = investmentRepository;
        this.payoutService = payoutService;
        this.appProperties = appProperties;
    }

    @Transactional
    public JoinLobbyResponse joinLobby(String rawCode, String existingToken) {
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
        int groupSize = appProperties.getGame().getGroupSize();
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

        int initialBalanceRappen = appProperties.getGame().getStartAmountRappen();
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
    public GameStateResponse submitInvestment(String sessionToken, int amountRappen) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein gültiges Spieler-Token vorhanden.");
        }

        Player tokenPlayer = playerRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spieler-Sitzung ungültig."));

        GameSession session = sessionRepository.findByIdForUpdate(tokenPlayer.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session nicht gefunden."));

        if (session.getStatus() == SessionStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Das Spiel ist derzeit durch den Versuchsleiter pausiert.");
        }
        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Investitionen sind nur während einer aktiven Runde möglich.");
        }

        int currentRound = session.getCurrentRound();
        if (investmentRepository.existsBySessionIdAndRoundNumberAndPlayerNumber(session.getId(), currentRound, tokenPlayer.getPlayerNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sie haben in Runde " + currentRound + " bereits investiert.");
        }

        List<Integer> allowedOptions = appProperties.getGame().getAllowedInvestmentOptions();
        if (!allowedOptions.contains(amountRappen)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ungültiger Investitionsbetrag: " + amountRappen + " Rappen. Erlaubt sind: " + allowedOptions
            );
        }

        if (tokenPlayer.getCurrentBalanceRappen() < amountRappen) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nicht genügend Guthaben (" + tokenPlayer.getCurrentBalanceRappen() + " Rappen verfügbar)."
            );
        }

        recordInvestmentInternal(session, tokenPlayer, currentRound, amountRappen, false);

        int groupSize = appProperties.getGame().getGroupSize();
        int submittedCount = investmentRepository.countBySessionIdAndRoundNumber(session.getId(), currentRound);
        if (submittedCount >= groupSize) {
            advanceRoundOrFinish(session);
        }

        List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
        List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
        Player updatedPlayer = allPlayers.stream()
                .filter(p -> p.getPlayerNumber() == tokenPlayer.getPlayerNumber())
                .findFirst()
                .orElse(tokenPlayer);

        return buildGameStateResponse(session, updatedPlayer, allPlayers, allInvestments);
    }

    @Transactional
    public void processSessionTimerTick(UUID sessionId) {
        Optional<GameSession> sessionOpt = sessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return;
        }
        GameSession session = sessionOpt.get();
        if (session.getStatus() != SessionStatus.RUNNING || session.getRoundDeadline() == null) {
            return;
        }

        Instant now = Instant.now();
        int currentRound = session.getCurrentRound();
        int groupSize = appProperties.getGame().getGroupSize();

        // Falls bereits alle Spieler gewählt haben, sofort weiter
        int submittedCount = investmentRepository.countBySessionIdAndRoundNumber(session.getId(), currentRound);
        if (submittedCount >= groupSize) {
            advanceRoundOrFinish(session);
            return;
        }

        if (now.isBefore(session.getRoundDeadline())) {
            return;
        }

        if (session.getRoundPhase() == RoundPhase.REGULAR) {
            // Übergang von 60s Regular Phase in 30s Overtime Phase
            session.setRoundPhase(RoundPhase.OVERTIME);
            session.setRoundDeadline(now.plusSeconds(appProperties.getGame().getOvertimePhaseSeconds()));
            sessionRepository.save(session);
        } else {
            // 30s Overtime Phase abgelaufen -> Auto-Bet (20 Rappen / INVEST_OPTION_1) für alle fehlenden Spieler
            List<Player> players = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
            List<RoundInvestment> roundInvestments = investmentRepository
                    .findBySessionIdAndRoundNumberOrderByPlayerNumberAsc(session.getId(), currentRound);
            Set<Integer> submittedPlayerNumbers = roundInvestments.stream()
                    .map(RoundInvestment::getPlayerNumber)
                    .collect(Collectors.toSet());

            int autoBetAmount = appProperties.getGame().getInvestOption1Rappen();
            for (Player player : players) {
                if (!submittedPlayerNumbers.contains(player.getPlayerNumber())) {
                    int actualAutoBet = Math.min(autoBetAmount, player.getCurrentBalanceRappen());
                    recordInvestmentInternal(session, player, currentRound, actualAutoBet, true);
                }
            }

            advanceRoundOrFinish(session);
        }
    }

    private void recordInvestmentInternal(
            GameSession session,
            Player player,
            int roundNumber,
            int amountRappen,
            boolean isAutoBet
    ) {
        RoundInvestment investment = new RoundInvestment();
        investment.setSession(session);
        investment.setPlayer(player);
        investment.setRoundNumber(roundNumber);
        investment.setPlayerNumber(player.getPlayerNumber());
        investment.setAmountRappen(amountRappen);
        investment.setAutoBet(isAutoBet);
        investmentRepository.save(investment);

        player.setCurrentBalanceRappen(player.getCurrentBalanceRappen() - amountRappen);
        if (!isAutoBet) {
            player.setLastSeenAt(Instant.now());
        }
        playerRepository.save(player);

        session.setTotalPotRappen(session.getTotalPotRappen() + amountRappen);
        sessionRepository.save(session);
    }

    private void advanceRoundOrFinish(GameSession session) {
        int totalRounds = appProperties.getGame().getTotalRounds();
        if (session.getCurrentRound() < totalRounds) {
            Instant now = Instant.now();
            session.setCurrentRound(session.getCurrentRound() + 1);
            session.setRoundPhase(RoundPhase.REGULAR);
            session.setRoundStartTime(now);
            session.setRoundDeadline(now.plusSeconds(appProperties.getGame().getRegularPhaseSeconds()));
            sessionRepository.save(session);
        } else {
            List<Player> allPlayers = playerRepository.findBySessionIdOrderByPlayerNumberAsc(session.getId());
            List<RoundInvestment> allInvestments = investmentRepository.findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(session.getId());
            payoutService.resolveEndgame(session, allPlayers, allInvestments);
            playerRepository.saveAll(allPlayers);
            sessionRepository.save(session);
        }
    }

    public int calculateRemainingSeconds(GameSession session) {
        if (session.getStatus() == SessionStatus.PAUSED) {
            long ms = session.getPauseRemainingMs() != null ? session.getPauseRemainingMs() : 0L;
            return (int) Math.max(0L, (ms + 999L) / 1000L);
        }
        if (session.getStatus() == SessionStatus.RUNNING && session.getRoundDeadline() != null) {
            long ms = Duration.between(Instant.now(), session.getRoundDeadline()).toMillis();
            return (int) Math.max(0L, (ms + 999L) / 1000L);
        }
        return 0;
    }

    public List<RoundHistoryDto> buildCompletedRoundHistory(
            GameSession session,
            List<RoundInvestment> allInvestments,
            int groupSize
    ) {
        int maxCompletedRound = session.getStatus() == SessionStatus.FINISHED
                ? appProperties.getGame().getTotalRounds()
                : Math.max(0, session.getCurrentRound() - 1);

        Map<Integer, Map<Integer, RoundInvestment>> byRoundAndPlayer = allInvestments.stream()
                .filter(inv -> inv.getRoundNumber() <= maxCompletedRound)
                .collect(Collectors.groupingBy(
                        RoundInvestment::getRoundNumber,
                        Collectors.toMap(
                                RoundInvestment::getPlayerNumber,
                                inv -> inv,
                                (a, b) -> a
                        )
                ));

        List<RoundHistoryDto> history = new ArrayList<>();
        for (int r = 1; r <= maxCompletedRound; r++) {
            Map<Integer, RoundInvestment> roundMap = byRoundAndPlayer.getOrDefault(r, Map.of());
            List<RoundHistoryDto.PlayerInvestmentDto> row = new ArrayList<>();
            // Statische Reihenfolge Spieler 1 bis 6 (Spec 1.4)
            for (int pNum = 1; pNum <= groupSize; pNum++) {
                RoundInvestment inv = roundMap.get(pNum);
                int amount = inv != null ? inv.getAmountRappen() : 0;
                boolean autoBet = inv != null && inv.isAutoBet();
                row.add(new RoundHistoryDto.PlayerInvestmentDto(pNum, amount, autoBet));
            }
            history.add(new RoundHistoryDto(r, row));
        }
        return history;
    }

    private GameStateResponse buildGameStateResponse(
            GameSession session,
            Player player,
            List<Player> allPlayers,
            List<RoundInvestment> allInvestments
    ) {
        int groupSize = appProperties.getGame().getGroupSize();
        int totalRounds = appProperties.getGame().getTotalRounds();
        int currentRound = session.getCurrentRound();

        Map<Integer, Player> playerByNumber = allPlayers.stream()
                .collect(Collectors.toMap(Player::getPlayerNumber, p -> p));

        Map<Integer, RoundInvestment> currentRoundInvestments = allInvestments.stream()
                .filter(inv -> inv.getRoundNumber() == currentRound)
                .collect(Collectors.toMap(RoundInvestment::getPlayerNumber, inv -> inv, (a, b) -> a));

        RoundInvestment myCurrentRoundInvestment = currentRoundInvestments.get(player.getPlayerNumber());
        boolean hasSubmitted = myCurrentRoundInvestment != null;
        Integer myCurrentAmount = myCurrentRoundInvestment != null ? myCurrentRoundInvestment.getAmountRappen() : null;

        List<Integer> allowedOptions = appProperties.getGame().getAllowedInvestmentOptions().stream()
                .filter(opt -> opt <= player.getCurrentBalanceRappen())
                .toList();

        int ownTotalInvested = player.getInitialBalanceRappen() - player.getCurrentBalanceRappen();

        PlayerStateDto playerDto = new PlayerStateDto(
                player.getPlayerNumber(),
                player.getInitialBalanceRappen(),
                player.getCurrentBalanceRappen(),
                ownTotalInvested,
                hasSubmitted,
                myCurrentAmount,
                allowedOptions,
                player.getFinalPayoutRappen()
        );

        List<PlayerSlotStatusDto> slots = new ArrayList<>();
        for (int pNum = 1; pNum <= groupSize; pNum++) {
            boolean isConnected = playerByNumber.containsKey(pNum);
            boolean isReady;
            if (session.getStatus() == SessionStatus.LOBBY) {
                isReady = isConnected;
            } else if (session.getStatus() == SessionStatus.FINISHED) {
                isReady = true;
            } else {
                isReady = currentRoundInvestments.containsKey(pNum);
            }
            slots.add(new PlayerSlotStatusDto(pNum, isConnected, isReady));
        }

        List<RoundHistoryDto> history = buildCompletedRoundHistory(session, allInvestments, groupSize);
        EndgameResultDto endgameDto = payoutService.buildEndgameResultDto(session, player);
        int riskPercent = (int) Math.round(session.getRiskLevel().doubleValue() * 100.0);

        return new GameStateResponse(
                session.getId(),
                session.getCode(),
                session.getStatus(),
                session.getRiskLevel(),
                riskPercent,
                session.getTargetSumRappen(),
                session.getCurrentRound(),
                totalRounds,
                session.getRoundPhase(),
                calculateRemainingSeconds(session),
                session.getStatus() == SessionStatus.PAUSED,
                allPlayers.size(),
                groupSize,
                playerDto,
                slots,
                history,
                endgameDto
        );
    }
}
