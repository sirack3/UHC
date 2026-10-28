package com.sirack.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML 기반 UHC 커스텀 레시피 관리자.
 * recipes.yml에서 모든 레시피 정의를 로드하고 Bukkit에 등록하며,
 * 제작 횟수 추적, GUI 도감 및 세부 조합법 GUI 생성을 총괄합니다.
 */
public class RecipeManager {

    // ── 레시피 레지스트리 ──
    private static final Map<String, CustomRecipe> recipes = new LinkedHashMap<>();
    private static final Map<String, CustomRecipe> recipesByTag = new HashMap<>();

    // ── 플레이어별 제작 횟수 (UUID -> (recipeId -> count)) ──
    private static final Map<UUID, Map<String, Integer>> playerCrafts = new ConcurrentHashMap<>();

    // ── GUI 타이틀 접두사 ──
    public static final String RECIPE_GUI_TITLE = ChatColor.GOLD + "조합법 도감";
    public static final String RECIPE_DETAIL_TITLE_PREFIX = ChatColor.GOLD + "조합법: ";

    // ── 다이아몬드 방어구 타입 목록 (퓨전 갑옷 판정용) ──
    public static final Set<Material> DIAMOND_ARMOR_TYPES = Set.of(
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS
    );

    // ── 이전 코드 호환성용 태그 상수들 ──
    public static final String APPRENTICE_HELMET_TAG = "견습용 투구";
    public static final String SWORD_TAG = "견습용 검";
    public static final String BOW_TAG = "견습용 활";
    public static final String COMPASS_TAG = "마스터의 나침반";
    public static final String SHARPNESS_BOOK_TAG = "날카로움의 책";
    public static final String POWER_BOOK_TAG = "힘의 책";
    public static final String DRAGON_SWORD_TAG = "용의 검";
    public static final String LEATHER_SAVING_TAG = "가죽 절약";
    public static final String PROTECTION_BOOK_TAG = "보호의 책";
    public static final String DRAGON_ARMOR_TAG = "용의 갑옷";
    public static final String NECTAR_TAG = "넥타르";
    public static final String BURNING_ARTIFACT_TAG = "불타는 아티팩트";
    public static final String DELICIOUS_MEAL_TAG = "맛있는 식사";
    public static final String TOUGHNESS_POTION_TAG = "강인함의 포션";
    public static final String SEVEN_LEAGUE_BOOTS_TAG = "세븐 리그 부츠";
    public static final String IRON_PACK_TAG = "아이언 팩";
    public static final String OBSIDIAN_MIX_TAG = "흑요석 만들기";
    public static final String TARNHELM_TAG = "탄헬름";
    public static final String PHILOSOPHER_PICKAXE_TAG = "철학자의 곡괭이";
    public static final String AWAKENING_PACK_TAG = "깨우침의 팩";
    public static final String LIGHT_ANVIL_TAG = "가벼운 모루";
    public static final String LIGHT_ENCHANTING_TABLE_TAG = "가벼운 마법 부여대";
    public static final String EVES_TEMPTATION_TAG = "이브의 유혹";
    public static final String FRUIT_OF_RECOVERY_TAG = "회복의 과일";
    public static final String LIGHT_APPLE_TAG = "가벼운 사과";
    public static final String GOLDEN_HEAD_TAG = "황금 머리";
    public static final String ARROW_SAVING_TAG = "화살 절약";
    public static final String SPEED_POTION_TAG = "속도의 포션";
    public static final String BLAST_OVEN_TAG = "화덕";
    public static final String FAST_PICKAXE_TAG = "빠른 곡괭이";
    public static final String GOLD_PACK_TAG = "골드 팩";
    public static final String COPPER_PACK_TAG = "구리 팩";
    public static final String SUGAR_RUSH_TAG = "설탕 질주";
    public static final String FUSION_ARMOR_TAG = "퓨전 갑옷";
    public static final String LEVIATHAN_LEATHER_TAG = "레비아탄의 가죽";

    // ── 호환용 GUI 타이틀 상수 ──
    public static final String APPRENTICE_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + APPRENTICE_HELMET_TAG;
    public static final String APPRENTICE_SWORD_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + SWORD_TAG;
    public static final String APPRENTICE_BOW_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + BOW_TAG;
    public static final String MASTER_COMPASS_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + COMPASS_TAG;
    public static final String SHARPNESS_BOOK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + SHARPNESS_BOOK_TAG;
    public static final String POWER_BOOK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + POWER_BOOK_TAG;
    public static final String DRAGON_SWORD_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.LIGHT_PURPLE + DRAGON_SWORD_TAG;
    public static final String LEATHER_SAVING_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.GREEN + LEATHER_SAVING_TAG;
    public static final String PROTECTION_BOOK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + PROTECTION_BOOK_TAG;
    public static final String DRAGON_ARMOR_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.DARK_PURPLE + DRAGON_ARMOR_TAG;
    public static final String NECTAR_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + NECTAR_TAG;
    public static final String BURNING_ARTIFACT_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.RED + BURNING_ARTIFACT_TAG;
    public static final String DELICIOUS_MEAL_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + DELICIOUS_MEAL_TAG;
    public static final String TOUGHNESS_POTION_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.LIGHT_PURPLE + TOUGHNESS_POTION_TAG;
    public static final String SEVEN_LEAGUE_BOOTS_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + SEVEN_LEAGUE_BOOTS_TAG;
    public static final String IRON_PACK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.WHITE + IRON_PACK_TAG;
    public static final String OBSIDIAN_MIX_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.DARK_PURPLE + OBSIDIAN_MIX_TAG;
    public static final String TARNHELM_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + TARNHELM_TAG;
    public static final String PHILOSOPHER_PICKAXE_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + PHILOSOPHER_PICKAXE_TAG;
    public static final String AWAKENING_PACK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.LIGHT_PURPLE + AWAKENING_PACK_TAG;
    public static final String LIGHT_ANVIL_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.WHITE + LIGHT_ANVIL_TAG;
    public static final String LIGHT_ENCHANTING_TABLE_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.LIGHT_PURPLE + LIGHT_ENCHANTING_TABLE_TAG;
    public static final String EVES_TEMPTATION_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.RED + EVES_TEMPTATION_TAG;
    public static final String FRUIT_OF_RECOVERY_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + FRUIT_OF_RECOVERY_TAG;
    public static final String LIGHT_APPLE_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + LIGHT_APPLE_TAG;
    public static final String GOLDEN_HEAD_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.GOLD + GOLDEN_HEAD_TAG;
    public static final String ARROW_SAVING_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.WHITE + ARROW_SAVING_TAG;
    public static final String SPEED_POTION_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + SPEED_POTION_TAG;
    public static final String BLAST_OVEN_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.GOLD + BLAST_OVEN_TAG;
    public static final String FAST_PICKAXE_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.GRAY + FAST_PICKAXE_TAG;
    public static final String GOLD_PACK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.YELLOW + GOLD_PACK_TAG;
    public static final String COPPER_PACK_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.GOLD + COPPER_PACK_TAG;
    public static final String SUGAR_RUSH_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.GREEN + SUGAR_RUSH_TAG;
    public static final String FUSION_ARMOR_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.AQUA + FUSION_ARMOR_TAG;
    public static final String LEVIATHAN_LEATHER_RECIPE_TITLE = RECIPE_DETAIL_TITLE_PREFIX + ChatColor.DARK_AQUA + LEVIATHAN_LEATHER_TAG;

