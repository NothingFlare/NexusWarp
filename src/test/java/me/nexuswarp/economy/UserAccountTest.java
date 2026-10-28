package me.nexuswarp.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserAccountTest {

    @Test
    public void testUserAccountBasicProperties() {
        UUID uuid = UUID.randomUUID();
        UserAccount account = new UserAccount(uuid, "Steve", new BigDecimal("100.00"));

        assertEquals(uuid, account.getUuid());
        assertEquals("Steve", account.getLastAccountName());
        assertEquals(0, new BigDecimal("100.00").compareTo(account.getBalance()));
        assertTrue(account.isAcceptingPay());
        assertFalse(account.isPromptPayConfirm());
        assertFalse(account.isNpc());

        account.setBalance(new BigDecimal("250.50"));
        assertEquals(0, new BigDecimal("250.50").compareTo(account.getBalance()));

        account.setAcceptingPay(false);
        assertFalse(account.isAcceptingPay());

        account.setPromptPayConfirm(true);
        assertTrue(account.isPromptPayConfirm());

        account.setNpc(true);
        account.setNpcName("ShopKeeper");
        assertTrue(account.isNpc());
        assertEquals("ShopKeeper", account.getNpcName());
    }
}
