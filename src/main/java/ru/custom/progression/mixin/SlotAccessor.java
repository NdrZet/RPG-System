package ru.custom.progression.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor-интерфейс для изменения {@code final} полей {@link Slot}.
 *
 * <p>В MC 1.21.11 поля {@code Slot.x} и {@code Slot.y} объявлены как {@code final},
 * поэтому прямое присваивание невозможно. {@link Accessor} с {@link Mutable}
 * генерирует сеттеры через ASM bytecode — final-флаг снимается на уровне JVM.
 *
 * <p><b>Имена полей (intermediary):</b>
 * <ul>
 *   <li>Mojang: {@code x} → intermediary: {@code field_7873}</li>
 *   <li>Mojang: {@code y} → intermediary: {@code field_7872}</li>
 * </ul>
 *
 * <p>Использование в {@link InventoryMenuMixin}:
 * <pre>
 *   ((SlotAccessor) slot).setX(-2000);
 *   ((SlotAccessor) slot).setY(-2000);
 *   int x = ((SlotAccessor) slot).getX();
 * </pre>
 */
@Mixin(Slot.class)
public interface SlotAccessor {

    /**
     * Устанавливает горизонтальную позицию слота.
     * Mojang mapping name: {@code x}
     */
    @Mutable
    @Accessor("x")
    void setX(int x);

    /**
     * Устанавливает вертикальную позицию слота.
     * Mojang mapping name: {@code y}
     */
    @Mutable
    @Accessor("y")
    void setY(int y);

    /**
     * Читает горизонтальную позицию (для проверки повторного применения).
     * Mojang mapping name: {@code x}
     */
    @Accessor("x")
    int getX();

    /**
     * Читает вертикальную позицию.
     * Mojang mapping name: {@code y}
     */
    @Accessor("y")
    int getY();
}
