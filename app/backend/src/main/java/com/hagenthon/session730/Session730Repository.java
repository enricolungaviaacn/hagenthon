package com.hagenthon.session730;

import com.hagenthon.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Session730Repository extends JpaRepository<Session730, UUID> {
    List<Session730> findByUserOrderByCreatedAtDesc(User user);
    Optional<Session730> findByIdAndUser(UUID id, User user);
}
