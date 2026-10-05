package com.anikettcodes.ims.repository;

import com.anikettcodes.ims.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryLotRepo extends JpaRepository<InventoryLot, UUID> {

    @Query("SELECT COALESCE(SUM(l.qty), 0) FROM InventoryLot l WHERE l.sku.id = :skuId")
    BigDecimal sumQtyBySkuId(@Param("skuId") UUID skuId);

    @Query("SELECT l.sku.id, COALESCE(SUM(l.qty), 0) FROM InventoryLot l WHERE l.sku.id IN :skuIds GROUP BY l.sku.id")
    List<Object[]> sumQtyBySkuIds(@Param("skuIds") Collection<UUID> skuIds);
}
