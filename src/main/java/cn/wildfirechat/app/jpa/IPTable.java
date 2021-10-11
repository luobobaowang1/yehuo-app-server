package cn.wildfirechat.app.jpa;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "t_ip_table", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ip"})
})
public class IPTable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;


    public String ip;


    public String memo;
}
