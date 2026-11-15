package com.example.talonstest.unit;

import com.example.talonstest.fixture.mapper.AddressMapper;
import com.example.talonstest.fixture.mapper.UserMapper;
import io.github.portaldalaran.talons.exception.TalonsException;
import io.github.portaldalaran.talons.utils.TalonsUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 命名推断、Mapper 猜测与 selectMaps 行取值工具。 */
class TalonsUtilsTest {

    @Test
    void guessReferencedColumnNameStripsDoAndPoSuffix() {
        assertEquals("userId", TalonsUtils.guessReferencedColumnName("UserDO"));
        assertEquals("orderId", TalonsUtils.guessReferencedColumnName("OrderPO"));
        assertEquals("userId", TalonsUtils.guessReferencedColumnName("userDO"));
        assertEquals("articleId", TalonsUtils.guessReferencedColumnName("Article"));
    }

    @Test
    void guessMapperClassNameGeneratesBothNamingConventions() {
        assertEquals(Arrays.asList("UserMapper", "IUserMapper"),
                TalonsUtils.guessMapperClassName("User"));
        assertEquals(Arrays.asList("UserMapper", "IUserMapper"),
                TalonsUtils.guessMapperClassName("UserDO"));
        assertEquals(Arrays.asList("IUserMapper", "UserMapper"),
                TalonsUtils.guessMapperClassName("IUserMapper"));
        assertEquals(Arrays.asList("OrderMapper", "IOrderMapper"),
                TalonsUtils.guessMapperClassName("OrderPO"));
    }

    @Test
    void guessMapperClassNameSkipsBlankNames() {
        assertTrue(TalonsUtils.guessMapperClassName("", "  ").isEmpty());
        assertTrue(TalonsUtils.guessMapperClassName(new String[]{null}).isEmpty());
    }

    @Test
    void guessMapperFindsRegisteredMapperOrThrows() {
        List<Class<?>> mappers = Arrays.asList(UserMapper.class, AddressMapper.class);

        assertEquals(UserMapper.class, TalonsUtils.guessMapper(mappers, "User"));
        assertEquals(UserMapper.class, TalonsUtils.guessMapper(mappers, "UserMapper"));
        assertEquals(AddressMapper.class, TalonsUtils.guessMapper(mappers, "Address"));

        assertThrows(TalonsException.class, () -> TalonsUtils.guessMapper(mappers, "Missing"));
        assertThrows(TalonsException.class,
                () -> TalonsUtils.guessMapper(Collections.<Class<?>>emptyList(), "User"));
    }

    @Test
    void getMapValueHandlesUppercaseDatabaseColumnLabels() {
        Map<String, Object> row = new HashMap<>();
        row.put("ROLE_ID", 7L);

        assertEquals(7L, TalonsUtils.getMapValue(row, "roleId", "role_id"));
        assertEquals(7L, TalonsUtils.getMapValue(row, "role_id", "roleId"));
        assertNull(TalonsUtils.getMapValue(row, "unknown"));
    }

    @Test
    void getMapValuePrefersExactPropertyName() {
        Map<String, Object> row = new HashMap<>();
        row.put("roleId", 9L);
        row.put("ROLE_ID", 7L);

        assertEquals(9L, TalonsUtils.getMapValue(row, "roleId", "role_id"));
    }

    @Test
    void getMapValueToleratesNullInputs() {
        assertNull(TalonsUtils.getMapValue(null, "roleId"));
        assertNull(TalonsUtils.getMapValue(new HashMap<>(), (String[]) null));
        assertNull(TalonsUtils.getMapValue(new HashMap<>()));
    }

    @Test
    void newCompatibleCollectionFollowsDeclaredFieldType() {
        assertEquals(HashSet.class, TalonsUtils.newCompatibleCollection(Set.class, null).getClass());
        assertEquals(HashSet.class, TalonsUtils.newCompatibleCollection(HashSet.class, List.of("a")).getClass());

        Collection<?> linked = TalonsUtils.newCompatibleCollection(LinkedHashSet.class, List.of("a", "b"));
        assertEquals(LinkedHashSet.class, linked.getClass());
        assertEquals(2, linked.size());

        assertEquals(ArrayList.class, TalonsUtils.newCompatibleCollection(List.class, null).getClass());
        assertEquals(ArrayList.class, TalonsUtils.newCompatibleCollection(null, null).getClass());
    }
}
