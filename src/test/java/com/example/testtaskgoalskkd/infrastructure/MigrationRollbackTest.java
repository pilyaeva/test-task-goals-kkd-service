package com.example.testtaskgoalskkd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Условие приёмки миграций: накат -> откат -> повторный накат без ошибок.
 * Выполняется на реальном PostgreSQL 18 (Testcontainers) вместе со стартовыми данными. HTTP не используется.
 */
@Testcontainers
class MigrationRollbackTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Test
    void migrateRollbackMigrateAgainWithoutErrors() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);

            // накат
            liquibase.update(new Contexts(), new LabelExpression());
            int changeSets = count(connection, "databasechangelog");
            assertThat(changeSets).isGreaterThan(0);
            assertSeedLoaded(connection);

            // откат всех changeSet'ов: остаются только служебные таблицы Liquibase
            liquibase.rollback(changeSets, null, new Contexts(), new LabelExpression());
            assertThat(count(connection, "databasechangelog")).isZero();
            assertThat(applicationTables(connection)).isEmpty();

            // повторный накат
            liquibase.update(new Contexts(), new LabelExpression());
            assertThat(count(connection, "databasechangelog")).isEqualTo(changeSets);
            assertSeedLoaded(connection);
        }
    }

    @Test
    void seedHasViolationsOfEveryRuleAndTwoRunsToCompare() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = connection.createStatement()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)
                    .update(new Contexts(), new LabelExpression());

            try (ResultSet rs = st.executeQuery("""
                    SELECT COUNT(*) FROM rules r
                    WHERE NOT EXISTS (SELECT 1 FROM run_problems p WHERE p.rule_id = r.id)""")) {
                rs.next();
                assertThat(rs.getInt(1)).as("правила без нарушений").isZero();
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM goals")) {
                rs.next();
                assertThat(rs.getInt(1)).isBetween(20, 30);
            }
        }
    }

    private static void assertSeedLoaded(Connection connection) throws SQLException {
        assertThat(count(connection, "goals")).isBetween(20, 30);
        assertThat(count(connection, "runs")).isEqualTo(2);
        assertThat(count(connection, "rules")).isEqualTo(4);
        assertThat(count(connection, "run_problems")).isPositive();
    }

    private static int count(Connection connection, String table) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static java.util.List<String> applicationTables(Connection connection) throws SQLException {
        java.util.List<String> tables = new java.util.ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT table_name FROM information_schema.tables
                     WHERE table_schema = 'public' AND table_name NOT LIKE 'databasechangelog%'""")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }
}
