package com.hookflow.hookflow.common.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UuidTypeHandlerTest {

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private CallableStatement callableStatement;

    private UuidTypeHandler typeHandler;
    private final UUID sampleUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        typeHandler = new UuidTypeHandler();
    }

    @Test
    void testSetNonNullParameter() throws SQLException {
        typeHandler.setNonNullParameter(preparedStatement, 1, sampleUuid, JdbcType.OTHER);
        verify(preparedStatement).setObject(1, sampleUuid, Types.OTHER);
    }

    @Test
    void testGetNullableResultFromColumnNameWithUuidObject() throws SQLException {
        when(resultSet.getObject("id")).thenReturn(sampleUuid);
        UUID result = typeHandler.getNullableResult(resultSet, "id");
        assertEquals(sampleUuid, result);
    }

    @Test
    void testGetNullableResultFromColumnNameWithString() throws SQLException {
        when(resultSet.getObject("id")).thenReturn(sampleUuid.toString());
        UUID result = typeHandler.getNullableResult(resultSet, "id");
        assertEquals(sampleUuid, result);
    }

    @Test
    void testGetNullableResultFromColumnNameNull() throws SQLException {
        when(resultSet.getObject("id")).thenReturn(null);
        UUID result = typeHandler.getNullableResult(resultSet, "id");
        assertNull(result);
    }

    @Test
    void testGetNullableResultFromColumnIndex() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(sampleUuid);
        UUID result = typeHandler.getNullableResult(resultSet, 1);
        assertEquals(sampleUuid, result);
    }

    @Test
    void testGetNullableResultFromCallableStatement() throws SQLException {
        when(callableStatement.getObject(1)).thenReturn(sampleUuid);
        UUID result = typeHandler.getNullableResult(callableStatement, 1);
        assertEquals(sampleUuid, result);
    }
}

