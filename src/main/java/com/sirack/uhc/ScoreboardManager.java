package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 사이드바 스코어보드 관리 클래스.
 * 게임 중 오른쪽 사이드바에 UHC 정보를 실시간으로 표시한다.
 */
public class ScoreboardManager {

    private final JavaPlugin plugin;
    private final GameManager gm;
    private int taskId = -1;

    private long gameStartTime = 0;
    private boolean pvpAnnounced = false;

    /** PVP 허용까지의 분 (기본 10) */
    private int pvpDelayMinutes = 10;

    public ScoreboardManager(JavaPlugin plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
    }

    public void start(int pvpDelayMin) {
        this.pvpDelayMinutes = pvpDelayMin;
        this.gameStartTime = System.currentTimeMillis();
        this.pvpAnnounced = false;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::update, 0L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    private void update() {
        if (!gm.isRunning()) return;

        long elapsedMs = System.currentTimeMillis() - gameStartTime;
        int elapsedSec = (int) (elapsedMs / 1000);

        // 현재 월드 보더 크기 (uhc_temp 맵 우선, 없으면 메인 월드)
        World gameWorld = Bukkit.getWorld("uhc_temp");
        if (gameWorld == null) gameWorld = Bukkit.getWorlds().get(0);
        double currentBorder = gameWorld.getWorldBorder().getSize();

        // PVP 남은 시간
        int pvpDelaySec = pvpDelayMinutes * 60;
        int pvpRemaining = pvpDelaySec - elapsedSec;
        boolean pvpActive = pvpRemaining <= 0;

        // 동기화를 위해 스코어보드에서 실시간 기준으로 이벤트 발생
        if (pvpActive && !pvpAnnounced) {
            pvpAnnounced = true;
            gm.triggerPvpAndBorder();
        }

        // 보더 10블록 도달까지 남은 시간
        // 반지름이 5초에 1블록(즉 지름은 5초에 2블록) 축소 => 지름 축소 속도는 1초당 0.4블록.
        // 남은 시간(초) = (현재지름 - 10) / 0.4 = (현재지름 - 10) * 2.5
        String borderCountdown;
        if (!pvpActive) {
            double blocksToShrink = currentBorder - gm.getBorderFinalSize();
            if (blocksToShrink < 0) blocksToShrink = 0;
            int totalShrinkSec = (int)(blocksToShrink * 2.5);
            int totalRemaining = pvpRemaining + totalShrinkSec;
            borderCountdown = formatTime(totalRemaining);
        } else {
            double blocksToShrink = currentBorder - gm.getBorderFinalSize();
            if (blocksToShrink <= 0.1) {
                borderCountdown = ChatColor.RED + "최소 도달!";
            } else {
                int remainingSec = (int)(blocksToShrink * 2.5);
                borderCountdown = formatTime(remainingSec);
            }
        }

        // 생존자 수
        int alive = gm.getAlivePlayers().size();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("uhc", Criteria.DUMMY,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ UHC ⚔");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            int score = 15;

            obj.getScore(ChatColor.GOLD + "━━━━━━━━━━━━━").setScore(score--);

            // 월드 보더
            obj.getScore(ChatColor.YELLOW + "월드보더: " + ChatColor.WHITE + (int)currentBorder + "블록").setScore(score--);

            obj.getScore(" ").setScore(score--);

            // PVP 전: PVP 남은 시간 표시
            // PVP 후: 같은 자리에 보더 10블록 도달 남은 시간 표시
            if (!pvpActive) {
                String pvpTime = formatTime(pvpRemaining);
                obj.getScore(ChatColor.RED + "⚔ PVP: " + ChatColor.WHITE + pvpTime + " 후").setScore(score--);
            } else {
                obj.getScore(ChatColor.AQUA + "보더 10블록: " + ChatColor.WHITE + borderCountdown).setScore(score--);
            }

            obj.getScore("  ").setScore(score--);

            // 생존자 수
            obj.getScore(ChatColor.GREEN + "생존자: " + ChatColor.WHITE + alive + "명").setScore(score--);

            // 팀 모드일 경우 팀원 표시 및 팀 색상 등록
            if (gm.getType() == GameManager.GameType.TEAM) {
                // 내 팀원 표시
                java.util.UUID buddyId = gm.getTeammate(p.getUniqueId());
                ChatColor myColor = gm.getTeamColor(p.getUniqueId());
                if (myColor == null) myColor = ChatColor.WHITE;
                
                String buddyName = "(혼자)";
                if (buddyId != null) {
                    Player bp = Bukkit.getPlayer(buddyId);
                    if (bp != null) {
                        buddyName = bp.getName();
                    } else {
                        buddyName = Bukkit.getOfflinePlayer(buddyId).getName();
                        if (buddyName == null) buddyName = "(알 수 없음)";
                    }
                }
                obj.getScore("   ").setScore(score--);
                obj.getScore(ChatColor.AQUA + "팀원: " + myColor + buddyName).setScore(score--);
                
                // 모든 플레이어의 닉네임 색상 적용 (Scoreboard Team)
                for (java.util.UUID aliveId : gm.getAlivePlayers()) {
                    Player ap = Bukkit.getPlayer(aliveId);
                    if (ap != null) {
                        ChatColor color = gm.getTeamColor(aliveId);
                        if (color != null) {
                            String teamName = "team_" + color.name();
                            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
                            if (team == null) {
                                team = board.registerNewTeam(teamName);
                                team.setColor(color);
                            }
                            team.addEntry(ap.getName());
                        }
                    }
                }
            }

            obj.getScore(ChatColor.GOLD + "━━━━━━━━━━━━━━").setScore(score--);

            p.setScoreboard(board);
        }
    }

    private String formatTime(int totalSec) {
        if (totalSec <= 0) return "0초";
        int min = totalSec / 60;
        int sec = totalSec % 60;
        if (min > 0) {
            return min + "분 " + sec + "초";
        }
        return sec + "초";
    }
}
