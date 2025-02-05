package org.bithub.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    UserInfo getUserInfoByUserId(String userId);
}
