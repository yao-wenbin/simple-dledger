package com.yaowenbinqwq.dledger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @Author yaowenbin
 * @Date 2023/4/26
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class DLedgerUtils {

    /**
     * 计算start到目前位置的过去时间戳
     */
    public static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

}
