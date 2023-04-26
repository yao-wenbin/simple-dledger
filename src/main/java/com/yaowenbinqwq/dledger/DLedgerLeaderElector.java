package com.yaowenbinqwq.dledger;

import com.yaowenbinqwq.dledger.response.VoteResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author yaowenbin
 * @Date 2023/4/26
 */
@Slf4j
public class DLedgerLeaderElector {
    private MemberState memberState = new MemberState();
    private Random random = new Random();
    private volatile long lastLeaderHeatBeatTime;
    private long heartBeatInterval;
    private int maxHeartBeatLeak;
    private long nextTimeToRequestVote;
    private boolean needIncreaseTermImmediately;
    private VoteResponse.ParseResult lastParseResult;
    private int maxVoteIntervalMs = 2000;
    private long lastVoteCost = -1;


    private void maintainState() {
        if (memberState.isLeader()) {
            maintainAsLeader();
        } else if (memberState.isFollower()) {
            maintainAsFollower();
        }else if (memberState.isCandidate()) {
            maintainAsCandidate();
        }
    }


    private void maintainAsLeader() {
        sendHeartBeats();
    }

    private void maintainAsFollower() {
        checkLastSendTime();
    }

    private void maintainAsCandidate() {
        // 还未到达投票时间或者是当前任期需要立即更新
        if (System.currentTimeMillis() < nextTimeToRequestVote || !needIncreaseTermImmediately) {
            return;
        }

        long term;
        long ledgerEndTerm;
        long ledgerEndIndex;
        // 双重检查memberState的状态
        if (!memberState.isCandidate()) {
            return;
        }
        synchronized (memberState) {
            if (!memberState.isCandidate()) {
                return;
            }

            if (lastParseResult == VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT || needIncreaseTermImmediately) {

            }
            term = memberState.currTerm();
            ledgerEndTerm = memberState.ledgerEndTerm();
            ledgerEndIndex = memberState.ledgerEndIndex();

        }

        if (needIncreaseTermImmediately) {
            nextTimeToRequestVote = calNextTimeToRequestVote();
            needIncreaseTermImmediately = false;
            return;
        }

        // start to vote
        // 开始投票
        final List<CompletableFuture<VoteResponse>> quorumVoteResponse = startVoteWithQuorum(term, ledgerEndTerm, ledgerEndIndex);
        calcVoteResponse(quorumVoteResponse, term);
    }

    private void calcVoteResponse(List<CompletableFuture<VoteResponse>> quorumVoteResponse, long term) {
        long startVoteTimeMs = System.currentTimeMillis();

        final AtomicLong knownMaxTermInGroup = new AtomicLong(term);
        final AtomicInteger allNum = new AtomicInteger(0);
        final AtomicInteger validNum = new AtomicInteger(0);
        final AtomicInteger acceptedNum = new AtomicInteger(0);
        final AtomicInteger notReadyTermNum = new AtomicInteger(0);
        final AtomicInteger biggerLedgerNum = new AtomicInteger(0);
        final AtomicBoolean alreadyHasLeader = new AtomicBoolean(false);

        // wait until vote end.
        // 等待投票结束
        CountDownLatch voteLatch = new CountDownLatch(1);
        quorumVoteResponse.forEach(future -> {
            future.whenComplete((VoteResponse res, Throwable ex) -> {
                try {
                    if (ex != null) {
                        throw ex;
                    }

                    if (res.result() != VoteResponse.Result.UNKNOWN) {
                        validNum.incrementAndGet();
                    }

                    synchronized (knownMaxTermInGroup) {
                        switch (res.result()) {
                            case ACCEPT:
                                acceptedNum.incrementAndGet();
                                break;
                            case REJECT_ALREADY_HAS_LEADER:
                                alreadyHasLeader.compareAndSet(false, true);
                                break;
                            case REJECT_EXPIRED_VOTE_TERM:
                            case REJECT_TERM_SMALL_THAN_LEDGER:
                                if (res.term() > knownMaxTermInGroup.get()) {
                                    knownMaxTermInGroup.set(res.term());
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    // has another leader or (acceptNum and acceptNum + noReadyTermNum achieve quorum count);
                    // 发生脑裂或者是达到quorum数量
                    if (alreadyHasLeader.get()
                            || memberState.isQuorum(acceptedNum.get())
                            || memberState.isQuorum(acceptedNum.get() + notReadyTermNum.get())) {
                        voteLatch.countDown();
                    }

                } catch (Throwable t) {
                    log.error("vote has exception: ", t);
                }
            });
        });

        try {
            voteLatch.await(2000 + random.nextInt(maxVoteIntervalMs), TimeUnit.MICROSECONDS);
        } catch (InterruptedException ignore) {
        }

        lastVoteCost = DLedgerUtils.elapsed(startVoteTimeMs);

        VoteResponse.ParseResult parseResult;
        // 如果投票的任期小于机群中的最大任期，则进入下一轮投票
        if (term < knownMaxTermInGroup.get()) {
            parseResult = VoteResponse.ParseResult.WAIT_TO_VOTE_NEXT;
            nextTimeToRequestVote = calNextTimeToRequestVote();
            changeRoleToCandidate(knownMaxTermInGroup.get());
        }


    }

    private void changeRoleToCandidate(long term) {
        synchronized (memberState) {
            if (term > memberState.currTerm()) {
                memberState.changeToCandidate(term);
                handleRoleChange(term, MemberState.Role.CANDIDATE);
                log.info("[{}]");
            } else {
                log.info("[{}] skip to be candidate in term, currTem: {}", memberState.selfId(), memberState.currTerm());
            }
        }
    }

    // expand to third part to handle role change event.
    // 拓展给三方进行角色变更处理
    private void handleRoleChange(long term, MemberState.Role candidate) {
    }

    // call by rpc to start vote.
    // 使用rpc进行投票。
    private List<CompletableFuture<VoteResponse>> startVoteWithQuorum(long term, long ledgerEndTerm, long ledgerEndIndex) {
        return null;
    }

    private long calNextTimeToRequestVote() {
        return System.currentTimeMillis() + 1000;
    }


    private void sendHeartBeats() {

    }

    private void checkLastSendTime() {
        if (DLedgerUtils.elapsed(this.lastLeaderHeatBeatTime) > 2 * this.heartBeatInterval) {
            synchronized (memberState) {
                if (memberState.isFollower() &&  DLedgerUtils.elapsed(this.lastLeaderHeatBeatTime) > this.heartBeatInterval) {
                    log.info("[{}] [HeartBeanTimeout] lastLeaderHeartBeatTime: {}, heartBeanInterval: {}, leaderId: {}", memberState.selfId(), lastLeaderHeatBeatTime, heartBeatInterval, memberState.leaderId());
                    changeToCandidate();
                }
            }
        }
    }

    private void changeToCandidate() {

    }










    class StateMaintainer implements Runnable{

        AtomicBoolean running = new AtomicBoolean(true);

        @Override
        public void run() {
            while (running.get()) {
                try {
                    DLedgerLeaderElector.this.maintainState();

                    Thread.sleep(10);
                } catch (Throwable t) {
                    log.error("maintainState Exception: ",t);
                }
            }
        }
    }



}
