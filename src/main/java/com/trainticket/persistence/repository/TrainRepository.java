package com.trainticket.persistence.repository;

import com.trainticket.persistence.entity.TrainEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TrainRepository extends JpaRepository<TrainEntity, Integer>, JpaSpecificationExecutor<TrainEntity> {
    List<TrainEntity> findByActiveTrueOrderByIdAsc();
    boolean existsByNumber(String number);
    boolean existsByNumberAndIdNot(String number, Integer id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TrainEntity t where t.id = :id")
    Optional<TrainEntity> lockById(@Param("id") int id);
}
