package com.bhartiyasaas.stayfile.config;

import com.bhartiyasaas.stayfile.security.SecurityUser;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Configuration
public class RlsDataSourceConfiguration implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !(bean instanceof RlsDelegatingDataSource)) {
            return new RlsDelegatingDataSource(dataSource);
        }
        return bean;
    }

    public static class RlsDelegatingDataSource extends DelegatingDataSource {

        public RlsDelegatingDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            applyRlsContext(connection);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = super.getConnection(username, password);
            applyRlsContext(connection);
            return connection;
        }

        private void applyRlsContext(Connection connection) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String orgIdStr = "";
                if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
                    UUID orgId = securityUser.getOrganizationId();
                    if (orgId != null) {
                        orgIdStr = orgId.toString();
                    }
                }
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SELECT set_config('app.current_organization_id', '" + orgIdStr + "', false)");
                }
            } catch (Exception ignored) {
                // Ignore errors for uninitialized or non-PostgreSQL datasources (e.g. H2 in tests)
            }
        }
    }
}
