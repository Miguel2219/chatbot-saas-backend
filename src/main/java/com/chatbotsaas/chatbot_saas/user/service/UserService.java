package com.chatbotsaas.chatbot_saas.user.service;

import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.dto.response.PersonResponse;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PersonRepository personRepository;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PersonRepository personRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.personRepository = personRepository;
    }

    @Transactional
    public List<PersonResponse> getUsersByTenant(UUID tenantId) {
        tenantRepository.findById(tenantId).orElseThrow(
                () -> new AppException("Tenant not found", HttpStatus.NOT_FOUND)
        );
        List<User> users = userRepository.getUsersByTenantId(tenantId);

        return users.stream()
                .map(user -> {
                    Person person = personRepository.findById(user.getUserId()).orElseThrow(
                            () -> new AppException("Person not found", HttpStatus.NOT_FOUND)
                    );
                    return PersonResponse.builder()
                            .userId(person.getUser().getUserId())
                            .fullName(
                                    person.getName() +
                                            " " +
                                            person.getLastname()
                            )
                            .build();
                }).toList();
    }
}
