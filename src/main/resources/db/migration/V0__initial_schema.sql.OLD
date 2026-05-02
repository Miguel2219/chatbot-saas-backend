--
-- PostgreSQL database dump
--
-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

-- ============================================================================
-- TABLAS BASE (sin dependencias)
-- ============================================================================

--
-- Name: subscription_plans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subscription_plans (
                                           id uuid NOT NULL,
                                           code character varying(50) NOT NULL,
                                           name character varying(100) NOT NULL,
                                           cycle_conversations_limit integer NOT NULL,
                                           excess_conversation_cost_cop integer NOT NULL,
                                           max_advisers integer,
                                           max_documents integer,
                                           max_document_size_mb integer,
                                           is_active boolean DEFAULT true NOT NULL,
                                           created_at timestamp without time zone DEFAULT now() NOT NULL,
                                           updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
                              created_at timestamp(6) without time zone,
                              role_id uuid NOT NULL,
                              description character varying(255),
                              name character varying(255) NOT NULL
);

--
-- Name: modules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.modules (
                                display_order integer NOT NULL,
                                module_id uuid NOT NULL,
                                icon character varying(255) NOT NULL,
                                name character varying(255) NOT NULL,
                                route character varying(255) NOT NULL
);

-- ============================================================================
-- TABLAS QUE DEPENDEN DE LAS ANTERIORES
-- ============================================================================

--
-- Name: tenants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tenants (
                                is_active boolean,
                                created_at timestamp(6) without time zone,
                                tenant_id uuid NOT NULL,
                                email character varying(255) NOT NULL,
                                implementation_type character varying(255),
                                name character varying(255) NOT NULL,
                                subscription_plan_id uuid NOT NULL,
                                billing_cycle_day smallint NOT NULL,
                                current_cycle_start date NOT NULL,
                                current_cycle_end date NOT NULL,
                                CONSTRAINT tenants_implementation_type_check CHECK (((implementation_type)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'WIDGET'::character varying, 'BOTH'::character varying])::text[])))
);

--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
                              must_change_password boolean NOT NULL,
                              created_at timestamp(6) without time zone,
                              tenant_id uuid,
                              user_id uuid NOT NULL,
                              email character varying(255) NOT NULL,
                              notification_channel character varying(255),
                              password character varying(255) NOT NULL,
                              CONSTRAINT users_notification_channel_check CHECK (((notification_channel)::text = ANY ((ARRAY['EMAIL'::character varying, 'WHATSAPP'::character varying, 'BOTH'::character varying])::text[])))
);

--
-- Name: person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person (
                               available boolean NOT NULL,
                               phone character varying(13),
                               number_document character varying(15),
                               user_id uuid NOT NULL,
                               lastname character varying(100),
                               name character varying(100)
);

--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
                                    module_id uuid NOT NULL,
                                    permission_id uuid NOT NULL,
                                    action character varying(255) NOT NULL
);

--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
                                         permission_id uuid NOT NULL,
                                         role_id uuid NOT NULL
);

--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
                                   role_id uuid NOT NULL,
                                   user_id uuid NOT NULL
);

--
-- Name: bots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bots (
                             is_active boolean,
                             last_assignee_index integer NOT NULL,
                             created_at timestamp(6) without time zone,
                             bot_id uuid NOT NULL,
                             tenant_id uuid NOT NULL,
                             description character varying(255) NOT NULL,
                             name character varying(255) NOT NULL,
                             system_prompt text
);

--
-- Name: bot_lead_assignees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bot_lead_assignees (
                                           bot_id uuid NOT NULL,
                                           user_id uuid NOT NULL
);

--
-- Name: conversations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conversations (
                                      status character varying(32) NOT NULL,
                                      created_at timestamp(6) without time zone,
                                      bot_id uuid NOT NULL,
                                      conversation_id uuid NOT NULL,
                                      message text NOT NULL,
                                      role character varying(255) NOT NULL,
                                      session_id character varying(255) NOT NULL
);

--
-- Name: documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documents (
                                  created_at timestamp(6) without time zone,
                                  file_size bigint NOT NULL,
                                  bot_id uuid NOT NULL,
                                  document_id uuid NOT NULL,
                                  file_name character varying(255) NOT NULL,
                                  file_path character varying(255) NOT NULL,
                                  file_type character varying(255) NOT NULL
);

--
-- Name: leads; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.leads (
                              lead_id uuid NOT NULL,
                              assigned_adviser_id uuid,
                              bot_id uuid NOT NULL,
                              created_at timestamp(6) without time zone,
                              email character varying(255),
                              channel character varying(255) NOT NULL,
                              name character varying(255) NOT NULL,
                              phone character varying(255),
                              request_detail text,
                              session_id character varying(255) NOT NULL,
                              status character varying(255),
                              CONSTRAINT leads_channel_check CHECK (((channel)::text = ANY ((ARRAY['WIDGET'::character varying, 'WHATSAPP'::character varying])::text[]))),
                              CONSTRAINT leads_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONTACTED'::character varying, 'CLOSED'::character varying])::text[])))
);

