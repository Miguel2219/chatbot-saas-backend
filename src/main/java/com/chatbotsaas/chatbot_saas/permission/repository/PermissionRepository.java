package com.chatbotsaas.chatbot_saas.permission.repository;

import com.chatbotsaas.chatbot_saas.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findByModule_ModuleId(UUID moduleModuleId);
}
