package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

import java.util.*;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
	Boolean existsByName(String name);
	Optional<Room> findByName(String name);
	Optional<Room> findByLocation(String location);
	boolean existsByLocation(String location);
}
