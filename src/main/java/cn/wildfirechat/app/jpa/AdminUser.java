package cn.wildfirechat.app.jpa;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "t_admin_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_name"})
})
public class AdminUser {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "user_name")
    public String userName;

    private Integer isSuper;

    public Integer status;

    public String userPassword;
}
