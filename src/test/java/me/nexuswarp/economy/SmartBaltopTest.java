package me.nexuswarp.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SmartBaltopTest {

    @Test
    public void testIncrementalBaltopRanking() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        EconomyManager.BalanceTopEntry e1 = new EconomyManager.BalanceTopEntry(u1, "Notch", new BigDecimal("1000"));
        EconomyManager.BalanceTopEntry e2 = new EconomyManager.BalanceTopEntry(u2, "Alex", new BigDecimal("500"));
        EconomyManager.BalanceTopEntry e3 = new EconomyManager.BalanceTopEntry(u3, "Steve", new BigDecimal("100"));

        List<EconomyManager.BalanceTopEntry> list = new ArrayList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.sort(EconomyManager.BALTOP_COMPARATOR);

        // Verify initial order
        assertEquals("Notch", list.get(0).getName());
        assertEquals("Alex", list.get(1).getName());
        assertEquals("Steve", list.get(2).getName());

        // Incremental update: Alex earns $1000 (total $1500)
        // 1. Remove old entry
        int oldIdx = Collections.binarySearch(list, e2, EconomyManager.BALTOP_COMPARATOR);
        assertTrue(oldIdx >= 0);
        list.remove(oldIdx);

        // 2. Insert new entry
        EconomyManager.BalanceTopEntry e2New = new EconomyManager.BalanceTopEntry(u2, "Alex", new BigDecimal("1500"));
        int newIdx = Collections.binarySearch(list, e2New, EconomyManager.BALTOP_COMPARATOR);
        if (newIdx < 0) newIdx = -newIdx - 1;
        list.add(newIdx, e2New);

        // Alex should now be #1
        assertEquals(0, newIdx);
        assertEquals("Alex", list.get(0).getName());
        assertEquals(new BigDecimal("1500"), list.get(0).getBalance());
        assertEquals("Notch", list.get(1).getName());
        assertEquals("Steve", list.get(2).getName());

        // Incremental update: Notch loses $950 (drops to $50)
        int notchOld = Collections.binarySearch(list, e1, EconomyManager.BALTOP_COMPARATOR);
        assertTrue(notchOld >= 0);
        list.remove(notchOld);

        EconomyManager.BalanceTopEntry e1New = new EconomyManager.BalanceTopEntry(u1, "Notch", new BigDecimal("50"));
        int notchNew = Collections.binarySearch(list, e1New, EconomyManager.BALTOP_COMPARATOR);
        if (notchNew < 0) notchNew = -notchNew - 1;
        list.add(notchNew, e1New);

        // Order should now be: Alex ($1500) -> Steve ($100) -> Notch ($50)
        assertEquals("Alex", list.get(0).getName());
        assertEquals("Steve", list.get(1).getName());
        assertEquals("Notch", list.get(2).getName());
    }
}
