package com.iispl.dao;

import com.iispl.entity.SourceSystem;
import com.iispl.enums.SourceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SourceSystemDaoImpl implements SourceSystemDao {

    private final Connection connection;

    public SourceSystemDaoImpl(Connection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    @Override
    public long save(SourceSystem sourceSystem) {
        String sql = "INSERT INTO source_system (system_code, file_path, is_active, created_at, updated_at) "
                   + "VALUES (?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sourceSystem.getSystemCode().name());
            ps.setString(2, sourceSystem.getFilePath());
            ps.setBoolean(3, sourceSystem.isActive());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    sourceSystem.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save SourceSystem systemCode="
                    + sourceSystem.getSystemCode() + ": " + e.getMessage(), e);
        }
        throw new RuntimeException("save SourceSystem: no generated key returned");
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    @Override
    public SourceSystem findById(Long id) {
        String sql = "SELECT id, system_code, file_path, is_active, created_at, updated_at "
                   + "FROM source_system WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SourceSystem id=" + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    // =========================================================================
    // FIND ALL
    // =========================================================================

    @Override
    public List<SourceSystem> findAll() {
        String sql = "SELECT id, system_code, file_path, is_active, created_at, updated_at "
                   + "FROM source_system";
        List<SourceSystem> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all SourceSystems: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND BY SOURCE TYPE
    // =========================================================================

    @Override
    public List<SourceSystem> findBySourceType(SourceType sourceType) {
        String sql = "SELECT id, system_code, file_path, is_active, created_at, updated_at "
                   + "FROM source_system WHERE system_code = ?";
        List<SourceSystem> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sourceType.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find SourceSystems by type=" + sourceType + ": " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // FIND ALL ACTIVE
    // =========================================================================

    @Override
    public List<SourceSystem> findAllActive() {
        String sql = "SELECT id, system_code, file_path, is_active, created_at, updated_at "
                   + "FROM source_system WHERE is_active = TRUE";
        List<SourceSystem> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active SourceSystems: " + e.getMessage(), e);
        }
        return list;
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    private SourceSystem mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new SourceSystem(
                rs.getLong("id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                SourceType.valueOf(rs.getString("system_code")),
                rs.getString("file_path"),
                rs.getBoolean("is_active")
        );
    }
}