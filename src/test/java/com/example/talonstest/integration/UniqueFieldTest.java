package com.example.talonstest.integration;

import com.example.talonstest.fixture.entity.AccountDO;
import io.github.portaldalaran.talons.exception.TalonsUniqueException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** @UniqueField 单字段唯一、联合分组唯一、空值跳过与异常信息。 */
class UniqueFieldTest extends BaseIntegrationTest {

    @Test
    void saveRejectsDuplicateSingleField() {
        accountService.save(account(1L, "alice", "a@x.com"), false);

        TalonsUniqueException ex = assertThrows(TalonsUniqueException.class,
                () -> accountService.save(account(2L, "bob", "a@x.com"), false));

        assertEquals("邮箱已存在", ex.getMessage());
        assertEquals("a@x.com", ex.getValue());
        assertEquals(1L, count("t_account"));
    }

    @Test
    void saveRejectsDuplicateGroupedFields() {
        accountService.save(account(1L, "alice", "a@x.com"), false);

        TalonsUniqueException ex = assertThrows(TalonsUniqueException.class,
                () -> accountService.save(account(1L, "alice", "b@x.com"), false));

        assertEquals("租户内用户名重复", ex.getMessage());
        assertEquals(1L, count("t_account"));
    }

    @Test
    void sameUsernameInDifferentTenantIsAllowed() {
        accountService.save(account(1L, "alice", "a@x.com"), false);

        assertDoesNotThrow(() -> accountService.save(account(2L, "alice", "b@x.com"), false));

        assertEquals(2L, count("t_account"));
    }

    @Test
    void nullUniqueFieldsAreSkipped() {
        assertDoesNotThrow(() -> accountService.save(account(null, null, null), false));
        assertDoesNotThrow(() -> accountService.save(account(null, null, null), false));
        assertEquals(2L, count("t_account"));
    }

    @Test
    void updatingSelfDoesNotRaiseUniqueException() {
        AccountDO saved = account(1L, "alice", "a@x.com");
        accountService.save(saved, false);

        saved.setUsername("alice-改");
        assertDoesNotThrow(() -> accountService.updateById(saved, false));
        assertEquals(1L, count("t_account"));
    }

    @Test
    void checkFieldOnCollectionValidatesEveryElement() {
        accountService.save(account(1L, "alice", "a@x.com"), false);

        assertThrows(TalonsUniqueException.class,
                () -> accountService.checkField(Collections.singletonList(account(2L, "bob", "a@x.com"))));
        assertDoesNotThrow(() -> accountService.checkField(Collections.<AccountDO>emptyList()));
        assertDoesNotThrow(() -> accountService.checkField((java.util.Collection<AccountDO>) null));
    }
}
