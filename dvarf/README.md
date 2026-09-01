# DwarvenCore (Purpur 1.21.x)

Плагин реализует расу дварфов и кастомные предметы под Purpur/Paper API 1.21.x.

## Что уже реализовано

- Выбор расы через предмет `Эль дворфа`.
- Все баффы/дебаффы из ТЗ применяются во всех мирах.
- Кастомные предметы с PersistentData тегами.
- Крафт-ограничения (золотой стержень, большой бутыль).
- Дополнительные механики: перенос сундуков, vein mining, молот 3x3x1.
- Отдельный `config.yml` для таймеров, шансов и множителей.
- Защита от конфликтов с регионами: универсальная проверка через отмену BlockBreakEvent (совместимо с region-плагинами).
- Выбор расы сохраняется после перезагрузки сервера (через PDC в playerdata).

## Команды

- `/dwarf race set [игрок]`
- `/dwarf race remove [игрок]`
- `/dwarf give <item> [игрок]`

## Ключи предметов для /dwarf give

`dwarf_ale, miner_helmet, dwarf_hammer, sunglasses, dwarf_snot, golden_rod, tinted_plate, cave_gas_balloon, empty_balloon, cave_gas, ore_shield, mountain_elixir, big_bottle`

## Сборка

1. Открой папку `plugin/`.
2. Запусти `gradle build` (или `./gradlew build`, если используешь wrapper).
3. Готовый `.jar` появится в `plugin/build/libs/`.
4. Если интернет нестабильный, используй уже добавленный `gradle.properties` с увеличенными таймаутами.
5. Проект использует Kotlin DSL: `build.gradle.kts` и `settings.gradle.kts`.
6. Проект рассчитан на JDK 25 toolchain, но компилируется с `--release 21` для совместимости с Purpur 1.21.x.

## Конфиг

Файл: `plugin/src/main/resources/config.yml`

Основные секции:

- `chances`: шансы механик.
- `timers`: длительности и интервалы.
- `values`: множители урона, голода и прочие числовые параметры.

## Пакет и путь классов

Классы плагина лежат в пакете `org.examplee.dvarf`:

- `src/main/java/org/examplee/dvarf/...`