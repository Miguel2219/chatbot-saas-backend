package com.chatbotsaas.chatbot_saas.user.repository;

import com.chatbotsaas.chatbot_saas.user.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {
}
