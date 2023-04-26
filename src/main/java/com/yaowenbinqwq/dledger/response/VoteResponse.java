package com.yaowenbinqwq.dledger.response;


import static com.yaowenbinqwq.dledger.response.VoteResponse.Result.UNKNOWN;

/**
 * @Author yaowenbin
 * @Date 2023/4/26
 */
public class VoteResponse {
    Result result = UNKNOWN;
    private long term;

    public Result result() {
        return result;
    }

    public long term() {
        return term;
    }

    /**
     * 响应的解析结果
     */
    public enum ParseResult {
        WAIT_TO_VOTE_NEXT
    }

    public enum Result {
        UNKNOWN,
        ACCEPT,
        REJECT_ALREADY_HAS_LEADER,
        REJECT_TERM_SMALL_THAN_LEDGER,
        REJECT_EXPIRED_VOTE_TERM,


    }
}
