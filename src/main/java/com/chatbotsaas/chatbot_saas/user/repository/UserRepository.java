package com.chatbotsaas.chatbot_saas.user.repository;

import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> getUsersByTenantId(UUID tenantId);

    Optional<User> findByEmail(String email);

    Page<User> findByTenant_Id(UUID tenantId, Pageable pageable);

    Optional<User> findUsersByUserIdAndRoles(UUID userId, RoleConstants role);
}
