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

    /** 현재 활성화된 UHC 임시 월드 이름 */
    private String currentUhcWorldName = "uhc_temp";

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
    private double playerHealth;
    private double teamSoloBuffHealth;
    private double initialAbsorption;
    private int bowTier2Min;
    private int swordTier2Min;
    private int bowTier3Min;

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
        shrinkDurationMin  = plugin.getConfig().getInt("border.shrink-duration-min", 30);
        countdownSeconds   = plugin.getConfig().getInt("game.countdown-seconds", 10);
        killRewardGoldenApple = plugin.getConfig().getBoolean("game.kill-reward-golden-apple", true);
        scatterRadius      = plugin.getConfig().getDouble("game.scatter-radius", 900);
        scatterMinDistance = plugin.getConfig().getDouble("game.scatter-min-distance", 100);
        playerHealth       = plugin.getConfig().getDouble("game.player-health", 40.0);
        teamSoloBuffHealth = plugin.getConfig().getDouble("game.team-solo-buff-health", 80.0);
        initialAbsorption  = plugin.getConfig().getDouble("game.initial-absorption", 10.0);
        bowTier2Min        = plugin.getConfig().getInt("game.upgrades.bow-tier2-min", 15);
        swordTier2Min      = plugin.getConfig().getInt("game.upgrades.sword-tier2-min", 25);
        bowTier3Min        = plugin.getConfig().getInt("game.upgrades.bow-tier3-min", 25);
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
        Bukkit.broadcastMessage(ChatColor.YELLOW + "⚠ 임시 야생 맵을 생성 중입니다...");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "서버가 잠시 멈출 수 있으니 기다려 주세요!");

        // Step 1: 현재 로드된 모든 UHC 관련 임시 월드 언로드
        World fallback = Bukkit.getWorlds().get(0);
        for (World w : Bukkit.getWorlds()) {
            if (w.getName().startsWith("uhc_game_") || w.getName().startsWith("uhc_temp")) {
                w.setAutoSave(false);
                for (Player p : w.getPlayers()) {
                    p.teleport(fallback.getSpawnLocation());
                }
                boolean unloaded = Bukkit.unloadWorld(w, false); // 저장 없이 언로드
                plugin.getLogger().info("기존 임시 월드 " + w.getName() + " 언로드 결과: " + unloaded);
            }
        }
        System.gc(); // Windows 리전 파일 핸들 해제 유도

        // 매 게임마다 항상 고유한 신규 월드 이름 생성 (이전 맵 재로드 100% 원천 차단)
        long now = System.currentTimeMillis();
        final String targetWorldName = "uhc_game_" + now;
        currentUhcWorldName = targetWorldName;

        // Step 2: 1초 대기 후 비동기로 오래된 이전 임시 월드 폴더 정리 시도
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                java.io.File worldContainer = Bukkit.getWorldContainer();
                java.io.File[] oldWorlds = worldContainer.listFiles((dir, name) ->
                        (name.startsWith("uhc_game_") && !name.equals(targetWorldName)) || name.startsWith("uhc_temp"));
                if (oldWorlds != null) {
                    for (java.io.File old : oldWorlds) {
                        deleteFolder(old);
                    }
                }

                // Step 3: 메인 스레드에서 WorldCreator로 신규 월드 생성
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("WorldCreator로 완전 신규 월드 '" + targetWorldName + "' 생성 시작...");
                    long seed = new java.util.Random().nextLong();
                    World uhcWorld = new org.bukkit.WorldCreator(targetWorldName)
                            .environment(World.Environment.NORMAL)
                            .generateStructures(true)
                            .seed(seed)
                            .createWorld();

                    if (uhcWorld != null) {
                        currentUhcWorldName = targetWorldName;
                        plugin.getLogger().info(targetWorldName + " 생성 성공! 신규 시드: " + seed);
                        launchGame(players, uhcWorld);
                    } else {
                        plugin.getLogger().severe(targetWorldName + " 월드 생성에 실패했습니다!");
                        Bukkit.broadcastMessage(ChatColor.RED + "오류: 임시 맵 생성에 실패했습니다. 관리자에게 문의하세요.");
                        state = GameState.LOBBY;
                    }
                });
            })
        , 20L); // 1초 대기
    }

    /** 폴더를 재귀적으로 안전하게 삭제하는 유틸리티 메서드 */
    private void deleteFolder(java.io.File folder) {
        if (!folder.exists()) return;
        if (folder.isDirectory()) {
            java.io.File[] files = folder.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    deleteFolder(f);
                }
            }
        }
        folder.setWritable(true);
        if (!folder.delete()) {
            plugin.getLogger().warning("삭제 실패 (파일 락 잠김): " + folder.getAbsolutePath());
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

        // 게임 시작 버프 (화염 저항, 흡수)
        for (Player p : players) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, shrinkStartMin * 60 * 20, 0, false, false, true));
            // 아이콘을 띄우기 위해 흡수 이펙트 추가 (시간은 동일하게)
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, shrinkStartMin * 60 * 20, 0, false, false, true));
            
            // 텔레포트로 인해 흡수량이 씹히는 현상을 방지하기 위해 10틱 뒤에 설정값 덮어쓰기
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    p.setAbsorptionAmount(initialAbsorption);
                }
            }, 10L);
        }

        // 플레이어 위치 추적 바(locator_bar) 비활성화 (위치 노출 원천 차단)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule locator_bar false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule locatorBar false");
            } catch (Exception ignored) {}
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

        // 기본 아이템 지급 (돌 도구 + 효율 2, 검은 날카로움 1, 조합법 책)
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(Material.STONE_SWORD);
        sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
        
        org.bukkit.inventory.ItemStack pickaxe = new org.bukkit.inventory.ItemStack(Material.STONE_PICKAXE);
        pickaxe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);
        
        org.bukkit.inventory.ItemStack axe = new org.bukkit.inventory.ItemStack(Material.STONE_AXE);
        axe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);
        
        org.bukkit.inventory.ItemStack shovel = new org.bukkit.inventory.ItemStack(Material.STONE_SHOVEL);
        shovel.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);

        org.bukkit.inventory.ItemStack recipeBook = new org.bukkit.inventory.ItemStack(Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta bookMeta = recipeBook.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.GOLD + "조합법");
            recipeBook.setItemMeta(bookMeta);
        }

        p.getInventory().addItem(sword, pickaxe, axe, shovel, recipeBook);
        
        // 게임 시작 시 최대 체력 설정
        try {
            if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                double maxHp = playerHealth;
                // 팀 모드인데 팀원이 없는 경우 (홀수) 체력 버프
                if (type == GameType.TEAM && teamBuddies.containsKey(p.getUniqueId()) && teamBuddies.get(p.getUniqueId()) == null) {
                    maxHp = teamSoloBuffHealth;
                }
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHp);
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
            if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
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
        RecipeManager.resetCrafts(); // 커스텀 아이템 제작 횟수 초기화
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
            
            // 팀원이 있다면 바로 옆에 텔레포트 (안전한 지상 Y좌표 계산)
            if (type == GameType.TEAM) {
                UUID buddyId = teamBuddies.get(p.getUniqueId());
                if (buddyId != null) {
                    Player buddy = Bukkit.getPlayer(buddyId);
                    if (buddy != null && buddy.isOnline()) {
                        int bx = x + 2;
                        int bz = z;
                        int by = world.getHighestBlockYAt(bx, bz) + 1;
                        Location buddyLoc = new Location(world, bx + 0.5, by, bz + 0.5);
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
            // 과도기 검/활 개량
            upgradeApprenticeSword(p, 1);
            upgradeApprenticeBow(p, 1);
        }

        // 무기 업그레이드 스케줄링 (설정 시간 기반)
        if (bowTier2Min > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state != GameState.RUNNING) return;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    upgradeApprenticeBow(online, 2);
                }
            }, (long) bowTier2Min * 60L * 20L);
        }

        if (swordTier2Min > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state != GameState.RUNNING) return;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    upgradeApprenticeSword(online, 2);
                }
            }, (long) swordTier2Min * 60L * 20L);
        }

        if (bowTier3Min > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state != GameState.RUNNING) return;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    upgradeApprenticeBow(online, 3);
                }
            }, (long) bowTier3Min * 60L * 20L);
        }

        // 보더 축소 시작 (UHC 월드에 적용)
        World uhcWorld = getUhcWorld();
        startBorderShrinkTask(uhcWorld);
    }

    private void upgradeApprenticeSword(Player p, int level) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getItemMeta() == null) continue;
            if (item.getItemMeta().getDisplayName().contains(RecipeManager.SWORD_TAG)) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, level);
                String lvlName = level == 1
                    ? ChatColor.YELLOW + "날카로움 I"
                    : ChatColor.GOLD + "날카로움 II";
                p.sendMessage(ChatColor.AQUA + "⚡ 견습용 검에 " + lvlName
                    + ChatColor.WHITE + "이 부여되었습니다!");
                p.updateInventory();
            }
        }
    }

    private void upgradeApprenticeBow(Player p, int level) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getItemMeta() == null) continue;
            if (item.getItemMeta().getDisplayName().contains(RecipeManager.BOW_TAG)) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, level);
                String lvlName = level == 1
                    ? ChatColor.YELLOW + "힘 I"
                    : (level == 2 ? ChatColor.GOLD + "힘 II" : ChatColor.RED + "힘 III");
                p.sendMessage(ChatColor.AQUA + "⚡ 견습용 활에 " + lvlName
                    + ChatColor.WHITE + "이 부여되었습니다!");
                p.updateInventory();
            }
        }
    }

    private void startBorderShrinkTask(World world) {
        WorldBorder border = world.getWorldBorder();
        
        // 보더 축소 시간: 설정된 shrinkDurationMin(분) 동안 borderFinalSize까지 축소
        double diff = border.getSize() - borderFinalSize;
        if (diff > 0) {
            long durationSeconds = (long) shrinkDurationMin * 60L;
            border.setSize(borderFinalSize, durationSeconds);
        } else {
            border.setSize(borderFinalSize);
        }

        Bukkit.broadcastMessage(ChatColor.RED + "⚠ 월드 보더가 축소되기 시작합니다! (" + shrinkDurationMin + "분 동안 축소)");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.8f);
        }

        // 보더 경고 태스크 (30초마다 현재 크기 안내) - UHC 월드 기준
        final String worldName = world.getName();
        borderShrinkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state != GameState.RUNNING) {
                Bukkit.getScheduler().cancelTask(borderShrinkTaskId);
                return;
            }
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                Bukkit.getScheduler().cancelTask(borderShrinkTaskId);
                return;
            }
            double current = w.getWorldBorder().getSize();
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

        // 사망한 위치 저장
        org.bukkit.Location deathLoc = victim.getLocation();

        // 즉시 부활 및 관전 모드로 전환 (10틱 후)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player v = Bukkit.getPlayer(victim.getUniqueId());
            if (v != null && v.isOnline()) {
                if (v.isDead()) {
                    v.spigot().respawn();
                }
                v.teleport(deathLoc);
                v.setGameMode(GameMode.SPECTATOR);
                v.sendMessage(ChatColor.GRAY + "탈락했습니다. 사망한 위치에서 관전 모드로 전환됩니다.");
            }
        }, 10L);

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

        if (alivePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "모든 플레이어가 탈락했습니다! 무승부!");
            cleanup();
            return;
        }

        if (type == GameType.TEAM) {
            List<UUID> aliveList = new ArrayList<>(alivePlayers);
            boolean singleTeamRemaining = false;

            if (aliveList.size() == 1) {
                singleTeamRemaining = true;
            } else if (aliveList.size() == 2) {
                UUID p1 = aliveList.get(0);
                UUID p2 = aliveList.get(1);
                UUID buddy1 = teamBuddies.get(p1);
                if (buddy1 != null && buddy1.equals(p2)) {
                    singleTeamRemaining = true;
                }
            }

            if (singleTeamRemaining) {
                List<Player> teamWinners = new ArrayList<>();
                for (UUID uid : aliveList) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null) teamWinners.add(p);
                }
                celebrateTeamVictory(teamWinners);
                Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 100L);
            }
        } else {
            if (alivePlayers.size() == 1) {
                UUID winnerId = alivePlayers.iterator().next();
                Player winner = Bukkit.getPlayer(winnerId);
                celebrateVictory(winner);
                Bukkit.getScheduler().runTaskLater(plugin, this::cleanup, 100L);
            }
        }
    }

    private void celebrateTeamVictory(List<Player> winners) {
        if (winners.isEmpty()) return;

        ChatColor teamColor = teamColors.getOrDefault(winners.get(0).getUniqueId(), ChatColor.GOLD);
        StringBuilder teamNames = new StringBuilder();
        int totalKills = 0;

        for (int i = 0; i < winners.size(); i++) {
            Player p = winners.get(i);
            teamNames.append(p.getName());
            if (i < winners.size() - 1) teamNames.append(" & ");
            totalKills += kills.getOrDefault(p.getUniqueId(), 0);
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage(teamColor + "" + ChatColor.BOLD + "  🏆 UHC 우승 팀: " + teamNames);
        for (Player p : winners) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "  · " + p.getName() + " 킬 수: " + kills.getOrDefault(p.getUniqueId(), 0));
        }
        Bukkit.broadcastMessage(ChatColor.GRAY + "  총 킬 수: " + totalKills);
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                teamColor + "" + ChatColor.BOLD + "🏆 " + teamNames + " 우승!",
                ChatColor.YELLOW + "최후의 생존 팀",
                20, 100, 20
            );
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1f);
        }

        for (Player p : winners) {
            spawnFireworks(p.getLocation());
        }

        state = GameState.ENDED;
        Bukkit.getScheduler().runTaskLater(plugin, this::resetPlayers, 100L);
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
        // locator_bar, locatorBar: 최신 마인크래프트 및 데이터팩의 플레이어 위치 추적 HUD를 UHC 중 강제 차단
        String[] rules = {"naturalRegeneration", "showDeathMessages", "announceAdvancements", "locator_bar", "locatorBar"};
        boolean[] values = {!gameOn, !gameOn, false, !gameOn, !gameOn};

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

        // 오프라인 유예 태스크 및 데이터 정리
        disconnectTasks.values().forEach(taskId -> Bukkit.getScheduler().cancelTask(taskId));
        disconnectTasks.clear();
        offlineItems.clear();
        offlineLocs.clear();

        // 스코어보드 제거
        scoreboardManager.stop();

        // UHC 월드 플레이어 로비 이동 및 월드 언로드 (파일 잠금 사전 해제)
        if (currentUhcWorldName != null) {
            World uhcW = Bukkit.getWorld(currentUhcWorldName);
            if (uhcW != null) {
                World fallback = Bukkit.getWorlds().get(0);
                Location returnLoc = lobbyLocation != null ? lobbyLocation : fallback.getSpawnLocation();
                for (Player p : uhcW.getPlayers()) {
                    p.teleport(returnLoc);
                }
                uhcW.setAutoSave(false);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.unloadWorld(uhcW, false);
                    System.gc();
                }, 20L);
            }
        }
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
            World uhcWorld = getUhcWorld();
            WorldBorder border = uhcWorld.getWorldBorder();
            border.setCenter(uhcWorld.getSpawnLocation());
            border.setSize(size);
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
    public double getBorderInitialSize()       { return borderInitialSize; }
    public double getBorderFinalSize()         { return borderFinalSize; }
    public int getShrinkStartMin()             { return shrinkStartMin; }
    public int getShrinkDurationMin()          { return shrinkDurationMin; }
    public JavaPlugin getPlugin()              { return plugin; }
    public GameType getType()                  { return type; }

    /** 현재 활성화된 UHC 월드 반환 */
    public World getUhcWorld() {
        if (currentUhcWorldName != null) {
            World w = Bukkit.getWorld(currentUhcWorldName);
            if (w != null) return w;
        }
        World temp = Bukkit.getWorld("uhc_temp");
        if (temp != null) return temp;
        return Bukkit.getWorlds().get(0);
    }

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
