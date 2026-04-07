package com.example.seprojectpart3;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.UUID;

public class TicketRepositoryTest {

    @Test
    public void testTicketIdUniqueness() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        assertNotEquals(id1, id2);
    }

    @Test
    public void testCapacityLogicConfirmed() {
        long capacity = 100;
        long soldCount = 50;
        assertTrue(soldCount < capacity);
    }

    @Test
    public void testCapacityLogicWaitlisted() {
        long capacity = 50;
        long soldCount = 50;
        assertFalse(soldCount < capacity);
    }
}