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

    /** Liefert nur die Session-ID, ohne die Spielerzeile zu laden (um zuerst die Session sperren zu können). */
    @Query("SELECT p.session.id FROM Player p WHERE p.sessionToken = :sessionToken")
    Optional<UUID> findSessionIdBySessionToken(@Param("sessionToken") String sessionToken);

    /**
     * Aktualisiert ausschliesslich den «zuletzt gesehen»-Zeitpunkt. Die Statusabfrage der Browser (jede Sekunde)
     * darf nie die ganze Spielerzeile zurückschreiben, sonst überschreibt sie gleichzeitig gespeicherte Werte.
     */
    @Modifying
    @Query("UPDATE Player p SET p.lastSeenAt = :now WHERE p.id = :playerId")
    int touchLastSeen(@Param("playerId") UUID playerId, @Param("now") Instant now);

    List<Player> findBySessionIdOrderByPlayerNumberAsc(UUID sessionId);

    int countBySessionId(UUID sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Player p WHERE p.id = :playerId")
    void deletePlayerById(@Param("playerId") UUID playerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Player p WHERE p.session.id = :sessionId")
    int deleteAllBySessionId(@Param("sessionId") UUID sessionId);
}
