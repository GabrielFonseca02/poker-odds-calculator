package com.GabrielFonseca.pokeroddscalculator.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(int status, String message, List<String> details, LocalDateTime timestamp) {
}