package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import io.github.stealthykamereon.passlock.command.eventcommand.EventCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public abstract class CommandExecutor implements org.bukkit.command.CommandExecutor {

    private Class<? extends EventCommand> eventCommandClass;
    protected PassLock passLock;

    public CommandExecutor(PassLock passLock) {
        this.passLock = passLock;
        this.eventCommandClass = null;
    }

    public CommandExecutor(PassLock passLock, Class<? extends EventCommand> eventCommandClass) {
        this.passLock = passLock;
        this.eventCommandClass = eventCommandClass;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Console commands support is not implemented, if you need it, please tell me on the spigot page.");
            return true;
        }
        Player p = (Player) commandSender;
        String permission = command.getPermission();
        if (permission == null || p.hasPermission(permission)) {
            boolean commandSuccessful = onCommand(p, command, strings);
            if (commandSuccessful && requireEvent()) {
                try {
                    EventCommand eventCommand = (EventCommand) this.eventCommandClass.getConstructors()[0].newInstance(passLock);
                    passLock.getCommandMap().put(p, eventCommand);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } else {
            passLock.sendMessage(p, "noPermissions");
        }
        return true;
    }

    protected abstract boolean onCommand(Player player, Command command, String[] args);

    public boolean requireEvent() {
        return eventCommandClass != null;
    }
}
