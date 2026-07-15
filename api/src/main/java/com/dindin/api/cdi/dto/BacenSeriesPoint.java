package com.dindin.api.cdi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BacenSeriesPoint(
		@JsonProperty("data") String date,
		@JsonProperty("valor") String value) {
}
