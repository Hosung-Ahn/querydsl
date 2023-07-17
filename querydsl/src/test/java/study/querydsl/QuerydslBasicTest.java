package study.querydsl;

import com.querydsl.core.Tuple;
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
import study.querydsl.entitiy.QTeam;
import study.querydsl.entitiy.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entitiy.QMember.*;
import static study.querydsl.entitiy.QTeam.*;

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

    @Test
    public void paging1() {
        List<Member> members = query
                .selectFrom(member)
                .orderBy(member.username.asc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);

        for (Member member : members) {
            System.out.println(member);
        }



        em.persist(new Member("member0", 10));

        members = query
                .selectFrom(member)
                .orderBy(member.username.asc())
                .offset(1)
                .limit(2)
                .fetch();

        for (Member member : members) {
            System.out.println(member);
        }

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);


        System.out.println(tuple.get(member.count()));
        System.out.println(tuple.get(member.age.sum()));
        System.out.println(tuple.get(member.age.avg()));
        System.out.println(tuple.get(member.age.max()));
        System.out.println(tuple.get(member.age.min()));

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25.0);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * team 의 이름과 각 팀의 평균 연령을 구하라
     * @throws Exception
     */
    @Test
    public void groupByTest() throws Exception {
        List<Tuple> result = query
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A 에 소속된 모든 회원을 찾아라
     * @throws Exception
     */
    @Test
    public void joinTest() throws Exception {
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * member 의 이름과 team의 이름이 같은 member를 모두 조회하기
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // member 와 team 을 모두 join 하는 과정이 포함된다
        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
}
