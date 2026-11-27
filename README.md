# MiraTags

## Version-specific downloads

| Paper version | Java | JAR | Download |
| --- | --- | --- | --- |
| **1.18.2** | 17 | `MiraTags_1.18.2.jar` | [Download legacy JAR](https://github.com/FiveSOCE/Mira-Tags/releases/download/dev-1.18.2/MiraTags_1.18.2.jar) |
| **1.21.11–26.2** | 21 baseline; server may require Java 25 | `MiraTags_1.21.11-26.2.jar` | [Download modern JAR](https://github.com/FiveSOCE/Mira-Tags/releases/download/compat-modern-1.21.11-26.2/MiraTags_1.21.11-26.2.jar) |

Both named JAR assets have been published following successful CI builds. **Intermediate versions 1.19–1.21.10 and in-game integration are not yet certified.** Use the matching MiraCore version.

[Legacy source](https://github.com/FiveSOCE/Mira-Tags/tree/compat/1.18.2) · [Modern source](https://github.com/FiveSOCE/Mira-Tags/tree/main) · [All releases](https://github.com/FiveSOCE/Mira-Tags/releases)

## Requirements and integrations

- **Required:** Paper and Java for the chosen server target, matching **MiraCore** and **LuckPerms**.
- **Optional:** MiraCosmetics for unlock/equip audio presentation.
- **Data:** Definitions in `tags.yml`, player entitlements and selections in `playerdata.yml`. Back up before migration.
- **Permissions:** `miratags.use` (everyone), `miratags.admin` (OP), `miratags.tag.<id>` (per-tag unlocks).

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/tags` | `miratags.use` | Open player tag selector |
| `/mtags create <name>`, `/mtags add <name>` | `miratags.admin` | Create a tag using private chat input |
| `/mtags delete <tag>` | `miratags.admin` | Delete tag |
| `/mtags grant <player> <tag>`, `/mtags revoke <player> <tag>` | `miratags.admin` | Manage ownership |
| `/mtags clear <player>`, `/mtags list`, `/mtags reload`, `/mtags test` | `miratags.admin` | Selection and administration |
| `/mtagtime <player> <tag> <30m\|12h\|7d>` | `miratags.admin` | Grant time-limited tag |

## How MiraTags Works

Tags are persistent definitions stored in `plugins/MiraTags/tags.yml`. Players open `/tags` to browse enabled tags, equip an unlocked tag or clear the currently equipped tag. Every tag entry uses a `NAME_TAG` item and the item name is the actual formatted suffix preview, including its configured colors and brackets, rather than the raw tag ID. Only one MiraTag can be active at a time. Player ownership and active selection are stored in `playerdata.yml`.

A tag can be unlocked by `default-unlocked: true`, an internal MiraTags grant, a timed grant, or the configured LuckPerms/Bukkit permission. In v0.1.5 every enabled tag is also synchronized into LuckPerms as a backing group named `miratag_<id>` containing its `miratags.tag.<id>` permission. Starter tags therefore exist in LuckPerms immediately on first boot instead of only existing inside `tags.yml`. When a player equips a tag, MiraTags applies it through the LuckPerms console command path at fixed weight `0` so chat/tab plugins that read LuckPerms metadata receive the selected tag in the expected suffix format.

Administrators can create tags in-game with `/mtags create <Tag Name>`. MiraTags then captures that administrator's next chat message privately as the tag format, cancels the chat broadcast, generates a persistent ID and backing permission such as `miratags.tag.king`, and immediately makes the tag available to the GUI/API. Typing `cancel` aborts the creation flow. Deleting a tag removes its MiraTags definition, internal grants and active selections but intentionally does not remove external LuckPerms assignments.

v0.1.5 adds MiraCore milestone-driven unlocks, timed tag ownership and seasonal champion support. Built-in milestone mappings currently cover FTop Champion, Pinata Slayer and Crate Jackpot, while season champion milestones can generate/grant season-specific tags. A fresh install also ships with eight editable generic starter tags: OG, Grinder, PvP, Builder, Collector, Lucky, Veteran and GG.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/tags` | `miratags.use` | Opens the player tag selector GUI. |
| `/mtags create <Tag Name>` | `miratags.admin` | Starts the two-step in-game tag creation flow. |
| `/mtags add <Tag Name>` | `miratags.admin` | Legacy alias for tag creation. |
| `/mtags delete <tag>` | `miratags.admin` | Deletes a persistent tag definition and clears MiraTags-owned grants/selections for it. |
| `/mtags grant <player> <tag>` | `miratags.admin` | Grants permanent internal ownership of a tag. |
| `/mtags revoke <player> <tag>` | `miratags.admin` | Revokes an internal tag grant. |
| `/mtags clear <player>` | `miratags.admin` | Clears the selected player's active tag. |
| `/mtags list` | `miratags.admin` | Lists registered tag definitions. |
| `/mtags reload` | `miratags.admin` | Reloads MiraTags configuration/data. |
| `/mtags test` | `miratags.admin` | Runs MiraTags diagnostics/self-tests. |
| `/mtags help` | `miratags.admin` | Shows administration help. |
| `/mtagtime <player> <tag> <30m|12h|7d>` | `miratags.admin` | Grants a tag for a limited duration in current source. |

Admin aliases: `/miratags`, `/mtags`, `/mtag`.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miratags.use` | Everyone | Allows opening and using the tag selector. |
| `miratags.admin` | OP | Allows tag creation, deletion, grants, timed grants, reloads and diagnostics. |
| `miratags.tag.<id>` | Configured per tag | Typical backing permission used to unlock a specific tag. |


## Starter LuckPerms Links

A fresh install creates/synchronizes the following LuckPerms-backed tag groups:

- `miratag_og` -> `miratags.tag.og`
- `miratag_grinder` -> `miratags.tag.grinder`
- `miratag_pvp` -> `miratags.tag.pvp`
- `miratag_builder` -> `miratags.tag.builder`
- `miratag_collector` -> `miratags.tag.collector`
- `miratag_lucky` -> `miratags.tag.lucky`
- `miratag_veteran` -> `miratags.tag.veteran`
- `miratag_gg` -> `miratags.tag.gg`

New tags created in-game are synchronized to LuckPerms immediately as well.

## MiraCosmetics Audio Integration (0.1.6)

MiraCosmetics audio hooks cover tag equip/remove, normal unlocks and milestone/season unlock celebrations. MiraCosmetics remains optional.

## LuckPerms Equip Hotfix (0.1.7)

Tag selection from `/tags` now applies the selected tag through LuckPerms as a console-level suffix command:

```text
lp user <username> meta addsuffix 0 "<suffix>"
```

Weight is always `0`.

When changing or clearing tags, MiraTags removes only suffix nodes that match known MiraTag values at the new weight `0` or the legacy MiraTags priority `500`. Active tag selections owned through default unlocks or LuckPerms permissions are also preserved correctly across menu refreshes/rejoins.

## Idempotent Suffix Reconciliation (0.1.8)

MiraTags still performs its periodic reconciliation pass so timed/expired tag state can be corrected, but it no longer blindly re-runs the LuckPerms suffix command every pass.

Before applying an active tag, MiraTags now checks the player's LuckPerms suffix nodes for the exact selected MiraTag value at weight `0`.

- if the correct suffix already exists, MiraTags does nothing
- clicking a new tag runs the console `addsuffix` command once
- if another system removes or changes the expected suffix later, MiraTags can repair it on a future reconciliation pass

This removes repeated LuckPerms command spam while keeping timed-tag cleanup and state repair working.
