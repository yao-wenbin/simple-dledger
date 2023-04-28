package com.yaowenbinqwq.dledger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @Author yaowenbin
 * @Date 2023/4/28
 */


@ExtendWith(MockitoExtension.class)
class DLedgerLeaderElectorTest {
    @Spy
    DLedgerLeaderElector elector;

    @Test
    void test() {
        assertDoesNotThrow(() -> {});
    }

    @Test
    void takingLeaderShip() {

        elector.takingLeaderShip();

        verify(elector).startVote();

        elector.memberState().isCandidate();
    }

}
