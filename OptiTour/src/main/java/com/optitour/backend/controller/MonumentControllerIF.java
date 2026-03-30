package com.optitour.backend.controller;

import com.optitour.backend.dto.MonumentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface MonumentControllerIF {

    ResponseEntity<List<MonumentResponse>> getMonumentsByCity(@RequestParam String city);

    ResponseEntity<MonumentResponse> getMonumentById(@PathVariable String id);
}