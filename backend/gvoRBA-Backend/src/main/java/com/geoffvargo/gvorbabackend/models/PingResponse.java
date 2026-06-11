package com.geoffvargo.gvorbabackend.models;

import lombok.*;

@Data
@AllArgsConstructor
public class PingResponse {
	String status;
	String timestamp;
}
