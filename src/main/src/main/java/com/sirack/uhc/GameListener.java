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
                damager.sendMessage(ChatColor.RED + "PVP는 게임 시작 " + gm.getShrinkStartMin() + "분 후부터 가능합니다!");
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
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                p.getInventory().setItemInMainHand(null);
            } else {
                p.getInventory().setItemInOffHand(null);
            }
        }

        // 버프 부여: 신속 I (5초)
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0)); // 5초, Lv1

        // 체력 즉시 3 하트 회복 (6 HP)
        double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null 
                ? p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue() 
                : 20.0;
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
    //  포탈(네더/엔드) 차단
    // =========================================================

    @EventHandler
    public void onPlayerPortal(org.bukkit.event.player.PlayerPortalEvent event) {
        if (!gm.isRunning()) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "UHC 게임 진행 중에는 포탈을 이용할 수 없습니다!");
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
        RecipeManager.openRecipeCatalogGUI(p, page);
    }

    private ItemStack makeBg() { return RecipeManager.makeBg(); }

    @EventHandler
    public void onRecipeGUIClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // 1. 도감 카탈로그 GUI 클릭
        if (title.startsWith(RECIPE_GUI_TITLE)) {
            event.setCancelled(true);
            int page = 1;
            if (title.contains("2페이지")) page = 2;
            else if (title.contains("3페이지")) page = 3;

            int slot = event.getRawSlot();
            if (slot == 49) {
                p.closeInventory();
                return;
            }
            if (slot == 53 && page < 3) {
                RecipeManager.openRecipeCatalogGUI(p, page + 1);
                return;
            }
            if (slot == 45 && page > 1) {
                RecipeManager.openRecipeCatalogGUI(p, page - 1);
                return;
            }

            // 슬롯에 배치된 레시피 찾아서 상세 GUI 오픈
            for (CustomRecipe recipe : RecipeManager.getAllRecipes()) {
                if (recipe.guiPage == page && recipe.guiSlot == slot) {
                    RecipeManager.openRecipeDetailGUI(p, recipe.id);
                    return;
                }
            }
        }
        // 2. 세부 조합법 GUI 뒤로가기 클릭
        else if (title.startsWith(RecipeManager.RECIPE_DETAIL_TITLE_PREFIX)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49) {
                String strippedTitle = ChatColor.stripColor(title.substring(RecipeManager.RECIPE_DETAIL_TITLE_PREFIX.length()).trim());
                CustomRecipe recipe = RecipeManager.getRecipeByTag(strippedTitle);
                int returnPage = recipe != null ? recipe.guiPage : 1;
                RecipeManager.openRecipeCatalogGUI(p, returnPage);
            }
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

        // 게임 중이면 사용 후 나침반 소모 (1개 차감)
        if (gm.isRunning()) {
            org.bukkit.inventory.EquipmentSlot hand = event.getHand();
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                if (!p.isOnline()) return;
                ItemStack inHand = hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                        ? p.getInventory().getItemInOffHand()
                        : p.getInventory().getItemInMainHand();

                if (inHand != null && inHand.getType() == Material.COMPASS
                        && inHand.getItemMeta() != null
                        && inHand.getItemMeta().getDisplayName().contains(RecipeManager.COMPASS_TAG)) {
                    if (inHand.getAmount() > 1) {
                        inHand.setAmount(inHand.getAmount() - 1);
                    } else {
                        if (hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                            p.getInventory().setItemInOffHand(null);
                        } else {
                            p.getInventory().setItemInMainHand(null);
                        }
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

        for (CustomRecipe recipe : RecipeManager.getAllRecipes()) {
            if (recipe.notifyTriggerMaterials.contains(type)) {
                if (RecipeManager.getCrafts(p.getUniqueId(), recipe.id) < recipe.maxCrafts) {
                    boolean ok = true;
                    for (Map.Entry<String, Integer> req : recipe.notifyRequirements.entrySet()) {
                        String reqKey = req.getKey();
                        int requiredCount = req.getValue();
                        int actualCount = 0;

                        if (RecipeManager.isGroupRequirement(reqKey)) {
                            List<Material> groupMats = RecipeManager.getMaterialGroup(reqKey);
                            actualCount = countGroupItems(p.getInventory(), groupMats);
                        } else {
                            try {
                                Material reqMat = Material.valueOf(reqKey);
                                actualCount = countItems(p.getInventory(), reqMat);
                            } catch (Exception e) {
                                actualCount = 0;
                            }
                        }

                        if (actualCount < requiredCount) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        notifyRecipe(p, recipe.tag, recipe.id);
                    }
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

    private int countGroupItems(org.bukkit.inventory.Inventory inv, List<Material> materials) {
        if (materials == null || materials.isEmpty()) return 0;
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && materials.contains(item.getType())) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @EventHandler
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player p = (Player) event.getView().getPlayer();

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

        CustomRecipe matched = null;
        if (!recipeKey.isEmpty()) {
            matched = RecipeManager.getRecipe(recipeKey);
        }
        if (matched == null && !dName.isEmpty()) {
            matched = RecipeManager.getRecipeByTag(ChatColor.stripColor(dName));
        }

        if (matched != null) {
            // 1. 제작 한도 초과 시 결과 제거
            if (RecipeManager.getCrafts(p.getUniqueId(), matched.id) >= matched.maxCrafts) {
                event.getInventory().setResult(null);
                return;
            }

            // 2. 랜덤 결과물을 지원하는 레시피의 경우 결과 아이템을 매번 무작위로 갱신
            if (matched.randomResults != null && !matched.randomResults.isEmpty()) {
                if (result != null && result.getType() != Material.AIR) {
                    event.getInventory().setResult(RecipeManager.createResultItem(matched));
                }
            }
        }
    }

    @EventHandler
    public void onCraftItem(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() == Material.AIR) return;

        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        String recipeKey = "";
        if (recipe instanceof org.bukkit.Keyed keyed) {
            recipeKey = keyed.getKey().getKey();
        }

        String displayName = "";
        if (currentItem.hasItemMeta() && currentItem.getItemMeta().hasDisplayName()) {
            displayName = currentItem.getItemMeta().getDisplayName();
        }

        CustomRecipe matched = null;
        if (!recipeKey.isEmpty()) {
            matched = RecipeManager.getRecipe(recipeKey);
        }
        if (matched == null && !displayName.isEmpty()) {
            matched = RecipeManager.getRecipeByTag(ChatColor.stripColor(displayName));
        }
        if (matched == null && displayName.contains(RecipeManager.FUSION_ARMOR_TAG)) {
            matched = RecipeManager.getRecipe("fusion_armor");
        }

        if (matched != null) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "커스텀 아이템은 쉬프트 클릭으로 여러 개를 동시에 제작할 수 없습니다. 하나씩 제작해 주세요.");
                return;
            }

            int curCrafts = RecipeManager.getCrafts(p.getUniqueId(), matched.id);
            if (curCrafts >= matched.maxCrafts) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "이미 " + matched.tag + "을(를) 제작했습니다.");
                return;
            }

            RecipeManager.addCraft(p.getUniqueId(), matched.id);
            int newCrafts = curCrafts + 1;
            CustomRecipe finalMatched = matched;
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                p.sendMessage(ChatColor.GOLD + "===================================");
                p.sendMessage(ChatColor.GREEN + " 성공적으로 " + ChatColor.AQUA + finalMatched.tag + ChatColor.GREEN + "을(를) 제작했습니다! (" + newCrafts + "/" + finalMatched.maxCrafts + ")");
                p.sendMessage(ChatColor.GOLD + "===================================");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }, 1L);
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

    // ================================================================
    //  화덕 보조 데이터: 설치된 화덕 위치 추적
    // ================================================================
    private final java.util.Set<org.bukkit.Location> blastOvenLocations = new java.util.HashSet<>();

    // 화덕 설치 감지 - 바닐라 FURNACE로 설치되므로 이름 확인
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.FURNACE) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().getDisplayName().contains(RecipeManager.BLAST_OVEN_TAG)) return;
        // 화덕 위치 기록
        blastOvenLocations.add(event.getBlockPlaced().getLocation());
        event.getPlayer().sendMessage(ChatColor.GOLD + "화덕을 설치했습니다. 인벤토리를 열어 아이템을 넣으세요!");
    }

    // 화덕 파괴 감지 - 화덕이 부서지면 위치 목록에서 안전하게 제거
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.FURNACE) {
            blastOvenLocations.remove(event.getBlock().getLocation());
        }
    }

    // 인베토리 열 감지 - 화덕이면 즉시 제련 처리
    @EventHandler
    public void onFurnaceOpen(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!gm.isRunning()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.FURNACE) return;

        org.bukkit.Location loc = event.getClickedBlock().getLocation();
        if (!blastOvenLocations.contains(loc)) return;

        // 화덕이다! 1틱 후 인베토리 확인 후 작동
        Player p = event.getPlayer();
        event.setCancelled(false); // 인베토리 열림 허용
        Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
            // 인베토리가 화덜로 열린 화덕 취급
            if (!(p.getOpenInventory().getTopInventory() instanceof org.bukkit.inventory.FurnaceInventory)) return;
            org.bukkit.inventory.FurnaceInventory furnaceInv = (org.bukkit.inventory.FurnaceInventory) p.getOpenInventory().getTopInventory();

            // 인베토리가 열린 시점에 아이템 확인 (패매트 도듈이 넣는 시점에 실행하는 훅콥을 위해 0.5초 후 확인)
            Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                triggerBlastOven(p, loc, furnaceInv);
            }, 10L); // 0.5초 후 확인
        }, 1L);
    }

    // 화덕에 아이템이 들어갈 때 감지
    @EventHandler
    public void onBlastOvenClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory() instanceof org.bukkit.inventory.FurnaceInventory)) return;

        org.bukkit.inventory.FurnaceInventory furnaceInv = (org.bukkit.inventory.FurnaceInventory) event.getView().getTopInventory();
        org.bukkit.block.Block furnaceBlock = furnaceInv.getLocation() != null ?
            furnaceInv.getLocation().getBlock() : null;
        if (furnaceBlock == null || !blastOvenLocations.contains(furnaceBlock.getLocation())) return;

        Player p = (Player) event.getWhoClicked();
        // 10틱 후에 자동 제련 트리거
        Bukkit.getScheduler().runTaskLater(gm.getPlugin(), () -> {
            if (furnaceBlock.getType() == Material.FURNACE) {
                triggerBlastOven(p, furnaceBlock.getLocation(), furnaceInv);
            }
        }, 5L);
    }

    private static final java.util.Map<Material, Material> SMELT_MAP = new java.util.HashMap<>();
    static {
        // 광석 원석
        SMELT_MAP.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELT_MAP.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        // 생고기
        SMELT_MAP.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        SMELT_MAP.put(Material.BEEF, Material.COOKED_BEEF);
        SMELT_MAP.put(Material.MUTTON, Material.COOKED_MUTTON);
        SMELT_MAP.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        SMELT_MAP.put(Material.RABBIT, Material.COOKED_RABBIT);
        SMELT_MAP.put(Material.COD, Material.COOKED_COD);
        SMELT_MAP.put(Material.SALMON, Material.COOKED_SALMON);
        // 기타 유용한 항목
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.GRAVEL, Material.FLINT);
        SMELT_MAP.put(Material.CLAY_BALL, Material.BRICK);
        SMELT_MAP.put(Material.POTATO, Material.BAKED_POTATO);
    }

    private void triggerBlastOven(Player p, org.bukkit.Location loc, org.bukkit.inventory.FurnaceInventory furnaceInv) {
        if (!gm.isRunning()) return;
        if (loc.getBlock().getType() != Material.FURNACE) return;
        if (!blastOvenLocations.contains(loc)) return;

        // 실라 슬롯(0번) 아이템 확인
        ItemStack inputItem = furnaceInv.getSmelting();
        if (inputItem == null || inputItem.getType() == Material.AIR) return;

        Material inputMat = inputItem.getType();
        Material outputMat = SMELT_MAP.get(inputMat);
        if (outputMat == null) return; // 구울 수 없는 아이템

        int amount = inputItem.getAmount();
        int smeltCount = Math.min(amount, 10); // 최대 10개
        int returnCount = amount - smeltCount; // 나머지는 반납

        // 리전주 시키기 전에 인베토리 닫기
        p.closeInventory();

        // 바닐라 화로 파괴 시 기존 내용물이 자동으로 드랍되는 것을 방지하기 위해 인벤토리 비우기
        ItemStack fuelItem = furnaceInv.getFuel();
        if (fuelItem != null) fuelItem = fuelItem.clone();
        furnaceInv.clear();

        // 화덜 사라지게
        blastOvenLocations.remove(loc);
        loc.getBlock().setType(Material.AIR);

        // 소리 효과
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.2f);
        loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 30, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.05);

        // 구워진 아이템 드랍
        ItemStack output = new ItemStack(outputMat, smeltCount);
        loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), output);

        // 나머지 반납
        if (returnCount > 0) {
            ItemStack returned = new ItemStack(inputMat, returnCount);
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), returned);
            p.sendMessage(ChatColor.YELLOW + "화덕은 최대 10개까지만 구울 수 있습니다. " + returnCount + "개가 반납됩니다.");
        }

        // 남은 연료 드랍
        if (fuelItem != null && fuelItem.getType() != Material.AIR) {
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), fuelItem);
        }

        p.sendMessage(ChatColor.GOLD + "화덕이 " + smeltCount + "개의 " + inputMat.name() + "을(를) 즉시 제련했습니다! 화덕은 파괴되었습니다.");
    }
}
