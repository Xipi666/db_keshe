package pers.luoluo.databasekeshe.simulation.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pers.luoluo.databasekeshe.simulation.dto.SimulationStatusResponse;

@Service
public class SimulationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationService.class);

    private final JdbcTemplate jdbcTemplate;

    public SimulationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SimulationStatusResponse start() {
        callWithoutArguments("{call PKG_PSM_SIM.START_RUN}");
        return status();
    }

    public SimulationStatusResponse stop() {
        callWithoutArguments("{call PKG_PSM_SIM.STOP_RUN}");
        return status();
    }

    public SimulationStatusResponse setAnomalyEnabled(boolean enabled) {
        jdbcTemplate.execute((CallableStatementCreator) (Connection connection) -> {
            CallableStatement statement = connection.prepareCall("{call PKG_PSM_SIM.SET_ANOMALY(?)}");
            statement.setInt(1, enabled ? 1 : 0);
            return statement;
        }, (CallableStatementCallback<Void>) statement -> {
            statement.execute();
            return null;
        });
        return status();
    }

    public SimulationStatusResponse status() {
        return jdbcTemplate.execute((CallableStatementCreator) (Connection connection) -> {
            CallableStatement statement = connection.prepareCall("{call PKG_PSM_SIM.GET_STATUS(?)}");
            statement.registerOutParameter(1, Types.REF_CURSOR);
            return statement;
        }, (CallableStatementCallback<SimulationStatusResponse>) statement -> {
            statement.execute();
            try (ResultSet resultSet = (ResultSet) statement.getObject(1)) {
                if (resultSet == null || !resultSet.next()) {
                    throw new IllegalStateException("Database simulation status is unavailable.");
                }
                return mapStatus(resultSet);
            }
        });
    }

    @Scheduled(fixedRate = 100)
    public void writeTick() {
        try {
            callWithoutArguments("{call PKG_PSM_SIM.TICK}");
        } catch (DataAccessException exception) {
            LOGGER.error("Database simulation tick failed", exception);
        }
    }

    private void callWithoutArguments(String sql) {
        jdbcTemplate.execute((CallableStatementCreator) (Connection connection) -> {
            return connection.prepareCall(sql);
        }, (CallableStatementCallback<Void>) statement -> {
            statement.execute();
            return null;
        });
    }

    private SimulationStatusResponse mapStatus(ResultSet resultSet) throws SQLException {
        return new SimulationStatusResponse(
                resultSet.getInt("RUNNING") == 1,
                resultSet.getInt("ANOMALY_ENABLED") == 1,
                toLocalDateTime(resultSet.getTimestamp("STARTED_AT")),
                toLocalDateTime(resultSet.getTimestamp("LAST_WRITE_AT")),
                resultSet.getLong("WRITE_COUNT"),
                resultSet.getLong("ALARM_COUNT"),
                resultSet.getLong("TASK_COUNT"),
                resultSet.getInt("NORMAL_INTERVAL_MS"),
                resultSet.getInt("ANOMALY_INTERVAL_MS"),
                resultSet.getInt("CURRENT_INTERVAL_MS")
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
