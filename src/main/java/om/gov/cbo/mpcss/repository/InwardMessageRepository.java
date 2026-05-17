package om.gov.cbo.mpcss.repository;

import om.gov.cbo.mpcss.entity.InwardMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InwardMessageRepository extends JpaRepository<InwardMessage, Long> {

    List<InwardMessage> findByMessageTypeAndReceivedAtBetween(
            String messageType, LocalDateTime from, LocalDateTime to);

    List<InwardMessage> findByProcessedFalseOrderByReceivedAtAsc();
}

