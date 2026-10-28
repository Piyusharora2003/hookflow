package com.hookflow.hookflow;

import com.hookflow.hookflow.auth.port.ApiKeyLookupPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false"
})
class HookflowApplicationTests {

    @MockitoBean
    private ApiKeyLookupPort apiKeyLookupPort;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DataSource dataSource() throws SQLException {
            DataSource ds = Mockito.mock(DataSource.class);
            Connection conn = Mockito.mock(Connection.class);
            DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
            Mockito.when(ds.getConnection()).thenReturn(conn);
            Mockito.when(conn.getMetaData()).thenReturn(metaData);
            Mockito.when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            return ds;
        }
    }

    @Test
    void contextLoads() {
    }

}