    // ── 재료 그룹 정의 (recipes.yml의 materialGroups 매핑) ──
    private static final Map<String, List<Material>> materialGroups = new LinkedHashMap<>();

    static {
        // 기본 재료 그룹 정적 등록 (YAML 누락 시 완벽한 폴백)
        materialGroups.put("_MEAT", Arrays.asList(
                Material.PORKCHOP, Material.BEEF, Material.MUTTON, Material.CHICKEN, Material.RABBIT, Material.COD, Material.SALMON
        ));
        materialGroups.put("_SAPLING", Arrays.asList(
                Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING,
                Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
        ));
        materialGroups.put("_DIAMOND_ARMOR", Arrays.asList(
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
        ));
    }

    public static List<Material> getMaterialGroup(String groupKey) {
        if (groupKey == null) return Collections.emptyList();
        return materialGroups.getOrDefault(groupKey, Collections.emptyList());
    }

    public static boolean isGroupRequirement(String key) {
        return key != null && key.startsWith("_");
    }

    // ================================================================
    //  초기화 및 YAML 로딩
    // ================================================================

    /**
     * recipes.yml을 로드하고 Bukkit에 커스텀 레시피를 등록합니다.
     */
    public static void registerRecipes(JavaPlugin plugin) {
        loadRecipes(plugin);

        int count = 0;
        for (CustomRecipe recipe : recipes.values()) {
            if ("none".equalsIgnoreCase(recipe.type) || "special".equalsIgnoreCase(recipe.type)) {
                continue;
            }

            NamespacedKey key = new NamespacedKey(plugin, recipe.id);
            Bukkit.removeRecipe(key);

            ItemStack resultItem = createResultItem(recipe);

            if ("shaped".equalsIgnoreCase(recipe.type)) {
                ShapedRecipe sr = new ShapedRecipe(key, resultItem);
                sr.shape(recipe.shape.toArray(new String[0]));

                // 재료 매핑 (단일 재료)
                for (Map.Entry<Character, Material> entry : recipe.ingredients.entrySet()) {
                    sr.setIngredient(entry.getKey(), entry.getValue());
                }

                // 재료 매핑 (다중 선택 재료 Choice)
                for (Map.Entry<Character, List<Material>> entry : recipe.materialChoices.entrySet()) {
                    sr.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(entry.getValue()));
                }

                Bukkit.addRecipe(sr);
                count++;
            } else if ("shapeless".equalsIgnoreCase(recipe.type)) {
                ShapelessRecipe slr = new ShapelessRecipe(key, resultItem);

                for (Material mat : recipe.shapelessIngredients) {
                    slr.addIngredient(mat);
                }

                // 무정형 Choice 재료 등록 (다이아 갑옷 5종 등)
                for (CustomRecipe.ShapelessChoice sc : recipe.shapelessChoices) {
                    List<Material> mats = sc.materials;
                    if ((mats == null || mats.isEmpty()) && sc.group != null) {
                        mats = getMaterialGroup(sc.group);
                    }
                    if (mats != null && !mats.isEmpty()) {
                        RecipeChoice.MaterialChoice choice = new RecipeChoice.MaterialChoice(mats);
                        for (int i = 0; i < sc.count; i++) {
                            slr.addIngredient(choice);
                        }
                    }
                }

                Bukkit.addRecipe(slr);
                count++;
            }
        }

