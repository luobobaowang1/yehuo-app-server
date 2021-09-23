package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource()
public interface ExtUserRepository extends CrudRepository<ExtUser, Long> {

    ExtUser getFirstByUserName(String userName);

    ExtUser getFirstByUserNameAndUserPassword(String userName,String password);
}
