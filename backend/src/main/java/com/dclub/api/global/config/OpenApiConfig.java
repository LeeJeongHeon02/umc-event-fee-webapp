package com.dclub.api.global.config;

import com.dclub.api.global.common.ProblemResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OpenAPI 문서의 공통 정보와 실제 구현된 엔드포인트의 운영 규칙을 정의한다. */
@Configuration
public class OpenApiConfig {
    private static final String SESSION_AUTH = "kakaoSession";
    private static final Map<String, EndpointDoc> ENDPOINTS = endpointDocs();

    @Bean
    OpenAPI clubEventFeeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("동아리 행사·회비 관리 API")
                        .version("v1")
                        .description("""
                                카카오 OAuth 또는 로컬 계정 로그인 세션 기반의 행사 참가, 회비·참가비 신고, 운영진 정산 API입니다.

                                **Swagger에서 상태 변경 요청을 시험하려면** 같은 브라우저에서 로그인한 뒤
                                `GET /auth/csrf`를 먼저 호출해 CSRF 토큰 쿠키를 발급받으세요.
                                이후 Swagger UI는 `XSRF-TOKEN` 쿠키를 `X-XSRF-TOKEN` 헤더로 전송합니다.

                                금액은 원 단위 정수이며, 날짜·시간은 ISO-8601 UTC 시각입니다.
                                `version` 필드는 낙관적 잠금용입니다. 최신 응답의 version을 상태 변경 요청에 그대로 전달하세요.

                                **공통 오류 응답**은 `application/problem+json`이며 다음 필드를 항상 포함합니다.
                                - `status`: HTTP 상태 코드
                                - `code`: 프론트엔드 분기 처리용 안정적인 오류 코드
                                - `detail`: 사용자에게 표시 가능한 설명
                                - `instance`: 오류가 발생한 API 경로
                                - `fieldErrors`: 입력 필드별 검증 오류

                                대표 오류 코드는 `AUTHENTICATION_REQUIRED`, `CSRF_TOKEN_INVALID`, `FORBIDDEN`,
                                `VALIDATION_FAILED`, `RESOURCE_NOT_FOUND`, `CONCURRENT_UPDATE`, `INTERNAL_SERVER_ERROR`입니다.
                                """)
                        .contact(new Contact().name("UMC Club Operations"))
                        .license(new License().name("Private club use")))
                .components(apiComponents())
                .addSecurityItem(new SecurityRequirement().addList(SESSION_AUTH))
                .tags(List.of(
                        new Tag().name("Authentication").description("카카오 OAuth·로컬 계정 로그인과 CSRF 토큰"),
                        new Tag().name("My profile").description("내 회원 정보와 온보딩"),
                        new Tag().name("Events").description("동아리원 행사 조회·참가·취소"),
                        new Tag().name("Payments").description("내 행사비·회비 조회와 송금 신고"),
                        new Tag().name("Notifications").description("내 알림 조회와 읽음 처리"),
                        new Tag().name("Admin overview").description("운영진 대시보드와 회원 관리"),
                        new Tag().name("Admin events").description("운영진 행사 생성과 상태 관리"),
                        new Tag().name("Admin dues").description("회비 차수 생성·공개·대상자 부과"),
                        new Tag().name("Admin payments").description("참가자·납부 현황, 신고 검토, 환불 완료 기록"),
                        new Tag().name("Admin payment settings").description("총무 계좌·카카오페이 코드송금 링크 관리")));
    }

    @Bean
    OpenApiCustomizer endpointDocumentationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().forEach((path, item) -> {
                customize("GET " + path, item.getGet());
                customize("POST " + path, item.getPost());
                customize("PATCH " + path, item.getPatch());
                customize("PUT " + path, item.getPut());
                customize("DELETE " + path, item.getDelete());
            });
            // Springdoc merges controller schemas after the base OpenAPI bean is created. Register this alias
            // at the final customization stage as well so all #/components/schemas/Problem references resolve.
            registerProblemSchemas(openApi.getComponents());
        };
    }

    private void customize(String key, Operation operation) {
        if (operation == null) return;
        EndpointDoc doc = ENDPOINTS.get(key);
        if (doc == null) return;

        operation.setOperationId(doc.operationId());
        operation.setTags(List.of(doc.tag()));
        operation.setSummary(doc.summary());
        operation.setDescription(doc.description());
        if (key.contains(" /auth/") && !key.equals("POST /auth/logout")) operation.setSecurity(List.of());
        response(operation, doc.successCode(), doc.successDescription());
        response(operation, "401", "`AUTHENTICATION_REQUIRED`: 로그인 세션이 없거나 만료되었습니다. 로컬 로그인은 `INVALID_CREDENTIALS`도 반환할 수 있습니다.");
        response(operation, "403", "`FORBIDDEN`: 권한이 없거나 `CSRF_TOKEN_INVALID`: 보안 토큰이 없거나 만료되었습니다.");
        for (String errorCode : doc.errorCodes()) {
            response(operation, errorCode, errorDescription(errorCode));
        }
        response(operation, "500", "`INTERNAL_SERVER_ERROR`: 예상하지 못한 서버 오류입니다. 상세 내부 오류는 응답에 노출하지 않습니다.");
    }

    private void response(Operation operation, String status, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        ApiResponse response = responses.get(status);
        if (response == null) {
            response = new ApiResponse();
            responses.addApiResponse(status, response);
        }
        response.setDescription(description);
        if (status.startsWith("4") || status.startsWith("5")) {
            response.setContent(problemContent(status));
        }
    }

    private Components apiComponents() {
        Components components = new Components().addSecuritySchemes(SESSION_AUTH,
                new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("카카오 또는 로컬 로그인 완료 후 브라우저에 자동 저장되는 서버 세션 쿠키입니다. 직접 입력하지 않습니다."));
        // ModelConverters uses the Java class name by default. Publish an explicit "Problem" alias so that
        // every error response has a stable schema reference even if the implementation class is renamed.
        registerProblemSchemas(components);
        return components;
    }

    private void registerProblemSchemas(Components components) {
        Map<String, Schema> problemSchemas = ModelConverters.getInstance().read(ProblemResponse.class);
        problemSchemas.forEach(components::addSchemas);
        Schema<?> problemSchema = problemSchemas.getOrDefault("Problem", problemSchemas.get("ProblemResponse"));
        if (problemSchema != null) {
            describeProblemSchema(problemSchema);
            components.addSchemas("Problem", problemSchema);
        }
    }

    private void describeProblemSchema(Schema<?> problemSchema) {
        problemSchema.setDescription("API 공통 오류 응답. Content-Type은 application/problem+json입니다.");
        problemSchema.setRequired(List.of(
                "type", "title", "status", "code", "detail", "instance", "timestamp", "fieldErrors"));
        describeProperty(problemSchema, "type", "오류 유형 URI", "about:blank");
        describeProperty(problemSchema, "title", "HTTP 상태 이름", "Bad Request");
        describeProperty(problemSchema, "status", "HTTP 상태 코드", 400);
        describeProperty(problemSchema, "code", "프론트엔드 분기 처리용 안정적인 오류 코드", "VALIDATION_FAILED");
        describeProperty(problemSchema, "detail", "사용자에게 표시할 수 있는 오류 설명", "요청 값을 확인해 주세요.");
        describeProperty(problemSchema, "instance", "오류가 발생한 요청 경로", "/api/v1/me/onboarding");
        // DateTimeSchema examples are serialized differently by springdoc; the complete response example below
        // already demonstrates the timestamp value, so the property only needs its precise description here.
        describeProperty(problemSchema, "timestamp", "오류 발생 시각(UTC)", null);
        describeProperty(problemSchema, "fieldErrors", "필드 단위 검증 오류. 검증 오류가 아니면 빈 배열입니다.", List.of());
    }

    private void describeProperty(Schema<?> schema, String name, String description, Object example) {
        if (schema.getProperties() == null || schema.getProperties().get(name) == null) return;
        Schema<?> property = schema.getProperties().get(name);
        property.setDescription(description);
        if (example != null) property.setExample(example);
    }

    private Content problemContent(String status) {
        String code = switch (status) {
            case "400" -> "VALIDATION_FAILED";
            case "401" -> "AUTHENTICATION_REQUIRED";
            case "403" -> "FORBIDDEN";
            case "404" -> "RESOURCE_NOT_FOUND";
            case "409" -> "CONCURRENT_UPDATE";
            default -> "INTERNAL_SERVER_ERROR";
        };
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("type", "about:blank");
        value.put("title", statusTitle(status));
        value.put("status", Integer.parseInt(status));
        value.put("code", code);
        value.put("detail", errorDetail(status));
        value.put("instance", "/api/v1/example");
        value.put("timestamp", "2026-09-03T09:30:00Z");
        value.put("fieldErrors", List.of());

        io.swagger.v3.oas.models.media.MediaType mediaType =
                new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/Problem"))
                        .addExamples("example", new Example().summary(code).value(value));
        return new Content().addMediaType("application/problem+json", mediaType);
    }

    private String statusTitle(String status) {
        return switch (status) {
            case "400" -> "Bad Request";
            case "401" -> "Unauthorized";
            case "403" -> "Forbidden";
            case "404" -> "Not Found";
            case "405" -> "Method Not Allowed";
            case "409" -> "Conflict";
            default -> "Internal Server Error";
        };
    }

    private String errorDetail(String status) {
        return switch (status) {
            case "400" -> "요청 값을 확인해 주세요.";
            case "401" -> "카카오 로그인이 필요하거나 로그인 세션이 만료되었습니다.";
            case "403" -> "요청을 수행할 권한이 없습니다.";
            case "404" -> "요청한 리소스를 찾을 수 없습니다.";
            case "409" -> "다른 요청으로 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.";
            default -> "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
        };
    }

    private String errorDescription(String status) {
        return switch (status) {
            case "400" -> "`VALIDATION_FAILED`, `MALFORMED_JSON`, `INVALID_REQUEST`: 요청 본문·경로·쿼리 값과 fieldErrors를 확인하세요.";
            case "404" -> "`RESOURCE_NOT_FOUND`: 대상 리소스가 없거나 현재 사용자에게 노출되지 않습니다.";
            case "409" -> "도메인 상태 충돌 또는 `CONCURRENT_UPDATE`: 최신 상세와 version을 다시 조회하세요.";
            default -> "요청을 처리할 수 없습니다.";
        };
    }

    private static Map<String, EndpointDoc> endpointDocs() {
        Map<String, EndpointDoc> docs = new LinkedHashMap<>();
        add(docs, "GET", "/auth/csrf", "getCsrfToken", "Authentication", "CSRF 토큰 조회",
                "로그인 세션의 상태 변경 요청에 사용할 CSRF 토큰과 헤더 이름을 반환합니다. Swagger에서 POST·PATCH·DELETE 호출 전 먼저 실행하세요.", "200", "CSRF 토큰과 헤더·파라미터 이름을 반환합니다.");
        add(docs, "POST", "/auth/logout", "logoutMember", "Authentication", "현재 웹앱 세션 로그아웃",
                "카카오·로컬 로그인 공통. 로그인 세션과 CSRF 토큰을 무효화하고 쿠키를 삭제합니다. POST와 CSRF 헤더가 필요하며 응답 본문은 없습니다. 카카오 계정 연결은 유지됩니다.", "204", "로그아웃 완료", "401", "403");
        add(docs, "POST", "/auth/local/register", "registerLocalMember", "Authentication", "로컬 계정 회원가입",
                "영문 소문자 기반 아이디, 8~72자 비밀번호, 전화번호로 계정을 생성합니다. 비밀번호는 BCrypt 해시만 저장하며 가입 직후에는 로그인되지 않습니다.", "201", "생성된 회원 ID와 로그인 아이디를 반환합니다.", "400", "409");
        add(docs, "POST", "/auth/local/login", "loginLocalMember", "Authentication", "로컬 계정 로그인",
                "아이디와 비밀번호를 검증해 서버 세션을 생성합니다. 신규 회원은 `/onboarding`, 온보딩 후 승인 대기는 `/pending`, 활성 회원은 `/home` 경로를 반환합니다.", "200", "현재 회원 정보와 다음 화면 경로를 반환합니다.", "400");
        add(docs, "GET", "/me", "getMyProfile", "My profile", "내 회원 정보 조회",
                "로그인한 회원의 역할, 승인 상태, 파트와 최종 닉네임을 반환합니다.", "200", "현재 회원 정보를 반환합니다.");
        add(docs, "PATCH", "/me/onboarding", "completeOnboarding", "My profile", "온보딩 완료",
                "이름과 파트를 저장합니다. 최종 닉네임은 서버가 `파트명 + 이름`으로 생성합니다.", "200", "갱신된 회원 정보를 반환합니다.", "400", "409");
        add(docs, "PATCH", "/me/profile", "updateMyProfile", "My profile", "내 프로필 수정",
                "승인된 회원의 이름과 파트를 수정하고 최종 닉네임을 다시 생성합니다.", "200", "갱신된 회원 정보를 반환합니다.", "400", "409");

        add(docs, "GET", "/events", "listPublishedEvents", "Events", "게시 행사 목록 조회",
                "승인된 회원에게 공개된 행사와 나의 참가·납부 상태를 반환합니다.", "200", "행사 목록 페이지를 반환합니다.");
        add(docs, "GET", "/events/{eventId}", "getEvent", "Events", "행사 상세 조회",
                "행사 상세, 신청 가능 여부, 나의 참가와 납부 요약을 반환합니다.", "200", "행사 상세를 반환합니다.", "404");
        add(docs, "POST", "/events/{eventId}/participation", "joinEvent", "Events", "행사 참가 신청",
                "행사가 게시 상태이고 신청 기한·정원이 유효할 때 참가 기록과 행사비 납부 항목을 생성합니다. 무료 행사는 `NOT_REQUIRED` 상태가 됩니다.", "201", "참가 기록과 납부 항목을 생성했습니다.", "404", "409");
        add(docs, "POST", "/events/{eventId}/participation/cancel", "cancelParticipation", "Events", "행사 참가 취소",
                "참가를 취소합니다. 미납 건은 `VOID`, 납부 확인 건은 `REFUND_PENDING`으로 전환될 수 있습니다.", "200", "참가·납부 상태와 환불 필요 여부를 반환합니다.", "400", "404", "409");

        add(docs, "GET", "/me/payment-obligations", "listMyPaymentObligations", "Payments", "내 납부 항목 목록 조회",
                "행사 참가비와 회비 납부 항목을 기한순으로 반환합니다.", "200", "납부 항목 목록 페이지를 반환합니다.");
        add(docs, "GET", "/payment-obligations/{paymentId}", "getMyPaymentObligation", "Payments", "내 납부 항목 상세 조회",
                "본인 납부 항목의 송금 계좌·카카오페이 코드송금 링크, 최신 신고와 상태 이력을 반환합니다.", "200", "납부 항목 상세를 반환합니다.", "404");
        add(docs, "POST", "/payment-obligations/{paymentId}/reports", "reportPayment", "Payments", "송금 완료 신고",
                "실제 이체 후 신고를 남깁니다. 이 요청은 금융기관 송금을 실행하지 않으며, 상태를 `REPORTED`(운영진 확인 대기)로 바꿉니다.", "201", "송금 신고를 생성하고 확인 대기 상태를 반환합니다.", "400", "404", "409");

        add(docs, "GET", "/notifications", "listNotifications", "Notifications", "내 알림 목록 조회",
                "현재 회원의 알림과 읽지 않은 알림 수를 반환합니다.", "200", "알림 목록을 반환합니다.");
        add(docs, "POST", "/notifications/{notificationId}/read", "markNotificationRead", "Notifications", "알림 읽음 처리",
                "본인 알림 한 건을 읽음 처리합니다.", "200", "읽음 처리된 알림을 반환합니다.", "404");
        add(docs, "POST", "/notifications/read-all", "markAllNotificationsRead", "Notifications", "전체 알림 읽음 처리",
                "현재 회원의 모든 알림을 읽음 처리합니다.", "204", "응답 본문 없이 완료되었습니다.");

        add(docs, "GET", "/admin/payment-reports", "getAdminPaymentReports", "Admin payments", "확인 대기 송금 신고 통합 조회",
                "STAFF/ADMIN 전용. 행사 참가비와 회비 중 REPORTED 상태 전체를 최신 신고 순으로 반환합니다. source에는 실제 행사 또는 회비 차수의 ID와 제목이 포함됩니다. 회비 차수가 없어도 행사 신고를 조회할 수 있으며 신고가 없으면 빈 배열을 반환합니다.", "200", "확인 대기 송금 목록");
        add(docs, "GET", "/admin/dashboard", "getAdminDashboard", "Admin overview", "운영진 대시보드 조회",
                "활성 회원 수, 미납·신고 건수, 예상·확정 금액과 최근 신고를 반환합니다. STAFF 이상이 필요합니다.", "200", "운영진 대시보드를 반환합니다.");
        add(docs, "GET", "/admin/members", "listMembers", "Admin overview", "회원 목록 조회",
                "회원의 온보딩·승인·역할 상태를 반환합니다. STAFF 이상이 필요합니다.", "200", "회원 목록을 반환합니다.");
        add(docs, "POST", "/admin/members/{memberId}/approve", "approveMember", "Admin overview", "회원 가입 승인",
                "PENDING 회원을 ACTIVE로 승인합니다. ADMIN만 실행할 수 있습니다.", "200", "승인된 회원 정보를 반환합니다.", "400", "404", "409");
        add(docs, "POST", "/admin/members/{memberId}/suspend", "suspendMember", "Admin overview", "회원 활동 정지",
                "ACTIVE 회원을 SUSPENDED로 변경합니다. 자신의 계정은 정지할 수 없고 ADMIN만 실행할 수 있습니다.", "200", "정지된 회원 정보를 반환합니다.", "400", "404", "409");
        add(docs, "PATCH", "/admin/members/{memberId}/role", "changeMemberRole", "Admin overview", "회원 역할 변경",
                "ACTIVE 회원의 MEMBER·STAFF·ADMIN 역할을 변경합니다. 자신의 역할은 변경할 수 없고 ADMIN만 실행할 수 있습니다.", "200", "변경된 회원 정보를 반환합니다.", "400", "404", "409");

        add(docs, "GET", "/admin/events", "listAdminEvents", "Admin events", "운영진 행사 목록 조회",
                "초안을 포함한 모든 행사를 반환합니다. STAFF 이상이 필요합니다.", "200", "운영진 행사 목록을 반환합니다.");
        add(docs, "POST", "/admin/events", "createEvent", "Admin events", "행사 초안 생성",
                "새 행사를 DRAFT 상태로 생성합니다. STAFF 이상이 필요합니다.", "201", "생성된 행사 초안을 반환합니다.", "400");
        add(docs, "GET", "/admin/events/{eventId}", "getAdminEvent", "Admin events", "운영진 행사 상세 조회",
                "행사의 내부 상태와 version을 포함한 상세를 반환합니다. STAFF 이상이 필요합니다.", "200", "행사 상세를 반환합니다.", "404");
        add(docs, "PATCH", "/admin/events/{eventId}", "updateEventDraft", "Admin events", "행사 초안 수정",
                "DRAFT 행사만 수정할 수 있습니다. 최신 version을 함께 보내야 합니다.", "200", "수정된 행사 초안을 반환합니다.", "400", "404", "409");
        add(docs, "POST", "/admin/events/{eventId}/publish", "publishEvent", "Admin events", "행사 게시",
                "DRAFT 행사를 PUBLISHED로 전환하고 활성 회원에게 알림을 생성합니다.", "200", "게시된 행사 정보를 반환합니다.", "400", "404", "409");
        add(docs, "POST", "/admin/events/{eventId}/close", "closeEvent", "Admin events", "행사 마감",
                "PUBLISHED 행사를 CLOSED로 전환합니다.", "200", "마감된 행사 정보를 반환합니다.", "400", "404", "409");
        add(docs, "POST", "/admin/events/{eventId}/cancel", "cancelEvent", "Admin events", "행사 취소",
                "행사를 CANCELED로 전환합니다. 미납 건은 VOID, 입금 확인 건은 REFUND_PENDING으로 처리합니다.", "200", "무효·환불 대기 건수를 포함한 취소 결과를 반환합니다.", "400", "404", "409");
        add(docs, "DELETE", "/admin/events/{eventId}", "deleteEventDraft", "Admin events", "행사 초안 삭제",
                "참가 이력이 없는 DRAFT 행사만 삭제합니다. version 쿼리 파라미터가 필요합니다.", "204", "응답 본문 없이 삭제되었습니다.", "400", "404", "409");

        add(docs, "GET", "/admin/dues-rounds", "listDuesRounds", "Admin dues", "회비 차수 목록 조회",
                "초안과 게시된 회비 차수, 대상자 수를 반환합니다. STAFF 이상이 필요합니다.", "200", "회비 차수 목록을 반환합니다.");
        add(docs, "POST", "/admin/dues-rounds", "createDuesRound", "Admin dues", "회비 차수 초안 생성",
                "회비 금액·기한·송금정보를 포함한 DRAFT 회비 차수를 생성합니다.", "201", "생성된 회비 차수를 반환합니다.", "400");
        add(docs, "PATCH", "/admin/dues-rounds/{duesRoundId}", "updateDuesRound", "Admin dues", "회비 차수 초안 수정",
                "DRAFT 회비 차수만 수정할 수 있으며 최신 version이 필요합니다.", "200", "수정된 회비 차수를 반환합니다.", "400", "404", "409");
        add(docs, "POST", "/admin/dues-rounds/{duesRoundId}/publish", "publishDuesRound", "Admin dues", "회비 차수 게시·부과",
                "활성 회원 전원에게 회비 납부 항목을 생성하고 알림을 보냅니다.", "200", "게시된 회비 차수와 생성된 납부 건수를 반환합니다.", "400", "404", "409");
        add(docs, "DELETE", "/admin/dues-rounds/{duesRoundId}", "deleteDuesRound", "Admin dues", "회비 차수 삭제",
                "납부 항목이 아직 생성되지 않은 DRAFT 회비 차수만 삭제합니다. version 쿼리 파라미터가 필요합니다.", "204", "응답 본문 없이 삭제되었습니다.", "400", "404", "409");

        add(docs, "GET", "/admin/events/{eventId}/participants", "listEventParticipants", "Admin payments", "행사별 참가자·납부 현황",
                "참가 회원의 파트, 참가 시각, 납부 상태와 최근 송금 신고를 반환합니다. STAFF 이상이 필요합니다.", "200", "행사 요약과 참가자 목록을 반환합니다.", "404");
        add(docs, "GET", "/admin/dues-rounds/{duesRoundId}/payments", "listDuesPayments", "Admin payments", "회비 차수별 납부 현황",
                "회비 차수의 회원별 납부 상태와 최근 신고를 반환합니다. STAFF 이상이 필요합니다.", "200", "회비 차수 요약과 납부 목록을 반환합니다.", "404");
        add(docs, "POST", "/admin/payment-obligations/{paymentId}/confirm", "confirmPayment", "Admin payments", "송금 신고 승인",
                "REPORTED 납부 항목을 CONFIRMED로 전환합니다. 실제 계좌 입금 확인 후 실행하세요.", "200", "검토된 납부 상태를 반환합니다.", "400", "404", "409");
        add(docs, "POST", "/admin/payment-obligations/{paymentId}/reject", "rejectPayment", "Admin payments", "송금 신고 반려",
                "REPORTED 납부 항목을 REJECTED로 전환합니다. 회원은 이후 다시 신고할 수 있습니다.", "200", "검토된 납부 상태를 반환합니다.", "400", "404", "409");
        add(docs, "GET", "/admin/refunds", "listPendingRefunds", "Admin payments", "환불 대기 목록 조회",
                "REFUND_PENDING 상태인 납부 항목을 반환합니다. 이 API는 실제 환불을 실행하지 않습니다.", "200", "환불 대기 납부 목록을 반환합니다.");
        add(docs, "POST", "/admin/payment-obligations/{paymentId}/refund", "completeRefund", "Admin payments", "환불 완료 기록",
                "운영진이 실제 환불을 끝낸 뒤 REFUND_PENDING 납부 항목을 REFUNDED로 기록합니다.", "200", "환불 완료 상태를 반환합니다.", "400", "404", "409");

        add(docs, "GET", "/admin/payment-settings/active", "getActivePaymentSetting", "Admin payment settings", "활성 송금정보 조회",
                "현재 활성화된 총무 계좌와 카카오페이 코드송금 링크를 반환합니다. ADMIN만 실행할 수 있습니다.", "200", "활성 송금정보를 반환합니다.", "404");
        add(docs, "GET", "/admin/payment-settings", "listPaymentSettings", "Admin payment settings", "송금정보 목록 조회",
                "현재·과거 송금정보 버전을 최신순으로 반환합니다. ADMIN만 실행할 수 있습니다.", "200", "송금정보 목록을 반환합니다.");
        add(docs, "POST", "/admin/payment-settings", "createPaymentSetting", "Admin payment settings", "송금정보 등록·활성화",
                "새 총무 계좌와 카카오페이 코드송금 링크를 등록하고 기존 활성 설정을 비활성화합니다. ADMIN만 실행할 수 있습니다.", "201", "새 활성 송금정보를 반환합니다.", "400");
        return Map.copyOf(docs);
    }

    private static void add(Map<String, EndpointDoc> docs, String method, String path, String operationId,
                            String tag, String summary, String description, String successCode,
                            String successDescription, String... errorCodes) {
        docs.put(method + " " + path, new EndpointDoc(operationId, tag, summary, description,
                successCode, successDescription, List.of(errorCodes)));
    }

    private record EndpointDoc(String operationId, String tag, String summary, String description,
                               String successCode, String successDescription, List<String> errorCodes) {}
}
