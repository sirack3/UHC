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
import java.util.UUID;
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
            openRecipeGUI(p, 1);
            event.setCancelled(true);
        }
    }

    private void openRecipeGUI(Player p, int page) {
        // 6*9 = 54칸
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, RECIPE_GUI_TITLE + " - " + page + "페이지");
        ItemStack bg = RecipeManager.makeBg();
        for (int i = 0; i < 54; i++) gui.setItem(i, bg);

        if (page == 1) {
            // 등록된 조합법 아이템
            gui.setItem(10, RecipeManager.getApprenticeHelmetForDisplay(p.getUniqueId())); // 견습용 투구
            gui.setItem(11, RecipeManager.getApprenticeSwordForDisplay(p.getUniqueId()));  // 견습용 검
            gui.setItem(12, RecipeManager.getApprenticeBowForDisplay(p.getUniqueId()));    // 견습용 활
            gui.setItem(13, RecipeManager.getCompassForDisplay(p.getUniqueId()));           // 마스터의 나침반
            gui.setItem(14, RecipeManager.getSharpnessBookForDisplay(p.getUniqueId()));     // 날카로움의 책
            gui.setItem(15, RecipeManager.getPowerBookForDisplay(p.getUniqueId()));         // 힘의 책
            gui.setItem(16, RecipeManager.getDragonSwordForDisplay(p.getUniqueId()));       // 용의 검
            
            gui.setItem(19, RecipeManager.getLeatherSavingForDisplay(p.getUniqueId()));     // 가죽 절약
            gui.setItem(20, RecipeManager.getProtectionBookForDisplay(p.getUniqueId()));    // 보호의 책
            gui.setItem(21, RecipeManager.getDragonArmorForDisplay(p.getUniqueId()));       // 용의 갑옷
            gui.setItem(22, RecipeManager.getNectarForDisplay(p.getUniqueId()));            // 넥타르
            gui.setItem(23, RecipeManager.getBurningArtifactForDisplay(p.getUniqueId()));   // 불타는 아티팩트
            gui.setItem(24, RecipeManager.getDeliciousMealForDisplay(p.getUniqueId()));     // 맛있는 식사
            gui.setItem(25, RecipeManager.getToughnessPotionForDisplay(p.getUniqueId()));   // 강인함의 포션
            
            gui.setItem(28, RecipeManager.getSevenLeagueBootsForDisplay(p.getUniqueId())); // 세븐 리그 부츠
            gui.setItem(29, RecipeManager.getIronPackForDisplay(p.getUniqueId()));          // 아이언 팩
            gui.setItem(30, RecipeManager.getObsidianMixForDisplay(p.getUniqueId()));       // 흑요석 만들기
            gui.setItem(31, RecipeManager.getTarnhelmForDisplay(p.getUniqueId()));          // 탄헬름
            gui.setItem(32, RecipeManager.getPhilosopherPickaxeForDisplay(p.getUniqueId()));// 철학자의 곡괭이
            gui.setItem(33, RecipeManager.getAwakeningPackForDisplay(p.getUniqueId()));     // 깨우침의 팩
            gui.setItem(34, RecipeManager.getLightAnvilForDisplay(p.getUniqueId()));        // 가벼운 모루

            // 다음 페이지 버튼
            ItemStack next = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) { nextMeta.setDisplayName(ChatColor.YELLOW + "다음 페이지"); next.setItemMeta(nextMeta); }
            gui.setItem(53, next);
        } else if (page == 2) {
            gui.setItem(10, RecipeManager.getLightEnchantingTableForDisplay(p.getUniqueId())); // 가벼운 마법 부여대
            gui.setItem(11, RecipeManager.getEvesTemptationForDisplay(p.getUniqueId()));       // 이브의 유혹
            gui.setItem(12, RecipeManager.getFruitOfRecoveryForDisplay(p.getUniqueId()));      // 회복의 과일
            gui.setItem(13, RecipeManager.getLightAppleForDisplay(p.getUniqueId()));           // 가벼운 사과
            gui.setItem(14, RecipeManager.getGoldenHeadForDisplay(p.getUniqueId()));           // 황금 머리

            // 이전 페이지 버튼
            ItemStack prev = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) { prevMeta.setDisplayName(ChatColor.YELLOW + "이전 페이지"); prev.setItemMeta(prevMeta); }
            gui.setItem(45, prev);
        }

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
        if (event.getView().getTitle().startsWith(RECIPE_GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player p = (Player) event.getWhoClicked();
                int page = event.getView().getTitle().contains("2페이지") ? 2 : 1;
                
                if (page == 1) {
                    if (event.getRawSlot() == 10) RecipeManager.openApprenticeRecipeGUI(p);
                    else if (event.getRawSlot() == 11) RecipeManager.openApprenticeSwordRecipeGUI(p);
                    else if (event.getRawSlot() == 12) RecipeManager.openApprenticeBowRecipeGUI(p);
                    else if (event.getRawSlot() == 13) RecipeManager.openMasterCompassRecipeGUI(p);
                    else if (event.getRawSlot() == 14) RecipeManager.openSharpnessBookRecipeGUI(p);
                    else if (event.getRawSlot() == 15) RecipeManager.openPowerBookRecipeGUI(p);
                    else if (event.getRawSlot() == 16) RecipeManager.openDragonSwordRecipeGUI(p);
                    else if (event.getRawSlot() == 19) RecipeManager.openLeatherSavingRecipeGUI(p);
                    else if (event.getRawSlot() == 20) RecipeManager.openProtectionBookRecipeGUI(p);
                    else if (event.getRawSlot() == 21) RecipeManager.openDragonArmorRecipeGUI(p);
                    else if (event.getRawSlot() == 22) RecipeManager.openNectarRecipeGUI(p);
                    else if (event.getRawSlot() == 23) RecipeManager.openBurningArtifactRecipeGUI(p);
                    else if (event.getRawSlot() == 24) RecipeManager.openDeliciousMealRecipeGUI(p);
                    else if (event.getRawSlot() == 25) RecipeManager.openToughnessPotionRecipeGUI(p);
                    else if (event.getRawSlot() == 28) RecipeManager.openSevenLeagueBootsRecipeGUI(p);
                    else if (event.getRawSlot() == 29) RecipeManager.openIronPackRecipeGUI(p);
                    else if (event.getRawSlot() == 30) RecipeManager.openObsidianMixRecipeGUI(p);
                    else if (event.getRawSlot() == 31) RecipeManager.openTarnhelmRecipeGUI(p);
                    else if (event.getRawSlot() == 32) RecipeManager.openPhilosopherPickaxeRecipeGUI(p);
                    else if (event.getRawSlot() == 33) RecipeManager.openAwakeningPackRecipeGUI(p);
                    else if (event.getRawSlot() == 34) RecipeManager.openLightAnvilRecipeGUI(p);
                    else if (event.getRawSlot() == 53) openRecipeGUI(p, 2);
                    else if (event.getRawSlot() == 49) p.closeInventory();
                } else if (page == 2) {
                    if (event.getRawSlot() == 10) RecipeManager.openLightEnchantingTableRecipeGUI(p);
                    else if (event.getRawSlot() == 11) RecipeManager.openEvesTemptationRecipeGUI(p);
                    else if (event.getRawSlot() == 12) RecipeManager.openFruitOfRecoveryRecipeGUI(p);
                    else if (event.getRawSlot() == 13) RecipeManager.openLightAppleRecipeGUI(p);
                    else if (event.getRawSlot() == 14) RecipeManager.openGoldenHeadRecipeGUI(p);
                    else if (event.getRawSlot() == 45) openRecipeGUI(p, 1);
                    else if (event.getRawSlot() == 49) p.closeInventory();
                }
            }
        } else if (event.getView().getTitle().equals(RecipeManager.APPRENTICE_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.APPRENTICE_SWORD_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.APPRENTICE_BOW_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.MASTER_COMPASS_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.SHARPNESS_BOOK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.POWER_BOOK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.DRAGON_SWORD_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.LEATHER_SAVING_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.PROTECTION_BOOK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.DRAGON_ARMOR_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.NECTAR_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.BURNING_ARTIFACT_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.DELICIOUS_MEAL_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.TOUGHNESS_POTION_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.SEVEN_LEAGUE_BOOTS_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.IRON_PACK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.OBSIDIAN_MIX_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.TARNHELM_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.PHILOSOPHER_PICKAXE_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.AWAKENING_PACK_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.LIGHT_ANVIL_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 1);
        } else if (event.getView().getTitle().equals(RecipeManager.LIGHT_ENCHANTING_TABLE_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 2);
        } else if (event.getView().getTitle().equals(RecipeManager.EVES_TEMPTATION_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 2);
        } else if (event.getView().getTitle().equals(RecipeManager.FRUIT_OF_RECOVERY_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 2);
        } else if (event.getView().getTitle().equals(RecipeManager.LIGHT_APPLE_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 2);
        } else if (event.getView().getTitle().equals(RecipeManager.GOLDEN_HEAD_RECIPE_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player)
                openRecipeGUI((Player) event.getWhoClicked(), 2);
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
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                checkRecipeAvailability(p, type);
            }, 1L);
        }
    }

    @EventHandler
    public void onCraftItemForCheck(org.bukkit.event.inventory.CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            org.bukkit.inventory.ItemStack result = event.getRecipe().getResult();
            if (result != null) {
                Material type = result.getType();
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    checkRecipeAvailability(p, type);
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

            // 용의 검 체크
            if (type == Material.BLAZE_POWDER || type == Material.DIAMOND_SWORD || type == Material.OBSIDIAN) {
                if (RecipeManager.getDragonSwordCrafts(p.getUniqueId()) < 1) {
                    int blazeCount = countItems(p.getInventory(), Material.BLAZE_POWDER);
                    int swordCount = countItems(p.getInventory(), Material.DIAMOND_SWORD);
                    int obsidianCount = countItems(p.getInventory(), Material.OBSIDIAN);
                    if (blazeCount >= 2 && swordCount >= 1 && obsidianCount >= 2) {
                        notifyRecipe(p, "용의 검", "dragon_sword");
                    }
                }
            }

            // 가죽 절약 체크
            if (type == Material.LEATHER || type == Material.STICK) {
                if (RecipeManager.getLeatherSavingCrafts(p.getUniqueId()) < 3) {
                    int leatherCount = countItems(p.getInventory(), Material.LEATHER);
                    int stickCount = countItems(p.getInventory(), Material.STICK);
                    if (leatherCount >= 3 && stickCount >= 6) {
                        notifyRecipe(p, "가죽 절약", "leather_saving");
                    }
                }
            }

            // 보호의 책 체크
            if (type == Material.PAPER || type == Material.IRON_INGOT) {
                if (RecipeManager.getProtectionBookCrafts(p.getUniqueId()) < 4) {
                    int paperCount = countItems(p.getInventory(), Material.PAPER);
                    int ironCount = countItems(p.getInventory(), Material.IRON_INGOT);
                    if (paperCount >= 3 && ironCount >= 1) {
                        notifyRecipe(p, "보호의 책", "protection_book");
                    }
                }
            }

            // 용의 갑옷 체크
            if (type == Material.MAGMA_CREAM || type == Material.DIAMOND_CHESTPLATE || type == Material.OBSIDIAN || type == Material.ANVIL) {
                if (RecipeManager.getDragonArmorCrafts(p.getUniqueId()) < 1) {
                    int magmaCount = countItems(p.getInventory(), Material.MAGMA_CREAM);
                    int chestCount = countItems(p.getInventory(), Material.DIAMOND_CHESTPLATE);
                    int obsidianCount = countItems(p.getInventory(), Material.OBSIDIAN);
                    int anvilCount = countItems(p.getInventory(), Material.ANVIL);
                    if (magmaCount >= 1 && chestCount >= 1 && obsidianCount >= 2 && anvilCount >= 1) {
                        notifyRecipe(p, "용의 갑옷", "dragon_armor");
                    }
                }
            }

            // 넥타르 체크
            if (type == Material.EMERALD || type == Material.GOLD_INGOT || type == Material.MELON_SLICE || type == Material.GLASS_BOTTLE) {
                if (RecipeManager.getNectarCrafts(p.getUniqueId()) < 3) {
                    int emeraldCount = countItems(p.getInventory(), Material.EMERALD);
                    int goldCount = countItems(p.getInventory(), Material.GOLD_INGOT);
                    int melonCount = countItems(p.getInventory(), Material.MELON_SLICE);
                    int bottleCount = countItems(p.getInventory(), Material.GLASS_BOTTLE);
                    if (emeraldCount >= 1 && goldCount >= 2 && melonCount >= 1 && bottleCount >= 1) {
                        notifyRecipe(p, "넥타르", "nectar");
                    }
                }
            }

            // 불타는 아티팩트 체크
            if (type == Material.ORANGE_STAINED_GLASS || type == Material.LAVA_BUCKET || type == Material.FIREWORK_ROCKET) {
                if (RecipeManager.getBurningArtifactCrafts(p.getUniqueId()) < 1) {
                    int glassCount = countItems(p.getInventory(), Material.ORANGE_STAINED_GLASS);
                    int lavaCount = countItems(p.getInventory(), Material.LAVA_BUCKET);
                    int fireworkCount = countItems(p.getInventory(), Material.FIREWORK_ROCKET);
                    if (glassCount >= 6 && lavaCount >= 2 && fireworkCount >= 1) {
                        notifyRecipe(p, "불타는 아티팩트", "burning_artifact");
                    }
                }
            }

            // 맛있는 식사 체크
            if (type == Material.PORKCHOP || type == Material.BEEF || type == Material.MUTTON || type == Material.COAL) {
                if (RecipeManager.getDeliciousMealCrafts(p.getUniqueId()) < 3) {
                    int meatCount = countItems(p.getInventory(), Material.PORKCHOP) + countItems(p.getInventory(), Material.BEEF) + countItems(p.getInventory(), Material.MUTTON);
                    int coalCount = countItems(p.getInventory(), Material.COAL);
                    if (meatCount >= 8 && coalCount >= 1) {
                        notifyRecipe(p, "맛있는 식사", "delicious_meal");
                    }
                }
            }

            // 강인함의 포션 체크
            if (type == Material.SLIME_BALL || type == Material.SNOW_BLOCK || type == Material.GLASS_BOTTLE) {
                if (RecipeManager.getToughnessPotionCrafts(p.getUniqueId()) < 3) {
                    int slimeCount = countItems(p.getInventory(), Material.SLIME_BALL);
                    int snowCount = countItems(p.getInventory(), Material.SNOW_BLOCK);
                    int bottleCount = countItems(p.getInventory(), Material.GLASS_BOTTLE);
                    if (slimeCount >= 1 && snowCount >= 1 && bottleCount >= 1) {
                        notifyRecipe(p, "강인함의 포션", "toughness_potion");
                    }
                }
            }

            // 세븐 리그 부츠 체크
            if (type == Material.FEATHER || type == Material.ENDER_PEARL || type == Material.DIAMOND_BOOTS || type == Material.WATER_BUCKET) {
                if (RecipeManager.getSevenLeagueBootsCrafts(p.getUniqueId()) < 1) {
                    int featherCount = countItems(p.getInventory(), Material.FEATHER);
                    int pearlCount = countItems(p.getInventory(), Material.ENDER_PEARL);
                    int bootsCount = countItems(p.getInventory(), Material.DIAMOND_BOOTS);
                    int waterCount = countItems(p.getInventory(), Material.WATER_BUCKET);
                    if (featherCount >= 6 && pearlCount >= 1 && bootsCount >= 1 && waterCount >= 1) {
                        notifyRecipe(p, "세븐 리그 부츠", "seven_league_boots");
                    }
                }
            }

            // 아이언 팩 체크
            if (type == Material.RAW_IRON || type == Material.COAL) {
                if (RecipeManager.getIronPackCrafts(p.getUniqueId()) < 4) {
                    int ironCount = countItems(p.getInventory(), Material.RAW_IRON);
                    int coalCount = countItems(p.getInventory(), Material.COAL);
                    if (ironCount >= 8 && coalCount >= 1) {
                        notifyRecipe(p, "아이언 팩", "iron_pack");
                    }
                }
            }

            // 흑요석 만들기 체크
            if (type == Material.WATER_BUCKET || type == Material.LAVA_BUCKET) {
                if (RecipeManager.getObsidianMixCrafts(p.getUniqueId()) < 3) {
                    int waterCount = countItems(p.getInventory(), Material.WATER_BUCKET);
                    int lavaCount = countItems(p.getInventory(), Material.LAVA_BUCKET);
                    if (waterCount >= 1 && lavaCount >= 1) {
                        notifyRecipe(p, "흑요석 만들기", "obsidian_mix");
                    }
                }
            }

            // 탄헬름 체크
            if (type == Material.DIAMOND || type == Material.IRON_INGOT || type == Material.REDSTONE_BLOCK) {
                if (RecipeManager.getTarnhelmCrafts(p.getUniqueId()) < 3) {
                    int diamondCount = countItems(p.getInventory(), Material.DIAMOND);
                    int ironCount = countItems(p.getInventory(), Material.IRON_INGOT);
                    int redstoneCount = countItems(p.getInventory(), Material.REDSTONE_BLOCK);
                    if (diamondCount >= 4 && ironCount >= 1 && redstoneCount >= 1) {
                        notifyRecipe(p, "탄헬름", "tarnhelm");
                    }
                }
            }

            // 철학자의 곡괭이 체크
            if (type == Material.RAW_IRON || type == Material.RAW_GOLD || type == Material.LAPIS_BLOCK || type == Material.STICK) {
                if (RecipeManager.getPhilosopherPickaxeCrafts(p.getUniqueId()) < 1) {
                    int ironCount = countItems(p.getInventory(), Material.RAW_IRON);
                    int goldCount = countItems(p.getInventory(), Material.RAW_GOLD);
                    int lapisCount = countItems(p.getInventory(), Material.LAPIS_BLOCK);
                    int stickCount = countItems(p.getInventory(), Material.STICK);
                    if (ironCount >= 2 && goldCount >= 1 && lapisCount >= 2 && stickCount >= 2) {
                        notifyRecipe(p, "철학자의 곡괭이", "philosopher_pickaxe");
                    }
                }
            }

            // 깨우침의 팩 체크
            if (type == Material.REDSTONE_BLOCK || type == Material.GLASS_BOTTLE) {
                if (RecipeManager.getAwakeningPackCrafts(p.getUniqueId()) < 3) {
                    int redstoneCount = countItems(p.getInventory(), Material.REDSTONE_BLOCK);
                    int bottleCount = countItems(p.getInventory(), Material.GLASS_BOTTLE);
                    if (redstoneCount >= 4 && bottleCount >= 1) {
                        notifyRecipe(p, "깨우침의 팩", "awakening_pack");
                    }
                }
            }

            // 가벼운 모루 체크
            if (type == Material.IRON_INGOT || type == Material.IRON_BLOCK) {
                if (RecipeManager.getLightAnvilCrafts(p.getUniqueId()) < 3) {
                    int ingotCount = countItems(p.getInventory(), Material.IRON_INGOT);
                    int blockCount = countItems(p.getInventory(), Material.IRON_BLOCK);
                    if (ingotCount >= 6 && blockCount >= 1) {
                        notifyRecipe(p, "가벼운 모루", "light_anvil");
                    }
                }
            }

        // 가벼운 마법 부여대 체크
        if (type == Material.BOOKSHELF || type == Material.OBSIDIAN || type == Material.DIAMOND || type == Material.EXPERIENCE_BOTTLE) {
            if (RecipeManager.getLightEnchantingTableCrafts(p.getUniqueId()) < 3) {
                int bookCount = countItems(p.getInventory(), Material.BOOKSHELF);
                int obsiCount = countItems(p.getInventory(), Material.OBSIDIAN);
                int diaCount = countItems(p.getInventory(), Material.DIAMOND);
                int expCount = countItems(p.getInventory(), Material.EXPERIENCE_BOTTLE);
                if (bookCount >= 1 && obsiCount >= 4 && diaCount >= 1 && expCount >= 1) {
                    notifyRecipe(p, "가벼운 마법 부여대", "light_enchanting_table");
                }
            }
        }

        // 이브의 유혹 체크
        if (type == Material.APPLE || type == Material.BONE_MEAL) {
            if (RecipeManager.getEvesTemptationCrafts(p.getUniqueId()) < 3) {
                int appleCount = countItems(p.getInventory(), Material.APPLE);
                int boneCount = countItems(p.getInventory(), Material.BONE_MEAL);
                if (appleCount >= 1 && boneCount >= 1) {
                    notifyRecipe(p, "이브의 유혹", "eves_temptation");
                }
            }
        }

        // 회복의 과일 체크
        if (type == Material.BONE_MEAL || type == Material.WHEAT_SEEDS || type == Material.APPLE) {
            if (RecipeManager.getFruitOfRecoveryCrafts(p.getUniqueId()) < 3) {
                int boneCount = countItems(p.getInventory(), Material.BONE_MEAL);
                int seedCount = countItems(p.getInventory(), Material.WHEAT_SEEDS);
                int appleCount = countItems(p.getInventory(), Material.APPLE);
                if (boneCount >= 4 && seedCount >= 4 && appleCount >= 1) {
                    notifyRecipe(p, "회복의 과일", "fruit_of_recovery");
                }
            }
        }

        // 가벼운 사과 체크
        if (type == Material.GOLD_INGOT || type == Material.APPLE) {
            if (RecipeManager.getLightAppleCrafts(p.getUniqueId()) < 1) {
                int goldCount = countItems(p.getInventory(), Material.GOLD_INGOT);
                int appleCount = countItems(p.getInventory(), Material.APPLE);
                if (goldCount >= 4 && appleCount >= 1) {
                    notifyRecipe(p, "가벼운 사과", "light_apple");
                }
            }
        }

        // 황금 머리 체크
        if (type == Material.GOLD_INGOT || type == Material.PLAYER_HEAD) {
            if (RecipeManager.getGoldenHeadCrafts(p.getUniqueId()) < 3) {
                int goldCount = countItems(p.getInventory(), Material.GOLD_INGOT);
                int headCount = countItems(p.getInventory(), Material.PLAYER_HEAD);
                if (goldCount >= 8 && headCount >= 1) {
                    notifyRecipe(p, "황금 머리", "golden_head");
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
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player p = (Player) event.getView().getPlayer();

        // recipe key 가져오기 (displayName이 없는 바닐라 아이템 감지용)
        org.bukkit.inventory.Recipe recipe = event.getInventory().getRecipe();
        String recipeKey = "";
        if (recipe instanceof org.bukkit.Keyed keyed) {
            recipeKey = keyed.getKey().getKey();
        }

        org.bukkit.inventory.ItemStack result = event.getInventory().getResult();
        String dName = "";
        if (result != null && result.getItemMeta() != null && result.getItemMeta().hasDisplayName()) {
            dName = result.getItemMeta().getDisplayName();
        }

        if (dName.contains("견습용 투구") && RecipeManager.getApprenticeCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.SWORD_TAG) && RecipeManager.getSwordCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.BOW_TAG) && RecipeManager.getBowCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.COMPASS_TAG) && RecipeManager.getCompassCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.SHARPNESS_BOOK_TAG) && RecipeManager.getSharpnessBookCrafts(p.getUniqueId()) >= 4) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.POWER_BOOK_TAG) && RecipeManager.getPowerBookCrafts(p.getUniqueId()) >= 4) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.DRAGON_SWORD_TAG) && RecipeManager.getDragonSwordCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.PROTECTION_BOOK_TAG) && RecipeManager.getProtectionBookCrafts(p.getUniqueId()) >= 4) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.DRAGON_ARMOR_TAG) && RecipeManager.getDragonArmorCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.NECTAR_TAG) && RecipeManager.getNectarCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.TOUGHNESS_POTION_TAG) && RecipeManager.getToughnessPotionCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if (dName.contains(RecipeManager.SEVEN_LEAGUE_BOOTS_TAG) && RecipeManager.getSevenLeagueBootsCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.LEATHER_SAVING_TAG) || recipeKey.equals("leather_saving")) && RecipeManager.getLeatherSavingCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.BURNING_ARTIFACT_TAG) || recipeKey.equals("burning_artifact")) && RecipeManager.getBurningArtifactCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.DELICIOUS_MEAL_TAG) || recipeKey.equals("delicious_meal")) && RecipeManager.getDeliciousMealCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.IRON_PACK_TAG) || recipeKey.equals("iron_pack")) && RecipeManager.getIronPackCrafts(p.getUniqueId()) >= 4) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.OBSIDIAN_MIX_TAG) || recipeKey.equals("obsidian_mix")) && RecipeManager.getObsidianMixCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.TARNHELM_TAG) || recipeKey.equals("tarnhelm")) && RecipeManager.getTarnhelmCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.PHILOSOPHER_PICKAXE_TAG) || recipeKey.equals("philosopher_pickaxe")) && RecipeManager.getPhilosopherPickaxeCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.AWAKENING_PACK_TAG) || recipeKey.equals("awakening_pack")) && RecipeManager.getAwakeningPackCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.LIGHT_ANVIL_TAG) || recipeKey.equals("light_anvil")) && RecipeManager.getLightAnvilCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.LIGHT_ENCHANTING_TABLE_TAG) || recipeKey.equals("light_enchanting_table")) && RecipeManager.getLightEnchantingTableCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.EVES_TEMPTATION_TAG) || recipeKey.equals("eves_temptation")) && RecipeManager.getEvesTemptationCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.FRUIT_OF_RECOVERY_TAG) || recipeKey.equals("fruit_of_recovery")) && RecipeManager.getFruitOfRecoveryCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.LIGHT_APPLE_TAG) || recipeKey.equals("light_apple")) && RecipeManager.getLightAppleCrafts(p.getUniqueId()) >= 1) {
            event.getInventory().setResult(null);
        } else if ((dName.contains(RecipeManager.GOLDEN_HEAD_TAG) || recipeKey.equals("golden_head")) && RecipeManager.getGoldenHeadCrafts(p.getUniqueId()) >= 3) {
            event.getInventory().setResult(null);
        }
    }


    @EventHandler
    public void onCraftItem(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();

        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        boolean isLeatherSaving = false;
        boolean isBurningArtifact = false;
        boolean isDeliciousMeal = false;
        boolean isIronPack = false;
        boolean isObsidianMix = false;
        boolean isTarnhelm = false;
        boolean isPhilosopherPickaxe = false;
        boolean isAwakeningPack = false;
        boolean isLightAnvilKey = false;
        boolean isLightEnchantingTableKey = false;
        boolean isEvesTemptationKey = false;
        boolean isFruitOfRecoveryKey = false;
        boolean isLightAppleKey = false;
        boolean isGoldenHeadKey = false;
        if (recipe instanceof org.bukkit.Keyed keyed) {
            String key = keyed.getKey().getKey();
            if (key.equals("leather_saving")) {
                isLeatherSaving = true;
            } else if (key.equals("burning_artifact")) {
                isBurningArtifact = true;
            } else if (key.equals("delicious_meal")) {
                isDeliciousMeal = true;
            } else if (key.equals("iron_pack")) {
                isIronPack = true;
            } else if (key.equals("obsidian_mix")) {
                isObsidianMix = true;
            } else if (key.equals("tarnhelm")) {
                isTarnhelm = true;
            } else if (key.equals("philosopher_pickaxe")) {
                isPhilosopherPickaxe = true;
            } else if (key.equals("awakening_pack")) {
                isAwakeningPack = true;
            } else if (key.equals("light_anvil")) {
                isLightAnvilKey = true;
            } else if (key.equals("light_enchanting_table")) {
                isLightEnchantingTableKey = true;
            } else if (key.equals("eves_temptation")) {
                isEvesTemptationKey = true;
            } else if (key.equals("fruit_of_recovery")) {
                isFruitOfRecoveryKey = true;
            } else if (key.equals("light_apple")) {
                isLightAppleKey = true;
            } else if (key.equals("golden_head")) {
                isGoldenHeadKey = true;
            }
        }

        org.bukkit.inventory.ItemStack result = recipe.getResult();
        // 일반 바닐라 아이템(displayName 없음)은 key 기반으로 이미 감지한 뒷에도 처리
        boolean keyMatched = isLeatherSaving || isBurningArtifact || isDeliciousMeal || isIronPack
                          || isObsidianMix || isTarnhelm || isPhilosopherPickaxe || isAwakeningPack
                          || isLightAnvilKey || isLightEnchantingTableKey || isEvesTemptationKey
                          || isFruitOfRecoveryKey || isLightAppleKey || isGoldenHeadKey;
        String displayName = "";
        if (result != null && result.getItemMeta() != null) {
            displayName = result.getItemMeta().hasDisplayName() ? result.getItemMeta().getDisplayName() : "";
        } else if (!keyMatched) {
            return; // 콌스텀 아이템도 아니고 displayName도 없으면 승인
        }

        isTarnhelm = isTarnhelm || displayName.contains(RecipeManager.TARNHELM_TAG);
        isPhilosopherPickaxe = isPhilosopherPickaxe || displayName.contains(RecipeManager.PHILOSOPHER_PICKAXE_TAG);
        isAwakeningPack = isAwakeningPack || displayName.contains(RecipeManager.AWAKENING_PACK_TAG);
        boolean isLightAnvil = isLightAnvilKey || displayName.contains(RecipeManager.LIGHT_ANVIL_TAG);
        boolean isLightEnchantingTable = isLightEnchantingTableKey || displayName.contains(RecipeManager.LIGHT_ENCHANTING_TABLE_TAG);
        boolean isEvesTemptation = isEvesTemptationKey || displayName.contains(RecipeManager.EVES_TEMPTATION_TAG);
        boolean isFruitOfRecovery = isFruitOfRecoveryKey || displayName.contains(RecipeManager.FRUIT_OF_RECOVERY_TAG);
        boolean isLightApple = isLightAppleKey || displayName.contains(RecipeManager.LIGHT_APPLE_TAG);
        boolean isGoldenHead = isGoldenHeadKey || displayName.contains(RecipeManager.GOLDEN_HEAD_TAG);

        boolean isCustom = isLeatherSaving || isBurningArtifact || isDeliciousMeal || isIronPack || isObsidianMix
                        || isTarnhelm || isPhilosopherPickaxe || isAwakeningPack
                        || isLightAnvil || isLightEnchantingTable || isEvesTemptation
                        || isFruitOfRecovery || isLightApple || isGoldenHead
                        || displayName.contains("견습용") 
                        || displayName.contains(RecipeManager.COMPASS_TAG) 
                        || displayName.contains(RecipeManager.SHARPNESS_BOOK_TAG) 
                        || displayName.contains(RecipeManager.POWER_BOOK_TAG)
                        || displayName.contains(RecipeManager.DRAGON_SWORD_TAG)
                        || displayName.contains(RecipeManager.PROTECTION_BOOK_TAG)
                        || displayName.contains(RecipeManager.DRAGON_ARMOR_TAG)
                        || displayName.contains(RecipeManager.NECTAR_TAG)
                        || displayName.contains(RecipeManager.TOUGHNESS_POTION_TAG)
                        || displayName.contains(RecipeManager.SEVEN_LEAGUE_BOOTS_TAG)
                        || displayName.contains(RecipeManager.TARNHELM_TAG)
                        || displayName.contains(RecipeManager.PHILOSOPHER_PICKAXE_TAG)
                        || displayName.contains(RecipeManager.AWAKENING_PACK_TAG)
                        || displayName.contains(RecipeManager.LIGHT_ANVIL_TAG)
                        || displayName.contains(RecipeManager.LIGHT_ENCHANTING_TABLE_TAG)
                        || displayName.contains(RecipeManager.EVES_TEMPTATION_TAG)
                        || displayName.contains(RecipeManager.FRUIT_OF_RECOVERY_TAG)
                        || displayName.contains(RecipeManager.LIGHT_APPLE_TAG)
                        || displayName.contains(RecipeManager.GOLDEN_HEAD_TAG);

        if (isCustom) {
            // 커스텀 아이템 조합은 게임 실행 중에만 가능
            if (!gm.isRunning()) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "커스텀 조합법은 게임 중에만 사용할 수 있습니다.");
                return;
            }
            // 커스텀 아이템은 쉬프트 클릭(한 번에 여러 개 제작) 차단
            if (event.isShiftClick()) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "커스텀 아이템은 쉬프트 클릭으로 여러 개를 동시에 제작할 수 없습니다. 하나씩 제작해 주세요.");
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
        } else if (displayName.contains(RecipeManager.DRAGON_SWORD_TAG)) {
            if (RecipeManager.getDragonSwordCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 용의 검을 제작했습니다.");
            } else {
                RecipeManager.addDragonSwordCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.LIGHT_PURPLE + "용의 검" + ChatColor.GREEN + "을 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isLeatherSaving) {
            if (RecipeManager.getLeatherSavingCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 가죽 절약을 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addLeatherSavingCraft(p.getUniqueId());
                int crafts = RecipeManager.getLeatherSavingCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.GREEN + "가죽 절약" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.PROTECTION_BOOK_TAG)) {
            if (RecipeManager.getProtectionBookCrafts(p.getUniqueId()) >= 4) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 보호의 책을 최대치(4/4)로 제작했습니다.");
            } else {
                RecipeManager.addProtectionBookCraft(p.getUniqueId());
                int crafts = RecipeManager.getProtectionBookCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "보호의 책" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/4)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.DRAGON_ARMOR_TAG)) {
            if (RecipeManager.getDragonArmorCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 용의 갑옷을 제작했습니다.");
            } else {
                RecipeManager.addDragonArmorCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.DARK_PURPLE + "용의 갑옷" + ChatColor.GREEN + "을 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.NECTAR_TAG)) {
            if (RecipeManager.getNectarCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 넥타르를 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addNectarCraft(p.getUniqueId());
                int crafts = RecipeManager.getNectarCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.YELLOW + "넥타르" + ChatColor.GREEN + "를 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.TOUGHNESS_POTION_TAG)) {
            if (RecipeManager.getToughnessPotionCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 강인함의 포션을 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addToughnessPotionCraft(p.getUniqueId());
                int crafts = RecipeManager.getToughnessPotionCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.LIGHT_PURPLE + "강인함의 포션" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (displayName.contains(RecipeManager.SEVEN_LEAGUE_BOOTS_TAG)) {
            if (RecipeManager.getSevenLeagueBootsCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 세븐 리그 부츠를 제작했습니다.");
            } else {
                RecipeManager.addSevenLeagueBootsCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "세븐 리그 부츠" + ChatColor.GREEN + "를 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isIronPack) {
            if (RecipeManager.getIronPackCrafts(p.getUniqueId()) >= 4) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 아이언 팩을 최대치(4/4)로 제작했습니다.");
            } else {
                RecipeManager.addIronPackCraft(p.getUniqueId());
                int crafts = RecipeManager.getIronPackCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.WHITE + "아이언 팩" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/4)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isObsidianMix) {
            if (RecipeManager.getObsidianMixCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 흑요석 만들기를 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addObsidianMixCraft(p.getUniqueId());
                int crafts = RecipeManager.getObsidianMixCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.DARK_PURPLE + "흑요석 만들기" + ChatColor.GREEN + "를 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isTarnhelm || displayName.contains(RecipeManager.TARNHELM_TAG)) {
            if (RecipeManager.getTarnhelmCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 탄헬름을 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addTarnhelmCraft(p.getUniqueId());
                int crafts = RecipeManager.getTarnhelmCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "탄헬름" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isPhilosopherPickaxe || displayName.contains(RecipeManager.PHILOSOPHER_PICKAXE_TAG)) {
            if (RecipeManager.getPhilosopherPickaxeCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 철학자의 곡괭이를 제작했습니다.");
            } else {
                RecipeManager.addPhilosopherPickaxeCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.YELLOW + "철학자의 곡괭이" + ChatColor.GREEN + "를 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isAwakeningPack || displayName.contains(RecipeManager.AWAKENING_PACK_TAG)) {
            if (RecipeManager.getAwakeningPackCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 깨우침의 팩을 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addAwakeningPackCraft(p.getUniqueId());
                int crafts = RecipeManager.getAwakeningPackCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.LIGHT_PURPLE + "깨우침의 팩" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isLightAnvil || displayName.contains(RecipeManager.LIGHT_ANVIL_TAG)) {
            if (RecipeManager.getLightAnvilCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 가벼운 모루를 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addLightAnvilCraft(p.getUniqueId());
                int crafts = RecipeManager.getLightAnvilCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.WHITE + "가벼운 모루" + ChatColor.GREEN + "를 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isLightEnchantingTable || displayName.contains(RecipeManager.LIGHT_ENCHANTING_TABLE_TAG)) {
            if (RecipeManager.getLightEnchantingTableCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 가벼운 마법 부여대를 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addLightEnchantingTableCraft(p.getUniqueId());
                int crafts = RecipeManager.getLightEnchantingTableCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.LIGHT_PURPLE + "가벼운 마법 부여대" + ChatColor.GREEN + "를 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isEvesTemptation || displayName.contains(RecipeManager.EVES_TEMPTATION_TAG)) {
            if (RecipeManager.getEvesTemptationCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 이브의 유혹을 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addEvesTemptationCraft(p.getUniqueId());
                int crafts = RecipeManager.getEvesTemptationCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.RED + "이브의 유혹" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isFruitOfRecovery || displayName.contains(RecipeManager.FRUIT_OF_RECOVERY_TAG)) {
            if (RecipeManager.getFruitOfRecoveryCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 회복의 과일을 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addFruitOfRecoveryCraft(p.getUniqueId());
                int crafts = RecipeManager.getFruitOfRecoveryCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + "회복의 과일" + ChatColor.GREEN + "을 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isLightApple || displayName.contains(RecipeManager.LIGHT_APPLE_TAG)) {
            if (RecipeManager.getLightAppleCrafts(p.getUniqueId()) >= 1) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 가벼운 사과를 제작했습니다.");
            } else {
                RecipeManager.addLightAppleCraft(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.YELLOW + "가벼운 사과" + ChatColor.GREEN + "를 제작했습니다! (1/1)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }, 1L);
            }
        } else if (isGoldenHead || displayName.contains(RecipeManager.GOLDEN_HEAD_TAG)) {
            if (RecipeManager.getGoldenHeadCrafts(p.getUniqueId()) >= 3) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 황금 머리를 최대치(3/3)로 제작했습니다.");
            } else {
                RecipeManager.addGoldenHeadCraft(p.getUniqueId());
                int crafts = RecipeManager.getGoldenHeadCrafts(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.GOLD + "황금 머리" + ChatColor.GREEN + "를 제작했습니다! (" + crafts + "/3)");
                    p.sendMessage(ChatColor.GOLD + "===================================");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    
                    // 팀원이 있을 경우, 팀원에게도 황금 머리 지급
                    UUID teammateId = gm.getTeammate(p.getUniqueId());
                    if (teammateId != null) {
                        Player member = Bukkit.getPlayer(teammateId);
                        if (member != null && member.isOnline()) {
                            member.getInventory().addItem(RecipeManager.getGoldenHead());
                            member.sendMessage(ChatColor.GOLD + "팀원 " + p.getName() + "님이 " + ChatColor.GOLD + "황금 머리" + ChatColor.GOLD + "를 제작하여 하나를 받았습니다!");
                        }
                    }
                }, 1L);
            }
        }
    }

    // 황금 머리 사용 (우클릭 발동)
    @EventHandler
    public void onGoldenHeadUse(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!gm.isRunning()) return;
        Player p = event.getPlayer();
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            org.bukkit.inventory.ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.PLAYER_HEAD) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains(RecipeManager.GOLDEN_HEAD_TAG)) {
                    event.setCancelled(true); // 머리 블록이 설치되는 것을 방지
                    
                    // 흡수 1 (2분), 재생 2 (5초) 지급
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 2400, 0));
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 100, 1));
                    
                    // 소리 재생
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
                    p.sendMessage(ChatColor.GOLD + "황금 머리를 사용하여 " + ChatColor.AQUA + "흡수" + ChatColor.GOLD + "와 " + ChatColor.AQUA + "재생" + ChatColor.GOLD + " 버프를 얻었습니다!");
                    
                    // 아이템 1개 차감
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                            p.getInventory().setItemInMainHand(null);
                        } else {
                            p.getInventory().setItemInOffHand(null);
                        }
                    }
                }
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
