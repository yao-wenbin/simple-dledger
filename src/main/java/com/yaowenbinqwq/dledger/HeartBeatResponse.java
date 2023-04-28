package com.yaowenbinqwq.dledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.yaowenbinqwq.dledger.HeartBeatResponse.RESULT.SUCCESS;
import static com.yaowenbinqwq.dledger.HeartBeatResponse.RESULT.UNKNOWN;

/**
 * @Author yaowenbin
 * @Date 2023/4/28
 */

@Data
@AllArgsConstructor
@Accessors(fluent = true)
public class HeartBeatResponse {

    private RESULT result = UNKNOWN;

    public static HeartBeatResponse success() {
        return new HeartBeatResponse(SUCCESS);
    }

    enum RESULT {
        UNKNOWN,
        SUCCESS,
        UNKNOWN_MEMBER,
        EXPIRED_TERM;
    }

}
