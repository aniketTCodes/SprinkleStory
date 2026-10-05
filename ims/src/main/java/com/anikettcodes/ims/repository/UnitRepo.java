package com.anikettcodes.ims.repository;

import com.anikettcodes.ims.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UnitRepo extends JpaRepository<Unit, UUID> {
}
