package om.gov.cbo.mpcss.repository;

import om.gov.cbo.mpcss.entity.MpcssTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<MpcssTransaction, Long> {

    Optional<MpcssTransaction> findByMessageId(String messageId);

    Optional<MpcssTransaction> findByEndToEndId(String endToEndId);

    Optional<MpcssTransaction> findByCorrelationId(String correlationId);

    List<MpcssTransaction> findByStatusAndDirection(String status, MpcssTransaction.Direction direction);

    List<MpcssTransaction> findByDebtorAccountIdAndCreatedAtBetween(
            String debtorAccountId, LocalDateTime from, LocalDateTime to);

    List<MpcssTransaction> findByCreditorAccountIdAndCreatedAtBetween(
            String creditorAccountId, LocalDateTime from, LocalDateTime to);

    List<MpcssTransaction> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByMessageId(String messageId);
}

