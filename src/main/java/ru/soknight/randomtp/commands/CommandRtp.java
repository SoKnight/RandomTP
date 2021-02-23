package ru.soknight.randomtp.commands;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.soknight.lib.argument.CommandArguments;
import ru.soknight.lib.command.preset.standalone.PermissibleCommand;
import ru.soknight.lib.configuration.Messages;
import ru.soknight.lib.cooldown.preset.RichPlayersCooldownStorage;
import ru.soknight.lib.format.DateFormatter;
import ru.soknight.randomtp.RandomTP;
import ru.soknight.randomtp.configuration.Configuration;

public class CommandRtp extends PermissibleCommand {
    
    private static final DateFormatter FORMATTER = new DateFormatter(" ");

    private final Configuration config;
    private final Messages messages;
    
    private final RandomTP plugin;
    private final RichPlayersCooldownStorage cooldownStorage;
    
    private final int radius;
    private final boolean protect;
    private final boolean playerIsCenter;
    private final List<String> enabledWorlds;
    
    public CommandRtp(RandomTP plugin, Configuration config, Messages messages) {
        super("rtp", "randomtp.command.rtp", messages);
        
        this.cooldownStorage = new RichPlayersCooldownStorage(config.getInt("teleport.cooldown"));
        this.messages = messages;
        this.config = config;
        this.plugin = plugin;
        
        this.radius = config.getInt("teleport.radius");
        this.protect = config.getBoolean("teleport.protect");
        this.playerIsCenter = config.getBoolean("teleport.player-is-center");
        this.enabledWorlds = config.getList("teleport.enabled-worlds");
        
        super.register(plugin, true);
    }
    
    @Override
    public void executeCommand(CommandSender sender, CommandArguments arguments) {
        String name = sender.getName();
        Player target;
        
        boolean other = false;
        
        // other rtp execution
        if(!arguments.isEmpty()) {
            // checks for permission
            if(!sender.hasPermission("randomtp.command.rtp.other")) {
                messages.getAndSend(sender, "teleport.failed.other-perm");
                return;
            }
            
            name = arguments.get(0);
            
            // checks for online target
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if(!isPlayerOnline(name)) {
                messages.sendFormatted(sender, "error.player-not-found", "%player%", name);
                return;
            }
            
            target = offline.getPlayer();
            
            // checks for allowed world
            if(enabledWorlds != null && !enabledWorlds.contains(target.getWorld().getName())) {
                messages.sendFormatted(sender, "teleport.failed.world.other", "%player%", name);
                return;
            }
            
            if(!name.equals(sender.getName()))
                other = true;
            
        // self rtp execution
        } else {
            // checks for player instance
            if(!isPlayer(sender)) {
                messages.getAndSend(sender, "error.only-for-players");
                return;
            }
            
            target = (Player) sender;
            
            // checks for allowed world
            if(!enabledWorlds.contains(target.getWorld().getName())) {
                messages.getAndSend(sender, "teleport.failed.world.self");
                return;
            }
            
            // checks for cooldown
            if(!sender.hasPermission("randomtp.command.rtp.bypass") && cooldownStorage.containsKey(name)) {
                // refresh custom cooldown time
                cooldownStorage.setCustomCooldown(name, getCooldown(target), false);
                long remained = cooldownStorage.getRemainedTime(name);
                
                if(remained > 0) {
                    long seconds = remained / 1000;
                    if(remained % 1000 != 0) seconds++;
                    
                    String formatted = FORMATTER.format(seconds);
                    messages.sendFormatted(sender, "teleport.failed.cooldown", "%cooldown%", formatted);
                    return;
                }
            }
            
            // resets cooldown
            cooldownStorage.refreshResetDate(name);
        }
        
        boolean isOther = other;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int doubleRadius = radius * 2;
            
            World world = target.getWorld();
            Location current = target.getLocation();
            
            int centerX = playerIsCenter ? current.getBlockX() : 0;
            int centerZ = playerIsCenter ? current.getBlockZ() : 0;
            
            Random random = new Random();
            boolean success = false;
            
            Location dest = null;
            
            while(!success) {
                int x = centerX + random.nextInt(doubleRadius) - radius;
                int z = centerZ + random.nextInt(doubleRadius) - radius;
                int y = world.getHighestBlockYAt(x, z);
                
                Block block = world.getBlockAt(x, y, z);
                while(!block.getType().equals(Material.AIR)) {
                    y++;
                    block = world.getBlockAt(x, y, z);
                }
                
                dest = new Location(world, x + 0.5, y, z + 0.5);
                
                if(protect && world.getBlockAt(x, y - 1, z).isLiquid())
                    continue;
                else success = true;
            }
            
            Location destionation = dest;
            int x = dest.getBlockX();
            int y = dest.getBlockY();
            int z = dest.getBlockZ();
            
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> target.teleport(destionation));
                
            if(isOther)
                messages.sendFormatted(sender, "teleport.success.other",
                        "%player%", target.getName(),
                        "%x%", x,
                        "%y%", y,
                        "%z%", z
                );
            
            messages.sendFormatted(target, "teleport.success.self",
                    "%x%", x,
                    "%y%", y,
                    "%z%", z
            );
        });
    }
    
    @Override
    public List<String> executeTabCompletion(CommandSender sender, CommandArguments arguments) {
        if(arguments.size() != 1 || !sender.hasPermission("randomtp.command.rtp.other")) return null;
        
        String arg = arguments.get(0).toLowerCase();
        return Bukkit.getOnlinePlayers()
                .parallelStream()
                .map(Player::getName)
                .filter(p -> p.toLowerCase().startsWith(arg))
                .collect(Collectors.toList());
    }
    
    public int getCooldown(Player p) {
        int cooldown = config.getInt("teleport.cooldown");
        
        int min = config.getGroups().keySet().stream()
                .filter(g -> p.hasPermission("group." + g))
                .mapToInt(g -> config.getGroups().get(g).getCooldown())
                .min().orElse(-1);
        
        return min < 0 ? cooldown : min;
    }
    
    static {
        FORMATTER.loadRussianTimeUnitFormats();
    }

}
