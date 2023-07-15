package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entitiy.Member;
import com.querydsl.core.NonUniqueResultException;
import study.querydsl.entitiy.QMember;
import study.querydsl.entitiy.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entitiy.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;
    JPAQueryFactory query;

    @BeforeEach
    public void beforeEach() {
        query = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // find member1 by username
        Member result = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        Member result = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    public void select() {
        Member result = query
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        assertThat(result.getUsername()).isEqualTo("member1");

        List<Member> findMembers = query
                .selectFrom(member)
                .where(member.age.between(10, 30))
                .fetch();

        assertThat(findMembers.size()).isEqualTo(3);
    }

    @Test
    public void searchAndParam() {
        Member result1 = query
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetchOne();

        Member result2 = query
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(result1.getUsername()).isEqualTo(result2.getUsername());
    }

    @Test
    public void resultFetch() {
        // fetch
        List<Member> members = query
                .selectFrom(member)
                .fetch();
        assertThat(members.size()).isEqualTo(4);

        // fetchOne - 결과 값이 1개
        Member member1 = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(member1.getUsername()).isEqualTo("member1");

        // fetchOne - 결과 값이 2개 이상
        assertThrows(
                NonUniqueResultException.class, () -> {
                    query
                            .selectFrom(member)
                            .fetchOne();
                }
        );

        // fetchFirst
        Member memberFirst = query
                .selectFrom(member)
                .fetchFirst();
        System.out.println("get first member : " + memberFirst);
    }

    /**
     * 회원정렬순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member("member5", 100));
        em.persist(new Member("member8", 100));
        em.persist(new Member(null, 200));
        em.persist(new Member("member7", 200));

        List<Member> results = query
                .selectFrom(member)
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        for (Member result : results) {
            System.out.println(result);
        }

    }
}
