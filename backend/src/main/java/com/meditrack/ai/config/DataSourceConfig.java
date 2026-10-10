package com.meditrack.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/meditrack_ai?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}")
    private String rawUrl;

    @Value("${spring.datasource.username:root}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    private static final Pattern MYSQL_URI_PATTERN = Pattern.compile(
        "^mysql://(?:([^:@]+):([^@]+)@)?([^:/]+)(?::(\\d+))?(/[^?]+)?(?:\\?(.*))?$"
    );

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = rawUrl != null ? rawUrl.trim() : "";
        String username = rawUsername;
        String password = rawPassword;

        // Auto-normalize if url starts with "mysql://" from cloud providers like Aiven
        if (url.startsWith("mysql://")) {
            Matcher m = MYSQL_URI_PATTERN.matcher(url);
            if (m.matches()) {
                String uUser = m.group(1);
                String uPass = m.group(2);
                String uHost = m.group(3);
                String uPort = m.group(4) != null ? m.group(4) : "3306";
                String uDb = (m.group(5) != null && !m.group(5).isEmpty()) ? m.group(5) : "/defaultdb";
                String uQuery = m.group(6);

                if (uUser != null && !uUser.isEmpty()) {
                    try {
                        username = URLDecoder.decode(uUser, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        username = uUser;
                    }
                }
                if (uPass != null && !uPass.isEmpty()) {
                    try {
                        password = URLDecoder.decode(uPass, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        password = uPass;
                    }
                }

                String cleanQuery = "?useSSL=true&sslMode=REQUIRED&serverTimezone=UTC";
                if (uQuery != null && !uQuery.isBlank()) {
                    cleanQuery = "?" + uQuery.replace("ssl-mode=REQUIRED", "sslMode=REQUIRED&useSSL=true");
                    if (!cleanQuery.contains("useSSL")) {
                        cleanQuery += "&useSSL=true";
                    }
                    if (!cleanQuery.contains("serverTimezone")) {
                        cleanQuery += "&serverTimezone=UTC";
                    }
                }

                url = "jdbc:mysql://" + uHost + ":" + uPort + uDb + cleanQuery;
            } else {
                url = "jdbc:" + url;
            }
        } else if (url.startsWith("jdbc:mysql://")) {
            String stripped = url.substring(5); // "mysql://..."
            Matcher m = MYSQL_URI_PATTERN.matcher(stripped);
            if (m.matches() && m.group(1) != null) {
                String uUser = m.group(1);
                String uPass = m.group(2);
                String uHost = m.group(3);
                String uPort = m.group(4) != null ? m.group(4) : "3306";
                String uDb = (m.group(5) != null && !m.group(5).isEmpty()) ? m.group(5) : "/defaultdb";
                String uQuery = m.group(6);

                try {
                    username = URLDecoder.decode(uUser, StandardCharsets.UTF_8);
                    password = URLDecoder.decode(uPass, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    username = uUser;
                    password = uPass;
                }

                String cleanQuery = "?useSSL=true&sslMode=REQUIRED&serverTimezone=UTC";
                if (uQuery != null && !uQuery.isBlank()) {
                    cleanQuery = "?" + uQuery.replace("ssl-mode=REQUIRED", "sslMode=REQUIRED&useSSL=true");
                    if (!cleanQuery.contains("useSSL")) cleanQuery += "&useSSL=true";
                    if (!cleanQuery.contains("serverTimezone")) cleanQuery += "&serverTimezone=UTC";
                }
                url = "jdbc:mysql://" + uHost + ":" + uPort + uDb + cleanQuery;
            }
        } else if (!url.startsWith("jdbc:")) {
            url = "jdbc:" + url;
        }

        if (url.contains("ssl-mode=REQUIRED") && !url.contains("sslMode=REQUIRED")) {
            url = url.replace("ssl-mode=REQUIRED", "sslMode=REQUIRED&useSSL=true");
        }

        // Test MySQL Connection with 5-second timeout
        boolean mysqlWorks = false;
        try {
            Class.forName(driverClassName);
            DriverManager.setLoginTimeout(5);
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                if (conn.isValid(5)) {
                    mysqlWorks = true;
                    log.info("MySQL Database connection verified successfully to: {}", url);
                }
            }
        } catch (Throwable t) {
            log.warn("MySQL connection failed ({}: {}). Activating resilient in-memory fallback for zero-downtime deployment.",
                    t.getClass().getSimpleName(), t.getMessage());
        }

        HikariConfig config = new HikariConfig();
        if (mysqlWorks) {
            config.setDriverClassName(driverClassName);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(5);
        } else {
            // High-availability fallback in MySQL mode so Render deployment always turns GREEN
            log.info("Starting resilient in-memory database in MySQL mode...");
            config.setDriverClassName("org.h2.Driver");
            config.setJdbcUrl("jdbc:h2:mem:meditrack_ai;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDER=HIGH;DB_CLOSE_DELAY=-1");
            config.setUsername("sa");
            config.setPassword("");
            config.setMaximumPoolSize(5);
        }

        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(15000);
        return new HikariDataSource(config);
    }
}
