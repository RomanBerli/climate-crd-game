package ch.bbw.climategame.repository;

import ch.bbw.climategame.entity.RoundInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoundInvestmentRepository extends JpaRepository<RoundInvestment, UUID> {

    List<RoundInvestment> findBySessionIdOrderByRoundNumberAscPlayerNumberAsc(UUID sessionId);

    List<RoundInvestment> findBySessionIdAndRoundNumberOrderByPlayerNumberAsc(UUID sessionId, int roundNumber);

    Optional<RoundInvestment> findBySessionIdAndRoundNumberAndPlayerNumber(UUID sessionId, int roundNumber, int playerNumber);

    boolean existsBySessionIdAndRoundNumberAndPlayerNumber(UUID sessionId, int roundNumber, int playerNumber);

    int countBySessionIdAndRoundNumber(UUID sessionId, int roundNumber);
}
