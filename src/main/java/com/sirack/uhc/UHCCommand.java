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

            // ── 조합 ──────────────────────────────────────────
            case "조합" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("플레이어만 사용할 수 있습니다.");
                    return true;
                }
                if (args.length < 2) return true;
                Player p = (Player) sender;
                String recipe = args[1];

                if (!gm.isRunning()) {
                    p.sendMessage(PREFIX + ChatColor.RED + "커스텀 조합법은 게임 중에만 사용할 수 있습니다.");
                    return true;
                }

                if (recipe.equals("apprentice_helmet")) {
                    if (RecipeManager.getApprenticeCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 견습용 투구를 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 5) && 
                        hasItems(p.getInventory(), org.bukkit.Material.REDSTONE_TORCH, 1)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 5);
                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE_TORCH, 1);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_TORCH, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("apprentice_sword")) {
                    if (RecipeManager.getSwordCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 견습용 검을 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.IRON_SWORD, 1) && 
                        hasItems(p.getInventory(), org.bukkit.Material.REDSTONE_BLOCK, 2)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.IRON_SWORD, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE_BLOCK, 2);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("apprentice_bow")) {
                    if (RecipeManager.getBowCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 견습용 활을 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.STRING, 3) && 
                        hasItems(p.getInventory(), org.bukkit.Material.REDSTONE_TORCH, 3)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.STRING, 3);
                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE_TORCH, 3);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            // " RS", "R S", " RS"
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_TORCH, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STRING, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_TORCH, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STRING, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_TORCH, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STRING, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("master_compass")) {
                    if (RecipeManager.getCompassCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 마스터의 나침반을 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.REDSTONE, 7) &&
                        hasItems(p.getInventory(), org.bukkit.Material.COMPASS, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.REDSTONE_TORCH, 1)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE, 7);
                        removeItems(p.getInventory(), org.bukkit.Material.COMPASS, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE_TORCH, 1);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            // RTR / RCR / RRR
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_TORCH, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("sharpness_book")) {
                    if (RecipeManager.getSharpnessBookCrafts(p.getUniqueId()) >= 4) {
                        p.sendMessage(ChatColor.RED + "이미 날카로움의 책을 최대치(4/4)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.FLINT, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.PAPER, 3) &&
                        hasItems(p.getInventory(), org.bukkit.Material.IRON_SWORD, 1)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.FLINT, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.PAPER, 3);
                        removeItems(p.getInventory(), org.bukkit.Material.IRON_SWORD, 1);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            // F  ,  PP,  PI
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FLINT, 1);
                            
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("power_book")) {
                    if (RecipeManager.getPowerBookCrafts(p.getUniqueId()) >= 4) {
                        p.sendMessage(ChatColor.RED + "이미 힘의 책을 최대치(4/4)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.FLINT, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.PAPER, 3) &&
                        hasItems(p.getInventory(), org.bukkit.Material.BONE, 1)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.FLINT, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.PAPER, 3);
                        removeItems(p.getInventory(), org.bukkit.Material.BONE, 1);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            // F  ,  PP,  PB
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FLINT, 1);
                            
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("dragon_sword")) {
                    if (RecipeManager.getDragonSwordCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 용의 검을 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.BLAZE_POWDER, 2) &&
                        hasItems(p.getInventory(), org.bukkit.Material.DIAMOND_SWORD, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.OBSIDIAN, 2)) {
                        
                        removeItems(p.getInventory(), org.bukkit.Material.BLAZE_POWDER, 2);
                        removeItems(p.getInventory(), org.bukkit.Material.DIAMOND_SWORD, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.OBSIDIAN, 2);
                        
                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLAZE_POWDER, 1);
                            
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1);
                            
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLAZE_POWDER, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("leather_saving")) {
                    if (RecipeManager.getLeatherSavingCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 가죽 절약을 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.LEATHER, 3) &&
                        hasItems(p.getInventory(), org.bukkit.Material.STICK, 6)) {

                        removeItems(p.getInventory(), org.bukkit.Material.LEATHER, 3);
                        removeItems(p.getInventory(), org.bukkit.Material.STICK, 6);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("dragon_armor")) {
                    if (RecipeManager.getDragonArmorCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 용의 갑옷을 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.MAGMA_CREAM, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.DIAMOND_CHESTPLATE, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.OBSIDIAN, 2) &&
                        hasItems(p.getInventory(), org.bukkit.Material.ANVIL, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.MAGMA_CREAM, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.DIAMOND_CHESTPLATE, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.OBSIDIAN, 2);
                        removeItems(p.getInventory(), org.bukkit.Material.ANVIL, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.MAGMA_CREAM, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ANVIL, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("protection_book")) {
                    if (RecipeManager.getProtectionBookCrafts(p.getUniqueId()) >= 4) {
                        p.sendMessage(ChatColor.RED + "이미 보호의 책을 최대치(4/4)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.PAPER, 3) &&
                        hasItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.PAPER, 3);
                        removeItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("nectar")) {
                    if (RecipeManager.getNectarCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 넥타르를 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.EMERALD, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.GOLD_INGOT, 2) &&
                        hasItems(p.getInventory(), org.bukkit.Material.MELON_SLICE, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.GLASS_BOTTLE, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.EMERALD, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.GOLD_INGOT, 2);
                        removeItems(p.getInventory(), org.bukkit.Material.MELON_SLICE, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.GLASS_BOTTLE, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.MELON_SLICE, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GLASS_BOTTLE, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("burning_artifact")) {
                    if (RecipeManager.getBurningArtifactCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 불타는 아티팩트를 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.ORANGE_STAINED_GLASS, 6) &&
                        hasItems(p.getInventory(), org.bukkit.Material.LAVA_BUCKET, 2) &&
                        hasItems(p.getInventory(), org.bukkit.Material.FIREWORK_ROCKET, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.ORANGE_STAINED_GLASS, 6);
                        removeItems(p.getInventory(), org.bukkit.Material.LAVA_BUCKET, 2);
                        removeItems(p.getInventory(), org.bukkit.Material.FIREWORK_ROCKET, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LAVA_BUCKET, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FIREWORK_ROCKET, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LAVA_BUCKET, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("delicious_meal")) {
                    if (RecipeManager.getDeliciousMealCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 맛있는 식사를 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (countMeatItems(p.getInventory()) >= 8 && hasItems(p.getInventory(), org.bukkit.Material.COAL, 1)) {
                        // 어떤 고기든 8개 + 석탄 1개 제거
                        List<org.bukkit.Material> meats = removeAnyMeatsAndGetList(p.getInventory(), 8);
                        removeItems(p.getInventory(), org.bukkit.Material.COAL, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(meats.get(0), 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(meats.get(1), 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(meats.get(2), 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(meats.get(3), 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COAL, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(meats.get(4), 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(meats.get(5), 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(meats.get(6), 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(meats.get(7), 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("toughness_potion")) {
                    if (RecipeManager.getToughnessPotionCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 강인함의 포션을 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.SLIME_BALL, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.SNOW_BLOCK, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.GLASS_BOTTLE, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.SLIME_BALL, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.SNOW_BLOCK, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.GLASS_BOTTLE, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SLIME_BALL, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOW_BLOCK, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GLASS_BOTTLE, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("seven_league_boots")) {
                    if (RecipeManager.getSevenLeagueBootsCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 세븐 리그 부츠를 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.FEATHER, 6) &&
                        hasItems(p.getInventory(), org.bukkit.Material.ENDER_PEARL, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.DIAMOND_BOOTS, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.WATER_BUCKET, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.FEATHER, 6);
                        removeItems(p.getInventory(), org.bukkit.Material.ENDER_PEARL, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.DIAMOND_BOOTS, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.WATER_BUCKET, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BOOTS, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WATER_BUCKET, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("iron_pack")) {
                    if (RecipeManager.getIronPackCrafts(p.getUniqueId()) >= 4) {
                        p.sendMessage(ChatColor.RED + "이미 아이언 팩을 최대치(4/4)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.RAW_IRON, 8) &&
                        hasItems(p.getInventory(), org.bukkit.Material.COAL, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.RAW_IRON, 8);
                        removeItems(p.getInventory(), org.bukkit.Material.COAL, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COAL, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("obsidian_mix")) {
                    if (RecipeManager.getObsidianMixCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 흑요석 만들기를 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.WATER_BUCKET, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.LAVA_BUCKET, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.WATER_BUCKET, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.LAVA_BUCKET, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WATER_BUCKET, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LAVA_BUCKET, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("tarnhelm")) {
                    if (RecipeManager.getTarnhelmCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 탄헬름을 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.DIAMOND, 4) &&
                        hasItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.REDSTONE_BLOCK, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.DIAMOND, 4);
                        removeItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE_BLOCK, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("philosopher_pickaxe")) {
                    if (RecipeManager.getPhilosopherPickaxeCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 철학자의 곡괭이를 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.RAW_IRON, 2) &&
                        hasItems(p.getInventory(), org.bukkit.Material.RAW_GOLD, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.LAPIS_BLOCK, 2) &&
                        hasItems(p.getInventory(), org.bukkit.Material.STICK, 2)) {

                        removeItems(p.getInventory(), org.bukkit.Material.RAW_IRON, 2);
                        removeItems(p.getInventory(), org.bukkit.Material.RAW_GOLD, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.LAPIS_BLOCK, 2);
                        removeItems(p.getInventory(), org.bukkit.Material.STICK, 2);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_GOLD, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RAW_IRON, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LAPIS_BLOCK, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LAPIS_BLOCK, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("awakening_pack")) {
                    if (RecipeManager.getAwakeningPackCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 깨우침의 팩을 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.REDSTONE_BLOCK, 4) &&
                        hasItems(p.getInventory(), org.bukkit.Material.GLASS_BOTTLE, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.REDSTONE_BLOCK, 4);
                        removeItems(p.getInventory(), org.bukkit.Material.GLASS_BOTTLE, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GLASS_BOTTLE, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_BLOCK, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("light_anvil")) {
                    if (RecipeManager.getLightAnvilCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 가벼운 모루를 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 6) &&
                        hasItems(p.getInventory(), org.bukkit.Material.IRON_BLOCK, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.IRON_INGOT, 6);
                        removeItems(p.getInventory(), org.bukkit.Material.IRON_BLOCK, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BLOCK, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("light_enchanting_table")) {
                    if (RecipeManager.getLightEnchantingTableCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 가벼운 마법 부여대를 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.BOOKSHELF, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.OBSIDIAN, 4) &&
                        hasItems(p.getInventory(), org.bukkit.Material.DIAMOND, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.EXPERIENCE_BOTTLE, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.BOOKSHELF, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.OBSIDIAN, 4);
                        removeItems(p.getInventory(), org.bukkit.Material.DIAMOND, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.EXPERIENCE_BOTTLE, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            // 2번 책장, 4번 흑요석, 5번 다이아몬드, 6번 흑요석, 7번 흑요석, 8번 경험치병, 9번 흑요석
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOOKSHELF, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.EXPERIENCE_BOTTLE, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("eves_temptation")) {
                    if (RecipeManager.getEvesTemptationCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 이브의 유혹을 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.APPLE, 1) &&
                        hasItems(p.getInventory(), org.bukkit.Material.BONE_MEAL, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.APPLE, 1);
                        removeItems(p.getInventory(), org.bukkit.Material.BONE_MEAL, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("fruit_of_recovery")) {
                    if (RecipeManager.getFruitOfRecoveryCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 회복의 과일을 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.BONE_MEAL, 4) &&
                        hasItems(p.getInventory(), org.bukkit.Material.WHEAT_SEEDS, 4) &&
                        hasItems(p.getInventory(), org.bukkit.Material.APPLE, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.BONE_MEAL, 4);
                        removeItems(p.getInventory(), org.bukkit.Material.WHEAT_SEEDS, 4);
                        removeItems(p.getInventory(), org.bukkit.Material.APPLE, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHEAT_SEEDS, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHEAT_SEEDS, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHEAT_SEEDS, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHEAT_SEEDS, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("light_apple")) {
                    if (RecipeManager.getLightAppleCrafts(p.getUniqueId()) >= 1) {
                        p.sendMessage(ChatColor.RED + "이미 가벼운 사과를 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.GOLD_INGOT, 4) &&
                        hasItems(p.getInventory(), org.bukkit.Material.APPLE, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.GOLD_INGOT, 4);
                        removeItems(p.getInventory(), org.bukkit.Material.APPLE, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
                } else if (recipe.equals("golden_head")) {
                    if (RecipeManager.getGoldenHeadCrafts(p.getUniqueId()) >= 3) {
                        p.sendMessage(ChatColor.RED + "이미 황금 머리를 최대치(3/3)로 제작했습니다.");
                        return true;
                    }
                    if (hasItems(p.getInventory(), org.bukkit.Material.GOLD_INGOT, 8) &&
                        hasItems(p.getInventory(), org.bukkit.Material.PLAYER_HEAD, 1)) {

                        removeItems(p.getInventory(), org.bukkit.Material.GOLD_INGOT, 8);
                        removeItems(p.getInventory(), org.bukkit.Material.PLAYER_HEAD, 1);

                        org.bukkit.inventory.InventoryView view = p.openWorkbench(null, true);
                        if (view != null && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory) {
                            org.bukkit.inventory.CraftingInventory craftInv = (org.bukkit.inventory.CraftingInventory) view.getTopInventory();
                            org.bukkit.inventory.ItemStack[] matrix = new org.bukkit.inventory.ItemStack[9];
                            matrix[0] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[1] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[2] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[3] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[4] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD, 1);
                            matrix[5] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[6] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[7] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            matrix[8] = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1);
                            craftInv.setMatrix(matrix);
                        }
                    } else {
                        p.sendMessage(PREFIX + ChatColor.RED + "재료가 부족합니다.");
                    }
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

    private boolean hasItems(org.bukkit.inventory.Inventory inv, org.bukkit.Material mat, int amount) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count >= amount;
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

    private int countMeatItems(org.bukkit.inventory.Inventory inv) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
            if (item != null && (item.getType() == org.bukkit.Material.PORKCHOP || item.getType() == org.bukkit.Material.BEEF || item.getType() == org.bukkit.Material.MUTTON)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private List<org.bukkit.Material> removeAnyMeatsAndGetList(org.bukkit.inventory.Inventory inv, int amount) {
        List<org.bukkit.Material> removed = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item != null && (item.getType() == org.bukkit.Material.PORKCHOP || item.getType() == org.bukkit.Material.BEEF || item.getType() == org.bukkit.Material.MUTTON)) {
                while (item.getAmount() > 0 && removed.size() < amount) {
                    item.setAmount(item.getAmount() - 1);
                    removed.add(item.getType());
                }
                if (item.getAmount() == 0) inv.setItem(i, null);
                if (removed.size() == amount) break;
            }
        }
        return removed;
    }
}
