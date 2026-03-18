package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.NotificationPreference;
import org.hartford.fireinsurance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUser(User user);
    Optional<NotificationPreference> findByUserUsername(String username);
}
