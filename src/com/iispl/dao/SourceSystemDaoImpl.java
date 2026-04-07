package com.iispl.dao;

import com.iispl.entity.SourceSystem;
import com.iispl.enums.SourceType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SourceSystemDaoImpl implements SourceSystemDao {

    private final Connection connection;

    public SourceSystemDaoImpl(Connection connection) {
        this.connection = connection;
    }

    // SAVE
    @Override
    public long save(SourceSystem sourceSystem) {

        String sql = "INSERT INTO source_system (system_code, file_path) VALUES (?, ?)";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sourceSystem.getSourceType().name());
            ps.setString(2, sourceSystem.getFilePath());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                long id = rs.getLong(1);
                sourceSystem.setSourceSystemId(id);
                return id;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error saving SourceSystem", e);
        }

        return 0;
    }

    // FIND BY ID
    @Override
    public SourceSystem findById(Long id) {

        String sql = "SELECT id, system_code, file_path FROM source_system WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding SourceSystem by id", e);
        }

        return null;
    }

    // FIND ALL
    @Override
    public List<SourceSystem> findAll() {

        String sql = "SELECT id, system_code, file_path FROM source_system";

        List<SourceSystem> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching SourceSystems", e);
        }

        return list;
    }

    // FIND BY SOURCE TYPE
    @Override
    public List<SourceSystem> findBySourceType(SourceType sourceType) {

        String sql = "SELECT id, system_code, file_path FROM source_system WHERE system_code=?";

        List<SourceSystem> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, sourceType.name());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding SourceSystem by type", e);
        }

        return list;
    }

    // FIND ACTIVE (since entity has no active flag, return all)
    @Override
    public List<SourceSystem> findAllActive() {
        return findAll();
    }

    // MAP RESULTSET TO ENTITY
    private SourceSystem mapRow(ResultSet rs) throws SQLException {

        long id = rs.getLong("id");
        SourceType type = SourceType.valueOf(rs.getString("system_code"));
        String filePath = rs.getString("file_path");

        return new SourceSystem(id, type, filePath);
    }
}