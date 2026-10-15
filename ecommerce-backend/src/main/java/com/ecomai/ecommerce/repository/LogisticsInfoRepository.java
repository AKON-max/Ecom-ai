package com.ecomai.ecommerce.repository;

import com.ecomai.ecommerce.entity.LogisticsInfo;
import com.ecomai.ecommerce.entity.OrderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsInfoRepository extends JpaRepository<LogisticsInfo, Long> {

    Optional<LogisticsInfo> findByOrderEntity(OrderEntity orderEntity);
}
