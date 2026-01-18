package top.aurlemon.godpotion;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;

public class GodPotionListener implements Listener {

    private final GodPotion plugin;

    public GodPotionListener(GodPotion plugin) {
        this.plugin = plugin;
    }

    /**
     * 辅助方法：检查药水 ItemStack 是否包含我们的死亡标记
     */
    private boolean isDeadlyPotion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(plugin.getIsDeadlyKey(), PersistentDataType.BYTE);
    }
    
    /**
     * 辅助方法：检查实体是否包含我们的死亡标记
     * 用于 AreaEffectCloud 的检测
     */
    private boolean isDeadlyCloud(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(plugin.getIsDeadlyKey(), PersistentDataType.BYTE);
    }

    /**
     * 核心逻辑：执行秒杀
     * @param target 目标实体
     * @param killer 杀手（药水投掷者/来源），用于辅助判断（可选），主要用于权限检查
     */
    private void executeBillLogic(LivingEntity target, ProjectileSource killerSource) {
        if (target.isDead()) return;

        // 如果是玩家，添加死亡标记
        if (target instanceof Player) {
            Player player = (Player) target;
            // 使用 Metadata 给玩家打上临时标签。
            // Metadata 在插件重载/重启后会消失，但对于即时死亡判定足够了。
            // 它是附加在 Entity 实例上的，不需要持久化。
            player.setMetadata(plugin.getDeathMarkKey().getKey(), new FixedMetadataValue(plugin, true));
        }

        // 秒杀：设置为0血
        // 这里不使用 damage()，因为 damage 可能会触发护甲计算或被无敌时间取消 (除非使用 DamageSource API 绕过，1.20+ 可以)
        // 题目要求“秒杀任何实体（包括创造模式玩家和无敌单位）”，setHealth(0) 是最直接的方法。
        // 注意：setHealth(0) 会触发死亡事件。
        try {
            target.setHealth(0);
        } catch (Exception e) {
            // 防止一些特殊实体报错
            target.damage(999999);
        }
    }
    
    /**
     * 1. 处理喷溅型药水逻辑 (Splash Potion)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ItemStack potionItem = event.getPotion().getItem();
        
        if (!isDeadlyPotion(potionItem)) {
            return;
        }

        // 检查投掷者权限
        ProjectileSource shooter = event.getPotion().getShooter();
        if (shooter instanceof Player) {
            Player p = (Player) shooter;
            if (!p.hasPermission("godpotion.use")) {
                p.sendMessage(ChatColor.RED + "你没有权限使用此神之药水！(药水效果已失效)");
                event.setCancelled(true);
                return;
            }
        }

        // 遍历受影响的实体并执行秒杀
        Collection<LivingEntity> affected = event.getAffectedEntities();
        for (LivingEntity entity : affected) {
            executeBillLogic(entity, shooter);
        }
    }

    /**
     * 2. 处理滞留型药水逻辑 (Lingering Potion) - 传递 Tag 阶段
     * 滞留药水落地时会触发 ProjectileHitEvent，我们需要在此处拦截并传递 Tag 给生成的云雾。
     * 由于 Spigot 不直接提供 "PotionBreakToCloudEvent"，我们通过手动生成 Cloud 来实现精确控制。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof ThrownPotion)) {
            return;
        }
        
        ThrownPotion potionEntity = (ThrownPotion) event.getEntity();
        ItemStack item = potionEntity.getItem();

        // 只需要处理滞留药水，且必须带有我们的 Tag
        // 注意：getEffects() 可能为空，但我们看 item meta
        if (!isDeadlyPotion(item)) {
            return;
        }

        // 判断是否是滞留药水材质。如果是 SPLASH_POTION，它已经在 onPotionSplash 处理了。
        // 但为了安全起见，我们只在这里处理 LINGERING_POTION。
        if (item.getType() != org.bukkit.Material.LINGERING_POTION) {
            return;
        }

        // 检查权限
        ProjectileSource shooter = potionEntity.getShooter();
        if (shooter instanceof Player) {
            Player p = (Player) shooter;
            if (!p.hasPermission("godpotion.use")) {
                p.sendMessage(ChatColor.RED + "你没有权限使用此神之药水！");
                event.setCancelled(true);
                potionEntity.remove();
                return;
            }
        }

        // 手动处理云雾生成
        // 如果我们不取消事件，原版会生成一个无 Tag 的云雾。所以我们取消事件（或移除实体），然后自己生成一个带 Tag 的云雾。
        // 注意：ProjectileHitEvent 发生时，药水已经撞击了。
        
        // 我们利用 event.getEntity().remove() 来阻止原版生成云雾逻辑 (Spigot特性依赖，通常移除实体能阻止后续 tick 的逻辑)
        // 或者保险起见，直接在撞击位置生成我们自己的云雾。
        
        // 获取撞击点位置
        org.bukkit.Location loc = potionEntity.getLocation();
        // 如果撞到了实体，可能位置稍微偏移，直接用 projectile 位置即可

        // 如果事件被取消，原版逻辑不会跑，但我们也无法生成效果。这里我们不取消事件，而是让原版逻辑失效（比如通过移除实体）。
        // 实际上，为了最稳健地传递 NBT，我们可以直接在这里 spawn 一个 AreaEffectCloud 并移除药水。
        
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        
        // 复制原版药水的属性
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            cloud.setBasePotionData(meta.getBasePotionData());
            cloud.setColor(Color.RED); // 强制红色显眼
            // 如果 meta 有自定义效果也可以加，但我们只需要视觉
        }
        
        cloud.setRadius(3.0f);
        cloud.setRadiusOnUse(-0.5f); // 每次使用减少半径
        cloud.setDuration(600); // 30秒
        cloud.setParticle(Particle.DRAGON_BREATH); // 龙息粒子
        
        if (shooter != null) {
            cloud.setSource(shooter);
        }

        // 重要：添加 Deadly Tag 到云雾实体
        PersistentDataContainer cloudData = cloud.getPersistentDataContainer();
        cloudData.set(plugin.getIsDeadlyKey(), PersistentDataType.BYTE, (byte) 1);

        // 移除原实体，阻止原版生成第二个云雾
        potionEntity.remove();
        
        // 由于我们移除了实体，可能会导致 PotionSplashEvent 不触发（对滞留药水来说本来就不重要），
        // 主要是防止原版生成那个没有 Tag 的云雾。
    }

    /**
     * 3. 处理滞留型药水云雾效果 (Lingering Cloud Apply)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        
        // 检查是否是我们的 deadly cloud
        if (!isDeadlyCloud(cloud)) {
            return;
        }

        // 再次检查来源权限（可选，因为生成时已检查，但防止后续来源变更或在线状态改变? 通常不需要，保持一致性即可）
        ProjectileSource source = cloud.getSource();
        if (source instanceof Player) {
            Player p = (Player) source;
            if (!p.hasPermission("godpotion.use") && !p.isOp()) { // 这里可加可不加，生成时已控
                // 如果为了严格安全性，可以在这里再次检查
            }
        }

        // 对受影响实体执行秒杀
        for (LivingEntity entity : event.getAffectedEntities()) {
            executeBillLogic(entity, source);
        }
    }

    /**
     * 4. 处理自定义死亡信息 (Death Messages)
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 检查是否有我们的死亡标记
        if (player.hasMetadata(plugin.getDeathMarkKey().getKey())) {
            // 获取 Metadata 值
            boolean marked = false;
            for (org.bukkit.metadata.MetadataValue value : player.getMetadata(plugin.getDeathMarkKey().getKey())) {
                if (value.getOwningPlugin().equals(plugin) && value.asBoolean()) {
                    marked = true;
                    break;
                }
            }

            if (marked) {
                // 修改死亡信息
                event.setDeathMessage(ChatColor.RED + player.getName() + " 被“神力”杀死了");
                
                // 移除标记，防止污染复活后状态（虽然 Metadata 在实体失效后通常会清空，但这里显式移除更好）
                // 实际上 Metadata 绑定在 Entity 对象上，玩家重生是新 Entity (部分服配置可能同Entity?)
                // Player 对象在重生时可能会变，也可能不变，移除总是好的。
                player.removeMetadata(plugin.getDeathMarkKey().getKey(), plugin);
            }
        }
    }
}
