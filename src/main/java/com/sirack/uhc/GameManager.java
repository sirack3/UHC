package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.FireworkEffect;
import org.bukkit.Color;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * UHC 게임의 핵심 로직을 담당하는 매니저 클래스.
 * 게임 상태(LOBBY → STARTING → RUNNING → ENDED), 스캐터, 보더 축소,
 * 킬 감지, 관전 모드 전환, 승리 처리를 담당한다.
 */
public class GameManager {

    private final JavaPlugin plugin;
    private final ScoreboardManager scoreboardManager;

    /** 현재 게임 상태 */
    public enum GameType {
        SOLO, TEAM
    }

    private GameState state = GameState.LOBBY;
    private GameType type = GameType.SOLO;

    /** 테스트 모드 여부 (1명으로도 시작 가능) */
    private boolean testMode = false;

    /** PVP 활성화 여부 */
    private boolean pvpEnabled = false;

    /** 팀 모드 파트너 맵 (혼자인 경우 value는 null) */
    private final Map<UUID, UUID> teamBuddies = new HashMap<>();
    private final Map<UUID, ChatColor> teamColors = new HashMap<>();
    private final ChatColor[] TEAM_COLORS = {
        ChatColor.AQUA, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.LIGHT_PURPLE,
        ChatColor.GOLD, ChatColor.BLUE, ChatColor.RED, ChatColor.DARK_GREEN
    };

    /** 재접속 유예 시스템 데이터 */
    private final Map<UUID, Integer> disconnectTasks = new HashMap<>();
    private final Map<UUID, ItemStack[]> offlineItems = new HashMap<>();
    private final Map<UUID, Location> offlineLocs = new HashMap<>();

    /** 현재 생존 중인 플레이어 UUID 집합 */
    private final Set<UUID> alivePlayers = new LinkedHashSet<>();

    /** 탈락(관전 모드) 플레이어 UUID 집합 */
    private final Set<UUID> spectators = new LinkedHashSet<>();

    /** 플레이어별 킬 수 */
    private final Map<UUID, Integer> kills = new HashMap<>();

    /** 월드 보더 초기 크기 (지름) */
    private double borderSize;

    /** 로비 복귀 위치 (명령어 입력 지점) */
    private Location lobbyLocation;

    // ---------- Bukkit Task ID ----------
    private int countdownTaskId = -1;
    private int borderShrinkTaskId = -1;
    private int compassTaskId = -1;

