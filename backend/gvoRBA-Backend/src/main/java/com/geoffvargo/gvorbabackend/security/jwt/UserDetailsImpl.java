package com.geoffvargo.gvorbabackend.security.jwt;

import com.geoffvargo.gvorbabackend.models.User;

import net.minidev.json.annotate.*;

import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.userdetails.*;

import java.io.*;
import java.util.*;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
	@Serial
	private static final long serialVersionUID = 1L;
	
	@Getter
	private Long id;
	
	@Getter
	private String username;
	
	@Getter
	private String email;
	
	@Getter
	@JsonIgnore
	private String password;
	
	@Getter
	private Collection<? extends GrantedAuthority> authorities;
	
	public UserDetailsImpl(Long id, String name, String email, String password,
	                       List<? extends GrantedAuthority> authority) {
		this.id = id;
		this.username = name;
		this.email = email;
		this.password = password;
		this.authorities = authority;
	}
	
	public static UserDetailsImpl build(User user) {
		GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getRoleName().name());
		return new UserDetailsImpl(
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.getPassword(),
			List.of(authority)
		);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UserDetailsImpl user = (UserDetailsImpl) o;
		return Objects.equals(id, user.id);
	}
	
}
