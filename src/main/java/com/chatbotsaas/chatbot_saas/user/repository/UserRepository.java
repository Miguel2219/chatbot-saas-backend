package com.chatbotsaas.chatbot_saas.user.repository;

import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<User> findByTenantIsNull();

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        JOIN u.roles r
        WHERE u.tenant.id = :tenantId
        AND r.roleId = :roleId
    """)
    List<User> findByTenantIdAndRoleId(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);

    @Query("""
        SELECT COUNT(u) FROM User u
        JOIN u.roles r
        WHERE u.tenant.id = :tenantId
        AND r.roleId = :roleId
    """)
    long countByTenantIdAndRoleId(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);

    @Query("""
        SELECT COUNT(u) > 0 FROM User u
        JOIN u.roles r
        WHERE u.email = :email
        AND r.roleId = :adminRoleId
    """)
    boolean isAdminByEmail(@Param("email") String email, @Param("adminRoleId") UUID adminRoleId);

    @Query("""
        SELECT COUNT(p) > 0 FROM User u
        JOIN u.roles r
        JOIN r.permissions p
        WHERE u.email = :email
        AND LOWER(p.module.route) = LOWER(:route)
        AND LOWER(p.action) = LOWER(:action)
    """)
    boolean hasPermissionForModule(
        @Param("email") String email,
        @Param("route") String route,
        @Param("action") String action
    );
}
