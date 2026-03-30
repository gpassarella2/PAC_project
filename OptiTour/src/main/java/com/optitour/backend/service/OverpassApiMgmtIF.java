package com.optitour.backend.service;

import com.optitour.backend.model.Monument;

import java.util.List;

public interface OverpassApiMgmtIF {

    List<Monument> fetchMonumentsByCity(String city);
}