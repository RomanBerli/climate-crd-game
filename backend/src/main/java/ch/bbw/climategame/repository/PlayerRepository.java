package ch.bbw.climategame.repository;

import ch.bbw.climategame.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findBySessionToken(String sessionToken);

    List<Player> findBySessionIdOrderByPlayerNumberAsc(UUID sessionId);

    int countBySessionId(UUID sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Player p WHERE p.id = :playerId")
    void deletePlayerById(@Param("playerId") UUID playerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Player p WHERE p.session.status = ch.bbw.climategame.entity.SessionStatus.LOBBY AND p.lastSeenAt < :cutoff")
    int deleteInactiveLobbyPlayers(@Param("cutoff") Instant cutoff);
}
