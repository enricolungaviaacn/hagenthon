package com.hagenthon.auth.dto;

public record LoginResponse(String token, String email, String nome) {}
