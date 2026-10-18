package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

@Repository
public interface UserPrefsRepository extends JpaRepository<UserPrefs, Long> {
	UserPrefs findByUserId(Long id);
}
