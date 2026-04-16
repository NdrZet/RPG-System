package ru.custom.progression.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ru.custom.progression.api.PlayerStats;

/**
 * Пакет Сервер → Клиент: передаёт актуальные данные прогрессии игрока.
 * Отправляется при каждом изменении статов (повышение уровня, трата очков и т.д.).
 * <p>
 * Поля записываются в {@link FriendlyByteBuf} вручную для максимальной
 * совместимости без зависимости от дополнительных кодеков.
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
        int    intelligence
) implements CustomPacketPayload {

    /** Уникальный идентификатор пакета. */
    public static final CustomPacketPayload.Type<StatsUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("progression", "stats_update")
            );

    /**
     * Кодек для сериализации / десериализации пакета.
     * Порядок записи и чтения полей должен совпадать.
     */
    public static final StreamCodec<FriendlyByteBuf, StatsUpdatePayload> STREAM_CODEC =
            StreamCodec.of(StatsUpdatePayload::encode, StatsUpdatePayload::decode);

    // ────────────────────────────────────────────────────────────────────────
    // Вспомогательные методы кодека
    // ────────────────────────────────────────────────────────────────────────

    /** Запись полей в буфер (порядок строго соответствует decode). */
    private static void encode(FriendlyByteBuf buf, StatsUpdatePayload payload) {
        buf.writeVarInt(payload.level());
        buf.writeVarInt(payload.experience());
        buf.writeUtf(payload.rank());
        buf.writeVarInt(payload.skillPoints());
        buf.writeUtf(payload.playerClass());
        buf.writeVarInt(payload.strength());
        buf.writeVarInt(payload.agility());
        buf.writeVarInt(payload.vitality());
        buf.writeVarInt(payload.intelligence());
    }

    /** Чтение полей из буфера. */
    private static StatsUpdatePayload decode(FriendlyByteBuf buf) {
        return new StatsUpdatePayload(
                buf.readVarInt(),   // level
                buf.readVarInt(),   // experience
                buf.readUtf(),      // rank
                buf.readVarInt(),   // skillPoints
                buf.readUtf(),      // playerClass
                buf.readVarInt(),   // strength
                buf.readVarInt(),   // agility
                buf.readVarInt(),   // vitality
                buf.readVarInt()    // intelligence
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // Фабричный метод
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Создаёт пакет из объекта {@link PlayerStats}.
     *
     * @param stats данные игрока
     * @return готовый пакет для отправки клиенту
     */
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
                stats.getIntelligence()
        );
    }

    /**
     * Конвертирует пакет обратно в {@link PlayerStats}.
     *
     * @return объект данных, восстановленный из пакета
     */
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
        return s;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
