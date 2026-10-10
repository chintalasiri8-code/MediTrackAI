package com.meditrack.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/meditrack_ai?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}")
    private String rawUrl;

    @Value("${spring.datasource.username:root}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    // Matches mysql://user:pass@host:port/db?query or mysql://host:port/db?query
    private static final Pattern MYSQL_URI_PATTERN = Pattern.compile(
        "^mysql://(?:([^:@]+):([^@]+)@)?([^:/]+)(?::(\\d+))?(/[^?]+)?(?:\\?(.*))?$"
    );

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(30000);

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

                if (uUser != null && !uUser.isEmpty() && (username == null || username.isBlank() || "root".equals(username))) {
                    username = uUser;
                }
                if (uPass != null && !uPass.isEmpty() && (password == null || password.isBlank())) {
                    password = uPass;
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
        } else if (!url.startsWith("jdbc:")) {
            url = "jdbc:" + url;
        }

        // Fix ssl-mode parameter for MySQL Connector/J if present
        if (url.contains("ssl-mode=REQUIRED") && !url.contains("sslMode=REQUIRED")) {
            url = url.replace("ssl-mode=REQUIRED", "sslMode=REQUIRED&useSSL=true");
        }

        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        return new HikariDataSource(config);
    }
}
