package com.dodo.backend.routepoint.repository;

import com.dodo.backend.routepoint.entity.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
}
