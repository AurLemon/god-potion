package top.aurlemon.godpotion;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class GodPotionCommand implements CommandExecutor, TabCompleter {

    private final GodPotion plugin;

    public GodPotionCommand(GodPotion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("godpotion.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("get")) {
            sender.sendMessage(ChatColor.RED + "用法: /godpotion get <splash|lingering>");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令获取药水。");
            return true;
        }

        Player player = (Player) sender;
        String type = args[1].toLowerCase();
        ItemStack potion = null;

        if (type.equals("splash")) {
            potion = createGodPotion(Material.SPLASH_POTION);
        } else if (type.equals("lingering")) {
            potion = createGodPotion(Material.LINGERING_POTION);
        } else {
            sender.sendMessage(ChatColor.RED + "未知类型: " + type + "。请使用 splash 或 lingering。");
            return true;
        }

        // 给予玩家
        player.getInventory().addItem(potion);
        player.sendMessage(ChatColor.GREEN + "你获得了 " + potion.getItemMeta().getDisplayName());

        return true;
    }

    private ItemStack createGodPotion(Material material) {
        ItemStack item = new ItemStack(material);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        if (meta != null) {
            // 设置基础属性：深红加粗名字
            meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚡ 诸神黄昏 ⚡");

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "蕴含着抹杀一切的规则之力");
            meta.setLore(lore);

            // 发光 (添加耐久附魔并隐藏)
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // 标记为特殊药水
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(plugin.getIsDeadlyKey(), PersistentDataType.BYTE, (byte) 1);

            // 设置药水颜色/类型 (使用 Harm 瞬间伤害作为视觉基础，虽然我们会覆盖逻辑)
            meta.setBasePotionData(new org.bukkit.potion.PotionData(PotionType.INSTANT_DAMAGE));
            
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("get");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            List<String> types = new ArrayList<>();
            types.add("splash");
            types.add("lingering");
            return types;
        }
        return Collections.emptyList();
    }
}
