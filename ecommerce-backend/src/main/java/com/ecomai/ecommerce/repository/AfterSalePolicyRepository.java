package com.ecomai.ecommerce.repository;

import com.ecomai.ecommerce.entity.AfterSalePolicy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AfterSalePolicyRepository extends JpaRepository<AfterSalePolicy, Long> {

    List<AfterSalePolicy> findBySceneKeyContainingIgnoreCase(String sceneKey);
}
