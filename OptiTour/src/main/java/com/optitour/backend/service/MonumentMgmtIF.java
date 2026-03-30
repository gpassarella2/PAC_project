package com.optitour.backend.service;

import com.optitour.backend.model.Monument;

import java.util.List;
import java.util.Optional;

public interface MonumentMgmtIF {

    List<Monument> getMonumentsByCity(String city);

    Optional<Monument> getMonumentById(String id);
}