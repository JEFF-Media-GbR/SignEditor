package de.jeff_media.signeditor;

import com.google.common.base.Strings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SignEditor extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("editsign").setExecutor(this);
        getCommand("editsign").setTabCompleter(this);
        saveDefaultConfig();
    }

    @EventHandler
    public void onSignEdit(SignChangeEvent event) {
        if(!event.getPlayer().hasPermission("signeditor.edit")) return;
        for(int i = 0; i < event.getLines().length; i++) {
            event.setLine(i,ChatColor.translateAlternateColorCodes('&',event.getLine(i)));
        }
    }

    private String joinString(String[] args) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> it = Arrays.stream(args).iterator();
        while(it.hasNext()) {
            builder.append(it.next());
            if(it.hasNext()) builder.append(" ");
        }
        return builder.toString();
    }

    private String[] getLines(String[] args) {
        String joined = joinString(args);
        String[] lines = joined.split("\\\\n");
        return lines;
    }

    @Nullable
    private static Sign getSignLookingAt(Player player) {
        Block lookingAt = player.getTargetBlockExact(10, FluidCollisionMode.NEVER);
        if(!(lookingAt.getState() instanceof Sign)) {
            return null;
        }
        return (Sign) lookingAt.getState();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player) sender;
        Sign sign = getSignLookingAt(player);
        if(sign == null) {
            player.sendMessage("§cYou must be looking at a sign.");
            return true;
        }
        String[] lines = getLines(args);
        if(lines.length>4) {
            player.sendMessage("§cYou cannot use more than 4 lines.");
            return true;
        }
        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(sign.getBlock(), player);
        Bukkit.getPluginManager().callEvent(blockBreakEvent);
        if(blockBreakEvent.isCancelled()) {
            player.sendMessage("§cYou are not allowed to change this sign.");
            return true;
        }
        for(int i = 0; i < 4; i++) {
            if(i >= lines.length) {
                sign.setLine(i,"");
            } else {
                sign.setLine(i, ChatColor.translateAlternateColorCodes('&',lines[i]));
            }
        }
        sign.update();
        return true;
    }

    private static String toSingleLine(Sign sign) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> it = Arrays.stream(sign.getLines()).iterator();
        while(it.hasNext()) {
            builder.append(it.next().replace("§","&"));
            if(it.hasNext()) builder.append("\\n");
        }
        String text = builder.toString();
        while(text.endsWith("\\n")) {
            text = text.substring(0,text.length()-2);
        }
        return text;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length==1 && args[0].length()==0 && sender instanceof Player) {
            Sign sign = getSignLookingAt((Player)sender);
            if(sign != null) {
                return Collections.singletonList(toSingleLine(sign));
            }
        }
        return  null;
    }
}
