package com.chatbotsaas.chatbot_saas.user.service;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.dto.request.CreateAdviserRequestDto;
import com.chatbotsaas.chatbot_saas.user.dto.response.AdviserResponseDto;
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
public class AdviserService {
    private final PersonRepository personRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;


    public AdviserService(PersonRepository personRepository, TenantRepository tenantRepository, UserRepository userRepository, BotRepository botRepository) {
        this.personRepository = personRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
    }

    @Transactional
    public AdviserResponseDto createAdviser(UUID tenantId, CreateAdviserRequestDto requestDto){

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(
                () -> new AppException("Tenant not found", HttpStatus.NOT_FOUND)
        );

        if(userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new AppException("User with this email already exists", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(requestDto.getEmail())
                .password("")
                .role(RoleConstants.ADVISER)
                .notificationChannel(requestDto.getNotificationChannel())
                .tenant(tenant)
                .build();
        userRepository.save(user);
        Person person = Person.create(
                requestDto.getName(), requestDto.getLastname(), requestDto.getPhone(), requestDto.getNumberDocument()
        );
        person.setUser(user);
        personRepository.save(person);

        List<Bot> bots = botRepository.findByTenant_IdAndBotIdIn(tenantId, requestDto.getBotIds());

        if (bots.size() != requestDto.getBotIds().size()) {
            throw new AppException("Some bots do not belong to this tenant", HttpStatus.CONFLICT);
        }

        bots.forEach(bot -> bot.getAdvisers().add(user));
        botRepository.saveAll(bots);

        return AdviserResponseDto.builder()
                .userId(user.getUserId())
                .name(requestDto.getName())
                .lastname(requestDto.getLastname())
                .phone(requestDto.getPhone())
                .email(requestDto.getEmail())
                .notificationChannel(requestDto.getNotificationChannel())
                .assignedBots(bots.stream()
                        .map(Bot::getName)
                        .toList()
                )
                .build();
    }

    @Transactional
    public List<AdviserResponseDto> getAdviserByTenant(UUID tenantId) {
        tenantRepository.findById(tenantId).orElseThrow(
                () -> new AppException("Tenant not found", HttpStatus.NOT_FOUND)
        );
        List<Person> advisers = userRepository.findByTenant_IdAndRole(tenantId, RoleConstants.ADVISER)
                .stream()
                .map(User::getPerson)
                .toList();

        return advisers.stream().map(
                adviser -> AdviserResponseDto.builder()
                        .userId(adviser.getUser().getUserId())
                        .name(adviser.getName())
                        .lastname(adviser.getLastname())
                        .phone(adviser.getPhone())
                        .email(adviser.getUser().getEmail())
                        .notificationChannel(adviser.getUser().getNotificationChannel())
                        .assignedBots(adviser.getUser().getBots().stream().map(
                                Bot::getName
                        ).toList())
                        .build()

        ).toList();
    }

    @Transactional
    public void deleteAdviser(UUID adviserId) {
        //Find user by userId and adviser role
        User adviser = userRepository.findUsersByUserIdAndRole(adviserId, RoleConstants.ADVISER).orElseThrow(
                () -> new AppException("Adviser not found", HttpStatus.NOT_FOUND)
        );

        //To get all bots from user
        List<Bot> bots = adviser.getBots();

        bots.forEach(bot -> bot.getAdvisers().remove(adviser));

        //Delete user of table's person
        Person person = adviser.getPerson();
        personRepository.delete(person);

        //Delete of table User
        userRepository.delete(adviser);
    }
}
