package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model;

import lombok.Getter;
import net.andrewcr.minecraft.plugin.BasePluginLib.config.ConfigurationFileBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.LocationUtil;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.Plugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigStore extends ConfigurationFileBase {
    //region Private Fields

    private static final String CONFIGURATION_VERSION_KEY = "ConfigurationVersion";
    private static final String TRIGGER_PORTAL_KEY = "TriggerPortal";
    private static final String REDIRECT_PORTAL_KEY = "RedirectPortal";
    private static final String CHEST_LOCATION_KEY = "ChestLocation";
    private static final String PLAYERS_KEY = "Players";

    private final Map<UUID, PlayerConfig> playerConfigs;

    @Getter private String triggerPortal;
    @Getter private String redirectPortal;
    @Getter private Location chestLocation;

    //endregion

    //region Singleton

    public static ConfigStore getInstance() {
        return Plugin.getInstance().getConfigStore();
    }

    //endregion

    // region Constructor

    public ConfigStore() {
        super(Plugin.getInstance());
        this.playerConfigs = new HashMap<>();
    }

    //endregion

    //region Serialization

    @Override
    protected String getFileName() {
        return "config.yml";
    }

    @Override
    protected void loadCore(YamlConfiguration configuration) {
        String version = configuration.getString(CONFIGURATION_VERSION_KEY);
        switch (version) {
            case "1.0":
                this.loadV1_0Config(configuration);
                return;

            default:
                Plugin.getInstance().getLogger().severe("Unknown portal configuration version '" + version + "'!");
        }

    }

    private void loadV1_0Config(YamlConfiguration config) {
        this.triggerPortal = config.getString(TRIGGER_PORTAL_KEY);
        this.redirectPortal = config.getString(REDIRECT_PORTAL_KEY);

        String locString = config.getString(CHEST_LOCATION_KEY);
        this.chestLocation = LocationUtil.locationFromString(locString);

        ConfigurationSection players = config.getConfigurationSection(PLAYERS_KEY);
        if (players != null) {
            for (String playerId : players.getKeys(false)) {
                this.addPlayerConfig(PlayerConfig.loadFrom(players.getConfigurationSection(playerId)));
            }
        }

        Plugin.getInstance().getLogger().info("Loaded " + this.playerConfigs.size() + " portal(s)!");
    }

    @Override
    protected void saveCore(YamlConfiguration configuration) {
        configuration.set(CONFIGURATION_VERSION_KEY, "1.0");
        configuration.set(TRIGGER_PORTAL_KEY, this.triggerPortal);
        configuration.set(REDIRECT_PORTAL_KEY, this.redirectPortal);

        if (this.chestLocation != null) {
            configuration.set(CHEST_LOCATION_KEY, LocationUtil.locationToIntString(this.chestLocation, true, false, false));
        }

        ConfigurationSection players = configuration.createSection(PLAYERS_KEY);
        for (PlayerConfig config : this.playerConfigs.values()) {
            config.save(players);
        }

        Plugin.getInstance().getLogger().info("Saved " + this.playerConfigs.size() + " portal(s)!");
    }

    //endregion

    public void addPlayerConfig(PlayerConfig config) {
        this.playerConfigs.put(config.getPlayerId(), config);
        this.notifyChanged();
    }

    public PlayerConfig getPlayerConfig(Player player) {
        if (this.playerConfigs.containsKey(player.getUniqueId())) {
            return this.playerConfigs.get(player.getUniqueId());
        }

        PlayerConfig config = new PlayerConfig(player.getUniqueId());
        this.addPlayerConfig(config);

        return config;
    }
}