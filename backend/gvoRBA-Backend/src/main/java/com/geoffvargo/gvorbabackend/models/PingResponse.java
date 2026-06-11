package com.geoffvargo.gvorbabackend.models;

import lombok.*;

@Data
@AllArgsConstructor
public class PingResponse {
	private String status;
	private String timestamp;
}
