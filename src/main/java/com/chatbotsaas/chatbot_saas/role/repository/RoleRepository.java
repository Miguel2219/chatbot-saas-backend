package com.chatbotsaas.chatbot_saas.role.repository;

import com.chatbotsaas.chatbot_saas.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findAllById(Iterable<UUID> roleIds);
}
