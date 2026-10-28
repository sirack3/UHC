package com.sirack.uhc;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                if (!sender.hasPermission("uhc.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                    return true;
                }
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
                if (!sender.hasPermission("uhc.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                    return true;
                }
                if (gm.getState() == GameState.LOBBY) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "진행 중인 게임이 없습니다.");
                    return true;
                }
                gm.stopGame();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "게임을 종료했습니다.");
            }

            // ── 테스트시작 ────────────────────────────────────
            case "테스트시작" -> {
                if (!sender.hasPermission("uhc.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                    return true;
                }
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
                if (!sender.hasPermission("uhc.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                    return true;
                }
                if (!gm.isTestMode()) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "현재 테스트 모드가 아닙니다.");
                    return true;
                }
                gm.stopGame();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "테스트 게임을 종료했습니다.");
            }

            // ── 월드보더 <크기> ───────────────────────────────
            case "월드보더" -> {
                if (!sender.hasPermission("uhc.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                    return true;
                }
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

            // ── 조합법도감 ─────────────────────────────────────
            case "조합법도감" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("플레이어만 사용할 수 있습니다.");
                    return true;
                }
                RecipeManager.openRecipeCatalogGUI((Player) sender, 1);
            }

            // ── 조합 ──────────────────────────────────────────
            case "조합" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("플레이어만 사용할 수 있습니다.");
                    return true;
                }
                if (args.length < 2) return true;
                Player p = (Player) sender;
                String recipeId = args[1];

                if (!gm.isRunning()) {
                    p.sendMessage(PREFIX + ChatColor.RED + "커스텀 조합법은 게임 중에만 사용할 수 있습니다.");
                    return true;
                }

                CustomRecipe recipe = RecipeManager.getRecipe(recipeId);
                if (recipe == null) {
                    p.sendMessage(PREFIX + ChatColor.RED + "존재하지 않는 조합법입니다.");
                    return true;
                }

                if (RecipeManager.getCrafts(p.getUniqueId(), recipe.id) >= recipe.maxCrafts) {
                    p.sendMessage(ChatColor.RED + "이미 " + recipe.tag + "을(를) 제작했습니다.");
                    return true;
                }

                // 재료 확인 (일반 재료 + 그룹 재료)
                boolean canCraft = true;
                for (Map.Entry<String, Integer> req : recipe.notifyRequirements.entrySet()) {
                    String reqKey = req.getKey();
                    int reqCount = req.getValue();
                    if (RecipeManager.isGroupRequirement(reqKey)) {
                        List<org.bukkit.Material> groupMats = RecipeManager.getMaterialGroup(reqKey);
                        if (countGroupItems(p.getInventory(), groupMats) < reqCount) {
                            canCraft = false;
                            break;
                        }
                    } else {
                        try {
                            org.bukkit.Material reqMat = org.bukkit.Material.valueOf(reqKey);
                            if (!hasItems(p.getInventory(), reqMat, reqCount)) {
                                canCraft = false;
                                break;
                            }
                        } catch (Exception e) {
                            canCraft = false;
                            break;
                        }
                    }
                }

                if (!canCraft) {
                    p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    return true;
                }

                // 재료 차감 및 추출
                Map<String, List<org.bukkit.Material>> extractedGroups = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> req : recipe.notifyRequirements.entrySet()) {
                    String reqKey = req.getKey();
                    int reqCount = req.getValue();
                    if (RecipeManager.isGroupRequirement(reqKey)) {
                        List<org.bukkit.Material> groupMats = RecipeManager.getMaterialGroup(reqKey);
                        List<org.bukkit.Material> extracted = removeGroupItems(p.getInventory(), groupMats, reqCount);
                        extractedGroups.put(reqKey, extracted);
                    } else {
                        removeItems(p.getInventory(), org.bukkit.Material.valueOf(reqKey), reqCount);
                    }
                }

                // 워크벤치 오픈 및 매트릭스 자동 배치
                org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory craftInv) {
                    org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];

                    // extractedGroups에서 남은 재료를 큐로 관리 (materialChoice 슬롯에 사용)
                    List<org.bukkit.Material> groupQueue = new ArrayList<>();
                    for (List<org.bukkit.Material> extList : extractedGroups.values()) {
                        groupQueue.addAll(extList);
                    }

                    if ("shaped".equalsIgnoreCase(recipe.type) && recipe.shape != null) {
                        for (int r = 0; r < recipe.shape.size() && r < 3; r++) {
                            String row = recipe.shape.get(r);
                            for (int c = 0; c < row.length() && c < 3; c++) {
                                char ch = row.charAt(c);
                                if (ch != ' ') {
                                    org.bukkit.Material mat = recipe.ingredients.get(ch);
                                    if (mat == null && recipe.materialChoices.containsKey(ch)) {
                                        // materialChoice 슬롯: extractedGroups 큐에서 가져오기
                                        if (!groupQueue.isEmpty()) {
                                            mat = groupQueue.remove(0);
                                        } else {
                                            // 큐가 비었으면 materialChoices 첫 번째 사용
                                            List<org.bukkit.Material> choices = recipe.materialChoices.get(ch);
                                            if (!choices.isEmpty()) mat = choices.get(0);
                                        }
                                    }
                                    if (mat != null) {
                                        matrix[r * 3 + c] = new org.bukkit.inventory.ItemStack(mat, 1);
                                    }
                                }
                            }
                        }
                    } else if ("shapeless".equalsIgnoreCase(recipe.type)) {
                        int idx = 0;
                        for (org.bukkit.Material mat : recipe.shapelessIngredients) {
                            if (idx < 9) matrix[idx++] = new org.bukkit.inventory.ItemStack(mat, 1);
                        }
                        for (CustomRecipe.ShapelessChoice sc : recipe.shapelessChoices) {
                            List<org.bukkit.Material> extList = null;
                            if (sc.group != null) {
                                extList = extractedGroups.get(sc.group);
                            }
                            if (extList != null && !extList.isEmpty()) {
                                for (org.bukkit.Material m : extList) {
                                    if (idx < 9) matrix[idx++] = new org.bukkit.inventory.ItemStack(m, 1);
                                }
                            } else if (sc.materials != null && !sc.materials.isEmpty()) {
                                for (int i = 0; i < sc.count && idx < 9; i++) {
                                    matrix[idx++] = new org.bukkit.inventory.ItemStack(sc.materials.get(i % sc.materials.size()), 1);
                                }
                            }
                        }
                    }
                    craftInv.setMatrix(matrix);
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
        sender.sendMessage(ChatColor.YELLOW + "/유챔 조합법도감"
            + ChatColor.GRAY + "  - 커스텀 조합법 도감 열기");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = sender.hasPermission("uhc.admin")
                ? Arrays.asList("시작", "종료", "테스트시작", "테스트종료", "월드보더", "조합법도감")
                : Arrays.asList("조합법도감");
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

    private int countItems(org.bukkit.inventory.Inventory inv, org.bukkit.Material mat) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean hasItems(org.bukkit.inventory.Inventory inv, org.bukkit.Material mat, int amount) {
        return countItems(inv, mat) >= amount;
    }

    private void removeItems(org.bukkit.inventory.Inventory inv, org.bukkit.Material mat, int amount) {
        int toRemove = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= toRemove) {
                    toRemove -= item.getAmount();
                    inv.setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - toRemove);
                    toRemove = 0;
                }
                if (toRemove == 0) break;
            }
        }
    }

    private int countGroupItems(org.bukkit.inventory.Inventory inv, List<org.bukkit.Material> materials) {
        if (materials == null || materials.isEmpty()) return 0;
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
            if (item != null && materials.contains(item.getType())) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private List<org.bukkit.Material> removeGroupItems(org.bukkit.inventory.Inventory inv, List<org.bukkit.Material> materials, int amount) {
        List<org.bukkit.Material> removed = new ArrayList<>();
        if (materials == null || materials.isEmpty() || amount <= 0) return removed;
        int remaining = amount;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item != null && materials.contains(item.getType())) {
                int take = Math.min(remaining, item.getAmount());
                for (int t = 0; t < take; t++) {
                    removed.add(item.getType());
                }
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                if (item.getAmount() <= 0) {
                    inv.setItem(i, null);
                }
            }
        }
        return removed;
    }
}
