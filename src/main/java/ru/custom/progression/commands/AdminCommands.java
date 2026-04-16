package ru.custom.progression.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.custom.progression.StatEffects;
import ru.custom.progression.api.PlayerStats;
import ru.custom.progression.network.NetworkHandler;
import ru.custom.progression.storage.DataManager;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Команды администратора для управления прогрессией игроков.
 * <p>
 * Требует op-уровень 2. Все команды работают только с онлайн-игроками.
 * <ul>
 *   <li>{@code /progression give <player> xp <amount>} — выдать XP</li>
 *   <li>{@code /progression setlevel <player> <level>}  — установить уровень</li>
 *   <li>{@code /progression reset <player>}             — сброс прогресса</li>
 *   <li>{@code /progression info <player>}              — показать все статы</li>
 * </ul>
 */
public final class AdminCommands {

    private AdminCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                literal("progression")
                    .requires(source -> source.hasPermission(2))
                    .then(literal("give")
                        .then(argument("player", EntityArgument.player())
                            .then(literal("xp")
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                    .executes(AdminCommands::executeGiveXp)
                                )
                            )
                        )
                    )
                    .then(literal("setlevel")
                        .then(argument("player", EntityArgument.player())
                            .then(argument("level", IntegerArgumentType.integer(1, 1000))
                                .executes(AdminCommands::executeSetLevel)
                            )
                        )
                    )
                    .then(literal("reset")
                        .then(argument("player", EntityArgument.player())
                            .executes(AdminCommands::executeReset)
                        )
                    )
                    .then(literal("info")
                        .then(argument("player", EntityArgument.player())
                            .executes(AdminCommands::executeInfo)
                        )
                    )
            )
        );
    }

    // ── /progression give <player> xp <amount> ───────────────────────────────

    private static int executeGiveXp(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        PlayerStats stats = DataManager.getPlayer(target.getUUID());
        if (stats == null) {
            ctx.getSource().sendFailure(Component.literal("Данные игрока не найдены"));
            return 0;
        }

        int levelBefore = stats.getLevel();
        stats.addExperience(amount);
        int levelAfter = stats.getLevel();

        if (levelAfter > levelBefore) {
            StatEffects.apply(target, stats);
        }
        DataManager.savePlayer(target.getUUID());
        NetworkHandler.sendStatsToPlayer(target, stats);

        String suffix = levelAfter > levelBefore ? " → уровень " + levelAfter : "";
        ctx.getSource().sendSuccess(() ->
            Component.literal("[Progression] " + target.getName().getString()
                + " получил " + amount + " XP" + suffix)
                .withStyle(ChatFormatting.GREEN),
            true
        );
        return 1;
    }

    // ── /progression setlevel <player> <level> ───────────────────────────────

    private static int executeSetLevel(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int newLevel = IntegerArgumentType.getInteger(ctx, "level");

        PlayerStats stats = DataManager.getPlayer(target.getUUID());
        if (stats == null) {
            ctx.getSource().sendFailure(Component.literal("Данные игрока не найдены"));
            return 0;
        }

        int oldLevel = stats.getLevel();
        // Пересчитываем очки навыков пропорционально разнице уровней (мин. 0)
        int spDelta = (newLevel - oldLevel) * 2;
        stats.setSkillPoints(Math.max(0, stats.getSkillPoints() + spDelta));
        stats.setLevel(newLevel);
        stats.setExperience(0);
        stats.setRank(stats.calculateRank());

        StatEffects.apply(target, stats);
        DataManager.savePlayer(target.getUUID());
        NetworkHandler.sendStatsToPlayer(target, stats);

        target.sendSystemMessage(
            Component.literal("⬆ Уровень изменён администратором: " + newLevel)
                .withStyle(ChatFormatting.GOLD)
        );
        ctx.getSource().sendSuccess(() ->
            Component.literal("[Progression] Уровень " + target.getName().getString()
                + " установлен на " + newLevel)
                .withStyle(ChatFormatting.GOLD),
            true
        );
        return 1;
    }

    // ── /progression reset <player> ──────────────────────────────────────────

    private static int executeReset(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        PlayerStats stats = DataManager.getPlayer(target.getUUID());
        if (stats == null) {
            ctx.getSource().sendFailure(Component.literal("Данные игрока не найдены"));
            return 0;
        }

        stats.reset();
        StatEffects.apply(target, stats);
        DataManager.savePlayer(target.getUUID());
        NetworkHandler.sendStatsToPlayer(target, stats);

        target.sendSystemMessage(
            Component.literal("Ваш прогресс был сброшен администратором")
                .withStyle(ChatFormatting.RED)
        );
        ctx.getSource().sendSuccess(() ->
            Component.literal("[Progression] Прогресс " + target.getName().getString() + " сброшен")
                .withStyle(ChatFormatting.RED),
            true
        );
        return 1;
    }

    // ── /progression info <player> ───────────────────────────────────────────

    private static int executeInfo(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        PlayerStats stats = DataManager.getPlayer(target.getUUID());
        if (stats == null) {
            ctx.getSource().sendFailure(Component.literal("Данные игрока не найдены"));
            return 0;
        }

        int xpNeeded = stats.getLevel() * 100;
        Component info = Component.literal("=== " + target.getName().getString() + " ===")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal("\nКласс: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(stats.getPlayerClass()).withStyle(ChatFormatting.YELLOW)))
            .append(Component.literal("\nУровень: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(stats.getLevel())).withStyle(ChatFormatting.YELLOW)))
            .append(Component.literal("\nОпыт: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(stats.getExperience() + " / " + xpNeeded).withStyle(ChatFormatting.GREEN)))
            .append(Component.literal("\nРанг: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(stats.getRank()).withStyle(ChatFormatting.AQUA)))
            .append(Component.literal("\nОчки навыков: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(stats.getSkillPoints())).withStyle(ChatFormatting.LIGHT_PURPLE)))
            .append(Component.literal("\nСила: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(stats.getStrength())).withStyle(ChatFormatting.RED)))
            .append(Component.literal("  Ловкость: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(stats.getAgility())).withStyle(ChatFormatting.GREEN)))
            .append(Component.literal("  Выносливость: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(stats.getVitality())).withStyle(ChatFormatting.BLUE)))
            .append(Component.literal("  Интеллект: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(stats.getIntelligence())).withStyle(ChatFormatting.LIGHT_PURPLE)));

        ctx.getSource().sendSuccess(() -> info, false);
        return 1;
    }
}
