package top.aurlemon.godpotion;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GodPotion extends JavaPlugin {

    public static final String KEY_IS_DEADLY = "is_deadly";
    // 玩家被药水击中后的标记，用于死亡信息
    public static final String KEY_DEATH_MARK = "godpotion_death_mark";

    private static GodPotion instance;
    private NamespacedKey isDeadlyKey;
    private NamespacedKey deathMarkKey;

    @Override
    public void onEnable() {
        instance = this;
        // 初始化 NamespacedKeys
        // NamespacedKey 必须小写
        this.isDeadlyKey = new NamespacedKey(this, KEY_IS_DEADLY);
        // 这个 key 我们主要作为元数据 key 使用，但 NamespacedKey 通用
        this.deathMarkKey = new NamespacedKey(this, KEY_DEATH_MARK);

        // 注册命令
        if (getCommand("godpotion") != null) {
            getCommand("godpotion").setExecutor(new GodPotionCommand(this));
            getCommand("godpotion").setTabCompleter(new GodPotionCommand(this));
        }

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new GodPotionListener(this), this);
        
        getLogger().info("GodPotion plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GodPotion plugin has been disabled!");
    }

    public static GodPotion getInstance() {
        return instance;
    }

    public NamespacedKey getIsDeadlyKey() {
        return isDeadlyKey;
    }

    public NamespacedKey getDeathMarkKey() {
        return deathMarkKey;
    }
}
