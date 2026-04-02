package com.loopang.userservice.infrastructure.repository;

import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.repository.CourierRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCourierRepository extends JpaRepository<Courier, UUID>, CourierRepository {
}