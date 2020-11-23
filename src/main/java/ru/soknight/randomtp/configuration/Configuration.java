package ru.soknight.randomtp.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import lombok.Getter;
import ru.soknight.lib.configuration.AbstractConfiguration;
import ru.soknight.randomtp.RandomTP;

public class Configuration extends AbstractConfiguration {
    
	@Getter
	private final Map<String, GroupSpecifications> groups;
	
	public Configuration(RandomTP plugin) {
		super(plugin, "config.yml");
		
		super.refresh();
		
		this.groups = new HashMap<>();
		
		ConfigurationSection section = getFileConfig().getConfigurationSection("groups");
		Set<String> groups = section.getKeys(false);
		
		if(!groups.isEmpty()) {
			int defCooldown = getInt("teleport.cooldown");
			
			groups.forEach(g -> {
				int cooldown = section.getInt(g + ".cooldown", defCooldown);
	        	
	        	GroupSpecifications group = new GroupSpecifications(cooldown);
	        	this.groups.put(g, group);
			});
		}
	}
}