--
-- Name: whatsapp_configs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.whatsapp_configs (
                                         is_active boolean NOT NULL,
                                         created_at timestamp(6) without time zone NOT NULL,
                                         bot_id uuid NOT NULL,
                                         id uuid NOT NULL,
                                         access_token text NOT NULL,
                                         phone_number_id character varying(255) NOT NULL
);

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
                                       id uuid DEFAULT gen_random_uuid() NOT NULL,
                                       user_id uuid NOT NULL,
                                       token_hash character varying(64) NOT NULL,
                                       expires_at timestamp without time zone NOT NULL,
                                       revoked boolean DEFAULT false NOT NULL,
                                       created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
                                              id uuid DEFAULT gen_random_uuid() NOT NULL,
                                              user_id uuid NOT NULL,
                                              token_hash character varying(64) NOT NULL,
                                              expires_at timestamp without time zone NOT NULL,
                                              used boolean DEFAULT false NOT NULL,
                                              created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

--
-- Name: tenant_quota_cycles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tenant_quota_cycles (
                                            id uuid NOT NULL,
                                            tenant_id uuid NOT NULL,
                                            cycle_start date NOT NULL,
                                            cycle_end date NOT NULL,
                                            conversations_count integer DEFAULT 0 NOT NULL,
                                            plan_limit_snapshot integer NOT NULL,
                                            excess_cost_snapshot_cop integer NOT NULL,
                                            notified_80 boolean DEFAULT false NOT NULL,
                                            notified_100 boolean DEFAULT false NOT NULL,
                                            notified_150 boolean DEFAULT false NOT NULL,
                                            created_at timestamp without time zone DEFAULT now() NOT NULL,
                                            updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: quota_session_days; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quota_session_days (
                                           tenant_id uuid NOT NULL,
                                           cycle_start date NOT NULL,
                                           session_id character varying(255) NOT NULL,
                                           day date NOT NULL,
                                           first_seen_at timestamp without time zone DEFAULT now() NOT NULL
);

-- ============================================================================
-- PRIMARY KEYS
-- ============================================================================

ALTER TABLE ONLY public.subscription_plans
    ADD CONSTRAINT subscription_plans_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT modules_pkey PRIMARY KEY (module_id);

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (tenant_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (permission_id);

ALTER TABLE ONLY public.bots
    ADD CONSTRAINT bots_pkey PRIMARY KEY (bot_id);

ALTER TABLE ONLY public.conversations
    ADD CONSTRAINT conversations_pkey PRIMARY KEY (conversation_id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (document_id);

ALTER TABLE ONLY public.leads
    ADD CONSTRAINT leads_pkey PRIMARY KEY (lead_id);

ALTER TABLE ONLY public.whatsapp_configs
    ADD CONSTRAINT whatsapp_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenant_quota_cycles
    ADD CONSTRAINT tenant_quota_cycles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quota_session_days
    ADD CONSTRAINT quota_session_days_pkey PRIMARY KEY (tenant_id, cycle_start, session_id, day);

-- ============================================================================
-- UNIQUE CONSTRAINTS
-- ============================================================================

ALTER TABLE ONLY public.subscription_plans
    ADD CONSTRAINT subscription_plans_code_key UNIQUE (code);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT modules_name_key UNIQUE (name);

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_email_key UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_number_document_key UNIQUE (number_document);

ALTER TABLE ONLY public.whatsapp_configs
    ADD CONSTRAINT whatsapp_configs_bot_id_key UNIQUE (bot_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash);

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash);

ALTER TABLE ONLY public.tenant_quota_cycles
    ADD CONSTRAINT uq_tenant_cycle UNIQUE (tenant_id, cycle_start);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_prt_expires_at ON public.password_reset_tokens USING btree (expires_at);
CREATE INDEX idx_prt_user_id_created ON public.password_reset_tokens USING btree (user_id, created_at);
CREATE INDEX idx_refresh_tokens_expires_at ON public.refresh_tokens USING btree (expires_at);
CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);
CREATE INDEX idx_tqc_cycle_end ON public.tenant_quota_cycles USING btree (cycle_end);
CREATE INDEX idx_tqc_tenant ON public.tenant_quota_cycles USING btree (tenant_id);

-- ============================================================================
-- FOREIGN KEYS
-- ============================================================================

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_subscription_plan_id_fkey FOREIGN KEY (subscription_plan_id) REFERENCES public.subscription_plans(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk21hn1a5ja1tve7ae02fnn4cld FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id);

ALTER TABLE ONLY public.person
    ADD CONSTRAINT fkemsnreyk6g37uoja1ngeog5sp FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT fk22lamcef3kykumk5rwjiv4x5u FOREIGN KEY (module_id) REFERENCES public.modules(module_id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(permission_id);

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.bots
    ADD CONSTRAINT fkqwdkf7wxmv49q8swk66tng76p FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id);

ALTER TABLE ONLY public.bot_lead_assignees
    ADD CONSTRAINT fkdjchxqm1edfs2iqjg85v8n4mf FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.bot_lead_assignees
    ADD CONSTRAINT fkta77b0fbka0l5nh6h38rnkcmd FOREIGN KEY (bot_id) REFERENCES public.bots(bot_id);

ALTER TABLE ONLY public.whatsapp_configs
    ADD CONSTRAINT fkodw8ftpvk6kd79ywpxmellawv FOREIGN KEY (bot_id) REFERENCES public.bots(bot_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tenant_quota_cycles
    ADD CONSTRAINT tenant_quota_cycles_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.quota_session_days
    ADD CONSTRAINT quota_session_days_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id) ON DELETE CASCADE;

--
-- PostgreSQL database dump complete
--