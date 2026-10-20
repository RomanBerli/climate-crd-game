package ch.bbw.climategame.repository;

import ch.bbw.climategame.entity.GameSession;
import ch.bbw.climategame.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    Optional<GameSession> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM GameSession s WHERE UPPER(s.code) = UPPER(:code)")
    Optional<GameSession> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM GameSession s WHERE s.id = :id")
    Optional<GameSession> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCodeIgnoreCase(String code);

    List<GameSession> findByStatusOrderByCreatedAtDesc(SessionStatus status);

    List<GameSession> findAllByOrderByCreatedAtDesc();
}
