package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {


    AdminUser findFirstByUserNameAndUserPassword(String name , String password );
}
