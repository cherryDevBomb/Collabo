package com.github.cherrydevbomb.collabo.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.SQLException;

public class DBLogger {

    public static DBLogger dbLogger;
    private static NamedParameterJdbcTemplate jdbcTemplate;

    private static String query = "INSERT INTO %s (elementId, userId, timestamp) VALUES (:elementId, :userId, :timestamp);";

    public static DBLogger getInstance() {
        if (dbLogger == null) {
            dbLogger = new DBLogger();
        }
        return dbLogger;
    }

    private DBLogger() {
        var dataSource = new SimpleDriverDataSource();
        try {
            dataSource.setDriver(new com.mysql.jdbc.Driver());
            dataSource.setUrl("jdbc:mysql://localhost:3306/collabodb");
            dataSource.setUsername("root");
            dataSource.setPassword("toor");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void log(Table table, String elementId, String userId, long timestamp) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("elementId", elementId);
        params.addValue("userId", userId);
        params.addValue("timestamp", timestamp);

        jdbcTemplate.update(String.format(query, table.getTableName()), params);
    }
}
