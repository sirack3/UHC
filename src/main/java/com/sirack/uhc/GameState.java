package com.sirack.uhc;

/**
 * UHC 게임 상태 열거형
 * LOBBY     : 대기 중 (게임 시작 전)
 * STARTING  : 카운트다운 중
 * RUNNING   : 게임 진행 중
 * ENDED     : 게임 종료
 */
public enum GameState {
    LOBBY,
    STARTING,
    RUNNING,
    ENDED
}
