package ch.bbw.climategame.repository;

import ch.bbw.climategame.entity.PlayerSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerSurveyRepository extends JpaRepository<PlayerSurvey, UUID> {

    List<PlayerSurvey> findBySessionIdOrderByPlayerNumberAsc(UUID sessionId);

    Optional<PlayerSurvey> findBySessionIdAndPlayerNumber(UUID sessionId, int playerNumber);

    boolean existsBySessionIdAndPlayerNumber(UUID sessionId, int playerNumber);

    int countBySessionId(UUID sessionId);
}
