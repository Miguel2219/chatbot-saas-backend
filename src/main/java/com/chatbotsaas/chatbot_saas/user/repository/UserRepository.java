package com.chatbotsaas.chatbot_saas.user.repository;

import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> getUsersByTenantId(UUID tenantId);

    Optional<User> findByEmail(String email);

    List<User> findByTenant_IdAndRole(UUID tenantId, RoleConstants role);

    Optional<User> findUsersByUserIdAndRole(UUID userId, RoleConstants role);

}
