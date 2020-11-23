package ru.soknight.randomtp;

import org.bukkit.plugin.java.JavaPlugin;

import ru.soknight.lib.configuration.Messages;
import ru.soknight.randomtp.commands.CommandRtp;
import ru.soknight.randomtp.configuration.Configuration;

public class RandomTP extends JavaPlugin {
    
    private Configuration config;
    private Messages messages;
    
    @Override
	public void onEnable() {
		// configs initialization
		loadConfigs();
		
		// command executor initialization
		registerCommands();
    }
    
    private void loadConfigs() {
        this.config = new Configuration(this);
        this.messages = new Messages(this, "messages.yml");
    }
    
    private void registerCommands() {
        new CommandRtp(this, config, messages);
    }
	
}
