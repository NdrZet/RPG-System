package ru.custom.progression.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.custom.progression.api.PlayerStats;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Управляет загрузкой и сохранением данных игрока в JSON-файлы.
 * <p>
 * Путь файла: {@code [мир]/progression_data/{uuid}.json}<br>
 * Данные кэшируются в памяти на время сессии и сбрасываются при отключении.
 */
public final class DataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("progression/DataManager");

    /** Gson с форматированием для читаемых JSON-файлов. */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** Имя поддиректории прогрессии внутри корня мира. */
    private static final String PROGRESSION_DIR_NAME = "progression_data";

    /** Кэш данных в памяти: UUID → данные игрока. */
    private static final Map<UUID, PlayerStats> cache = new HashMap<>();

    /** Ссылка на сервер для получения пути к миру. */
    private static MinecraftServer server;

    // Приватный конструктор — утилитный класс.
    private DataManager() { }

    // ────────────────────────────────────────────────────────────────────────
    // Инициализация
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Устанавливает ссылку на сервер. Вызывается при запуске сервера.
     *
     * @param minecraftServer активный экземпляр сервера
     */
    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Публичные методы
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Загружает данные игрока из JSON-файла (или создаёт новые, если файл отсутствует).
     * Результат помещается в кэш.
     *
     * @param uuid уникальный идентификатор игрока
     * @return актуальные данные игрока (никогда не {@code null})
     */
    public static PlayerStats loadPlayer(UUID uuid) {
        // Возвращаем кэшированные данные, если уже загружены
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }

        Path filePath = getPlayerFilePath(uuid);

        if (Files.exists(filePath)) {
            try (Reader reader = Files.newBufferedReader(filePath)) {
                PlayerStats stats = GSON.fromJson(reader, PlayerStats.class);
                if (stats == null) stats = new PlayerStats();
                cache.put(uuid, stats);
                LOGGER.info("[Progression] Загружены данные игрока {}", uuid);
                return stats;
            } catch (IOException e) {
                LOGGER.error("[Progression] Ошибка чтения файла для {}: {}", uuid, e.getMessage());
            }
        }

        // Файл не найден — создаём новые данные
        PlayerStats newStats = new PlayerStats();
        cache.put(uuid, newStats);
        LOGGER.info("[Progression] Созданы новые данные для игрока {}", uuid);
        return newStats;
    }

    /**
     * Сохраняет кэшированные данные игрока в JSON-файл.
     * Если данных нет в кэше — операция игнорируется.
     *
     * @param uuid уникальный идентификатор игрока
     */
    public static void savePlayer(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats == null) {
            LOGGER.warn("[Progression] Попытка сохранить данные несуществующего игрока {}", uuid);
            return;
        }

        Path filePath = getPlayerFilePath(uuid);

        try {
            // Создаём директорию, если её нет
            Files.createDirectories(filePath.getParent());

            try (Writer writer = Files.newBufferedWriter(filePath)) {
                GSON.toJson(stats, writer);
            }
            LOGGER.info("[Progression] Данные игрока {} сохранены", uuid);
        } catch (IOException e) {
            LOGGER.error("[Progression] Ошибка записи файла для {}: {}", uuid, e.getMessage());
        }
    }

    /**
     * Возвращает кэшированные данные без чтения файла.
     * Может вернуть {@code null}, если игрок не был загружен.
     *
     * @param uuid уникальный идентификатор игрока
     * @return данные игрока или {@code null}
     */
    public static PlayerStats getPlayer(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Удаляет данные игрока из кэша после сохранения.
     * Вызывается при отключении игрока.
     *
     * @param uuid уникальный идентификатор игрока
     */
    public static void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Вспомогательные методы
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Формирует путь к JSON-файлу конкретного игрока.
     * Структура: {@code [мир]/progression_data/{uuid}.json}
     *
     * @param uuid UUID игрока
     * @return абсолютный путь к файлу
     */
    private static Path getPlayerFilePath(UUID uuid) {
        // LevelResource.ROOT — корень мира (например saves/world/).
        // Резолвим поддиректорию progression_data вручную,
        // т.к. конструктор LevelResource(String) закрыт в 1.21.11.
        return server.getWorldPath(LevelResource.ROOT)
                .resolve(PROGRESSION_DIR_NAME)
                .resolve(uuid + ".json");
    }
}
