package study.querydsl;

import com.querydsl.core.QueryFactory;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entitiy.Member;
import com.querydsl.core.NonUniqueResultException;
import study.querydsl.entitiy.Number;
import study.querydsl.entitiy.QMember;
import study.querydsl.entitiy.QTeam;
import study.querydsl.entitiy.Team;

import java.util.List;
import java.util.Optional;

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

    /**
     * 회원을 모두 조회한다. 그리고 team 이름이 "teamA" 인 team 만 조인한다.
     */
    @Test
    public void join_on() {
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }

    }
    /**
     * member 의 이름과 team의 이름이 같은 member를 모두 조회하기
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    public void select_one() {
        List<Member> result = query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Member member1 : result) {
            System.out.println(member1);
            System.out.println(member1.getTeam());
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetch_join() {
        em.flush();
        em.clear();

        Member member1 = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());

        assertThat(loaded).as("패치 조인 미적용").isFalse();

        em.flush();
        em.clear();

        Member fetchJoinMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded_fetch = emf.getPersistenceUnitUtil().isLoaded(fetchJoinMember.getTeam());

        assertThat(loaded_fetch).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    public void sub_query() {
        QMember memberSub = new QMember("sub");
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }
    /**
     * 나이가 평균 초과인 회원 조회
     */
    @Test
    public void sub_query2() {
        QMember memberSub = new QMember("sub");
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.gt(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("sub");
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.gt(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                                .where(memberSub.age.gt(20))
                ))
                .fetch();

        Double avgGt20 = query
                .select(member.age.avg())
                .from(member)
                .where(member.age.gt(20))
                .fetchOne();

        System.out.println(avgGt20);
        System.out.println(result);

        assertThat(result).extracting("age").containsExactly(40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("sub");
        List<Tuple> result = query
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    public void basicCase() {
        List<Tuple> result = query
                .select(member.username, member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }
    @Test
    public void complexCase() {
        List<Tuple> result = query
                .select(member.username, new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    public void concat() {
        //기대값 : {username}_{age}
        List<String> result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void getEnumValue() {
        Member memberEnum = new Member("member_enum");
        memberEnum.setNumber(Number.ONE);
        em.persist(memberEnum);

        // 초기화
        em.flush();
        em.clear();

        // 조회
        List<String> result = query
                .select(member.username.concat("_").concat(member.number.stringValue()))
                .from(member)
                .where(member.username.eq("member_enum"))
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void simple_projection() {
        List<String> result = query.select(member.username)
                .from(member)
                .fetch();
    }

    @Test
    public void tuple_projection() {
        List<Tuple> result = query.select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println(tuple.get(member.username));
            System.out.println(tuple.get(member.age));
        }
    }

//    @Test
//    public void findDtoByJPQL() {
//        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) " +
//                "from Member m", MemberDto.class).getResultList();
//
//        for (MemberDto memberDto : resultList) {
//            System.out.println(memberDto);
//        }
//    }
//
//    @Test
//    public void DtoQuerydslSetter() {
//        List<MemberDto> result = query
//                .select(Projections.bean(MemberDto.class,
//                        member.username,
//                        member.age))
//                .from(member)
//                .fetch();
//
//        for (MemberDto memberDto : result) {
//            System.out.println(memberDto);
//        }
//    }
//    @Test
//    public void DtoQuerydslField() {
//        List<MemberDto> result = query
//                .select(Projections.fields(MemberDto.class,
//                        member.username,
//                        member.age))
//                .from(member)
//                .fetch();
//
//        for (MemberDto memberDto : result) {
//            System.out.println(memberDto);
//        }
//    }
//    @Test
//    public void DtoQuerydslConstructor() {
//        List<MemberDto> result = query
//                .select(Projections.constructor(MemberDto.class,
//                        member.username,
//                        member.age))
//                .from(member)
//                .fetch();
//
//        for (MemberDto memberDto : result) {
//            System.out.println(memberDto);
//        }
//    }

    @Test
    public void findDtoByProjection() {
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }
}
