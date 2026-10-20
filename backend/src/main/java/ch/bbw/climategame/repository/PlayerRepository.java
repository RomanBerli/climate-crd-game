package ch.bbw.climategame.repository;

import ch.bbw.climategame.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findBySessionToken(String sessionToken);

    List<Player> findBySessionIdOrderByPlayerNumberAsc(UUID sessionId);

    int countBySessionId(UUID sessionId);
}
