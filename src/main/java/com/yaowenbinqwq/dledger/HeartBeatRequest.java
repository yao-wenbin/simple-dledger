package com.yaowenbinqwq.dledger;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author yaowenbin
 * @Date 2023/4/28
 */
@Data
@Accessors(fluent = true)
public class HeartBeatRequest {

    private String leaderId;

    private long currTerm;

}
