package com.geoffvargo.gvorbabackend.models.DTOs;

import java.util.*;

/** Service → controller result of confirm. IssuedSession is in this package, so it needs no import. */
public record ConfirmResult(
	List<String> recoveryCodes,
	IssuedSession session
) {}
