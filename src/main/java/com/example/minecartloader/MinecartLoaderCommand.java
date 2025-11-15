package com.example.minecartloader;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MinecartLoaderCommand implements CommandExecutor, TabCompleter {

    private final MinecartLoaderPlugin plugin;
    private final ChunkUsageManager chunkUsageManager;

    public MinecartLoaderCommand(MinecartLoaderPlugin plugin, ChunkUsageManager chunkUsageManager) {
        this.plugin = plugin;
        this.chunkUsageManager = chunkUsageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecartloader.admin")) {
            sender.sendMessage("§c你没有权限使用此命令。");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e用法: /minecartloader <on|off|freeze|unfreeze|status|reload>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "on":
                plugin.setMinecartLoaderEnabled(true);
                sender.sendMessage("§aMinecartLoader 已开启。");
                break;
            case "off":
                plugin.setMinecartLoaderEnabled(false);
                sender.sendMessage("§cMinecartLoader 已关闭，新矿车不会创建/续期加载器。");
                break;
            case "freeze":
                plugin.setMinecartLoaderEnabled(false);
                chunkUsageManager.freezeAll();
                sender.sendMessage("§cMinecartLoader 已冻结并移除由本插件创建的加载器。");
                break;
            case "unfreeze":
                plugin.setMinecartLoaderEnabled(true);
                sender.sendMessage("§aMinecartLoader 已解冻，开始处理新的矿车移动。");
                if (sender instanceof Player) {
                    sender.sendMessage("§7提示: 可重新登录或移动矿车以重新建立加载器。");
                }
                break;
            case "status":
                sender.sendMessage("§eMinecartLoader 状态: " + (plugin.isMinecartLoaderEnabled() ? "§a开启" : "§c关闭"));
                sender.sendMessage("§e当前活跃 chunk 计数: §b" + chunkUsageManager.getActiveChunkCount());
                break;
            case "reload":
                plugin.reloadPluginConfig();
                sender.sendMessage("§aMinecartLoader 配置已重新加载。");
                break;
            default:
                sender.sendMessage("§e用法: /minecartloader <on|off|freeze|unfreeze|status|reload>");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("on", "off", "freeze", "unfreeze", "status", "reload");
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(prefix)) {
                    result.add(option);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
