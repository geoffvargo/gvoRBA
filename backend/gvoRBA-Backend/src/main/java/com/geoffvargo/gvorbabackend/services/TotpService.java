package com.geoffvargo.gvorbabackend.services;

import java.util.*;

public interface TotpService {
	public byte[] newSecret();
	public String toBase32(byte[] secret);
	public String otpAuthUri(String issuer, String accountName, byte[] secret);
	public OptionalLong matchStep(byte[] secret, String suppliedCode);
}
