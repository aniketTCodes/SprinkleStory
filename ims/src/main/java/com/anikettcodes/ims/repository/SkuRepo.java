package com.anikettcodes.ims.repository;

import com.anikettcodes.ims.entity.Sku;
import com.anikettcodes.ims.util.enums.SkuStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkuRepo extends JpaRepository<Sku, UUID> {

    @Query(value = "SELECT CAST(nextval('sku_product_code_seq') AS bigint)", nativeQuery = true)
    Long nextProductCode();

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, UUID id);

    @Query("""
            SELECT s FROM Sku s
            JOIN FETCH s.category
            JOIN FETCH s.unit
            WHERE s.id = :id
            """)
    Optional<Sku> findByIdWithRelations(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Sku s
            JOIN FETCH s.category
            JOIN FETCH s.unit
            WHERE (CAST(:q AS string) IS NULL OR CAST(:q AS string) = ''
                OR LOWER(s.productCode) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR LOWER(s.displayName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR (s.barcode IS NOT NULL AND LOWER(s.barcode) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
            AND (:status IS NULL OR s.status = :status)
            AND (:categoryId IS NULL OR s.category.id = :categoryId)
            ORDER BY s.displayName ASC
            """)
    List<Sku> search(
            @Param("q") String q,
            @Param("status") SkuStatus status,
            @Param("categoryId") UUID categoryId
    );
}
