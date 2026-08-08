package com.sirack.uhc;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /유챔 명령어 처리
 *
 * 서브 명령어:
 *   시작        - 정식 게임 시작 (최소 2명)
 *   종료        - 게임 강제 종료
 *   테스트시작   - 테스트 게임 시작 (1명 가능)
 *   테스트종료   - 테스트 게임 강제 종료
 *   월드보더 <크기> - 월드 보더 즉시 변경
 */
public class UHCCommand implements CommandExecutor, TabCompleter {

    private final GameManager gm;

    private static final String PREFIX =
        ChatColor.GOLD + "[UHC] " + ChatColor.RESET;

    public UHCCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0]) {

            // ── 시작 ──────────────────────────────────────────
            case "시작" -> {
                if (gm.getState() != GameState.LOBBY) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이미 게임이 진행 중입니다.");
                    return true;
                }
                
                GameManager.GameType type = GameManager.GameType.SOLO;
                if (args.length > 1 && args[1].equalsIgnoreCase("팀")) {
                    type = GameManager.GameType.TEAM;
                }

                boolean started = gm.startGame(type);
                if (!started) {
                    String minMsg = type == GameManager.GameType.TEAM
                        ? "최소 3명의 플레이어가 필요합니다! (팀모드)"
                        : "최소 2명의 플레이어가 필요합니다!";
                    sender.sendMessage(PREFIX + ChatColor.RED + minMsg);
                } else {
                    if (sender instanceof Player) {
                        gm.setLobbyLocation(((Player) sender).getLocation());
                    }
                }
            }

            // ── 종료 ──────────────────────────────────────────
            case "종료" -> {
                if (gm.getState() == GameState.LOBBY) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "진행 중인 게임이 없습니다.");
                    return true;
                }
                gm.stopGame();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "게임을 종료했습니다.");
            }

            // ── 테스트시작 ────────────────────────────────────
            case "테스트시작" -> {
                if (gm.getState() != GameState.LOBBY) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이미 게임이 진행 중입니다.");
                    return true;
                }

                GameManager.GameType type = GameManager.GameType.SOLO;
                if (args.length > 1 && args[1].equalsIgnoreCase("팀")) {
                    type = GameManager.GameType.TEAM;
                }

                boolean started = gm.startTestGame(type);
                if (!started) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "최소 1명의 플레이어가 필요합니다!");
                } else {
                    if (sender instanceof Player) {
                        gm.setLobbyLocation(((Player) sender).getLocation());
                    }
                    String modeStr = type == GameManager.GameType.TEAM ? "팀 모드" : "솔로 모드";
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "[테스트 모드] " + modeStr + " 게임을 시작합니다.");
                }
            }

            // ── 테스트종료 ────────────────────────────────────
            case "테스트종료" -> {
                if (!gm.isTestMode()) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "현재 테스트 모드가 아닙니다.");
                    return true;
                }
                gm.stopGame();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "테스트 게임을 종료했습니다.");
            }

            // ── 월드보더 <크기> ───────────────────────────────
            case "월드보더" -> {
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "사용법: /유챔 월드보더 <크기>");
                    return true;
                }
                try {
                    double size = Double.parseDouble(args[1]);
                    if (size < 1) {
                        sender.sendMessage(PREFIX + ChatColor.RED + "크기는 1 이상이어야 합니다.");
                        return true;
                    }
                    gm.setWorldBorderNow(size);
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "월드 보더를 " + (int) size + "블록으로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "올바른 숫자를 입력하세요.");
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "━━━━ UHC 명령어 ━━━━");
        sender.sendMessage(ChatColor.YELLOW + "/유챔 시작"
            + ChatColor.GRAY + "  - 게임 시작 (최소 2명)");
        sender.sendMessage(ChatColor.YELLOW + "/유챔 종료"
            + ChatColor.GRAY + "  - 게임 강제 종료");
        sender.sendMessage(ChatColor.YELLOW + "/유챔 테스트시작"
            + ChatColor.GRAY + "  - 1명으로 테스트");
        sender.sendMessage(ChatColor.YELLOW + "/유챔 테스트종료"
            + ChatColor.GRAY + "  - 테스트 게임 종료");
        sender.sendMessage(ChatColor.YELLOW + "/유챔 월드보더 <크기>"
            + ChatColor.GRAY + "  - 보더 즉시 변경");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                "시작", "종료", "테스트시작", "테스트종료", "월드보더"
            );
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0])) result.add(s);
            }
            return result;
        }
        if (args.length == 2) {
            String sub = args[0];
            if (sub.equals("시작") || sub.equals("테스트시작")) {
                List<String> modes = Arrays.asList("솔로", "팀");
                List<String> result = new ArrayList<>();
                for (String m : modes) {
                    if (m.startsWith(args[1])) result.add(m);
                }
                return result;
            }
            if (sub.equals("월드보더")) {
                return Arrays.asList("500", "1000", "2000", "3000");
            }
        }
        return new ArrayList<>();
    }
}
