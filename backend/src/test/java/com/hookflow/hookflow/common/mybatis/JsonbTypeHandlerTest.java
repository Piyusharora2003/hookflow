package com.hookflow.hookflow.common.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonbTypeHandlerTest {

    @Mock
    private PreparedStatement ps;

    @Mock
    private ResultSet rs;

    private JsonbTypeHandler mapHandler;
    private JsonbListTypeHandler listHandler;

    @BeforeEach
    void setUp() {
        mapHandler = new JsonbTypeHandler();
        listHandler = new JsonbListTypeHandler();
    }

    @Test
    void testMapHandlerSetNonNullParameter() throws SQLException {
        Map<String, Object> map = Map.of("foo", "bar");
        mapHandler.setNonNullParameter(ps, 1, map, JdbcType.OTHER);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture(), eq(Types.OTHER));

        assertEquals("{\"foo\":\"bar\"}", captor.getValue());
    }

    @Test
    void testMapHandlerGetNullableResult() throws SQLException {
        when(rs.getString("payload")).thenReturn("{\"foo\":\"bar\"}");

        Map<String, Object> result = mapHandler.getNullableResult(rs, "payload");
        assertEquals("bar", result.get("foo"));
    }

    @Test
    void testMapHandlerGetNullableResultNull() throws SQLException {
        when(rs.getString("payload")).thenReturn(null);

        Map<String, Object> result = mapHandler.getNullableResult(rs, "payload");
        assertNull(result);
    }

    @Test
    void testListHandlerSetNonNullParameter() throws SQLException {
        List<String> list = List.of("tag1", "tag2");
        listHandler.setNonNullParameter(ps, 2, list, JdbcType.OTHER);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(2), captor.capture(), eq(Types.OTHER));

        assertEquals("[\"tag1\",\"tag2\"]", captor.getValue());
    }

    @Test
    void testListHandlerGetNullableResult() throws SQLException {
        when(rs.getString("tags")).thenReturn("[\"tag1\",\"tag2\"]");

        List<String> result = listHandler.getNullableResult(rs, "tags");
        assertEquals(2, result.size());
        assertEquals("tag1", result.get(0));
        assertEquals("tag2", result.get(1));
    }
}
