package com.geoffvargo.gvorbabackend.controllers;

import com.geoffvargo.gvorbabackend.models.*;
import com.geoffvargo.gvorbabackend.models.DTOs.*;
import com.geoffvargo.gvorbabackend.repos.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserDirectoryController}. These call the controller directly, so they check how it
 * queries {@link UserRepository} and maps users to {@link UserSummaryDto}s. The {@code enabled} filter itself
 * lives in the repository query and the endpoint's access rules live in {@code SecurityConfig}; neither is
 * exercised here.
 */
@ExtendWith(MockitoExtension.class)
class UserDirectoryControllerTest {

	/// Stand-in for the real repository: no database, and each test decides what it returns.
	@Mock private UserRepository userRepository;

	/// Built by Mockito through the Lombok-generated constructor, with the mock above passed in.
	@InjectMocks private UserDirectoryController controller;

	/**
	 * Tests for {@code GET /api/directory/users}.
	 */
	@Nested
	class ListUsers {

		/**
		 * Each enabled user comes back as a summary with only its id and name, in the order the repository
		 * returned them.
		 */
		@Test
		void mapsEnabledUsersToSummariesInRepositoryOrder() {
			/// Two users the mock repository will return. Only the fields the DTO reads are set.
			User pat = User.builder().id(1L).name("Pat").enabled(true).build();
			User sam = User.builder().id(2L).name("Sam").enabled(true).build();
			when(userRepository.findAllByEnabledTrue()).thenReturn(List.of(pat, sam));

			ResponseEntity<List<UserSummaryDto>> resp = controller.listUsers();

			assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
			/// Records compare by value, so the expected DTOs can be built inline. containsExactly also checks
			/// order, which would catch an accidental sort or a switch to a Set.
			assertThat(resp.getBody()).containsExactly(
				new UserSummaryDto(1L, "Pat"),
				new UserSummaryDto(2L, "Sam")
			);
		}

		/**
		 * With no enabled users the endpoint still answers {@code 200} with an empty list, so the frontend can
		 * loop over the body without checking for {@code null}.
		 */
		@Test
		void noEnabledUsers_returnsEmptyListNotNull() {
			when(userRepository.findAllByEnabledTrue()).thenReturn(List.of());

			ResponseEntity<List<UserSummaryDto>> resp = controller.listUsers();

			assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(resp.getBody()).isNotNull().isEmpty();
		}

		/**
		 * The controller reads users only through the enabled-only query, so disabled accounts can't reach the
		 * directory through some other repository method.
		 */
		@Test
		void onlyQueriesEnabledUsers() {
			/// The return value doesn't matter here; the test only cares which repository methods get called.
			when(userRepository.findAllByEnabledTrue()).thenReturn(List.of());

			controller.listUsers();

			/// The enabled-only query must be the one and only repository call. verifyNoMoreInteractions fails
			/// if a later change adds findAll(), findById() or any other method that could return disabled users.
			verify(userRepository).findAllByEnabledTrue();
			verifyNoMoreInteractions(userRepository);
		}
	}
}
