package com.sirack.uhc;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * UHC 플러그인 메인 클래스
 */
public class UHCPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        // config.yml 저장 (없으면 기본값으로 생성)
        saveDefaultConfig();

        gameManager = new GameManager(this);

        // 명령어 등록
        UHCCommand commandHandler = new UHCCommand(gameManager);
        getCommand("유챔").setExecutor(commandHandler);
        getCommand("유챔").setTabCompleter(commandHandler);

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);

        getLogger().info("UHC 플러그인이 활성화되었습니다! (Paper 26.2)");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.forceStop();
        }
        getLogger().info("UHC 플러그인이 비활성화되었습니다.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
