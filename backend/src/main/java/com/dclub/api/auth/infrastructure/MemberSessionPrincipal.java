package com.dclub.api.auth.infrastructure;

import java.io.Serializable;

/** 로그인 제공자와 무관하게 서버 세션에서 회원을 식별하는 최소 principal입니다. */
public record MemberSessionPrincipal(Long memberId) implements Serializable {}
