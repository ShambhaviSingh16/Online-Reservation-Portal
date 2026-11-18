package com.trainticket.persistence.repository;

import com.trainticket.persistence.entity.StationEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<StationEntity, Integer>, JpaSpecificationExecutor<StationEntity> {
    List<StationEntity> findByActiveTrueOrderByIdAsc();
    Optional<StationEntity> findByIdAndActiveTrue(int id);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Integer id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StationEntity s where s.id = :id")
    Optional<StationEntity> lockById(@Param("id") int id);
}
