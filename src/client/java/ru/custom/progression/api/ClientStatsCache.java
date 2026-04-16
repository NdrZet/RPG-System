package ru.custom.progression.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Клиентский кэш данных прогрессии текущего игрока.
 * <p>
 * Обновляется при получении {@link ru.custom.progression.network.StatsUpdatePayload}.
 * Используется {@link ru.custom.progression.mixin.client.InventoryScreenMixin}
 * для отрисовки интерфейса прогрессии.
 * <p>
 * Хранит флаг {@link #needsGuiReinit}, чтобы GUI мог перестроить кнопки
 * при изменении количества очков навыков.
 */
@Environment(EnvType.CLIENT)
public final class ClientStatsCache {

    /** Текущие данные игрока (никогда не null — инициализированы значениями по умолчанию). */
    private static PlayerStats cachedStats = new PlayerStats();

    /**
     * Флаг, сигнализирующий GUI о необходимости пересоздать кнопки.
     * Устанавливается при обновлении статов, сбрасывается в методе {@link #consumeReinitFlag()}.
     */
    private static boolean needsGuiReinit = false;

    private ClientStatsCache() { }

    // ────────────────────────────────────────────────────────────────────────
    // Публичные методы
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Обновляет кэш новыми данными, полученными с сервера.
     * Устанавливает флаг перестройки GUI.
     *
     * @param stats актуальные данные прогрессии
     */
    public static void update(PlayerStats stats) {
        if (stats == null) return;
        cachedStats = stats;
        needsGuiReinit = true;
    }

    /**
     * Возвращает текущие кэшированные данные игрока.
     * Гарантированно не возвращает {@code null}.
     *
     * @return данные прогрессии (дефолтные или с сервера)
     */
    public static PlayerStats get() {
        return cachedStats;
    }

    /**
     * Проверяет и сбрасывает флаг перестройки GUI.
     * Вызывается один раз за кадр из метода render() микина инвентаря.
     *
     * @return {@code true}, если GUI должен пересоздать кнопки
     */
    public static boolean consumeReinitFlag() {
        boolean value = needsGuiReinit;
        needsGuiReinit = false;
        return value;
    }

    /**
     * Сбрасывает кэш при выходе из мира.
     * После сброса возвращаются дефолтные данные нового Странника.
     */
    public static void reset() {
        cachedStats = new PlayerStats();
        needsGuiReinit = false;
    }
}
