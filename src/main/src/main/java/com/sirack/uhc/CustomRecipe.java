package com.sirack.uhc;

import org.bukkit.Material;

import java.util.*;

/**
 * YAML에서 로드한 개별 레시피 데이터를 담는 모델 클래스.
 * RecipeManager가 이 객체들을 레지스트리에 저장하고 통합적으로 관리합니다.
 */
public class CustomRecipe {
    public String id;
    public String tag;
    public String nameColor;
    public int maxCrafts;

    // 결과 아이템
    public Material resultMaterial;
    public int resultAmount;
    public List<Material> randomResults;

    // 레시피 타입
    public String type; // "shaped", "shapeless", "none"
    public List<String> shape;
    public Map<Character, Material> ingredients;
    public Map<Character, List<Material>> materialChoices;
    public List<Material> shapelessIngredients;

    public static class ShapelessChoice {
        public String group;
        public List<Material> materials = new ArrayList<>();
        public int count = 1;
    }
    public List<ShapelessChoice> shapelessChoices;

    // 아이템 메타
    public Map<String, Integer> enchantments;
    public Map<String, Integer> storedEnchantments;
    public List<String> lore;
    public List<String> itemFlags;
    public int damage; // Damageable
    public Map<String, Map<String, Object>> attributes;

    // 포션
    public List<Map<String, Object>> potionEffects;
    public String potionColor;

    // GUI 위치
    public int guiPage;
    public int guiSlot;

    // 알림 조건
    public List<Material> notifyTriggerMaterials;
    public Map<String, Integer> notifyRequirements;

    // 특수 처리
    public String special;

    public CustomRecipe() {
        this.ingredients = new LinkedHashMap<>();
        this.materialChoices = new LinkedHashMap<>();
        this.shapelessIngredients = new ArrayList<>();
        this.shapelessChoices = new ArrayList<>();
        this.randomResults = new ArrayList<>();
        this.enchantments = new LinkedHashMap<>();
        this.storedEnchantments = new LinkedHashMap<>();
        this.lore = new ArrayList<>();
        this.itemFlags = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
        this.potionEffects = new ArrayList<>();
        this.notifyTriggerMaterials = new ArrayList<>();
        this.notifyRequirements = new LinkedHashMap<>();
        this.shape = new ArrayList<>();
        this.damage = 0;
    }

    /**
     * 이 레시피가 특수 처리가 필요한지 여부
     */
    public boolean isSpecial() {
        return special != null && !special.isEmpty();
    }

    /**
     * 이 레시피가 포션 타입인지 여부
     */
    public boolean isPotion() {
        return resultMaterial == Material.POTION || resultMaterial == Material.SPLASH_POTION
                || resultMaterial == Material.LINGERING_POTION;
    }

    /**
     * 이 레시피가 인챈트 북인지 여부
     */
    public boolean isEnchantedBook() {
        return resultMaterial == Material.ENCHANTED_BOOK && !storedEnchantments.isEmpty();
    }
}
