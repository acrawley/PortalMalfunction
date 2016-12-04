package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.commands;

import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandExecutorBase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PMResetCommand extends CommandBase {
    @Override
    public CommandExecutorBase getExecutor() {
        return new PMResetCommandExecutor();
    }

    private class PMResetCommandExecutor extends CommandExecutorBase {
        private PMResetCommandExecutor() {
            super("pm reset", "pm.command");
        }

        @Override
        protected boolean invoke(String[] args) {
            if (args.length != 1) {
                return false;
            }

            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                this.error("Unknown player '" + args[0] + "'!");
                return false;
            }

            this.sendMessage("Resetting player '" + player.getName() + "'");

            player.setWalkSpeed(0.2f);
            player.setBedSpawnLocation(null);

            return true;
        }
    }
}
