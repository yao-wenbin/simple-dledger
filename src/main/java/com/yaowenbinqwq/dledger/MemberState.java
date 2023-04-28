package com.yaowenbinqwq.dledger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.yaowenbinqwq.dledger.MemberState.Role.*;


/**
 * @Author yaowenbin
 * @Date 2023/4/26
 */
public class MemberState {

    private String selfId;

    private String leaderId;

    Role role = CANDIDATE;
    private volatile long currTerm = 0;
    private volatile long ledgerEndTerm = -1;
    private volatile long ledgerEndIndex = -1;

    private Map<String, String> groupMap = new HashMap<>();

    public String selfId() {
        return selfId;
    }

    public String leaderId() {
        return leaderId;
    }

    public boolean isCandidate() {
        return Objects.equals(this.role, CANDIDATE);
    }

    public boolean isLeader() {
        return Objects.equals(this.role, LEADER);
    }

    public boolean isFollower() {
        return Objects.equals(this.role, FOLLOWER);
    }

    public long currTerm() {
        return currTerm;
    }

    public long ledgerEndTerm() {
        return ledgerEndTerm;
    }

    public long ledgerEndIndex() {
        return ledgerEndIndex;
    }

    public boolean isQuorum(int i) {
        return i > groupMap.size() / 2;
    }

    public boolean isPeerMember(String memberId) {
        return groupMap.containsKey(memberId);
    }

    public void changeToCandidate(long term) {
        role = CANDIDATE;
        currTerm = term;
    }


    public enum Role {
        CANDIDATE,
        LEADER,
        FOLLOWER


    }

}
