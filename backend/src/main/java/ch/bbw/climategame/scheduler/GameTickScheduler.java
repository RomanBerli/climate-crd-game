package ch.bbw.climategame.scheduler;

import ch.bbw.climategame.entity.GameSession;
import ch.bbw.climategame.entity.SessionStatus;
import ch.bbw.climategame.repository.GameSessionRepository;
import ch.bbw.climategame.service.GameEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameTickScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameTickScheduler.class);

    private final GameSessionRepository sessionRepository;
    private final GameEngineService gameEngineService;

    public GameTickScheduler(GameSessionRepository sessionRepository, GameEngineService gameEngineService) {
        this.sessionRepository = sessionRepository;
        this.gameEngineService = gameEngineService;
    }

    @Scheduled(fixedRate = 1000)
    public void tickActiveSessions() {
        List<GameSession> activeSessions = sessionRepository.findByStatusOrderByCreatedAtDesc(SessionStatus.RUNNING);
        for (GameSession session : activeSessions) {
            try {
                gameEngineService.processSessionTimerTick(session.getId());
            } catch (Exception e) {
                log.error("Fehler beim Timer-Tick für Session {}: {}", session.getCode(), e.getMessage(), e);
            }
        }
    }
}
