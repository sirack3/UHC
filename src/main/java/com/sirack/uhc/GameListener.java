package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import java.util.HashMap;
import java.util.Map;

import java.util.List;

/**
 * UHC 게임 중 발생하는 주요 이벤트를 처리하는 리스너.
 *
 * 담당:
 *   - 자연 체력 회복 차단
 *   - PVP 무적 시간 차단
 *   - 사망 이벤트 → GameManager.handleDeath() + 플레이어 머리 드롭
 *   - 플레이어 머리 우클릭 → 버프 부여 (재생 + 이속)
 *   - 리스폰 차단 (스펙테이터로 전환)
 *   - 게임 중 입장한 플레이어 스펙테이터 전환
 */
public class GameListener implements Listener {

    private final GameManager gm;

    /** 플레이어 머리 아이템의 Lore에 붙는 태그 (UHC 머리인지 구분) */
    private static final String HEAD_LORE_TAG = ChatColor.GOLD + "UHC 플레이어 머리";
    private static final String HEAD_LORE_USE = ChatColor.GRAY + "우클릭으로 사용 (즉시 3하트 회복 + 신속 5초)";

    public GameListener(GameManager gm) {
        this.gm = gm;
    }

    // 아이템 픽업 시 중복 알림 방지용 (플레이어UUID_조합법)
    private final Map<String, Long> notifyCooldowns = new HashMap<>();

    // 나침반 우클릭 쿨다운 (10초)
    private final Map<java.util.UUID, Long> compassCooldowns = new HashMap<>();

