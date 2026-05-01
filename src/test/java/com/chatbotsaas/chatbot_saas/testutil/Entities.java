package com.chatbotsaas.chatbot_saas.testutil;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.user.entity.User;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class Entities {

    private Entities() {}

    public static Role role(UUID roleId, String name) {
        Role role = new Role();
        setField(role, "roleId", roleId);
        setField(role, "name", name);
        return role;
    }

    public static Tenant tenant(UUID tenantId, String name, ImplementationType implType) {
        Tenant tenant = new Tenant(name, name + "@example.com", true, implType);
        setField(tenant, "id", tenantId);
        return tenant;
    }

    public static User user(UUID userId, String email, Tenant tenant, Role... roles) {
        List<Role> roleList = new ArrayList<>(Arrays.asList(roles));
        User user = new User(email, "hashed", false, roleList, tenant, null);
        setField(user, "userId", userId);
        return user;
    }

    public static Bot bot(UUID botId, Tenant tenant) {
        Bot bot = new Bot("bot", "desc", tenant, true);
        setField(bot, "botId", botId);
        return bot;
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("failed to set " + fieldName + " on " + target.getClass(), e);
        }
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
