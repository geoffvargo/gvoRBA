package com.geoffvargo.gvorbabackend.security;

import org.apache.commons.codec.binary.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.encrypt.*;
import org.springframework.stereotype.*;

import lombok.extern.slf4j.*;

/**
 * Encrypts TOTP secrets at rest. They cannot be hashed like passwords: the server needs the raw secret
 * back to compute codes. A database dump without the encryption password is then useless for TOTP.
 */
@Slf4j
@Component
public class SecretCipher {
	private final BytesEncryptor encryptor;
	
	public SecretCipher(@Value("${app.mfa.ecryption-password}") String password,
	                    @Value("${app.mfa.ecryption-salt}") String hexSalt) {
		this.encryptor = AesGcmBytesEncryptor.withPassword(password, hexSalt)
			                 .build();
	}
	
	public String encrypt(byte[] plaintext) {
		return Base64.encodeBase64String(encryptor.encrypt(plaintext));
	}
	
	public byte[] decrypt(String stored) {
		return encryptor.decrypt(Base64.decodeBase64(stored));
	}
}
