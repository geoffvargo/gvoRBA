package com.geoffvargo.gvorbabackend.repos;

import com.geoffvargo.gvorbabackend.models.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

import java.util.*;

@Repository
// Not wired up yet — no service or controller uses this repository.
public interface AuthHandoffCodeRepository extends JpaRepository<AuthHandoffCode, Long> {
	Optional<AuthHandoffCode> findByCodeHash(String codeHash);
}
