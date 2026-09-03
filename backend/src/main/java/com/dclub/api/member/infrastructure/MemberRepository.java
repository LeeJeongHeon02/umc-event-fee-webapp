package com.dclub.api.member.infrastructure;

import com.dclub.api.member.domain.Member;
import com.dclub.api.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByKakaoId(String kakaoId);
    List<Member> findAllByOrderByCreatedAtDesc();
    List<Member> findAllByStatusOrderByCreatedAtAsc(MemberStatus status);
    long countByStatus(MemberStatus status);
}
