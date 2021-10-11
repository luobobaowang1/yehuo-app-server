package cn.wildfirechat.app.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource()
public interface ExtUserRepository extends JpaRepository<ExtUser, Long> {

    ExtUser getFirstByUserName(String userName);

    List<ExtUser> findAllByLoginIp(String ip);

    ExtUser getFirstByUserNameAndUserPassword(String userName,String password);


    Page<ExtUser> findAllByUserNameLikeOrderByIdDesc(String userName, Pageable pageRequest);
}
