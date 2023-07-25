package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
public class MemberDto {
    private String username;
    private int age;
    private String sail;

    public MemberDto(String username, int age, String sail) {
        this.username = username;
        this.age = age;
        this.sail = sail;
    }
}
