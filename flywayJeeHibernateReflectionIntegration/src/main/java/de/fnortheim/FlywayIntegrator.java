package de.fnortheim;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by sebastianbasner on 04.05.17.
 */
public class FlywayIntegrator implements Integrator {

    private final Logger logger = LoggerFactory.getLogger(FlywayIntegrator.class);

    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {

        logger.info("Migrating database to the latest version");

        final JdbcServices jdbcServices = serviceRegistry.getService(JdbcServices.class);
        Connection connection;
        DataSource dataSource = null;

        try {
            connection = jdbcServices.getBootstrapJdbcConnectionAccess().obtainConnection();
            final Method method = connection != null ? connection.getClass().getMethod("getDataSource", null) : null;
            dataSource = (DataSource) (method != null ? method.invoke(connection, null) : null);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | SQLException e) {
            e.printStackTrace();
        }

        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);

        MigrationInfo migrationInfo = flyway.info().current();

        if (migrationInfo == null) {
            logger.info("No existing database at the actual datasource");
        }
        else {
            logger.info("Found a database with the version: {}", migrationInfo.getVersion());
        }

        flyway.migrate();
        logger.info("Successfully migrated to database version: {}", flyway.info().current().getVersion());
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        // Not needed here
    }
}
