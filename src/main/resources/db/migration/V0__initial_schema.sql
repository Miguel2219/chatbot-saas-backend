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

--
-- Name: bot_lead_assignees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bot_lead_assignees (
    bot_id uuid NOT NULL,
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
-- Name: modules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.modules (
    display_order integer NOT NULL,
    module_id uuid NOT NULL,
    icon character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    route character varying(255) NOT NULL
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
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    module_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    action character varying(255) NOT NULL
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
-- Name: quota_session_days; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quota_session_days (
    tenant_id uuid NOT NULL,
    cycle_start date NOT NULL,
    session_id character varying(255) NOT NULL,
    day date NOT NULL,
    first_seen_at timestamp without time zone DEFAULT now() NOT NULL
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
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    permission_id uuid NOT NULL,
    role_id uuid NOT NULL
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
    CONSTRAINT tenants_implementation_type_check CHECK (((implementation_type)::text = ANY (ARRAY[('WHATSAPP'::character varying)::text, ('WIDGET'::character varying)::text, ('BOTH'::character varying)::text])))
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    role_id uuid NOT NULL,
    user_id uuid NOT NULL
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
    CONSTRAINT users_notification_channel_check CHECK (((notification_channel)::text = ANY (ARRAY[('EMAIL'::character varying)::text, ('WHATSAPP'::character varying)::text, ('BOTH'::character varying)::text])))
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
-- Data for Name: bot_lead_assignees; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: bots; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: conversations; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: documents; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: modules; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.modules VALUES (1, '6e2f8b14-3d7a-4c9e-b5f2-8a1d0e7c3b6f', 'dashboard', 'Dashboard', 'dashboard');
INSERT INTO public.modules VALUES (2, '2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', 'smart_toy', 'Bots', 'bots');
INSERT INTO public.modules VALUES (3, '7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', 'person_search', 'Leads', 'leads');
INSERT INTO public.modules VALUES (4, '1b8e4f27-5c2a-4d9b-a631-7f0e3c5b8d4a', 'chat_bubble_outline', 'Conversaciones', 'conversations');
INSERT INTO public.modules VALUES (5, '5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', 'description', 'Documentos', 'documents');
INSERT INTO public.modules VALUES (6, '3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', 'group', 'Usuarios', 'users');
INSERT INTO public.modules VALUES (7, '8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', 'smartphone', 'WhatsApp', 'whatsapp-config');
INSERT INTO public.modules VALUES (8, '4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', 'business', 'Negocios', 'tenants');
INSERT INTO public.modules VALUES (9, '0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', 'admin_panel_settings', 'Roles', 'roles');
INSERT INTO public.modules VALUES (10, 'd1e5f2a4-8c9b-4a3d-b7e6-0f1c2d4e5a6b', 'bar-chart', 'Cuotas', 'quotas');


--
-- Data for Name: password_reset_tokens; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.permissions VALUES ('6e2f8b14-3d7a-4c9e-b5f2-8a1d0e7c3b6f', 'c3a7f291-5b8d-4e6c-a142-9f0d3b5e7c2a', 'VIEW');
INSERT INTO public.permissions VALUES ('2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', '7e1b4f83-2c9a-4d5e-b637-0f4a8c2e1b6d', 'VIEW');
INSERT INTO public.permissions VALUES ('2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', '2f8c5d14-9a3b-4e7f-c261-6d5b0a9f3c8e', 'CREATE');
INSERT INTO public.permissions VALUES ('2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', '8a3e6b27-1f4c-4d9a-b583-2e7c1f0d4b5a', 'EDIT');
INSERT INTO public.permissions VALUES ('2a5d9f31-8b4e-4a7c-b183-5f2e0d8c6a4b', '5b9f2c48-7d1a-4e3b-a896-3c4f8e2d0b7a', 'DELETE');
INSERT INTO public.permissions VALUES ('7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', '1d4a7e59-3c8f-4b2d-b614-7a5e9c1f3d0b', 'VIEW');
INSERT INTO public.permissions VALUES ('7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', '9f2d5b63-8a4c-4e1f-c729-4b6d0e3a8c2f', 'EDIT');
INSERT INTO public.permissions VALUES ('7f3c1e84-2a9d-4b6f-c8e5-1d4a7b0f3e2c', '4c8e1f74-2b7d-4a5c-b843-8f1a6d2e5b9c', 'DELETE');
INSERT INTO public.permissions VALUES ('1b8e4f27-5c2a-4d9b-a631-7f0e3c5b8d4a', '6a1c9d85-5f3b-4e8a-b256-1d9c4f7b2e0a', 'VIEW');
INSERT INTO public.permissions VALUES ('5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', '3b7f4e96-1a8c-4d2b-c671-5e2b9a1f6d4c', 'VIEW');
INSERT INTO public.permissions VALUES ('5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', '8e5a2d07-4c1f-4b9e-b134-2f7d5c8a0e3b', 'CREATE');
INSERT INTO public.permissions VALUES ('5d9a2c63-7f1b-4e8a-b274-3c6e0f9d1a5b', '0f9b6c18-7d4a-4e3f-a892-6c1e0b4d7f5a', 'DELETE');
INSERT INTO public.permissions VALUES ('3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', '7d3e8f29-2b5c-4a1d-b967-9a4f2e6c1b0d', 'VIEW');
INSERT INTO public.permissions VALUES ('3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', '2c6d1b30-8f7a-4e5c-b241-3b8d9f0e4c6a', 'CREATE');
INSERT INTO public.permissions VALUES ('3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', '9a4f5c41-1e2b-4d8f-c583-7c3a0d1e9b4f', 'EDIT');
INSERT INTO public.permissions VALUES ('3a6f1b94-8e2c-4d7a-b548-9f3d5e0c2b7a', '5f1a8d52-6c3e-4b7a-b916-1e5f4c2b0d8a', 'DELETE');
INSERT INTO public.permissions VALUES ('8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', '1e7b3f63-4a9d-4c2e-b357-8d2c5a6f1e0b', 'VIEW');
INSERT INTO public.permissions VALUES ('8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', '6b2e9c74-3f5a-4d1b-a624-4a7e1d3c8f2b', 'CREATE');
INSERT INTO public.permissions VALUES ('8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', '3c5f4a85-8b1e-4e6d-b793-2f9a7b4c0d1e', 'EDIT');
INSERT INTO public.permissions VALUES ('8c4b7f25-1a3e-4d9c-b6f2-0e7a5d3b8c1f', '8d9a1b96-2c6f-4f3a-c168-5b4d2e9a3f7c', 'DELETE');
INSERT INTO public.permissions VALUES ('4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', '4e2c7f07-9a3b-4b8e-b435-7c1f6d0e5a2b', 'VIEW');
INSERT INTO public.permissions VALUES ('4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', '0a6d4e18-1f8c-4c5b-b872-3e9b1a7f2d4c', 'CREATE');
INSERT INTO public.permissions VALUES ('4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', '7f8b2d29-5e1a-4a9c-c241-9d3c4b8e0f6a', 'DELETE');
INSERT INTO public.permissions VALUES ('0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', '2b1f6c30-4d7e-4e4a-b596-1a8f3c2d7b0e', 'VIEW');
INSERT INTO public.permissions VALUES ('0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', '9c4a8e41-7b2f-4d3b-a913-6f2d0c5a4e1b', 'CREATE');
INSERT INTO public.permissions VALUES ('0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', '5a7d3f52-2e9b-4c6d-b248-4b1e8a0c6d3f', 'EDIT');
INSERT INTO public.permissions VALUES ('0e7d3c58-4b9a-4f2e-b761-5a2c8f1d0e6b', '1f3b9a63-6c4d-4f7e-c582-8e5a2d4b1c0f', 'DELETE');
INSERT INTO public.permissions VALUES ('d1e5f2a4-8c9b-4a3d-b7e6-0f1c2d4e5a6b', 'a2b3c4d5-6e7f-4a89-90ab-cdef12345678', 'VIEW');
INSERT INTO public.permissions VALUES ('4f2e9b67-6c1a-4b8d-a395-2f7c0e4b9d3a', 'b4a1c8e7-2d5f-4a6e-9b3c-8f1d7e0c9a3b', 'EDIT');


--
-- Data for Name: person; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: quota_session_days; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.role_permissions VALUES ('c3a7f291-5b8d-4e6c-a142-9f0d3b5e7c2a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('7e1b4f83-2c9a-4d5e-b637-0f4a8c2e1b6d', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('2f8c5d14-9a3b-4e7f-c261-6d5b0a9f3c8e', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('8a3e6b27-1f4c-4d9a-b583-2e7c1f0d4b5a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('5b9f2c48-7d1a-4e3b-a896-3c4f8e2d0b7a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('1d4a7e59-3c8f-4b2d-b614-7a5e9c1f3d0b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('9f2d5b63-8a4c-4e1f-c729-4b6d0e3a8c2f', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('4c8e1f74-2b7d-4a5c-b843-8f1a6d2e5b9c', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('6a1c9d85-5f3b-4e8a-b256-1d9c4f7b2e0a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('3b7f4e96-1a8c-4d2b-c671-5e2b9a1f6d4c', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('8e5a2d07-4c1f-4b9e-b134-2f7d5c8a0e3b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('0f9b6c18-7d4a-4e3f-a892-6c1e0b4d7f5a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('7d3e8f29-2b5c-4a1d-b967-9a4f2e6c1b0d', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('2c6d1b30-8f7a-4e5c-b241-3b8d9f0e4c6a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('9a4f5c41-1e2b-4d8f-c583-7c3a0d1e9b4f', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('5f1a8d52-6c3e-4b7a-b916-1e5f4c2b0d8a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('1e7b3f63-4a9d-4c2e-b357-8d2c5a6f1e0b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('6b2e9c74-3f5a-4d1b-a624-4a7e1d3c8f2b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('3c5f4a85-8b1e-4e6d-b793-2f9a7b4c0d1e', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('8d9a1b96-2c6f-4f3a-c168-5b4d2e9a3f7c', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('4e2c7f07-9a3b-4b8e-b435-7c1f6d0e5a2b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('0a6d4e18-1f8c-4c5b-b872-3e9b1a7f2d4c', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('7f8b2d29-5e1a-4a9c-c241-9d3c4b8e0f6a', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('2b1f6c30-4d7e-4e4a-b596-1a8f3c2d7b0e', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('9c4a8e41-7b2f-4d3b-a913-6f2d0c5a4e1b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('5a7d3f52-2e9b-4c6d-b248-4b1e8a0c6d3f', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('1f3b9a63-6c4d-4f7e-c582-8e5a2d4b1c0f', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('a2b3c4d5-6e7f-4a89-90ab-cdef12345678', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('b4a1c8e7-2d5f-4a6e-9b3c-8f1d7e0c9a3b', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b');
INSERT INTO public.role_permissions VALUES ('c3a7f291-5b8d-4e6c-a142-9f0d3b5e7c2a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('7e1b4f83-2c9a-4d5e-b637-0f4a8c2e1b6d', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('2f8c5d14-9a3b-4e7f-c261-6d5b0a9f3c8e', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('8a3e6b27-1f4c-4d9a-b583-2e7c1f0d4b5a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('5b9f2c48-7d1a-4e3b-a896-3c4f8e2d0b7a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('1d4a7e59-3c8f-4b2d-b614-7a5e9c1f3d0b', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('9f2d5b63-8a4c-4e1f-c729-4b6d0e3a8c2f', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('4c8e1f74-2b7d-4a5c-b843-8f1a6d2e5b9c', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('6a1c9d85-5f3b-4e8a-b256-1d9c4f7b2e0a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('3b7f4e96-1a8c-4d2b-c671-5e2b9a1f6d4c', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('8e5a2d07-4c1f-4b9e-b134-2f7d5c8a0e3b', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('0f9b6c18-7d4a-4e3f-a892-6c1e0b4d7f5a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('7d3e8f29-2b5c-4a1d-b967-9a4f2e6c1b0d', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('2c6d1b30-8f7a-4e5c-b241-3b8d9f0e4c6a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('9a4f5c41-1e2b-4d8f-c583-7c3a0d1e9b4f', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('5f1a8d52-6c3e-4b7a-b916-1e5f4c2b0d8a', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('1e7b3f63-4a9d-4c2e-b357-8d2c5a6f1e0b', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('6b2e9c74-3f5a-4d1b-a624-4a7e1d3c8f2b', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('3c5f4a85-8b1e-4e6d-b793-2f9a7b4c0d1e', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');
INSERT INTO public.role_permissions VALUES ('8d9a1b96-2c6f-4f3a-c168-5b4d2e9a3f7c', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48');


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.roles VALUES ('2026-05-01 19:03:47.128897', 'a3f8e2d1-7c4b-4e9a-b621-5f0d3e8c1a2b', 'DueÃ±o de la plataforma', 'ADMIN');
INSERT INTO public.roles VALUES ('2026-05-01 19:03:47.128897', '9b1d5f73-2a8e-4c6d-b394-7e0f2a5c8d1e', 'Administrador de empresa', 'USER');
INSERT INTO public.roles VALUES ('2026-05-01 19:03:48.155204', '7e2b6f0c-4d91-4a5e-8f3b-0c9a1d6e2b48', 'DueÃ±o del tenant â€” administra su tenant e invita usuarios', 'TENANT_OWNER');


--
-- Data for Name: subscription_plans; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.subscription_plans VALUES ('c1a51a01-b001-4000-a000-000000000001', 'BASIC', 'Plan BÃ¡sico', 2000, 100, NULL, NULL, NULL, true, '2026-05-01 19:03:48.393772', '2026-05-01 19:03:48.393772');


--
-- Data for Name: tenant_quota_cycles; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: tenants; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: whatsapp_configs; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Name: bots bots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bots
    ADD CONSTRAINT bots_pkey PRIMARY KEY (bot_id);


--
-- Name: conversations conversations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conversations
    ADD CONSTRAINT conversations_pkey PRIMARY KEY (conversation_id);


--
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (document_id);


--
-- Name: modules modules_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT modules_name_key UNIQUE (name);


--
-- Name: modules modules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modules
    ADD CONSTRAINT modules_pkey PRIMARY KEY (module_id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (permission_id);


--
-- Name: person person_number_document_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_number_document_key UNIQUE (number_document);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (user_id);


--
-- Name: quota_session_days quota_session_days_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quota_session_days
    ADD CONSTRAINT quota_session_days_pkey PRIMARY KEY (tenant_id, cycle_start, session_id, day);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);


--
-- Name: subscription_plans subscription_plans_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscription_plans
    ADD CONSTRAINT subscription_plans_code_key UNIQUE (code);


--
-- Name: subscription_plans subscription_plans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscription_plans
    ADD CONSTRAINT subscription_plans_pkey PRIMARY KEY (id);


--
-- Name: tenant_quota_cycles tenant_quota_cycles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenant_quota_cycles
    ADD CONSTRAINT tenant_quota_cycles_pkey PRIMARY KEY (id);


--
-- Name: tenants tenants_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_email_key UNIQUE (email);


--
-- Name: tenants tenants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (tenant_id);


--
-- Name: password_reset_tokens uq_password_reset_tokens_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash);


--
-- Name: refresh_tokens uq_refresh_tokens_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash);


--
-- Name: tenant_quota_cycles uq_tenant_cycle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenant_quota_cycles
    ADD CONSTRAINT uq_tenant_cycle UNIQUE (tenant_id, cycle_start);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: whatsapp_configs whatsapp_configs_bot_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.whatsapp_configs
    ADD CONSTRAINT whatsapp_configs_bot_id_key UNIQUE (bot_id);


--
-- Name: whatsapp_configs whatsapp_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.whatsapp_configs
    ADD CONSTRAINT whatsapp_configs_pkey PRIMARY KEY (id);


--
-- Name: idx_prt_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_prt_expires_at ON public.password_reset_tokens USING btree (expires_at);


--
-- Name: idx_prt_user_id_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_prt_user_id_created ON public.password_reset_tokens USING btree (user_id, created_at);


--
-- Name: idx_refresh_tokens_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_expires_at ON public.refresh_tokens USING btree (expires_at);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_tqc_cycle_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tqc_cycle_end ON public.tenant_quota_cycles USING btree (cycle_end);


--
-- Name: idx_tqc_tenant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tqc_tenant ON public.tenant_quota_cycles USING btree (tenant_id);


--
-- Name: users fk21hn1a5ja1tve7ae02fnn4cld; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk21hn1a5ja1tve7ae02fnn4cld FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id);


--
-- Name: permissions fk22lamcef3kykumk5rwjiv4x5u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT fk22lamcef3kykumk5rwjiv4x5u FOREIGN KEY (module_id) REFERENCES public.modules(module_id);


--
-- Name: password_reset_tokens fk_password_reset_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: refresh_tokens fk_refresh_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: bot_lead_assignees fkdjchxqm1edfs2iqjg85v8n4mf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bot_lead_assignees
    ADD CONSTRAINT fkdjchxqm1edfs2iqjg85v8n4mf FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: role_permissions fkegdk29eiy7mdtefy5c7eirr6e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(permission_id);


--
-- Name: person fkemsnreyk6g37uoja1ngeog5sp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT fkemsnreyk6g37uoja1ngeog5sp FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: user_roles fkh8ciramu9cc9q3qcqiv4ue8a6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- Name: user_roles fkhfh9dx7w3ubf1co1vdev94g3f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: role_permissions fkn5fotdgk8d1xvo8nav9uv3muc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- Name: whatsapp_configs fkodw8ftpvk6kd79ywpxmellawv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.whatsapp_configs
    ADD CONSTRAINT fkodw8ftpvk6kd79ywpxmellawv FOREIGN KEY (bot_id) REFERENCES public.bots(bot_id);


--
-- Name: bots fkqwdkf7wxmv49q8swk66tng76p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bots
    ADD CONSTRAINT fkqwdkf7wxmv49q8swk66tng76p FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id);


--
-- Name: bot_lead_assignees fkta77b0fbka0l5nh6h38rnkcmd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bot_lead_assignees
    ADD CONSTRAINT fkta77b0fbka0l5nh6h38rnkcmd FOREIGN KEY (bot_id) REFERENCES public.bots(bot_id);


--
-- Name: quota_session_days quota_session_days_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quota_session_days
    ADD CONSTRAINT quota_session_days_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id) ON DELETE CASCADE;


--
-- Name: tenant_quota_cycles tenant_quota_cycles_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenant_quota_cycles
    ADD CONSTRAINT tenant_quota_cycles_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.tenants(tenant_id) ON DELETE CASCADE;


--
-- Name: tenants tenants_subscription_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_subscription_plan_id_fkey FOREIGN KEY (subscription_plan_id) REFERENCES public.subscription_plans(id);


--
-- PostgreSQL database dump complete
--