    // =========================================================
    //  자연 체력 회복 차단
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!gm.isRunning()) return;
        if (!(event.getEntity() instanceof Player)) return;

        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.SATIATED
                || reason == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    //  무적 시간 (PVP 차단)
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!gm.isRunning()) return;

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();
            
            if (gm.getType() == GameManager.GameType.TEAM) {
                java.util.UUID vBuddy = gm.getTeammate(victim.getUniqueId());
                if (vBuddy != null && vBuddy.equals(damager.getUniqueId())) {
                    event.setCancelled(true);
                    damager.sendMessage(ChatColor.RED + "같은 팀원은 공격할 수 없습니다!");
                    return;
                }
            }

            if (!gm.isPvpEnabled()) {
                event.setCancelled(true);
                damager.sendMessage(ChatColor.RED + "PVP는 게임 시작 10분 후부터 가능합니다!");
                return;
            }
        }
    }

    // =========================================================
    //  플레이어 사망 → 머리 드롭
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gm.isRunning()) return;

        Player victim = event.getPlayer();
        if (!gm.isAlive(victim)) return;

        Player killer = victim.getKiller();

        event.setDeathMessage(null);

        // 사망 위치에 플레이어 머리 드롭 (인벤토리 드롭 아이템 목록에 추가)
        try {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(victim);
                meta.setDisplayName(ChatColor.YELLOW + victim.getName() + "의 머리");
                meta.setLore(List.of(HEAD_LORE_TAG, HEAD_LORE_USE));
                skull.setItemMeta(meta);
            }
            event.getDrops().add(skull);
        } catch (Exception e) {
            gm.getPlugin().getLogger().warning("플레이어 머리 드롭 중 오류: " + e.getMessage());
        }

        gm.handleDeath(victim, killer);
    }

    // =========================================================
    //  플레이어 머리 우클릭 → 버프 부여
    // =========================================================

    @EventHandler
    public void onPlayerUseHead(PlayerInteractEvent event) {
        if (!gm.isRunning()) return;

        // 우클릭만 처리
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        // UHC 머리인지 Lore 태그로 확인
        if (item.getItemMeta() == null) return;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || !lore.contains(HEAD_LORE_TAG)) return;

        // 머리 소모 (1개 줄이기)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            p.getInventory().setItemInMainHand(null);
        }

        // 버프 부여: 신속 I (5초)
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0)); // 5초, Lv1

        // 체력 즉시 3 하트 회복 (6 HP)
        double maxHp = p.getMaxHealth();
        p.setHealth(Math.min(p.getHealth() + 6.0, maxHp));

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        p.sendMessage(ChatColor.GOLD + "☠ 플레이어 머리를 사용했습니다! " + ChatColor.GREEN + "즉시 3하트 회복 + 신속 5초 효과 부여!");

        event.setCancelled(true);
    }

    // =========================================================
    //  리스폰 처리
    // =========================================================

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!gm.isRunning()) return;
        // 탈락 처리된 플레이어 → handleDeath에서 스펙테이터 예약됨
    }

    // =========================================================
    //  게임 도중 접속한 플레이어 → 스펙테이터 강제 전환
    // =========================================================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!gm.isRunning()) return;

        Player p = event.getPlayer();
        if (!gm.isAlive(p)) {
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                if (p.isOnline()) {
                    p.getInventory().clear();
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage(ChatColor.GRAY + "게임 진행 중입니다. 관전 모드로 입장합니다.");
                }
            }, 5L);
        } else {
            // 탈주 유예 중 들어온 경우
            if (gm.getDisconnectTasks().containsKey(p.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(gm.getDisconnectTasks().remove(p.getUniqueId()));
                gm.getOfflineItems().remove(p.getUniqueId());
                gm.getOfflineLocs().remove(p.getUniqueId());
                Bukkit.broadcastMessage(ChatColor.GREEN + p.getName() + " 님이 1분 내에 재접속했습니다!");
            }
        }
    }

    // =========================================================
    //  게임 도중 퇴장 처리 (탈락 처리)
    // =========================================================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!gm.isRunning()) return;

        Player p = event.getPlayer();
        if (gm.isAlive(p)) {
            // 퇴장 시 즉시 죽이지 않고 인벤토리와 위치 임시 저장 후 1분 대기
            gm.getOfflineItems().put(p.getUniqueId(), p.getInventory().getContents());
            gm.getOfflineLocs().put(p.getUniqueId(), p.getLocation());

            Bukkit.broadcastMessage(ChatColor.RED + p.getName() + " 님이 게임을 나갔습니다! 1분 내에 재접속하지 않으면 탈락합니다.");

            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(gm.getPlugin(), () -> {
                // 1분 후에도 오프라인이면 사망 처리
                gm.getDisconnectTasks().remove(p.getUniqueId());
                
                ItemStack[] items = gm.getOfflineItems().remove(p.getUniqueId());
                Location loc = gm.getOfflineLocs().remove(p.getUniqueId());
                
                if (loc != null) {
                    // 아이템 드롭
                    if (items != null) {
                        for (ItemStack item : items) {
                            if (item != null && item.getType() != Material.AIR) {
                                loc.getWorld().dropItemNaturally(loc, item);
                            }
                        }
                    }
                    // 머리 드롭
                    try {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(Bukkit.getOfflinePlayer(p.getUniqueId()));
                            meta.setDisplayName(ChatColor.YELLOW + p.getName() + "의 머리");
                            meta.setLore(List.of(HEAD_LORE_TAG, HEAD_LORE_USE));
                            skull.setItemMeta(meta);
                        }
                        loc.getWorld().dropItemNaturally(loc, skull);
                    } catch (Exception e) {
                        gm.getPlugin().getLogger().warning("오프라인 플레이어 머리 드롭 중 오류: " + e.getMessage());
                    }
                }

                gm.handleOfflineDeath(p.getUniqueId(), p.getName());
            }, 1200L); // 1분 = 1200틱

            gm.getDisconnectTasks().put(p.getUniqueId(), taskId);
        }
    }

    // =========================================================
    //  조합법 책 우클릭 및 GUI 처리
    // =========================================================

    private static final String RECIPE_GUI_TITLE = ChatColor.GOLD + "조합법 도감";

    @EventHandler
    public void onRecipeBookClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BOOK) return;
        
        if (item.getItemMeta() != null && item.getItemMeta().getDisplayName().contains("조합법")) {
            openRecipeGUI(p);
            event.setCancelled(true);
        }
    }

    private void openRecipeGUI(Player p) {
        // 6*9 = 54칸
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, RECIPE_GUI_TITLE);
        ItemStack bg = RecipeManager.makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        // 등록된 조합법 아이템
        gui.setItem(10, RecipeManager.getApprenticeHelmetForDisplay(p.getUniqueId())); // 견습용 투구
        gui.setItem(11, RecipeManager.getApprenticeSwordForDisplay(p.getUniqueId()));  // 견습용 검
        gui.setItem(12, RecipeManager.getApprenticeBowForDisplay(p.getUniqueId()));    // 견습용 활
        gui.setItem(13, RecipeManager.getCompassForDisplay(p.getUniqueId()));           // 마스터의 나침반
        gui.setItem(14, RecipeManager.getSharpnessBookForDisplay(p.getUniqueId()));     // 날카로움의 책
        gui.setItem(15, RecipeManager.getPowerBookForDisplay(p.getUniqueId()));         // 힘의 책

        // 닫기 버튼 (0-indexed 49 = 6행 가운데)
        ItemStack close = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) { closeMeta.setDisplayName(ChatColor.RED + "닫기"); close.setItemMeta(closeMeta); }
        gui.setItem(49, close);

        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    private ItemStack makeBg() { return RecipeManager.makeBg(); }

    @EventHandler
    public void onRecipeGUIClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getView().getTitle().equals(RECIPE_GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player p = (Player) event.getWhoClicked();
                if (event.getRawSlot() == 10) RecipeManager.openApprenticeRecipeGUI(p);
                else if (event.getRawSlot() == 11) RecipeManager.openApprenticeSwordRecipeGUI(p);
                else if (event.getRawSlot() == 12) RecipeManager.openApprenticeBowRecipeGUI(p);
                else if (event.getRawSlot() == 13) RecipeManager.openMasterCompassRecipeGUI(p);
                else if (event.getRawSlot() == 14) RecipeManager.openSharpnessBookRecipeGUI(p);
                else if (event.getRawSlot() == 15) RecipeManager.openPowerBookRecipeGUI(p);
                else if (event.getRawSlot() == 49) p.closeInventory();
            }
        } else if (event.getView().getTitle().equals(RecipeManager.APPRENTICE_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked());
        } else if (event.getView().getTitle().equals(RecipeManager.APPRENTICE_SWORD_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked());
        } else if (event.getView().getTitle().equals(RecipeManager.APPRENTICE_BOW_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked());
        } else if (event.getView().getTitle().equals(RecipeManager.MASTER_COMPASS_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked());
        } else if (event.getView().getTitle().equals(RecipeManager.SHARPNESS_BOOK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked());
        } else if (event.getView().getTitle().equals(RecipeManager.POWER_BOOK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked());
        }
    }

    // =========================================================
    //  마스터의 나침반 우클릭 추적
    // =========================================================

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack held = event.getItem();
        if (held == null || held.getItemMeta() == null) return;
        if (!held.getItemMeta().getDisplayName().contains(RecipeManager.COMPASS_TAG)) return;

        event.setCancelled(true);
        Player p = event.getPlayer();

        // 게임 중에만 사용 가능
        if (!gm.isRunning()) {
            // 테스트: 게임 밖에서도 파티클만 보이게 (부수지는 않음)
        }

        // 가장 가까운 플레이어 찾기 (spectator 제외, 본인 제외)
        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (Player other : p.getWorld().getPlayers()) {
            if (other.equals(p)) continue;
            if (other.getGameMode() == GameMode.SPECTATOR) continue;
            double dist = other.getLocation().distanceSquared(p.getLocation());
            if (dist < minDist) {
                minDist = dist;
                target = other;
            }
        }

        // 방향 벡터 결정
        final Vector dir;
        if (target != null) {
            dir = target.getLocation().toVector()
                .subtract(p.getLocation().toVector())
                .normalize();
        } else {
            // 추적할 사람이 없으면 바라보는 방향
            dir = p.getLocation().getDirection().normalize();
        }

        // 5블록 입자 트레일 스폰
        Location start = p.getLocation().clone().add(0, 1.2, 0);
        final int steps = 30;
        for (int i = 1; i <= steps; i++) {
            final int idx = i;
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                Location pos = start.clone().add(dir.clone().multiply(idx * (5.0 / steps)));
                p.getWorld().spawnParticle(Particle.FLAME, pos, 1, 0, 0, 0, 0);
                p.getWorld().spawnParticle(Particle.DUST, pos, 2,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 100, 0), 0.8f));
            }, i);
        }

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
        if (target != null) {
            p.sendMessage(ChatColor.GOLD + "화살표가 가장 가까운 플레이어를 가리킵니다!");
        } else {
            p.sendMessage(ChatColor.GRAY + "주변에 추적할 대상이 없습니다.");
        }

        // 게임 중이면 사용 후 나침반 소머 (한 번만 사용)
        if (gm.isRunning()) {
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                // 오프핸드 아이템 슬롯 찾아서 제거
                for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
                    ItemStack it = p.getInventory().getItem(slot);
                    if (it != null && it.getItemMeta() != null
                            && it.getItemMeta().getDisplayName().contains(RecipeManager.COMPASS_TAG)) {
                        p.getInventory().setItem(slot, null);
                        break;
                    }
                }
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                p.getWorld().spawnParticle(Particle.ITEM, p.getLocation().clone().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.05, new ItemStack(Material.COMPASS));
                p.updateInventory();
            }, steps + 2L);
        }
    }

    @EventHandler
    public void onItemPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            Material type = event.getItem().getItemStack().getType();
            if (type == Material.IRON_INGOT || type == Material.REDSTONE_TORCH 
                || type == Material.IRON_SWORD || type == Material.REDSTONE_BLOCK
                || type == Material.STRING || type == Material.REDSTONE || type == Material.COMPASS
                || type == Material.FLINT || type == Material.PAPER || type == Material.BONE) {
                // 픽업 이벤트는 인벤토리 반영 전에 발생하므로 1틱 후 실행
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    if (p.isOnline()) checkRecipeAvailability(p, type);
                }, 1L);
            }
        }
    }

    private void checkRecipeAvailability(Player p, Material type) {
        if (!gm.isRunning()) return;
        
        // 투구 체크 (철괴, 레드스톤 횃불을 주웠을 때)
        if (type == Material.IRON_INGOT || type == Material.REDSTONE_TORCH) {
            if (RecipeManager.getApprenticeCrafts(p.getUniqueId()) < 1) {
                int ironCount = countItems(p.getInventory(), Material.IRON_INGOT);
                int torchCount = countItems(p.getInventory(), Material.REDSTONE_TORCH);
                if (ironCount >= 5 && torchCount >= 1) {
                    notifyRecipe(p, "견습용 투구", "apprentice_helmet");
                }
            }
        }
        
        // 검 체크 (철검, 레드스톤 블록을 주웠을 때)
        if (type == Material.IRON_SWORD || type == Material.REDSTONE_BLOCK) {
            if (RecipeManager.getSwordCrafts(p.getUniqueId()) < 1) {
                int ironCount = countItems(p.getInventory(), Material.IRON_SWORD);
                int blockCount = countItems(p.getInventory(), Material.REDSTONE_BLOCK);
                if (ironCount >= 1 && blockCount >= 2) {
                    notifyRecipe(p, "견습용 검", "apprentice_sword");
                }
            }
        }

        // 활 체크 (실, 레드스톤 횃불을 주웠을 때)
        if (type == Material.STRING || type == Material.REDSTONE_TORCH) {
            if (RecipeManager.getBowCrafts(p.getUniqueId()) < 1) {
                int stringCount = countItems(p.getInventory(), Material.STRING);
                int torchCount = countItems(p.getInventory(), Material.REDSTONE_TORCH);
                if (stringCount >= 3 && torchCount >= 3) {
                    notifyRecipe(p, "견습용 활", "apprentice_bow");
                }
            }
        }

        // 마스터의 나침반 체크
        if (type == Material.REDSTONE || type == Material.REDSTONE_TORCH || type == Material.COMPASS) {
            if (RecipeManager.getCompassCrafts(p.getUniqueId()) < 1) {
                int redstoneCount = countItems(p.getInventory(), Material.REDSTONE);
                int torchCount = countItems(p.getInventory(), Material.REDSTONE_TORCH);
                int compassCount = countItems(p.getInventory(), Material.COMPASS);
                if (redstoneCount >= 7 && torchCount >= 1 && compassCount >= 1) {
                    notifyRecipe(p, "마스터의 나침반", "master_compass");
                }
            }
        }

        // 날카로움의 책 체크
        if (type == Material.FLINT || type == Material.PAPER || type == Material.IRON_SWORD) {
            if (RecipeManager.getSharpnessBookCrafts(p.getUniqueId()) < 4) {
                int flintCount = countItems(p.getInventory(), Material.FLINT);
                int paperCount = countItems(p.getInventory(), Material.PAPER);
                int swordCount = countItems(p.getInventory(), Material.IRON_SWORD);
                if (flintCount >= 1 && paperCount >= 3 && swordCount >= 1) {
                    notifyRecipe(p, "날카로움의 책", "sharpness_book");
                }
            }
        }

        // 힘의 책 체크
        if (type == Material.FLINT || type == Material.PAPER || type == Material.BONE) {
            if (RecipeManager.getPowerBookCrafts(p.getUniqueId()) < 4) {
                int flintCount = countItems(p.getInventory(), Material.FLINT);
                int paperCount = countItems(p.getInventory(), Material.PAPER);
                int boneCount = countItems(p.getInventory(), Material.BONE);
                if (flintCount >= 1 && paperCount >= 3 && boneCount >= 1) {
                    notifyRecipe(p, "힘의 책", "power_book");
                }
            }
        }
    }

    private void notifyRecipe(Player p, String name, String cmd) {
        String key = p.getUniqueId().toString() + "_" + cmd;
        long lastNotified = notifyCooldowns.getOrDefault(key, 0L);
        
        // 동일 조합법에 대해 2초 쿨다운 (중복 알림 방지용)
        if (System.currentTimeMillis() - lastNotified > 2000) {
            notifyCooldowns.put(key, System.currentTimeMillis());
            
            p.sendMessage("");
            p.sendMessage(ChatColor.GOLD + "========== [ 조합 알림 ] ==========");
            p.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "를 제작할 수 있는 재료가 모였습니다!");
            TextComponent btn = new TextComponent("[만들기]");
            btn.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            btn.setBold(true);
            btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/유챔 조합 " + cmd));
            TextComponent msg = new TextComponent("여기를 클릭하세요: ");
            msg.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            msg.addExtra(btn);
            p.spigot().sendMessage(msg);
            p.sendMessage(ChatColor.GOLD + "===================================");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        }
    }

    private int countItems(org.bukkit.inventory.Inventory inv, Material mat) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @EventHandler
    public void onCraftItem(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();

        org.bukkit.inventory.ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getItemMeta() == null) return;
        String displayName = result.getItemMeta().getDisplayName();

        // 커스텀 아이템 조합은 게임 실행 중에만 가능
        if (displayName.contains("견습용")) {
            if (!gm.isRunning()) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "커스텀 조합법은 게임 중에만 사용할 수 있습니다.");
                return;
            }
        }

        if (displayName.contains("견습용 투구")) {
            if (RecipeManager.getApprenticeCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 견습용 투구를 제작했습니다.");
            } else {
                RecipeManager.addApprenticeCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "견습용 투구" + ChatColor.GREEN + "를 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.SWORD_TAG)) {
            if (RecipeManager.getSwordCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 견습용 검을 제작했습니다.");
            } else {
                RecipeManager.addSwordCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "견습용 검" + ChatColor.GREEN + "을 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.BOW_TAG)) {
            if (RecipeManager.getBowCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 견습용 활을 제작했습니다.");
            } else {
                RecipeManager.addBowCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "견습용 활" + ChatColor.GREEN + "을 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.COMPASS_TAG)) {
            if (RecipeManager.getCompassCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 마스터의 나침반을 제작했습니다.");
            } else {
                RecipeManager.addCompassCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.YELLOW + "마스터의 나침반" + ChatColor.GREEN + "을 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.SHARPNESS_BOOK_TAG)) {
            if (RecipeManager.getSharpnessBookCrafts(p.getUniqueId()) >= 4) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 날카로움의 책을 최대치(4/4)로 제작했습니다.");
            } else {
                RecipeManager.addSharpnessBookCraft(p.getUniqueId());
                int crafts = RecipeManager.getSharpnessBookCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.YELLOW + "날카로움의 책" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/4)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.POWER_BOOK_TAG)) {
            if (RecipeManager.getPowerBookCrafts(p.getUniqueId()) >= 4) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 힘의 책을 최대치(4/4)로 제작했습니다.");
            } else {
                RecipeManager.addPowerBookCraft(p.getUniqueId());
                int crafts = RecipeManager.getPowerBookCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.YELLOW + "힘의 책" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/4)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        }
    }

    // 모루 사용 차단 (견습용 검/활)
    @EventHandler
    public void onAnvilUse(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        org.bukkit.inventory.AnvilInventory inv = (org.bukkit.inventory.AnvilInventory) event.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getItemMeta() != null) {
                String dName = item.getItemMeta().getDisplayName();
                if (dName.contains(RecipeManager.SWORD_TAG) || dName.contains(RecipeManager.BOW_TAG)) {
                    event.setResult(null);
                    return;
                }
            }
        }
    }

    // 마법부여대 사용 차단 (견습용 검/활)
    @EventHandler
    public void onEnchant(org.bukkit.event.enchantment.EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getItemMeta() != null) {
            String dName = item.getItemMeta().getDisplayName();
            if (dName.contains(RecipeManager.SWORD_TAG) || dName.contains(RecipeManager.BOW_TAG)) {
                event.setCancelled(true);
                event.getEnchanter().sendMessage(ChatColor.RED + "커스텀 무기는 마법부여대를 사용할 수 없습니다.");
            }
        }
    }
}
