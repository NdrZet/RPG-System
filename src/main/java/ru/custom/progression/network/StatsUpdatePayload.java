package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ru.custom.progression.api.PlayerStats;

import java.util.HashSet;
import java.util.Set;

/**
 * Пакет Сервер → Клиент: передаёт актуальные данные прогрессии игрока.
 * Отправляется при каждом изменении статов.
 */
public record StatsUpdatePayload(
        int    level,
        int    experience,
        String rank,
        int    skillPoints,
        String playerClass,
        int    strength,
        int    agility,
        int    vitality,
        int    intelligence,
        Set<String> unlockedNodes
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatsUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("progression", "stats_update")
            );

    public static final StreamCodec<FriendlyByteBuf, StatsUpdatePayload> STREAM_CODEC =
            StreamCodec.of(StatsUpdatePayload::encode, StatsUpdatePayload::decode);

    private static void encode(FriendlyByteBuf buf, StatsUpdatePayload p) {
        buf.writeVarInt(p.level());
        buf.writeVarInt(p.experience());
        buf.writeUtf(p.rank());
        buf.writeVarInt(p.skillPoints());
        buf.writeUtf(p.playerClass());
        buf.writeVarInt(p.strength());
        buf.writeVarInt(p.agility());
        buf.writeVarInt(p.vitality());
        buf.writeVarInt(p.intelligence());
        buf.writeVarInt(p.unlockedNodes().size());
        for (String id : p.unlockedNodes()) buf.writeUtf(id);
    }

    private static StatsUpdatePayload decode(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        int exp   = buf.readVarInt();
        String rank = buf.readUtf();
        int sp = buf.readVarInt();
        String cls = buf.readUtf();
        int str = buf.readVarInt();
        int agi = buf.readVarInt();
        int vit = buf.readVarInt();
        int inti = buf.readVarInt();
        int nodeCount = buf.readVarInt();
        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) nodes.add(buf.readUtf());
        return new StatsUpdatePayload(level, exp, rank, sp, cls, str, agi, vit, inti, nodes);
    }

    public static StatsUpdatePayload from(PlayerStats stats) {
        return new StatsUpdatePayload(
                stats.getLevel(),
                stats.getExperience(),
                stats.getRank(),
                stats.getSkillPoints(),
                stats.getPlayerClass(),
                stats.getStrength(),
                stats.getAgility(),
                stats.getVitality(),
                stats.getIntelligence(),
                new HashSet<>(stats.getUnlockedNodes())
        );
    }

    public PlayerStats toStats() {
        PlayerStats s = new PlayerStats();
        s.setLevel(level);
        s.setExperience(experience);
        s.setRank(rank);
        s.setSkillPoints(skillPoints);
        s.setPlayerClass(playerClass);
        s.setStrength(strength);
        s.setAgility(agility);
        s.setVitality(vitality);
        s.setIntelligence(intelligence);
        s.setUnlockedNodes(new HashSet<>(unlockedNodes));
        return s;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
