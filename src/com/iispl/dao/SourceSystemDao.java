package com.iispl.dao;

import com.iispl.entity.SourceSystem;
import com.iispl.enums.SourceType;

import java.util.List;

public interface SourceSystemDao {

    // Persist a new source system record; returns the generated DB id
    long save(SourceSystem sourceSystem);

    // Fetch a single source system by its primary key; returns null if not found
    SourceSystem findById(Long id);

    // Fetch all source systems in the system
    List<SourceSystem> findAll();

    // Fetch all source systems matching a given source type (CBS, RTGS, SWIFT, etc.)
    List<SourceSystem> findBySourceType(SourceType sourceType);

    // Fetch only active source systems
    List<SourceSystem> findAllActive();
}