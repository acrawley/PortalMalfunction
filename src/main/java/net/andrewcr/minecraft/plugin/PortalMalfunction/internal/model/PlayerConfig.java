package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model;

import lombok.Getter;
import lombok.Synchronized;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerConfig {
    private static final String INVENTORY_KEY = "Inventory";
    private static final String IS_TRIGGERED_KEY = "IsTriggered";
    private static final String IS_IN_PROGRESS_KEY = "IsInProgress";
    private static final String PREVIOUS_SPEED_KEY = "PreviousSpeed";

    private final Object configLock = ConfigStore.getInstance().getSyncObj();

    @Getter private final UUID playerId;
    @Getter private boolean isTriggered;
    @Getter private boolean isInProgress;
    @Getter private float previousSpeed;
    @Getter private final List<ItemStack> inventory;

    //region Constructors

    public PlayerConfig(UUID playerId) {
        this(playerId, false, false, 0, new ArrayList<>());
    }

    private PlayerConfig(UUID playerId, boolean isTriggered, boolean isInProgress, float previousSpeed, List<ItemStack> inventory) {
        this.playerId = playerId;
        this.isTriggered = isTriggered;
        this.isInProgress = isInProgress;
        this.previousSpeed = previousSpeed;
        this.inventory = inventory;
    }

    //endregion

    //region Serialization

    static PlayerConfig loadFrom(ConfigurationSection playerSection) {
        UUID playerId = UUID.fromString(playerSection.getName());
        boolean isTriggered = playerSection.getBoolean(IS_TRIGGERED_KEY, false);
        boolean isInProgress = playerSection.getBoolean(IS_IN_PROGRESS_KEY, false);
        float previousSpeed = (float)playerSection.getDouble(PREVIOUS_SPEED_KEY, 0);

        List<ItemStack> inventory = (List<ItemStack>) playerSection.getList(INVENTORY_KEY, new ArrayList<ItemStack>());

        return new PlayerConfig(playerId, isTriggered, isInProgress, previousSpeed, inventory);
    }

    void save(ConfigurationSection playersSection) {
        ConfigurationSection playerSection = playersSection.createSection(this.playerId.toString());

        playerSection.set(IS_TRIGGERED_KEY, this.isTriggered);

        if (this.isInProgress) {
            playerSection.set(IS_IN_PROGRESS_KEY, this.isInProgress);
            playerSection.set(PREVIOUS_SPEED_KEY, this.previousSpeed);
        }

        playerSection.set(INVENTORY_KEY, this.inventory);
    }

    //endregion

    //region Getters / Setters

    @Synchronized("configLock")
    public void setIsTriggered(boolean value) {
        this.isTriggered = value;

        ConfigStore.getInstance().notifyChanged();
    }

    @Synchronized("configLock")
    public void setIsInProgress(boolean value) {
        this.isInProgress = value;

        ConfigStore.getInstance().notifyChanged();
    }

    @Synchronized("configLock")
    public void setPreviousSpeed(float value) {
        this.previousSpeed = value;

        ConfigStore.getInstance().notifyChanged();
    }

    public void notifyInventoryChanged() {
        ConfigStore.getInstance().notifyChanged();
    }

    //endregion
}