        plugin.getLogger().info("총 " + recipes.size() + "개의 커스텀 레시피 중 " + count + "개가 조합대에 등록되었습니다.");
    }

    /**
     * recipes.yml에서 레시피 정의를 읽어옵니다.
     */
    public static void loadRecipes(JavaPlugin plugin) {
        recipes.clear();
        recipesByTag.clear();

        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // materialGroups 로드
        if (config.isConfigurationSection("materialGroups")) {
            loadMaterialGroupsFromSection(config.getConfigurationSection("materialGroups"));
        }

        // 레시피 로드
        List<Map<?, ?>> list = config.getMapList("recipes");
        for (Map<?, ?> rawMap : list) {
            try {
                CustomRecipe cr = parseRecipe(rawMap);
                if (cr != null && cr.id != null) {
                    recipes.put(cr.id, cr);
                    recipesByTag.put(cr.tag, cr);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("레시피 파싱 실패: " + rawMap.get("id") + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("recipes.yml에서 " + recipes.size() + "개의 레시피를 성공적으로 로드했습니다.");
    }

    private static void loadMaterialGroupsFromSection(org.bukkit.configuration.ConfigurationSection sec) {
        if (sec == null) return;
        for (String gKey : sec.getKeys(false)) {
            List<String> matNames = sec.getStringList(gKey);
            List<Material> list = new ArrayList<>();
            for (String name : matNames) {
                try {
                    list.add(Material.valueOf(name.trim()));
                } catch (Exception ignored) {}
            }
            if (!list.isEmpty()) {
                materialGroups.put(gKey, list);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static CustomRecipe parseRecipe(Map<?, ?> map) {
        CustomRecipe cr = new CustomRecipe();
        cr.id = (String) map.get("id");
        cr.tag = (String) map.get("tag");
        cr.nameColor = map.containsKey("nameColor") ? String.valueOf(map.get("nameColor")) : "WHITE";
        cr.maxCrafts = map.containsKey("maxCrafts") ? ((Number) map.get("maxCrafts")).intValue() : 1;

        // 결과 아이템
        if (map.containsKey("result")) {
            Map<?, ?> resMap = (Map<?, ?>) map.get("result");
            String matStr = (String) resMap.get("material");
            cr.resultMaterial = Material.valueOf(matStr);
            cr.resultAmount = resMap.containsKey("amount") ? ((Number) resMap.get("amount")).intValue() : 1;
        }

        // 무작위 결과물 목록 (randomResults)
        if (map.containsKey("randomResults")) {
            List<String> rList = (List<String>) map.get("randomResults");
            for (String rName : rList) {
                try {
                    cr.randomResults.add(Material.valueOf(rName.trim()));
                } catch (Exception ignored) {}
            }
        }

        // 타입
        cr.type = map.containsKey("type") ? String.valueOf(map.get("type")) : "shaped";

        // 조합 패턴 (shaped)
        if (map.containsKey("shape")) {
            cr.shape = (List<String>) map.get("shape");
        }

        // 재료
        if (map.containsKey("ingredients")) {
            Map<?, ?> ingMap = (Map<?, ?>) map.get("ingredients");
            for (Map.Entry<?, ?> entry : ingMap.entrySet()) {
                char ch = entry.getKey().toString().charAt(0);
                Material mat = Material.valueOf(entry.getValue().toString());
                cr.ingredients.put(ch, mat);
            }
        }

        // 재료 Choice (여러 재료 중 택 1)
        if (map.containsKey("materialChoice")) {
            Map<?, ?> choiceMap = (Map<?, ?>) map.get("materialChoice");
            for (Map.Entry<?, ?> entry : choiceMap.entrySet()) {
                char ch = entry.getKey().toString().charAt(0);
                List<String> matNames = (List<String>) entry.getValue();
                List<Material> matList = new ArrayList<>();
                for (String m : matNames) {
                    matList.add(Material.valueOf(m));
                }
                cr.materialChoices.put(ch, matList);
            }
        }

        // shapelessIngredients
        if (map.containsKey("shapelessIngredients")) {
            List<String> list = (List<String>) map.get("shapelessIngredients");
            for (String s : list) {
                cr.shapelessIngredients.add(Material.valueOf(s));
            }
        }

        // shapelessChoices (무정형 다중 재료 선택지, 예: 다이아몬드 갑옷 5개)
        if (map.containsKey("shapelessChoices")) {
            List<?> scList = (List<?>) map.get("shapelessChoices");
            for (Object obj : scList) {
                if (obj instanceof Map<?, ?> scMap) {
                    CustomRecipe.ShapelessChoice sc = new CustomRecipe.ShapelessChoice();
                    if (scMap.containsKey("group")) {
                        sc.group = scMap.get("group").toString();
                        sc.materials.addAll(getMaterialGroup(sc.group));
                    }
                    if (scMap.containsKey("choices")) {
                        List<String> cList = (List<String>) scMap.get("choices");
                        for (String mName : cList) {
                            try {
                                sc.materials.add(Material.valueOf(mName.trim()));
                            } catch (Exception ignored) {}
                        }
                    }
                    if (scMap.containsKey("count")) {
                        sc.count = ((Number) scMap.get("count")).intValue();
                    }
                    cr.shapelessChoices.add(sc);
                }
            }
        }

        // 인챈트
        if (map.containsKey("enchantments")) {
            Map<?, ?> encMap = (Map<?, ?>) map.get("enchantments");
            for (Map.Entry<?, ?> entry : encMap.entrySet()) {
                cr.enchantments.put(entry.getKey().toString(), ((Number) entry.getValue()).intValue());
            }
        }

        // 마법부여 책 인챈트
        if (map.containsKey("storedEnchantments")) {
            Map<?, ?> encMap = (Map<?, ?>) map.get("storedEnchantments");
            for (Map.Entry<?, ?> entry : encMap.entrySet()) {
                cr.storedEnchantments.put(entry.getKey().toString(), ((Number) entry.getValue()).intValue());
            }
        }

        // Lore
        if (map.containsKey("lore")) {
            cr.lore = (List<String>) map.get("lore");
        }

        // Flags
        if (map.containsKey("itemFlags")) {
            cr.itemFlags = (List<String>) map.get("itemFlags");
        }

        // Damage
        if (map.containsKey("damage")) {
            cr.damage = ((Number) map.get("damage")).intValue();
        }

        // Attributes
        if (map.containsKey("attributes")) {
            Map<?, ?> attrMap = (Map<?, ?>) map.get("attributes");
            for (Map.Entry<?, ?> entry : attrMap.entrySet()) {
                cr.attributes.put(entry.getKey().toString(), (Map<String, Object>) entry.getValue());
            }
        }

        // Potion
        if (map.containsKey("potionEffects")) {
            cr.potionEffects = (List<Map<String, Object>>) map.get("potionEffects");
        }
        if (map.containsKey("potionColor")) {
            cr.potionColor = (String) map.get("potionColor");
        }

        // GUI 위치
        if (map.containsKey("guiPage")) {
            cr.guiPage = ((Number) map.get("guiPage")).intValue();
        }
        if (map.containsKey("guiSlot")) {
            cr.guiSlot = ((Number) map.get("guiSlot")).intValue();
        }

        // 알림 트리거
        if (map.containsKey("notify")) {
            Map<?, ?> notifMap = (Map<?, ?>) map.get("notify");
            if (notifMap.containsKey("triggerMaterials")) {
                List<String> trigList = (List<String>) notifMap.get("triggerMaterials");
                for (String t : trigList) {
                    cr.notifyTriggerMaterials.add(Material.valueOf(t));
                }
            }
            if (notifMap.containsKey("requirements")) {
                Map<?, ?> reqMap = (Map<?, ?>) notifMap.get("requirements");
                for (Map.Entry<?, ?> entry : reqMap.entrySet()) {
                    cr.notifyRequirements.put(entry.getKey().toString(), ((Number) entry.getValue()).intValue());
                }
            }
        }

        // 특수 마킹
        if (map.containsKey("special")) {
            cr.special = String.valueOf(map.get("special"));
        }

        return cr;
    }

    // ================================================================
    //  레시피 조회 & 제작 횟수 추적
    // ================================================================

    public static CustomRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    public static CustomRecipe getRecipeByTag(String tag) {
        return recipesByTag.get(tag);
    }

    public static Collection<CustomRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public static int getCrafts(UUID uuid, String recipeId) {
        if (uuid == null || recipeId == null) return 0;
        Map<String, Integer> userMap = playerCrafts.get(uuid);
        return userMap != null ? userMap.getOrDefault(recipeId, 0) : 0;
    }

    public static void addCraft(UUID uuid, String recipeId) {
        if (uuid == null || recipeId == null) return;
        playerCrafts.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(recipeId, 1, Integer::sum);
    }

    public static void resetCrafts() {
        playerCrafts.clear();
    }

    // ================================================================
    //  아이템 생성 유틸
    // ================================================================

    /**
     * CustomRecipe 정의에 따라 결과 ItemStack을 생성합니다.
     */
    public static ItemStack createResultItem(CustomRecipe recipe) {
        if (recipe == null) return new ItemStack(Material.AIR);

        Material targetMat = recipe.resultMaterial;
        if (recipe.randomResults != null && !recipe.randomResults.isEmpty()) {
            targetMat = recipe.randomResults.get((int) (Math.random() * recipe.randomResults.size()));
        } else if ("fusion_armor".equals(recipe.id)) {
            Material[] faMats = {Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
            targetMat = faMats[(int) (Math.random() * faMats.length)];
        }

        ItemStack item = new ItemStack(targetMat != null ? targetMat : Material.AIR, Math.max(1, recipe.resultAmount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 이름 설정
        ChatColor color = parseChatColor(recipe.nameColor);
        meta.setDisplayName(color + recipe.tag);

        // Lore 설정
        List<String> coloredLore = new ArrayList<>();
        for (String line : recipe.lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        // 인챈트 설정 (일반 아이템)
        for (Map.Entry<String, Integer> entry : recipe.enchantments.entrySet()) {
            Enchantment enc = matchEnchantment(entry.getKey());
            if (enc != null) {
                meta.addEnchant(enc, entry.getValue(), true);
            }
        }

        // 마법이 부여된 책 (EnchantmentStorageMeta)
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<String, Integer> entry : recipe.storedEnchantments.entrySet()) {
                Enchantment enc = matchEnchantment(entry.getKey());
                if (enc != null) {
                    storageMeta.addStoredEnchant(enc, entry.getValue(), true);
                }
            }
        }

        // 내구도 손상 (Damageable)
        if (recipe.damage > 0 && meta instanceof Damageable damageable) {
            damageable.setDamage(recipe.damage);
        }

        // ItemFlags
        for (String flagStr : recipe.itemFlags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagStr));
            } catch (Exception ignored) {}
        }

        // Attributes (어트리뷰트)
        for (Map.Entry<String, Map<String, Object>> entry : recipe.attributes.entrySet()) {
            try {
                String attrName = entry.getKey();
                Map<String, Object> attrData = entry.getValue();
                double amount = ((Number) attrData.get("amount")).doubleValue();
                String opStr = (String) attrData.getOrDefault("operation", "ADD_NUMBER");
                AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(opStr);
                String slotStr = (String) attrData.getOrDefault("slot", "ANY");
                EquipmentSlotGroup slotGroup = parseEquipmentSlotGroup(slotStr);

                Attribute attribute = matchAttribute(attrName);
                if (attribute != null) {
                    NamespacedKey modifierKey = new NamespacedKey("uhc", attrName.toLowerCase().replace('.', '_'));
                    AttributeModifier modifier = new AttributeModifier(
                            modifierKey,
                            amount,
                            operation,
                            slotGroup
                    );
                    meta.addAttributeModifier(attribute, modifier);
                }
            } catch (Exception ignored) {}
        }

        // 포션 메타 (PotionMeta)
        if (meta instanceof PotionMeta potionMeta) {
            if (recipe.potionColor != null) {
                try {
                    Color c = parseRgbColor(recipe.potionColor);
                    if (c != null) potionMeta.setColor(c);
                } catch (Exception ignored) {}
            }
            for (Map<String, Object> effectData : recipe.potionEffects) {
                try {
                    String effTypeStr = (String) effectData.get("type");
                    int duration = ((Number) effectData.get("duration")).intValue();
                    int amplifier = ((Number) effectData.get("amplifier")).intValue();
                    PotionEffectType pet = PotionEffectType.getByName(effTypeStr);
                    if (pet != null) {
                        potionMeta.addCustomEffect(new PotionEffect(pet, duration, amplifier), true);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 특수 레시피 처리 (황금 머리 등)
        if ("golden_head".equals(recipe.id) || "golden_head".equals(recipe.special)) {
            if (meta instanceof SkullMeta skullMeta) {
                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(new URL("http://textures.minecraft.net/texture/4ad0476e8671696af3a8949afa2a814b9bdde65eccd1a8b593aeeff5a0318d"));
                    profile.setTextures(textures);
                    skullMeta.setOwnerProfile(profile);
                } catch (Exception ignored) {}
            }
        }

        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getResultItem(String recipeId) {
        CustomRecipe recipe = getRecipe(recipeId);
        return recipe != null ? createResultItem(recipe) : null;
    }

    /**
     * 도감 및 레시피 GUI 표시용 아이템 생성 (남은 제작 횟수 정보 포함)
     */
    public static ItemStack getForDisplay(UUID viewer, String recipeId) {
        CustomRecipe recipe = getRecipe(recipeId);
        if (recipe == null) return new ItemStack(Material.AIR);

        ItemStack item = createResultItem(recipe);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        int crafts = viewer == null ? 0 : getCrafts(viewer, recipeId);
        String limitText = crafts >= recipe.maxCrafts
                ? ChatColor.RED + "" + crafts + "/" + recipe.maxCrafts + " (제한 도달)"
                : ChatColor.GREEN + "" + crafts + "/" + recipe.maxCrafts + " (제작 가능)";

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "제작 한도: " + limitText);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ================================================================
    //  GUI 관리: 도감 & 세부 조합법 화면
    // ================================================================

    /**
     * 1~3페이지 조합법 도감 GUI를 엽니다.
     */
    public static void openRecipeCatalogGUI(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, RECIPE_GUI_TITLE + " - " + page + "페이지");
        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bg);
        }

        // 해당 페이지의 레시피들 배치
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.guiPage == page && recipe.guiSlot >= 0 && recipe.guiSlot < 54) {
                gui.setItem(recipe.guiSlot, getForDisplay(player.getUniqueId(), recipe.id));
            }
        }

        // 네비게이션 버튼
        if (page > 1) {
            gui.setItem(45, makePrev());
        }
        if (page < 3) {
            gui.setItem(53, makeNext());
        }
        gui.setItem(49, makeClose());

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    /**
     * 특정 레시피의 조합법 세부 GUI를 엽니다.
     */
    public static void openRecipeDetailGUI(Player player, String recipeId) {
        CustomRecipe recipe = getRecipe(recipeId);
        if (recipe == null) return;

        ChatColor color = parseChatColor(recipe.nameColor);
        String title = RECIPE_DETAIL_TITLE_PREFIX + color + recipe.tag;
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // 3x3 조합 슬롯 매핑
        int[] craftSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};
        Set<Integer> craftSlotSet = new HashSet<>();
        for (int s : craftSlots) craftSlotSet.add(s);

        ItemStack bg = makeBg();
        for (int i = 0; i < 54; i++) {
            if (!craftSlotSet.contains(i)) {
                gui.setItem(i, bg);
            }
        }

        if ("shaped".equalsIgnoreCase(recipe.type) && recipe.shape != null) {
            for (int r = 0; r < recipe.shape.size() && r < 3; r++) {
                String row = recipe.shape.get(r);
                for (int c = 0; c < row.length() && c < 3; c++) {
                    char ch = row.charAt(c);
                    int slot = craftSlots[r * 3 + c];
                    if (ch != ' ') {
                        if (recipe.ingredients.containsKey(ch)) {
                            gui.setItem(slot, new ItemStack(recipe.ingredients.get(ch)));
                        } else if (recipe.materialChoices.containsKey(ch)) {
                            // 다중 선택지 중 첫 번째 아이템 표시
                            List<Material> list = recipe.materialChoices.get(ch);
                            if (!list.isEmpty()) {
                                gui.setItem(slot, new ItemStack(list.get(0)));
                            }
                        }
                    }
                }
            }
        } else if ("shapeless".equalsIgnoreCase(recipe.type)) {
            int idx = 0;
            for (Material mat : recipe.shapelessIngredients) {
                if (idx < craftSlots.length) {
                    gui.setItem(craftSlots[idx++], new ItemStack(mat));
                }
            }
            for (CustomRecipe.ShapelessChoice sc : recipe.shapelessChoices) {
                List<Material> mats = sc.materials;
                if ((mats == null || mats.isEmpty()) && sc.group != null) {
                    mats = getMaterialGroup(sc.group);
                }
                if (mats != null && !mats.isEmpty()) {
                    for (int i = 0; i < sc.count && idx < craftSlots.length; i++) {
                        gui.setItem(craftSlots[idx++], new ItemStack(mats.get(i % mats.size())));
                    }
                }
            }
            if ("fusion_armor".equals(recipe.id) && idx < craftSlots.length) {
                ItemStack info = new ItemStack(Material.PAPER);
                ItemMeta infoMeta = info.getItemMeta();
                if (infoMeta != null) {
                    infoMeta.setDisplayName(ChatColor.YELLOW + "어떤 위치에 5개든 상관없음!");
                    infoMeta.setLore(Arrays.asList(
                            ChatColor.GRAY + "다이아몬드 갑옷(투구/흉갑/레깅스/부츠)",
                            ChatColor.GRAY + "아무 조합으로 5개 배치 → 제작 클릭"
                    ));
                    info.setItemMeta(infoMeta);
                }
                gui.setItem(craftSlots[idx], info);
            }
        }

        // 화살표와 결과물, 뒤로가기 버튼
        gui.setItem(23, makeArrow());
        gui.setItem(24, getForDisplay(player.getUniqueId(), recipe.id));
        gui.setItem(49, makeBack());

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ================================================================
    //  GUI 버튼 빌더 유틸
    // ================================================================

    public static ItemStack makeBg() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeArrow() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "조합 결과 →");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeBack() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "뒤로 가기");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeClose() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "닫기");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeNext() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "다음 페이지 →");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makePrev() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "← 이전 페이지");
            item.setItemMeta(meta);
        }
        return item;
    }

    // ================================================================
    //  내부 헬퍼 파서
    // ================================================================

    private static ChatColor parseChatColor(String colorName) {
        if (colorName == null) return ChatColor.WHITE;
        try {
            return ChatColor.valueOf(colorName.toUpperCase());
        } catch (Exception e) {
            return ChatColor.WHITE;
        }
    }

    private static Color parseRgbColor(String str) {
        if (str.startsWith("#")) {
            int rgb = Integer.parseInt(str.substring(1), 16);
            return Color.fromRGB(rgb);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static Enchantment matchEnchantment(String name) {
        try {
            Enchantment enc = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
            if (enc != null) return enc;
        } catch (Exception ignored) {}
        return Enchantment.getByName(name.toUpperCase());
    }

    private static Attribute matchAttribute(String name) {
        try {
            return Attribute.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private static EquipmentSlotGroup parseEquipmentSlotGroup(String slot) {
        return switch (slot.toUpperCase()) {
            case "MAINHAND" -> EquipmentSlotGroup.MAINHAND;
            case "OFFHAND" -> EquipmentSlotGroup.OFFHAND;
            case "FEET" -> EquipmentSlotGroup.FEET;
            case "LEGS" -> EquipmentSlotGroup.LEGS;
            case "CHEST" -> EquipmentSlotGroup.CHEST;
            case "HEAD" -> EquipmentSlotGroup.HEAD;
            case "ARMOR" -> EquipmentSlotGroup.ARMOR;
            case "HAND" -> EquipmentSlotGroup.HAND;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    // ================================================================
    //  하위 호환성 위임 메서드들 (기존 코드 안전 보장)
    // ================================================================

    public static int getApprenticeCrafts(UUID u) { return getCrafts(u, "apprentice_helmet"); }
    public static void addApprenticeCraft(UUID u) { addCraft(u, "apprentice_helmet"); }
    public static ItemStack getApprenticeHelmet() { return getResultItem("apprentice_helmet"); }
    public static ItemStack getApprenticeHelmetForDisplay(UUID u) { return getForDisplay(u, "apprentice_helmet"); }
    public static void openApprenticeRecipeGUI(Player p) { openRecipeDetailGUI(p, "apprentice_helmet"); }

    public static int getSwordCrafts(UUID u) { return getCrafts(u, "apprentice_sword"); }
    public static void addSwordCraft(UUID u) { addCraft(u, "apprentice_sword"); }
    public static ItemStack getApprenticeSword() { return getResultItem("apprentice_sword"); }
    public static ItemStack getApprenticeSwordForDisplay(UUID u) { return getForDisplay(u, "apprentice_sword"); }
    public static void openApprenticeSwordRecipeGUI(Player p) { openRecipeDetailGUI(p, "apprentice_sword"); }

    public static int getBowCrafts(UUID u) { return getCrafts(u, "apprentice_bow"); }
    public static void addBowCraft(UUID u) { addCraft(u, "apprentice_bow"); }
    public static ItemStack getApprenticeBow() { return getResultItem("apprentice_bow"); }
    public static ItemStack getApprenticeBowForDisplay(UUID u) { return getForDisplay(u, "apprentice_bow"); }
    public static void openApprenticeBowRecipeGUI(Player p) { openRecipeDetailGUI(p, "apprentice_bow"); }

    public static int getCompassCrafts(UUID u) { return getCrafts(u, "master_compass"); }
    public static void addCompassCraft(UUID u) { addCraft(u, "master_compass"); }
    public static ItemStack getMasterCompass() { return getResultItem("master_compass"); }
    public static ItemStack getCompassForDisplay(UUID u) { return getForDisplay(u, "master_compass"); }
    public static void openMasterCompassRecipeGUI(Player p) { openRecipeDetailGUI(p, "master_compass"); }

    public static int getSharpnessBookCrafts(UUID u) { return getCrafts(u, "sharpness_book"); }
    public static void addSharpnessBookCraft(UUID u) { addCraft(u, "sharpness_book"); }
    public static ItemStack getSharpnessBook() { return getResultItem("sharpness_book"); }
    public static ItemStack getSharpnessBookForDisplay(UUID u) { return getForDisplay(u, "sharpness_book"); }
    public static void openSharpnessBookRecipeGUI(Player p) { openRecipeDetailGUI(p, "sharpness_book"); }

    public static int getPowerBookCrafts(UUID u) { return getCrafts(u, "power_book"); }
    public static void addPowerBookCraft(UUID u) { addCraft(u, "power_book"); }
    public static ItemStack getPowerBook() { return getResultItem("power_book"); }
    public static ItemStack getPowerBookForDisplay(UUID u) { return getForDisplay(u, "power_book"); }
    public static void openPowerBookRecipeGUI(Player p) { openRecipeDetailGUI(p, "power_book"); }

    public static int getDragonSwordCrafts(UUID u) { return getCrafts(u, "dragon_sword"); }
    public static void addDragonSwordCraft(UUID u) { addCraft(u, "dragon_sword"); }
    public static ItemStack getDragonSword() { return getResultItem("dragon_sword"); }
    public static ItemStack getDragonSwordForDisplay(UUID u) { return getForDisplay(u, "dragon_sword"); }
    public static void openDragonSwordRecipeGUI(Player p) { openRecipeDetailGUI(p, "dragon_sword"); }

    public static int getLeatherSavingCrafts(UUID u) { return getCrafts(u, "leather_saving"); }
    public static void addLeatherSavingCraft(UUID u) { addCraft(u, "leather_saving"); }
    public static ItemStack getLeatherSaving() { return getResultItem("leather_saving"); }
    public static ItemStack getLeatherSavingForDisplay(UUID u) { return getForDisplay(u, "leather_saving"); }
    public static void openLeatherSavingRecipeGUI(Player p) { openRecipeDetailGUI(p, "leather_saving"); }

    public static int getProtectionBookCrafts(UUID u) { return getCrafts(u, "protection_book"); }
    public static void addProtectionBookCraft(UUID u) { addCraft(u, "protection_book"); }
    public static ItemStack getProtectionBook() { return getResultItem("protection_book"); }
    public static ItemStack getProtectionBookForDisplay(UUID u) { return getForDisplay(u, "protection_book"); }
    public static void openProtectionBookRecipeGUI(Player p) { openRecipeDetailGUI(p, "protection_book"); }

    public static int getDragonArmorCrafts(UUID u) { return getCrafts(u, "dragon_armor"); }
    public static void addDragonArmorCraft(UUID u) { addCraft(u, "dragon_armor"); }
    public static ItemStack getDragonArmor() { return getResultItem("dragon_armor"); }
    public static ItemStack getDragonArmorForDisplay(UUID u) { return getForDisplay(u, "dragon_armor"); }
    public static void openDragonArmorRecipeGUI(Player p) { openRecipeDetailGUI(p, "dragon_armor"); }

    public static int getNectarCrafts(UUID u) { return getCrafts(u, "nectar"); }
    public static void addNectarCraft(UUID u) { addCraft(u, "nectar"); }
    public static ItemStack getNectar() { return getResultItem("nectar"); }
    public static ItemStack getNectarForDisplay(UUID u) { return getForDisplay(u, "nectar"); }
    public static void openNectarRecipeGUI(Player p) { openRecipeDetailGUI(p, "nectar"); }

    public static int getBurningArtifactCrafts(UUID u) { return getCrafts(u, "burning_artifact"); }
    public static void addBurningArtifactCraft(UUID u) { addCraft(u, "burning_artifact"); }
    public static ItemStack getBurningArtifact() { return getResultItem("burning_artifact"); }
    public static ItemStack getBurningArtifactForDisplay(UUID u) { return getForDisplay(u, "burning_artifact"); }
    public static void openBurningArtifactRecipeGUI(Player p) { openRecipeDetailGUI(p, "burning_artifact"); }

    public static int getDeliciousMealCrafts(UUID u) { return getCrafts(u, "delicious_meal"); }
    public static void addDeliciousMealCraft(UUID u) { addCraft(u, "delicious_meal"); }
    public static ItemStack getDeliciousMeal() { return getResultItem("delicious_meal"); }
    public static ItemStack getDeliciousMealForDisplay(UUID u) { return getForDisplay(u, "delicious_meal"); }
    public static void openDeliciousMealRecipeGUI(Player p) { openRecipeDetailGUI(p, "delicious_meal"); }

    public static int getToughnessPotionCrafts(UUID u) { return getCrafts(u, "toughness_potion"); }
    public static void addToughnessPotionCraft(UUID u) { addCraft(u, "toughness_potion"); }
    public static ItemStack getToughnessPotion() { return getResultItem("toughness_potion"); }
    public static ItemStack getToughnessPotionForDisplay(UUID u) { return getForDisplay(u, "toughness_potion"); }
    public static void openToughnessPotionRecipeGUI(Player p) { openRecipeDetailGUI(p, "toughness_potion"); }

    public static int getSevenLeagueBootsCrafts(UUID u) { return getCrafts(u, "seven_league_boots"); }
    public static void addSevenLeagueBootsCraft(UUID u) { addCraft(u, "seven_league_boots"); }
    public static ItemStack getSevenLeagueBoots() { return getResultItem("seven_league_boots"); }
    public static ItemStack getSevenLeagueBootsForDisplay(UUID u) { return getForDisplay(u, "seven_league_boots"); }
    public static void openSevenLeagueBootsRecipeGUI(Player p) { openRecipeDetailGUI(p, "seven_league_boots"); }

    public static int getIronPackCrafts(UUID u) { return getCrafts(u, "iron_pack"); }
    public static void addIronPackCraft(UUID u) { addCraft(u, "iron_pack"); }
    public static ItemStack getIronPack() { return getResultItem("iron_pack"); }
    public static ItemStack getIronPackForDisplay(UUID u) { return getForDisplay(u, "iron_pack"); }
    public static void openIronPackRecipeGUI(Player p) { openRecipeDetailGUI(p, "iron_pack"); }

    public static int getObsidianMixCrafts(UUID u) { return getCrafts(u, "obsidian_mix"); }
    public static void addObsidianMixCraft(UUID u) { addCraft(u, "obsidian_mix"); }
    public static ItemStack getObsidianMix() { return getResultItem("obsidian_mix"); }
    public static ItemStack getObsidianMixForDisplay(UUID u) { return getForDisplay(u, "obsidian_mix"); }
    public static void openObsidianMixRecipeGUI(Player p) { openRecipeDetailGUI(p, "obsidian_mix"); }

    public static int getTarnhelmCrafts(UUID u) { return getCrafts(u, "tarnhelm"); }
    public static void addTarnhelmCraft(UUID u) { addCraft(u, "tarnhelm"); }
    public static ItemStack getTarnhelm() { return getResultItem("tarnhelm"); }
    public static ItemStack getTarnhelmForDisplay(UUID u) { return getForDisplay(u, "tarnhelm"); }
    public static void openTarnhelmRecipeGUI(Player p) { openRecipeDetailGUI(p, "tarnhelm"); }

    public static int getPhilosopherPickaxeCrafts(UUID u) { return getCrafts(u, "philosopher_pickaxe"); }
    public static void addPhilosopherPickaxeCraft(UUID u) { addCraft(u, "philosopher_pickaxe"); }
    public static ItemStack getPhilosopherPickaxe() { return getResultItem("philosopher_pickaxe"); }
    public static ItemStack getPhilosopherPickaxeForDisplay(UUID u) { return getForDisplay(u, "philosopher_pickaxe"); }
    public static void openPhilosopherPickaxeRecipeGUI(Player p) { openRecipeDetailGUI(p, "philosopher_pickaxe"); }

    public static int getAwakeningPackCrafts(UUID u) { return getCrafts(u, "awakening_pack"); }
    public static void addAwakeningPackCraft(UUID u) { addCraft(u, "awakening_pack"); }
    public static ItemStack getAwakeningPack() { return getResultItem("awakening_pack"); }
    public static ItemStack getAwakeningPackForDisplay(UUID u) { return getForDisplay(u, "awakening_pack"); }
    public static void openAwakeningPackRecipeGUI(Player p) { openRecipeDetailGUI(p, "awakening_pack"); }

    public static int getLightAnvilCrafts(UUID u) { return getCrafts(u, "light_anvil"); }
    public static void addLightAnvilCraft(UUID u) { addCraft(u, "light_anvil"); }
    public static ItemStack getLightAnvil() { return getResultItem("light_anvil"); }
    public static ItemStack getLightAnvilForDisplay(UUID u) { return getForDisplay(u, "light_anvil"); }
    public static void openLightAnvilRecipeGUI(Player p) { openRecipeDetailGUI(p, "light_anvil"); }

    public static int getLightEnchantingTableCrafts(UUID u) { return getCrafts(u, "light_enchanting_table"); }
    public static void addLightEnchantingTableCraft(UUID u) { addCraft(u, "light_enchanting_table"); }
    public static ItemStack getLightEnchantingTable() { return getResultItem("light_enchanting_table"); }
    public static ItemStack getLightEnchantingTableForDisplay(UUID u) { return getForDisplay(u, "light_enchanting_table"); }
    public static void openLightEnchantingTableRecipeGUI(Player p) { openRecipeDetailGUI(p, "light_enchanting_table"); }

    public static int getEvesTemptationCrafts(UUID u) { return getCrafts(u, "eves_temptation"); }
    public static void addEvesTemptationCraft(UUID u) { addCraft(u, "eves_temptation"); }
    public static ItemStack getEvesTemptation() { return getResultItem("eves_temptation"); }
    public static ItemStack getEvesTemptationForDisplay(UUID u) { return getForDisplay(u, "eves_temptation"); }
    public static void openEvesTemptationRecipeGUI(Player p) { openRecipeDetailGUI(p, "eves_temptation"); }

    public static int getFruitOfRecoveryCrafts(UUID u) { return getCrafts(u, "fruit_of_recovery"); }
    public static void addFruitOfRecoveryCraft(UUID u) { addCraft(u, "fruit_of_recovery"); }
    public static ItemStack getFruitOfRecovery() { return getResultItem("fruit_of_recovery"); }
    public static ItemStack getFruitOfRecoveryForDisplay(UUID u) { return getForDisplay(u, "fruit_of_recovery"); }
    public static void openFruitOfRecoveryRecipeGUI(Player p) { openRecipeDetailGUI(p, "fruit_of_recovery"); }

    public static int getLightAppleCrafts(UUID u) { return getCrafts(u, "light_apple"); }
    public static void addLightAppleCraft(UUID u) { addCraft(u, "light_apple"); }
    public static ItemStack getLightApple() { return getResultItem("light_apple"); }
    public static ItemStack getLightAppleForDisplay(UUID u) { return getForDisplay(u, "light_apple"); }
    public static void openLightAppleRecipeGUI(Player p) { openRecipeDetailGUI(p, "light_apple"); }

    public static int getGoldenHeadCrafts(UUID u) { return getCrafts(u, "golden_head"); }
    public static void addGoldenHeadCraft(UUID u) { addCraft(u, "golden_head"); }
    public static ItemStack getGoldenHead() { return getResultItem("golden_head"); }
    public static ItemStack getGoldenHeadForDisplay(UUID u) { return getForDisplay(u, "golden_head"); }
    public static void openGoldenHeadRecipeGUI(Player p) { openRecipeDetailGUI(p, "golden_head"); }

    public static int getArrowSavingCrafts(UUID u) { return getCrafts(u, "arrow_saving"); }
    public static void addArrowSavingCraft(UUID u) { addCraft(u, "arrow_saving"); }
    public static ItemStack getArrowSaving() { return getResultItem("arrow_saving"); }
    public static ItemStack getArrowSavingForDisplay(UUID u) { return getForDisplay(u, "arrow_saving"); }
    public static void openArrowSavingRecipeGUI(Player p) { openRecipeDetailGUI(p, "arrow_saving"); }

    public static int getSpeedPotionCrafts(UUID u) { return getCrafts(u, "speed_potion"); }
    public static void addSpeedPotionCraft(UUID u) { addCraft(u, "speed_potion"); }
    public static ItemStack getSpeedPotion() { return getResultItem("speed_potion"); }
    public static ItemStack getSpeedPotionForDisplay(UUID u) { return getForDisplay(u, "speed_potion"); }
    public static void openSpeedPotionRecipeGUI(Player p) { openRecipeDetailGUI(p, "speed_potion"); }

    public static int getBlastOvenCrafts(UUID u) { return getCrafts(u, "blast_oven"); }
    public static void addBlastOvenCraft(UUID u) { addCraft(u, "blast_oven"); }
    public static ItemStack getBlastOven() { return getResultItem("blast_oven"); }
    public static ItemStack getBlastOvenForDisplay(UUID u) { return getForDisplay(u, "blast_oven"); }
    public static void openBlastOvenRecipeGUI(Player p) { openRecipeDetailGUI(p, "blast_oven"); }

    public static int getFastPickaxeCrafts(UUID u) { return getCrafts(u, "fast_pickaxe"); }
    public static void addFastPickaxeCraft(UUID u) { addCraft(u, "fast_pickaxe"); }
    public static ItemStack getFastPickaxe() { return getResultItem("fast_pickaxe"); }
    public static ItemStack getFastPickaxeForDisplay(UUID u) { return getForDisplay(u, "fast_pickaxe"); }
    public static void openFastPickaxeRecipeGUI(Player p) { openRecipeDetailGUI(p, "fast_pickaxe"); }

    public static int getGoldPackCrafts(UUID u) { return getCrafts(u, "gold_pack"); }
    public static void addGoldPackCraft(UUID u) { addCraft(u, "gold_pack"); }
    public static ItemStack getGoldPack() { return getResultItem("gold_pack"); }
    public static ItemStack getGoldPackForDisplay(UUID u) { return getForDisplay(u, "gold_pack"); }
    public static void openGoldPackRecipeGUI(Player p) { openRecipeDetailGUI(p, "gold_pack"); }

    public static int getCopperPackCrafts(UUID u) { return getCrafts(u, "copper_pack"); }
    public static void addCopperPackCraft(UUID u) { addCraft(u, "copper_pack"); }
    public static ItemStack getCopperPack() { return getResultItem("copper_pack"); }
    public static ItemStack getCopperPackForDisplay(UUID u) { return getForDisplay(u, "copper_pack"); }
    public static void openCopperPackRecipeGUI(Player p) { openRecipeDetailGUI(p, "copper_pack"); }

    public static int getSugarRushCrafts(UUID u) { return getCrafts(u, "sugar_rush"); }
    public static void addSugarRushCraft(UUID u) { addCraft(u, "sugar_rush"); }
    public static ItemStack getSugarRush() { return getResultItem("sugar_rush"); }
    public static ItemStack getSugarRushForDisplay(UUID u) { return getForDisplay(u, "sugar_rush"); }
    public static void openSugarRushRecipeGUI(Player p) { openRecipeDetailGUI(p, "sugar_rush"); }

    public static int getFusionArmorCrafts(UUID u) { return getCrafts(u, "fusion_armor"); }
    public static void addFusionArmorCraft(UUID u) { addCraft(u, "fusion_armor"); }
    public static ItemStack getFusionArmor() { return getResultItem("fusion_armor"); }
    public static ItemStack getFusionArmorForDisplay(UUID u) { return getForDisplay(u, "fusion_armor"); }
    public static void openFusionArmorRecipeGUI(Player p) { openRecipeDetailGUI(p, "fusion_armor"); }

    public static int getLeviathanLeatherCrafts(UUID u) { return getCrafts(u, "leviathan_leather"); }
    public static void addLeviathanLeatherCraft(UUID u) { addCraft(u, "leviathan_leather"); }
    public static ItemStack getLeviathanLeather() { return getResultItem("leviathan_leather"); }
    public static ItemStack getLeviathanLeatherForDisplay(UUID u) { return getForDisplay(u, "leviathan_leather"); }
    public static void openLeviathanLeatherRecipeGUI(Player p) { openRecipeDetailGUI(p, "leviathan_leather"); }
}
