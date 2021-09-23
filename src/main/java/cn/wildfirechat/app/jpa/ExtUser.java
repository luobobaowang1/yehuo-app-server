package cn.wildfirechat.app.jpa;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "t_ext_user1",uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_name"})
})
public class ExtUser {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "user_name")
    public String userName;

    public String userPassword;

    public String mobile;

    public String code;//邀请码
}