    // ---------- config 값 ----------
    private double borderInitialSize;
    private double borderFinalSize;
    private int shrinkStartMin;
    private int shrinkDurationMin;
    private int countdownSeconds;
    private boolean killRewardGoldenApple;
    private double scatterRadius;
    private double scatterMinDistance;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scoreboardManager = new ScoreboardManager(plugin, this);
        loadConfig();
        this.borderSize = borderInitialSize;
    }

    // =========================================================
    //  설정 로드
    // =========================================================

    public void loadConfig() {
        plugin.reloadConfig();
        borderInitialSize  = plugin.getConfig().getDouble("border.initial-size", 2000);
        borderFinalSize    = plugin.getConfig().getDouble("border.final-size", 10);
        shrinkStartMin     = plugin.getConfig().getInt("border.shrink-start-min", 10);
        shrinkDurationMin  = plugin.getConfig().getInt("border.shrink-duration-min", 50);
        countdownSeconds   = plugin.getConfig().getInt("game.countdown-seconds", 10);
        killRewardGoldenApple = plugin.getConfig().getBoolean("game.kill-reward-golden-apple", true);
        scatterRadius      = plugin.getConfig().getDouble("game.scatter-radius", 900);
        scatterMinDistance = plugin.getConfig().getDouble("game.scatter-min-distance", 100);
        borderSize         = borderInitialSize;
    }

    // =========================================================
    //  게임 시작 / 종료
    // =========================================================

    /**
     * 정식 게임 시작 (최소 2명 필요)
     */
    public boolean startGame(GameType type) {
        return startInternal(false, type);
    }

    /**
     * 테스트 게임 시작 (1명으로도 가능, 승리 조건 무시)
     */
    public boolean startTestGame(GameType type) {
        return startInternal(true, type);
    }

    private boolean startInternal(boolean test, GameType type) {
        if (state != GameState.LOBBY) {
            return false; // 이미 게임 중
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        // 팀 모드는 소수파 1명 + 짝 2명 최소 3명 필요. 솔로는 2명 필요.
        int required = test ? 1 : (type == GameType.TEAM ? 3 : 2);
        if (onlinePlayers.size() < required) {
            return false;
        }

        this.testMode = test;
        this.type = type;
        state = GameState.STARTING;

        // 플레이어 데이터 초기화
        alivePlayers.clear();
        spectators.clear();
        kills.clear();
        teamBuddies.clear();
        teamColors.clear();
        disconnectTasks.values().forEach(taskId -> Bukkit.getScheduler().cancelTask(taskId));
        disconnectTasks.clear();
        offlineItems.clear();
        offlineLocs.clear();
        
        for (Player p : onlinePlayers) {
            alivePlayers.add(p.getUniqueId());
            kills.put(p.getUniqueId(), 0);
        }

        // 카운트다운 시작
        startCountdown(onlinePlayers);
        return true;
    }

    /**
     * 게임 강제 종료 (명령어 /유챔 종료 or /유챔 테스트종료)
     */
    public void stopGame() {
        if (state == GameState.LOBBY) return;
        cleanup();
        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "⬛ UHC 게임이 강제 종료되었습니다.");
        resetPlayers();
    }

    /**
     * 플러그인 비활성화 시 강제 정리
     */
    public void forceStop() {
        cleanup();
        resetPlayers();
    }

    // =========================================================
    //  카운트다운
    // =========================================================

    private void startCountdown(List<Player> players) {
        final int[] remaining = {countdownSeconds};

        countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (remaining[0] > 0) {
                // 타이틀 + 사운드
                String msg = ChatColor.YELLOW + "" + ChatColor.BOLD + remaining[0] + "초 후 게임 시작!";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                        ChatColor.GOLD + "" + ChatColor.BOLD + remaining[0],
                        ChatColor.YELLOW + "UHC 시작까지...",
                        0, 25, 5
                    );
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    p.sendMessage(msg);
                }
                remaining[0]--;
            } else {
                // 카운트다운 완료 → 실제 게임 시작 전 맵 생성
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                countdownTaskId = -1;
                prepareWorldAndLaunch(players);
            }
        }, 0L, 20L);
    }

    private void prepareWorldAndLaunch(List<Player> players) {
        Bukkit.broadcastMessage(ChatColor.YELLOW + "⚠ 임시 야생 맵(uhc_temp)을 생성 중입니다...");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "서버가 잠시 멈출 수 있으니 기다려 주세요!");

        // Step 1: 기존 uhc_temp가 로드되어 있으면 플레이어 이동 후 언로드
        World existing = Bukkit.getWorld("uhc_temp");
        java.io.File targetFolder;

        if (existing != null) {
            targetFolder = existing.getWorldFolder();
            World fallback = Bukkit.getWorlds().get(0);
            for (Player p : existing.getPlayers()) {
                p.teleport(fallback.getSpawnLocation());
            }
            boolean unloaded = Bukkit.unloadWorld(existing, false); // 저장 없이 언로드
            plugin.getLogger().info("uhc_temp 언로드 결과: " + unloaded);
        } else {
            targetFolder = new java.io.File(Bukkit.getWorldContainer(), "uhc_temp");
        }

        // Step 2: 2초 대기 (session.lock 해제 여유) 후 비동기 폴더 삭제
        final java.io.File folderToDelete = targetFolder;
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getLogger().info("uhc_temp 폴더 경로: " + folderToDelete.getAbsolutePath());
                plugin.getLogger().info("uhc_temp 폴더 존재 여부: " + folderToDelete.exists());

                if (folderToDelete.exists()) {
                    deleteFolder(folderToDelete);
                    plugin.getLogger().info("uhc_temp 폴더 삭제 완료. 남아있는가: " + folderToDelete.exists());
                }

                // Step 3: 메인 스레드에서 WorldCreator로 직접 생성
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("WorldCreator로 uhc_temp 생성 시작...");
                    long seed = new java.util.Random().nextLong();
                    World uhcWorld = new org.bukkit.WorldCreator("uhc_temp")
                            .environment(World.Environment.NORMAL)
                            .generateStructures(true)
                            .seed(seed)
                            .createWorld();

                    if (uhcWorld != null) {
                        plugin.getLogger().info("uhc_temp 생성 성공! 시드: " + seed);
                        launchGame(players, uhcWorld);
                    } else {
                        plugin.getLogger().severe("uhc_temp 월드 생성에 실패했습니다!");
                        Bukkit.broadcastMessage(ChatColor.RED + "오류: 임시 맵 생성에 실패했습니다. 관리자에게 문의하세요.");
                        state = GameState.LOBBY;
                    }
                });
            })
        , 40L); // 2초 대기
    }

    /** 폴더를 재귀적으로 삭제하는 유틸리티 메서드 */
    private void deleteFolder(java.io.File folder) {
        if (folder.isDirectory()) {
            java.io.File[] files = folder.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    deleteFolder(f);
                }
            }
        }
        if (!folder.delete()) {
            plugin.getLogger().warning("삭제 실패: " + folder.getAbsolutePath());
        }
    }






    // =========================================================
    //  게임 런칭 (스캐터, 보더 설정, 태스크 시작)
    // =========================================================

    private void launchGame(List<Player> players, World uhcWorld) {
        state = GameState.RUNNING;

        try {
            // 새 월드에 게임 룰 적용 (자연 회복, locator_bar 등)
            applyGameRulesOnWorld(uhcWorld, true);
            // 전체 게임 룰도 적용
            applyGameRules(true);

            // 월드 보더 초기화 (게임 시작 시에만 표시되도록)
            WorldBorder border = uhcWorld.getWorldBorder();
            border.setCenter(uhcWorld.getSpawnLocation());
            border.setSize(borderSize);

            // 팀 모드 배정
            if (type == GameType.TEAM) {
                List<Player> shuffled = new ArrayList<>(players);
                Collections.shuffle(shuffled);
                int colorIndex = 0;
                for (int i = 0; i < shuffled.size(); i += 2) {
                    ChatColor color = TEAM_COLORS[colorIndex % TEAM_COLORS.length];
                    colorIndex++;

                    if (i + 1 < shuffled.size()) {
                        Player p1 = shuffled.get(i);
                        Player p2 = shuffled.get(i + 1);
                        teamBuddies.put(p1.getUniqueId(), p2.getUniqueId());
                        teamBuddies.put(p2.getUniqueId(), p1.getUniqueId());
                        teamColors.put(p1.getUniqueId(), color);
                        teamColors.put(p2.getUniqueId(), color);
                        p1.sendMessage(color + "당신의 팀원은 " + ChatColor.WHITE + p2.getName() + color + " 입니다!");
                        p2.sendMessage(color + "당신의 팀원은 " + ChatColor.WHITE + p1.getName() + color + " 입니다!");
                    } else {
                        Player odd = shuffled.get(i);
                        teamBuddies.put(odd.getUniqueId(), null);
                        teamColors.put(odd.getUniqueId(), color);
                        odd.sendMessage(color + "짝이 맞지 않아 혼자 팀이 되었습니다. 대신 " + ChatColor.RED + "체력 2배(80HP) 버프" + color + "가 주어집니다!");
                    }
                }
            } else {
                for (Player p : players) {
                    teamBuddies.put(p.getUniqueId(), null);
                }
            }

            // 플레이어 초기화 (인벤토리, 체력 등)
            for (Player p : players) {
                resetPlayer(p);
            }

            // 랜덤 스캐터
            scatter(players, uhcWorld, border);
        } catch (Exception e) {
            plugin.getLogger().severe("게임 시작 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        // 게임 시작 브로드캐스트
        String modeTag = testMode ? ChatColor.YELLOW + "[테스트] " : "";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage(modeTag + ChatColor.RED + "" + ChatColor.BOLD + "  ⚔ UHC 게임 시작! ⚔");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  · 자연 회복 불가능");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  · 황금 사과 혹은 플레이어 머리로 회복 가능");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  · 월드보더: " + ChatColor.WHITE + (int) borderSize + "블록");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  · " + ChatColor.WHITE + shrinkStartMin + "분 후" + ChatColor.GRAY + " PVP 허용 및 월드보더 축소");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");

        // 게임 시작 버프 (화염 저항 10분, 흡수 5칸(10HP))
        for (Player p : players) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, shrinkStartMin * 60 * 20, 0, false, false, true));
            // 아이콘을 띄우기 위해 흡수 이펙트 추가 (시간은 동일하게)
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, shrinkStartMin * 60 * 20, 0, false, false, true));
            
            // 텔레포트로 인해 흡수량이 씹히는 현상을 방지하기 위해 10틱 뒤에 5칸 덮어쓰기 및 locator_bar 재확인
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    p.setAbsorptionAmount(10.0);
                }
            }, 10L);
        }
        
        // 데이터팩 충돌이나 월드 초기화 시점을 고려해 10틱 뒤에 확실하게 콘솔로 명령어 한 번 더 전송
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule locator_bar false");
        }, 10L);


        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "UHC 시작!",
                ChatColor.GRAY + "생존하라!",
                10, 60, 20
            );
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }

        // PVP 타이머 및 보더 축소 시작은 ScoreboardManager에서 
        // 실시간(System.currentTimeMillis) 기준으로 동기화되어 triggerPvpAndBorder()를 호출합니다.
        pvpEnabled = false;

        // 나침반 업데이트 태스크 시작 (생존자 위치 추적)
        startCompassTask();

        // 스코어보드 시작
        scoreboardManager.start(shrinkStartMin);
    }

    // =========================================================
    //  플레이어 초기화 / 리셋
    // =========================================================

    private void resetPlayer(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setLevel(0);
        p.setExp(0f);
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        
        // 게임 시작 시 최대 체력 설정
        try {
            if (p.getAttribute(Attribute.MAX_HEALTH) != null) {
                double maxHp = 40.0;
                // 팀 모드인데 팀원이 없는 경우 (홀수) 체력 2배 (80HP)
                if (type == GameType.TEAM && teamBuddies.containsKey(p.getUniqueId()) && teamBuddies.get(p.getUniqueId()) == null) {
                    maxHp = 80.0;
                }
                p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
                p.setHealthScaled(false);
                p.setHealth(maxHp);
            } else {
                p.setHealth(20.0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("체력 설정 중 오류: " + e.getMessage());
        }
        // 모든 포션 효과 제거
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    }

    private void resetPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) {
                p.setGameMode(GameMode.SURVIVAL);
            }
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
            // 게임 종료 후 체력 20.0 (한 줄) 복구
            if (p.getAttribute(Attribute.MAX_HEALTH) != null) {
                p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
            }
            p.setHealth(20.0);
            
            // 로비(원래 위치)로 귀환
            if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            } else {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
        }
        // 보더 복구
        for (World w : Bukkit.getWorlds()) {
            w.getWorldBorder().reset();
        }
        applyGameRules(false);
    }

    // =========================================================
    //  랜덤 스캐터
    // =========================================================

    private void scatter(List<Player> players, World world, WorldBorder border) {
        Random rng = new Random();
        double cx = border.getCenter().getX();
        double cz = border.getCenter().getZ();
        
        // 보더 크기의 절반에서 가장자리에 닿지 않도록 90% 정도만 사용
        double radius = Math.min(scatterRadius, (borderSize / 2.0) * 0.9);

        // 플레이어들을 원형으로 균등하게 배치하여 거리를 최대로 벌림
        List<Player> teamLeaders = new ArrayList<>();
        Set<UUID> processed = new HashSet<>();
        
        if (type == GameType.TEAM) {
            for (Player p : players) {
                if (!processed.contains(p.getUniqueId())) {
                    teamLeaders.add(p);
                    processed.add(p.getUniqueId());
                    UUID buddy = teamBuddies.get(p.getUniqueId());
                    if (buddy != null) {
                        processed.add(buddy);
                    }
                }
            }
        } else {
            teamLeaders.addAll(players);
        }

        int n = teamLeaders.size();
        if (n == 0) return;
        
        double startAngle = rng.nextDouble() * 2 * Math.PI;

        for (int i = 0; i < n; i++) {
            Player p = teamLeaders.get(i);
            // 1팀이라도 랜덤 각도로 스폰하여 스캐터를 체감할 수 있게 함
            double angle = startAngle + (2 * Math.PI * i / n);
            
            int x = (int) (cx + Math.cos(angle) * radius);
            int z = (int) (cz + Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            p.teleport(loc);
            p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            
            // 팀원이 있다면 바로 옆에 텔레포트
            if (type == GameType.TEAM) {
                UUID buddyId = teamBuddies.get(p.getUniqueId());
                if (buddyId != null) {
                    Player buddy = Bukkit.getPlayer(buddyId);
                    if (buddy != null && buddy.isOnline()) {
                        Location buddyLoc = new Location(world, x + 1.5, y, z + 0.5);
                        buddy.teleport(buddyLoc);
                        buddy.playSound(buddyLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    }
                }
            }
        }
    }

    // =========================================================
    //  PVP 활성화 및 보더 자동 축소
    // =========================================================

    /**
     * ScoreboardManager에서 실시간 10분이 경과했을 때 호출합니다.
     */
    public void triggerPvpAndBorder() {
        if (state != GameState.RUNNING) return;

        // PVP 활성화 공지
        pvpEnabled = true;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.RED + "⚠ " + ChatColor.BOLD + "PVP가 활성화되었습니다! 이제 서로 공격할 수 있습니다!");
        Bukkit.broadcastMessage("");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            
            // 초기 지급된 버프 강제 제거
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION);
            if (p.getAbsorptionAmount() > 0) {
                p.setAbsorptionAmount(0.0);
            }
        }

        // 보더 축소 시작
        World mainWorld = Bukkit.getWorlds().get(0);
        startBorderShrinkTask(mainWorld);
    }

    private void startBorderShrinkTask(World world) {
        WorldBorder border = world.getWorldBorder();
        
        // 반지름이 5초에 1블록(즉 지름은 5초에 2블록) 축소 => 지름 축소 속도는 1초당 0.4블록.
        // 축소해야 할 양: 현재 보더(지름) - 목표 보더(지름)
        double diff = border.getSize() - borderFinalSize;
        if (diff > 0) {
            long durationSeconds = (long)(diff * 2.5);
            border.setSize(borderFinalSize, durationSeconds);
        } else {
            border.setSize(borderFinalSize);
        }

        Bukkit.broadcastMessage(ChatColor.RED + "⚠ 월드 보더가 축소되기 시작합니다! (반지름 5초당 1블록 축소)");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.8f);
        }

        // 보더 경고 태스크 (30초마다 현재 크기 안내)
        borderShrinkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state != GameState.RUNNING) {
                Bukkit.getScheduler().cancelTask(borderShrinkTaskId);
                return;
            }
            double current = world.getWorldBorder().getSize();
            if (current <= borderFinalSize + 1) {
                Bukkit.getScheduler().cancelTask(borderShrinkTaskId);
                Bukkit.broadcastMessage(ChatColor.RED + "⚠ 월드 보더가 최소 크기(" + (int)borderFinalSize + "블록)에 도달했습니다!");
            }
        }, 600L, 600L); // 30초마다
    }

    // =========================================================
    //  나침반 태스크 (가장 가까운 생존자 방향)
    // =========================================================

    private void startCompassTask() {
        compassTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state != GameState.RUNNING) return;

            for (UUID uid : alivePlayers) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null || !p.isOnline()) continue;

                // 가장 가까운 다른 생존자 찾기
                Player nearest = null;
                double minDist = Double.MAX_VALUE;
                for (UUID otherId : alivePlayers) {
                    if (otherId.equals(uid)) continue;
                    Player other = Bukkit.getPlayer(otherId);
                    if (other == null || !other.isOnline()) continue;
                    if (!other.getWorld().equals(p.getWorld())) continue;
                    double d = p.getLocation().distanceSquared(other.getLocation());
                    if (d < minDist) {
                        minDist = d;
                        nearest = other;
                    }
                }

                if (nearest != null) {
                    p.setCompassTarget(nearest.getLocation());
                }
            }
        }, 20L, 20L);
    }

    // =========================================================
    //  킬 처리
    // =========================================================

    /**
     * 플레이어 사망 처리 (GameListener에서 호출)
     * @param victim   사망한 플레이어
     * @param killer   킬한 플레이어 (없으면 null)
     */
    public void handleDeath(Player victim, Player killer) {
        if (state != GameState.RUNNING) return;
        if (!alivePlayers.contains(victim.getUniqueId())) return;

        // 탈락 처리
        alivePlayers.remove(victim.getUniqueId());
        spectators.add(victim.getUniqueId());

        // 킬 보상 & 킬 카운트
        if (killer != null && !killer.equals(victim)) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);

            if (killRewardGoldenApple) {
                killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
                killer.sendMessage(ChatColor.GOLD + "⚔ 킬 보상: 황금 사과 1개!");
            }

            String killerName = killer.getName();
            String victimName = victim.getName();
            broadcastKill(killerName, victimName);
        } else {
            // 자연사 / 환경
            Bukkit.broadcastMessage(
                ChatColor.GRAY + "☠ " + ChatColor.WHITE + victim.getName()
                + ChatColor.GRAY + "이(가) 탈락했습니다."
            );
        }

        // 관전 모드로 전환 (1틱 후 – 사망 화면이 끝난 뒤)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player v = Bukkit.getPlayer(victim.getUniqueId());
            if (v != null && v.isOnline()) {
                v.setGameMode(GameMode.SPECTATOR);
                v.sendMessage(ChatColor.GRAY + "탈락했습니다. 관전 모드로 전환됩니다.");
            }
        }, 20L);

        // 승리 체크
        checkWin();
    }

    /**
     * 오프라인 플레이어 탈락 처리 (타이머 만료 시)
     */
    public void handleOfflineDeath(UUID victimId, String victimName) {
        if (state != GameState.RUNNING) return;
        if (!alivePlayers.contains(victimId)) return;

        alivePlayers.remove(victimId);
        spectators.add(victimId);

        Bukkit.broadcastMessage(
            ChatColor.GRAY + "☠ " + ChatColor.WHITE + victimName
            + ChatColor.GRAY + "이(가) 접속하지 않아 탈락되었습니다."
        );

        checkWin();
    }

    private void broadcastKill(String killerName, String victimName) {
        Bukkit.broadcastMessage(
            ChatColor.RED + "⚔ " + ChatColor.WHITE + killerName
            + ChatColor.RED + "이(가) " + ChatColor.WHITE + victimName
            + ChatColor.RED + "을(를) 처치했습니다!"
        );
        Bukkit.broadcastMessage(
            ChatColor.GRAY + "  남은 생존자: " + ChatColor.YELLOW + alivePlayers.size() + "명"
        );
    }

    // =========================================================
    //  승리 체크
    // =========================================================

    private void checkWin() {
        if (testMode) return; // 테스트 모드는 승리 처리 없음

        if (alivePlayers.size() == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            celebrateVictory(winner);
            Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 100L);
        } else if (alivePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "모든 플레이어가 탈락했습니다! 무승부!");
            cleanup();
        }
    }

    private void celebrateVictory(Player winner) {
        String name = winner != null ? winner.getName() : "알 수 없음";
        int killCount = winner != null ? kills.getOrDefault(winner.getUniqueId(), 0) : 0;

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "  🏆 UHC 우승자: " + name);
        Bukkit.broadcastMessage(ChatColor.GRAY + "  킬 수: " + killCount);
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                ChatColor.GOLD + "" + ChatColor.BOLD + "🏆 " + name + " 우승!",
                ChatColor.YELLOW + "마지막 생존자",
                20, 100, 20
            );
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1f);
        }

        if (winner != null) {
            spawnFireworks(winner.getLocation());
        }

        state = GameState.ENDED;

        // 5초 후 플레이어 리셋
        Bukkit.getScheduler().runTaskLater(plugin, this::resetPlayers, 100L);
    }

    private void spawnFireworks(Location loc) {
        for (int i = 0; i < 3; i++) {
            final int delay = i * 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.fromRGB(255,165,0), Color.fromRGB(255,255,0), Color.WHITE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withFlicker().withTrail().build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }, delay);
        }
    }

    // =========================================================
    //  게임 규칙
    // =========================================================

    /**
     * 모든 로드된 월드에 게임 룰 적용.
     * uhc_temp 포함 모든 월드에 적용.
     */
    private void applyGameRules(boolean gameOn) {
        for (World w : Bukkit.getWorlds()) {
            applyGameRulesOnWorld(w, gameOn);
        }
    }

    /** 특정 월드에만 게임룰 적용 */
    @SuppressWarnings({"deprecation", "unchecked"})
    private void applyGameRulesOnWorld(World world, boolean gameOn) {
        // GameRule 상수를 문자열로 찾아서 적용 (버전 호환성 극대화)
        String[] rules = {"naturalRegeneration", "showDeathMessages", "announceAdvancements", "locator_bar"};
        boolean[] values = {!gameOn, !gameOn, !gameOn, !gameOn};

        for (int i = 0; i < rules.length; i++) {
            try {
                GameRule<?> rule = GameRule.getByName(rules[i]);
                if (rule != null) {
                    world.setGameRule((GameRule<Object>) rule, values[i]);
                }
            } catch (Exception ignored) {
                // 특정 게임룰이 이 버전에 없으면 무시
            }
        }
    }




    // =========================================================
    //  정리
    // =========================================================

    private void cleanup() {
        state = GameState.LOBBY;
        testMode = false;
        pvpEnabled = false;

        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
        if (borderShrinkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(borderShrinkTaskId);
            borderShrinkTaskId = -1;
        }
        if (compassTaskId != -1) {
            Bukkit.getScheduler().cancelTask(compassTaskId);
            compassTaskId = -1;
        }

        alivePlayers.clear();
        spectators.clear();
        kills.clear();

        // 스코어보드 제거
        scoreboardManager.stop();
    }

    // =========================================================
    //  월드 보더 즉시 설정
    // =========================================================

    /**
     * 보더 크기 설정.
     * 게임 시작 전에는 값만 저장하고(보더 표시는 안 함), 
     * 게임 중일 때는 즉시 월드 보더를 변경한다.
     */
    public void setWorldBorderNow(double size) {
        this.borderSize = size;
        
        if (state == GameState.RUNNING || state == GameState.STARTING) {
            for (World w : Bukkit.getWorlds()) {
                WorldBorder border = w.getWorldBorder();
                border.setCenter(w.getSpawnLocation());
                border.setSize(size);
            }
        }
    }

    // =========================================================
    //  Getters
    // =========================================================

    public GameState getState()                { return state; }
    public boolean isRunning()                 { return state == GameState.RUNNING; }
    public boolean isTestMode()                { return testMode; }
    public boolean isPvpEnabled()              { return pvpEnabled; }
    public Set<UUID> getAlivePlayers()         { return Collections.unmodifiableSet(alivePlayers); }
    public Map<UUID, Integer> getKills()       { return Collections.unmodifiableMap(kills); }
    public double getBorderSize()              { return borderSize; }
    public double getBorderFinalSize()         { return borderFinalSize; }
    public JavaPlugin getPlugin()              { return plugin; }
    public GameType getType()                  { return type; }

    public UUID getTeammate(UUID player) {
        return teamBuddies.get(player);
    }
    
    public ChatColor getTeamColor(UUID player) {
        return teamColors.get(player);
    }
    
    public Map<UUID, Integer> getDisconnectTasks() { return disconnectTasks; }
    public Map<UUID, ItemStack[]> getOfflineItems() { return offlineItems; }
    public Map<UUID, Location> getOfflineLocs() { return offlineLocs; }
    
    public void setLobbyLocation(Location loc) { this.lobbyLocation = loc; }
    public Location getLobbyLocation() { return lobbyLocation; }

    /** 플레이어가 현재 생존자 목록에 있는지 확인 */
    public boolean isAlive(Player p) {
        return alivePlayers.contains(p.getUniqueId());
    }
}
