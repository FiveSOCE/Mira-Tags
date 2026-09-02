# MiraTags

MiraTags is the GUI-first player-tag system for the Mira Minecraft plugin ecosystem. It targets **Paper 1.21.11** and **Java 21**, requires **MiraCore 0.1.0+** and **LuckPerms**, and exposes the currently equipped tag as a LuckPerms suffix.

## Current development version

**v0.1.0**

## Player workflow

Run:

```text
/tags
```

The selector shows enabled tags from `tags.yml`.

- unlocked tag: click to equip
- active tag: click again to clear it
- locked tag: shown as locked unless `gui.hide-locked` is enabled
- clear button: removes the current active tag
- pagination supports more than 45 tags

Only one MiraTag can be active at once.

## Ownership

A tag can be unlocked in three independent ways:

1. `default-unlocked: true`
2. an internal permanent grant from MiraTags
3. a LuckPerms/Bukkit permission configured on that tag

A blank permission means the tag is grant-only unless it is default unlocked.

Example:

```yaml
tags:
  veteran:
    enabled: true
    display-name: "&6Veteran"
    suffix: " &8[&6Veteran&8]"
    icon: GOLD_INGOT
    permission: "miratags.tag.veteran"
    default-unlocked: false
    sort-order: 20
    description:
      - "&7For long-time players."
```

## LuckPerms integration

MiraTags keeps ownership and the selected tag in its own `playerdata.yml`.

While a player is online, MiraTags manages exactly one LuckPerms suffix node at a reserved priority. Equipping or refreshing a tag clears the previous suffix at that reserved priority before applying the selected tag. The managed suffix is removed when the player leaves or MiraTags disables, then restored from `playerdata.yml` on the next join.

This keeps MiraTags selections persistent without accumulating old tag suffixes in LuckPerms.

The default reserved suffix priority is:

```yaml
luckperms:
  suffix-priority: 500
```

Keep this priority reserved for MiraTags. Any chat, tab or formatting system that reads LuckPerms suffix metadata can consume the active MiraTag. MiraTab can also use the public MiraTags API directly later.

## Admin commands

```text
/mtags grant <player> <tag>
/mtags revoke <player> <tag>
/mtags clear <player>
/mtags list
/mtags reload
/mtags test
/mtags help
```

Grant/revoke currently targets online players through the command path. The public API grants by UUID and can be used by Crates, Kits, Shop or other Mira plugins.

## Public API

MiraTags registers `MiraTagsApi` in MiraCore's service registry.

```java
MiraTagsApi tags = core.services().require(MiraTagsApi.class);
tags.grant(playerId, "veteran");
tags.equip(player, "veteran");
```

The API supports:

- enumerate tag definitions
- find a tag
- grant/revoke by UUID
- ownership checks
- active-tag lookup
- equip
- clear

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `miratags.use` | Everyone | Open and use `/tags` |
| `miratags.admin` | OP | MiraTags admin commands |

Individual tag permissions are configured per tag in `tags.yml`.

## Data files

```text
plugins/MiraTags/
├── config.yml
├── tags.yml
└── playerdata.yml
```

## First test pass

1. Install MiraCore, LuckPerms and MiraTags.
2. Restart Paper 1.21.11.
3. Run `/mtags test` and expect `7/7 passed`.
4. Run `/miracore status` and confirm MiraTags is HEALTHY.
5. Run `/mtags grant <yourname> example`.
6. Open `/tags` and equip **Example**.
7. Confirm your LuckPerms-aware chat/tab formatting receives the suffix.
8. Click the active tag again and confirm it clears.
9. Re-equip it, reconnect, and confirm the selection persists.
10. Revoke the tag and confirm it becomes locked unless another ownership source still grants it.

## Building

```bash
gradle clean test build
```

Output:

```text
build/libs/MiraTags-0.1.0.jar
```
