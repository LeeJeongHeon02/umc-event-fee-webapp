package com.dclub.api.member.infrastructure;

import com.dclub.api.member.domain.Member;
import com.dclub.api.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByKakaoId(String kakaoId);
    long countByStatus(MemberStatus status);
}
