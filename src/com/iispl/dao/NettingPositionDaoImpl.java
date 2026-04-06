package com.iispl.dao;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.dao.NettingPositionDao;
import com.iispl.entity.NettingPosition;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NettingPositionDaoImpl implements NettingPositionDao {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @Override
    public void saveNettingPosition(NettingPosition nettingPosition) {
        String sql = "INSERT INTO netting_position "
                   + "(position_id, counterparty_bank_id, gross_debit_amount, gross_credit_amount, net_amount, position_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, nettingPosition.getPositiionId());
            ps.setLong(2, nettingPosition.getCounterpartyBankId());
            ps.setBigDecimal(3, nettingPosition.getGrossDebitAmount());
            ps.setBigDecimal(4, nettingPosition.getGrossCreditAmount());
            ps.setBigDecimal(5, nettingPosition.getNetAmount());
            ps.setObject(6, nettingPosition.getPositionDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save netting position [" + nettingPosition.getPositiionId() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteNettingPosition(long positionId) {
        String sql = "DELETE FROM netting_position WHERE position_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, positionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete netting position [" + positionId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Override
    public List<NettingPosition> findAllNettingPositions() {
        String sql = "SELECT position_id, counterparty_bank_id, gross_debit_amount, "
                   + "gross_credit_amount, net_amount, position_date FROM netting_position";
        List<NettingPosition> positions = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) positions.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all netting positions: " + e.getMessage(), e);
        }
        return positions;
    }

    @Override
    public NettingPosition findNettingPositionById(long positionId) {
        String sql = "SELECT position_id, counterparty_bank_id, gross_debit_amount, "
                   + "gross_credit_amount, net_amount, position_date FROM netting_position WHERE position_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, positionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find netting position [" + positionId + "]: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<NettingPosition> findPositionsByCounterpartyBank(long counterpartyBankId) {
        String sql = "SELECT position_id, counterparty_bank_id, gross_debit_amount, "
                   + "gross_credit_amount, net_amount, position_date FROM netting_position "
                   + "WHERE counterparty_bank_id = ?";
        List<NettingPosition> positions = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, counterpartyBankId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) positions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find positions by counterparty bank [" + counterpartyBankId + "]: " + e.getMessage(), e);
        }
        return positions;
    }

    @Override
    public List<NettingPosition> findPositionsByDateRange(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT position_id, counterparty_bank_id, gross_debit_amount, "
                   + "gross_credit_amount, net_amount, position_date FROM netting_position "
                   + "WHERE position_date BETWEEN ? AND ?";
        List<NettingPosition> positions = new ArrayList<>();
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) positions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find positions by date range: " + e.getMessage(), e);
        }
        return positions;
    }

    @Override
    public NettingPosition findPositionByCounterpartyAndDate(long counterpartyBankId, LocalDateTime positionDate) {
        String sql = "SELECT position_id, counterparty_bank_id, gross_debit_amount, "
                   + "gross_credit_amount, net_amount, position_date FROM netting_position "
                   + "WHERE counterparty_bank_id = ? AND position_date = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, counterpartyBankId);
            ps.setObject(2, positionDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find position by counterparty and date: " + e.getMessage(), e);
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Update operations
    // -----------------------------------------------------------------------

    @Override
    public void updateNettingAmounts(long positionId, BigDecimal grossDebit,
                                     BigDecimal grossCredit, BigDecimal netAmount) {
        String sql = "UPDATE netting_position "
                   + "SET gross_debit_amount = ?, gross_credit_amount = ?, net_amount = ? "
                   + "WHERE position_id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, grossDebit);
            ps.setBigDecimal(2, grossCredit);
            ps.setBigDecimal(3, netAmount);
            ps.setLong(4, positionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update netting amounts for position [" + positionId + "]: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Row mapper
    // -----------------------------------------------------------------------

    private NettingPosition mapRow(ResultSet rs) throws SQLException {
        return new NettingPosition(
            rs.getLong("position_id"),
            rs.getLong("counterparty_bank_id"),
            rs.getBigDecimal("gross_debit_amount"),
            rs.getBigDecimal("gross_credit_amount"),
            rs.getBigDecimal("net_amount"),
            rs.getObject("position_date", LocalDateTime.class)
        );
    }
}