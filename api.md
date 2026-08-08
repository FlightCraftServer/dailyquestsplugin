# FCClans — документация API

Документация по программному интерфейсу (API) плагина FCClans v1.0.0
для разработчиков плагинов Minecraft (Paper/Spigot, API 26.2).

Автор: Saspe

---

## Оглавление

1. [Обзор](#обзор)
2. [Подключение к API](#подключение-к-api)
3. [Публичное API: `FCClansAPI`](#публичное-api-fcclansapi)
4. [Доступ к менеджеру кланов (`ClanManager`)](#доступ-к-менеджеру-кланов-clanmanager)
5. [События](#события)
6. [Модели данных](#модели-данных)
7. [PlaceholderAPI](#placeholderapi)
8. [Команды](#команды)
9. [Права (permissions)](#права-permissions)
10. [Примеры кода](#примеры-кода)
11. [База данных](#база-данных)

---

## Обзор

FCClans — плагин кланов для Minecraft. Предоставляет два уровня интеграции:

| Уровень | Способ | Описание |
|---|---|---|
| Публичное API | `ru.fcclans.api.FCClansAPI` | Статические методы для быстрых проверок: состоит ли игрок в клане, получить клан или его имя. |
| Полный доступ | `ru.fcclans.FCClans` + `ClanManager` | Чтение и управление кланами, рангами, деньгами, участниками из любого другого плагина. |
| События | `ru.fcclans.api.events` | Вступление (`ClanJoinEvent`) и выход (`ClanLeaveEvent`) из клана. |

---

## Подключение к API

Добавьте плагин как мягкую зависимость (`softdepend`) в `plugin.yml`
своего плагина:

```yaml
softdepend: [FCClans]
```

Получить доступ к плагину можно двумя способами:

```java
// 1. Через Bukkit PluginManager
FCClans plugin = (FCClans) Bukkit.getPluginManager().getPlugin("FCClans");
if (plugin == null) {
    // FCClans не установлен
}

// 2. Через статический синглтон (доступен только после onEnable)
FCClans plugin = FCClans.getInstance();
```

Для сборки подключите jar плагина (или зависимость в градле):

```gradle
dependencies {
    compileOnly files("libs/FCClans.jar")
}
```

---

## Публичное API (`FCClansAPI`)

**Пакет:** `ru.fcclans.api`
**Класс:** `FCClansAPI` (статические методы)

Предназначено для быстрых проверок, без необходимости брать зависимость
на внутренние классы плагина.

### Методы

| Метод | Возврат | Описание |
|---|---|---|
| `isInClan(Player player)` | `boolean` | Состоит ли игрок в каком-либо клане. |
| `getClan(Player player)` | `Clan` | Клан игрока (или `null`). |
| `getClanName(Player player)` | `String` | Название клана игрока (или `null`). |
| `addClanMoney(String clanName, double amount)` | `boolean` | Начислить указанную сумму в казну клана (серверные деньги, без Vault). `true` при успехе, `false` если провайдер не установлен или клан не найден. |

### Внутренний интерфейс `ClanProvider`

Плагин FCClans регистрирует реализацию интерфейса `FCClansAPI.ClanProvider`
при включении. Другим плагинам **не нужно** вызывать `setProvider()`.
Интерфейс существует для перекрытия реализации извне.

```java
public interface ClanProvider {
    boolean isInClan(UUID playerUuid);
    boolean isInClan(Player player);
    Clan getClan(UUID playerUuid);
    Clan getClan(Player player);
    String getClanName(UUID playerUuid);
    String getClanName(Player player);
    boolean addClanMoney(String clanName, double amount);
}
```

Все обращения к API безопасны: если провайдер не установлен (или плагин
выключен), методы вернут `false` / `null`, а не бросят исключение.

---

## Доступ к менеджеру кланов (`ClanManager`)

**Пакет:** `ru.fcclans`
**Класс:** `FCClans` (главный класс плагина)

`ClanManager` — главная точка управления кланами. Получить его:

```java
ClanManager cm = FCClans.getInstance().getClanManager();
```

Также из `FCClans` доступны:
- `getInstance()` — синглтон плагина;
- `getConfiguration()` — `Config` (настройки и сообщения);
- `getDatabaseManager()` — `DatabaseManager` (пул подключений HikariCP);
- `getVaultHook()` — `VaultHook` (интеграция с Vault).

### Создание и удаление

| Метод | Синтаксис | Описание |
|---|---|---|
| `Clan createClan(String name, Player leader)` | `throws Exception` | Создаёт клан. Лидеру выставляется ранг `Leader` (order 0), списывается стоимость из конфига. Вызывает `ClanJoinEvent`. |
| `void deleteClan(int clanId)` | `throws Exception` | Полностью удаляет клан; генерирует `ClanLeaveEvent` для всех онлайн-участников. |
| `void forceDeleteClan(String clanName)` | `throws Exception` | Админская версия удаления по имени. |
| `void renameClan(Player player, String newName)` | `throws Exception` | Переименование клана (права: лидер или право `nameedit`). |

### Получение (read-only)

| Метод | Возврат | Описание |
|---|---|---|
| `Clan getClanByName(String name)` | `Clan` | По названию (или `null`). |
| `Clan getClanById(int id)` | `Clan` | По ID (или `null`). |
| `Clan getPlayerClan(UUID uuid)` | `Clan` | Клан игрока (или `null`). |
| `List<Clan> getAllClans()` | `List<Clan>` | Все кланы на сервере. |
| `List<ClanMember> getMembers(int clanId)` | `List<ClanMember>` | Участники клана. |
| `ClanMember getLeader(int clanId)` | `ClanMember` | Лидер клана (ранг `Leader`) или `null`. |
| `int getMemberCount(int clanId)` | `int` | Число участников. |
| `String getMemberRank(UUID uuid)` | `String` | Название ранга игрока (или `null`). |
| `boolean isLeader(UUID uuid)` | `boolean` | Лидер ли игрок (ранг с order == 0). |
| `String getMotd(int clanId)` | `String` | Текст MOTD клана. |
| `double getBalance(int clanId)` | `double` | Баланс клана. |

### Ранги

| Метод | Описание |
|---|---|
| `List<ClanRank> getRanks(int clanId)` | Все ранги клана, отсортироанные по `order`. |
| `ClanRank getRank(int clanId, String rankName)` | Ранг по названию (или `null`). |
| `boolean hasRankPermission(UUID uuid, String permission)` | Есть ли у игрока право в его ранге. |
| `void addRank(Player player, String rankName, int order)` | Создать ранг (только лидер, order 0 зарезервирован). |
| `void renameRank(Player player, String oldName, String newName)` | Переименовать ранг (только лидер). |
| `void deleteRank(Player player, String rankName)` | Удалить ранг (нельзя лидерский и если есть участники). |
| `void setPlayerRank(Player sender, Player target, String rankName)` | Установить ранг игроку (лидер или право `setrank`). |
| `void addPermissionToRank(Player player, String rankName, String permission)` | Выдать право рангу (только лидер). |

> **Примечание.** Управляющие методы рангов ориентированы на командное
> использование: первым аргументом принимают `Player`-инициатора и сами
> проверяют права/отправляют сообщения. Для программного изменения
> рекомендуется вызывать их от онлайн-игрока или добавлять собственные
> SQL-запросы через `DatabaseManager`.

### Приглашения и членство

| Метод | Описание |
|---|---|
| `void invitePlayer(Player inviter, Player target)` | Пригласить игрока (лидер или право `addmember`). |
| `void acceptInvite(Player player)` | Принять приглашение; вступает с рангом `Member`. Вызывает `ClanJoinEvent`. |
| `void denyInvite(Player player)` | Отклонить приглашение. |
| `void kickMember(Player kicker, String targetName)` | Исключить участника (нужен ранг выше; лидер или право `delmember`). |
| `void leaveClan(Player player)` | Покинуть клан (лидер не может; вызвывает `ClanLeaveEvent`). |
| `void forceKickPlayer(String playerName)` | Админское исключение (вызывает `ClanLeaveEvent`). |
| `void forceAddMember(Player target, String clanName)` | Админское добавление игрока в клан. |
| `void setLeader(String targetName, String clanName)` | Админская смена лидера. |

### Финансы

| Метод | Описание |
|---|---|
| `void deposit(Player player, double amount)` | Пополнить баланс клана из своих средств (Vault). |
| `void withdraw(Player player, double amount)` | Снять средства (лидер или право `balanceedit`). |
| `void giveBonus(Player issuer, String targetName, double amount)` | Выдать премию участнику из баланса. |
| `void adminAddFunds(String clanName, double amount, UUID actorUuid)` | Админское начисление серверных денег в казну (без Vault). `actorUuid` — инициатор (для консоли используйте `ClanManager.CONSOLE_UUID`). |
| `List<BalanceTransaction> getBalanceHistory(int clanId, int limit)` | История транзакций (последние `limit`). |
| `double getBalance(int clanId)` | Текущий баланс. |

Типы транзакций: `DEPOSIT` (пополнение), `WITHDRAW` (снятие), `BONUS` (премия), `ADMIN_DEPOSIT` (начисление админом).

### MOTD, дом, префикс, цвет

| Метод | Описание |
|---|---|
| `void setMotd(Player player, String motd)` | Установить MOTD (лидер или право `motd`). |
| `void removeMotd(Player player)` | Удалить MOTD. |
| `void setClanHome(Player player)` | Установить точку дома в текущей позиции. |
| `void sendHomeInfo(Player player)` | Показать координаты базы. |
| `void setPrefix(Player player, String prefix)` | Префикс (лидер или право `prefixedit`, длина ≤ `prefix-max-length`). |
| `void setColor(Player player, String color)` | Цвет (лидер или право `coloredit`). |
| `void broadcastMessage(Player sender, String message)` | Оповещение клана (лидер или право `broadcast`). |

---

## События

**Пакет:** `ru.fcclans.api.events`

### `ClanJoinEvent`

Вызывается при вступлении в клан:
- сразу после создания клана (`createClan`);
- при принятии приглашения (`acceptInvite`);
- при админском добавлении (`forceAddMember`).

| Метод | Описание |
|---|---|
| `Player getPlayer()` | Игрок, вступивший в клан. |
| `Clan getClan()` | Клан, в который вступил. |

### `ClanLeaveEvent`

Вызывается при выходе из клана — самостоятельно (`leaveClan`), по исключении
(`kickMember`, `forceKickPlayer`) или при удалении клана (`deleteClan`
— для всех онлайн-участников).

| Метод | Описание |
|---|---|
| `Player getPlayer()` | Игрок, покинувший клан. |
| `Clan getClan()` | Клан, из которого вышел игрок. Может быть `null`, если клан уже не существует. |

### Пример обработки события

```java
public class MyListener implements Listener {

    @EventHandler
    public void onClanJoin(ClanJoinEvent event) {
        Player p = event.getPlayer();
        Clan clan = event.getClan();
        p.sendMessage("Теперь вы в клане " + clan.getName());
        // p.teleport к базе клана, награда и т.п.
    }

    @EventHandler
    public void onClanLeave(ClanLeaveEvent event) {
        Player p = event.getPlayer();
        Clan clan = event.getClan();
        if (clan != null) {
            p.sendMessage("Вы покинули клан " + clan.getName());
        }
    }
}
```

---

## Модели данных

**Пакет:** `ru.fcclans.models`

### `Clan`

Представление клана. Хранит: `id`, `name`, `prefix`, `color`, координаты
дома (`homeWorld`, `homeX`, `homeY`, `homeZ`), `balance`, `motd`.

| Метод | Описание |
|---|---|
| `int getId()` / `String getName()` | ID и название. |
| `String getPrefix()` / `setPrefix(String)` | Тег (может быть с цвет-кодами `&`). |
| `String getColor()` / `setColor(String)` | Цветовая метка. |
| `String getHomeWorld()` / `double getHomeX/Y/Z()` | Координаты дома. |
| `boolean hasHome()` | Есть ли установленный дом. |
| `double getBalance()` | Баланс. |
| `String getMotd()` / `setMotd(String)` | MOTD клана. |

### `ClanMember`
Участник клана: `id`, `clanId`, `uuid` (игрока), `rankName`.

### `ClanRank`
Ранг клана: `id`, `clanId`, `name`, `order` (0 — лидер, чем больше — тем
ниже в иерархии), `permissions` (`Set<String>`).

| Метод | Описание |
|---|---|
| `String getName()`, `int getOrder()` | Название и порядок. |
| `Set<String> getPermissions()` | Права ранга. |
| `boolean hasPermission(String permission)` | Проверка права. |
| `addPermission(String)` / `removePermission(String)` | Изменение прав (в памяти; сохраните через `ClanManager.addPermissionToRank` или SQL). |
| `String permissionsToString()` | Права через запятую (формат БД). |

### `BalanceTransaction`
Запись истории: `id`, `clanId`, `playerUuid`, `amount`, `type`, `timestamp`.

### `ClanInvite`
Приглашение: `id`, `clanId`, `uuid` (кого пригласили), `inviterUuid`,
`timestamp` (мс).

---

## PlaceHolderAPI

**Пакет:** `ru.fcclans`

Плагин регистрирует расширение PlaceholderAPI с идентификатором `fcclans`
(зависимость: PlaceholderAPI). Работает только с игроком, состоящим в
клане; вне клана пустые значения.

| Плейсхолдер | Описание |
|---|---|
| `%fcclans_name%` | Название клана. |
| `%fcclans_prefix%` | Префикс (сырой, с `&-кодами`). |
| `%fcclans_tag%` | Префикс с раскрашенными цвет-кодами (или пусто). |
| `%fcclans_color%` | Цвет клана. |
| `%fcclans_balance%` | Баланс клана. |
| `%fcclans_rank%` | Название ранга игрока. |

Использование: `%fcclans_name%`, placeholder в любом сообщении, в
конфиге FCClans (в `broadcast-format` и `socialspy-format` они уже
поддерживаются автоматически).

---

## Команды

Главная команда: `/clan` (алиасы: `/fcclans`, `/c`).

| Команда | Доступ | Описание |
|---|---|---|
| `/clan create <название>` | все | Создать клан (с оплатой). |
| `/clan info <клан>` | все | Информация о клане. |
| `/clan clanlist [стр]` | все | Список кланов (с пагинацией). |
| `/clan accept` / `/clan deny` | все | Принять/отклонить приглашение. |
| `/clan home` | участник | Координаты базы. |
| `/clan balance` | участник | Баланс клана. |
| `/clan balance put <сумма>` | участник | Пополнить баланс. |
| `/clan balance draw <сумма>` | лидер / `balanceedit` | Снять средства. |
| `/clan balance bonus <игрок> <сумма>` | лидер / `balanceedit` | Премия участнику. |
| `/clan balance add <клан> <сумма>` | `fcclans.admin` | Начислить средства клану (серверные деньги, работает и из консоли). |
| `/clan balance history` | участник | История операций (10 шт.). |
| `/clan list [стр]` / `members` | участник | Участники. |
| `/clan leave` | участник | Покинуть клан. |
| `/clan invite <игрок>` | лидер / `addmember` | Приглашение. |
| `/clan kick <игрок>` | лидер / `delmember` | Исключить участника. |
| `/clan color <цвет>` | лидер / `coloredit` | Сменить цвет. |
| `/clan prefix <префикс>` | лидер / `prefixedit` | Установить префикс. |
| `/clan rename <название>` | лидер / `nameedit` | Переименовать клан. |
| `/clan sethome` | лидер / `sethome` | Размыкать точку дома. |
| `/clan motd <текст>` | лидер / `motd` | Установить MOTD. |
| `/clan offmotd` | лидер / `motd` | Удалить MOTD. |
| `/clan broadcast <текст>` | лидер / `broadcast` | Оповещение клана. |
| `/clan rank` | лидер | Список рангов. |
| `/clan addrank <название> <порядок>` | лидер | Создать ранг. |
| `/clan renamerank <старый> <новый>` | лидер | Переименовать ранг. |
| `/clan delrank <название>` | лидер | Удалить ранг. |
| `/clan setrank <игрок> <ранг>` | лидер / `setrank` | Назначить ранг. |
| `/clan addperm <ранг> <право>` | лидер | Выдать право рангу. |
| `/clan remove` (свою клан) | лидер | Удалить свой клан. |
| `/clan top [стр]` | все | Топ кланов по балансу. |

### Админские команды (право `fcclans.admin`)

| Команда | Описание |
|---|---|
| `/clan remove <клан>` | Удалить любой клан. |
| `/clan setleader <игрок> <клан>` | Назначить лидера. |
| `/clan invite <игрок> <клан>` | Добавить игрока в любой клан. |
| `/clan kick <игрок>` | Исключить из любого клана. |
| `/clan balance add <клан> <сумма>` | Начислить серверные деньги в казну клана (доступна и из консоли). |
| `/clan members <клан> [стр]` | Участники любого клана. |

---

## Права (permissions)

В `plugin.yml` объявлено право `fcclans.admin` (по умолчанию `op`) —
доступ ко всем админским функциям.

Внутри клана используются права рангов (не Bukkit-права), хранящиеся в
ранге и проверяемые через `hasRankPermission`:
`addmember`, `delmember`, `setrank`, `balanceedit`, `prefixedit`,
`coloredit`, `nameedit`, `sethome`, `motd`, `broadcast`.

---

## Примеры кода

### Пример: проверка игрока и вывод имени клана

```java
import ru.fcclans.api.FCClansAPI;
import org.bukkit.entity.Player;

public void sendClanStatus(Player player) {
    if (FCClansAPI.isInClan(player)) {
        player.sendMessage("Клан: " + FCClansAPI.getClanName(player));
    } else {
        player.sendMessage("Вы без клана.");
    }
}
```

### Пример: использование ClanManager из другого плагина

```java
import ru.fcclans.FCClans;
import ru.fcclans.models.Clan;
import ru.fcclans.models.ClanMember;

public void example(Player player) {
    FCClans api = FCClans.getInstance();
    if (api == null) return;

    Clan clan = api.getClanManager().getPlayerClan(player.getUniqueId());
    if (clan == null) {
        player.sendMessage("Вы не в клане");
        return;
    }

    int count = api.getClanManager().getMemberCount(clan.getId());
    List<ClanMember> members = api.getClanManager().getMembers(clan.getId());
    double balance = api.getClanManager().getBalance(clan.getId());

    player.sendMessage("Клан " + clan.getName() +
            " | участников: " + count +
            " | баланс: " + balance);
}
```

### Пример: слушатель событий

```java
import ru.fcclans.api.events.ClanJoinEvent;
import ru.fcclans.api.events.ClanLeaveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClansListener implements Listener {

    @EventHandler
    public void onJoin(ClanJoinEvent e) {
        // e.getPlayer(), e.getClan()
    }

    @EventHandler
    public void onLeave(ClanLeaveEvent e) {
        // e.getPlayer(), e.getClan() // может быть null
    }
}
```

### Пример: плейсхолдер

```
Слайс: %fcclans_name% | Ранг: %fcclans_rank% | Баланс: %fcclans_balance%
```

---

## База данных

Поддерживается SQLite (по умолчанию, файл `plugins/FCClans/clans.db`) и
MySQL. Настройки в `config.yml` (секция `database`).

Таблицы:

- `fc_clans` — кланы (`id`, `name`, `prefix`, `color`, `home_world`, `home_x/y/z`, `balance`, `motd`);
- `fc_members` — участники (`clan_id`, `uuid`, `rank_name`);
- `fc_ranks` — ранги (`clan_id`, `name`, `rank_order`, `permissions` — CSV через запятую);
- `fc_invites` — приглашения (`clan_id`, `uuid`, `inviter_uuid`, `timestamp`);
- `fc_balance_history` — финансовые операции (`clan_id`, `player_uuid`, `amount`, `type`, `timestamp`).

При удалении клана записи из связанных таблиц удаляются каскадно (foreign keys).

---

## Ограничения и примечания

- Публичные методы `FCClansAPI` возвращают `null`/`false`, если FCClans
  не включен или провайдер не зарегистрирован.
- `ClanManager` — методы работы с кланами кидают `Exception` с сообщением для
  игрока. В своём коде либо оборачивайте в try/catch, либо вызывайте
  read-only методы.
- Событие `ClanLeaveEvent` может иметь `getClan() == null`.
- Плейсхолдеры проверки прав рангов не привязаны к Bukkit-permissions.