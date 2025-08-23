package com.om.backend.Repositories;

import com.om.backend.Model.UserSession;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    Optional<UserSession> findByPhoneNumberAndIsActive(String phoneNumber, boolean active);

    @Query("select s from UserSession s where s.userId = :userId order by s.lastSeenAt desc")
    List<UserSession> findAllByUserIdOrderByLastSeenAtDesc(@Param("userId") Long userId);
}
