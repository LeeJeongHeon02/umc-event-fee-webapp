package com.dclub.api.repository;

import com.dclub.api.domain.Member;
import com.dclub.api.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    long countByStatus(MemberStatus status);
}
