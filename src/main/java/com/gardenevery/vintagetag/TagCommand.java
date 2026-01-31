package com.gardenevery.vintagetag;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public class TagCommand extends CommandBase {

    public final CommandRegistry registry = new CommandRegistry();

    public TagCommand() {
        registry.register("info", 1, this::executeInfo);
        registry.register("reload", 2, this::executeReload);
    }

    @Nonnull
    @Override
    public String getName() {
        return "tag";
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return "tag.command.usage";
    }

    @Override
    public void execute(
            @Nonnull MinecraftServer server,
            @Nonnull ICommandSender sender,
            String[] args
    ) {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        registry.execute(server, sender, args);
    }

    @Nonnull
    @Override
    public List<String> getTabCompletions(
            @Nonnull MinecraftServer server,
            @Nonnull ICommandSender sender,
            @Nonnull String[] args,
            BlockPos targetPos
    ) {
        return registry.getSuggestions(args);
    }

    @Override
    public boolean checkPermission(
            @Nonnull MinecraftServer server,
            @Nonnull ICommandSender sender
    ) {
        return true;
    }

    public void showHelp(ICommandSender sender) {
        sender.sendMessage(new TextComponentTranslation("tag.command.help.title"));

        for (var cmd : registry.getCommands()) {
            hasPermission(sender, cmd.level);
            var description = new TextComponentTranslation(cmd.getDescription());
            sender.sendMessage(description);
        }
    }

    public void executeInfo(MinecraftServer server, ICommandSender sender, String[] args) {
        sender.sendMessage(new TextComponentTranslation("tag.command.statistics.title"));

        sender.sendMessage(
                new TextComponentTranslation(
                        "tag.command.statistics.items",
                        TagHelper.item().tagCount(),
                        TagHelper.item().associationCount(),
                        TagHelper.item().keyCount()
                )
        );

        sender.sendMessage(
                new TextComponentTranslation(
                        "tag.command.statistics.fluids",
                        TagHelper.fluid().tagCount(),
                        TagHelper.fluid().associationCount(),
                        TagHelper.fluid().keyCount()
                )
        );

        sender.sendMessage(
                new TextComponentTranslation(
                        "tag.command.statistics.blocks",
                        TagHelper.block().tagCount(),
                        TagHelper.block().associationCount(),
                        TagHelper.block().keyCount()
                )
        );

        sender.sendMessage(
                new TextComponentTranslation(
                        "tag.command.statistics.total",
                        TagHelper.tagCount(),
                        TagHelper.associationCount(),
                        TagHelper.keyCount()
                )
        );
    }

    public void executeReload(MinecraftServer server, ICommandSender sender, String[] args) {
        long startTime = System.currentTimeMillis();

        if (TagConfig.enableOreSync) {
            OreDictSync.sync();
        }

        if (TagConfig.enableModScanner) {
            TagLoader.scanModTags();
        }

        if (TagConfig.enableConfigScanner) {
            TagLoader.scanConfigTags();
        }

        TagManager.bake();
        NetworkSync.sync(null);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        sender.sendMessage(
                new TextComponentTranslation(
                        "tag.command.reload.success.time",
                        duration,
                        duration / 1000.0
                )
        );
    }

    public boolean hasPermission(ICommandSender sender, int level) {
        return sender.canUseCommand(level, "tag");
    }

    public class CommandRegistry {
        private final Map<String, RegisteredCommand> commands = new HashMap<>();
        private final List<String> commandNames = new ArrayList<>();

        public void register(String name, int level, CommandExecutor executor) {
            var cmd = new RegisteredCommand(name, level, executor);
            commands.put(name.toLowerCase(), cmd);
            commandNames.add(name);
        }

        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            var commandName = args[0].toLowerCase();
            var cmd = commands.get(commandName);

            if (cmd == null) {
                showHelp(sender);
                return;
            }

            if (!hasPermission(sender, cmd.level)) {
                sender.sendMessage(new TextComponentTranslation("tag.command.no_permission"));
                return;
            }

            cmd.execute(server, sender, Arrays.copyOfRange(args, 1, args.length));
        }

        public List<String> getSuggestions(String[] args) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, commandNames);
            }

            return Collections.emptyList();
        }

        public Collection<RegisteredCommand> getCommands() {
            return commands.values();
        }
    }

    @FunctionalInterface
    public interface CommandExecutor {
        void execute(MinecraftServer server, ICommandSender sender, String[] args);
    }

    @Desugar
    public record RegisteredCommand(String name, int level, CommandExecutor executor) {

        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            executor.execute(server, sender, args);
        }

        public String getDescription() {
            return "tag.command.help." + name;
        }
    }
}
