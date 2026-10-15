package com.ecomai.ecommerce.repository;

import com.ecomai.ecommerce.entity.LogisticsEvent;
import com.ecomai.ecommerce.entity.LogisticsInfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsEventRepository extends JpaRepository<LogisticsEvent, Long> {

    List<LogisticsEvent> findByLogisticsInfoOrderByOccurredAtDesc(LogisticsInfo logisticsInfo);
}
