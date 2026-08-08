package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
}
