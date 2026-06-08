package mobi.foo.mpcss.repository;

import mobi.foo.mpcss.entity.CustomerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRegistrationRepository extends JpaRepository<CustomerRegistration, Long> {

    Optional<CustomerRegistration> findByMessageId(String messageId);

    List<CustomerRegistration> findByMobileNumber(String mobileNumber);

    List<CustomerRegistration> findByCustomerIdNumber(String customerIdNumber);

    List<CustomerRegistration> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByMessageId(String messageId);
}

