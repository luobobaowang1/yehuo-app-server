package cn.wildfirechat.app.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource()
public interface IPTableRepository extends JpaRepository<IPTable, Long> {


    Page<IPTable> findAllByIpLikeOrderByIdDesc(String ip, Pageable pageable);


    IPTable findFirstByIp(String ip);
}
