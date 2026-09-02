# MiraTags

MiraTags is the GUI-first player-tag system for the Mira Minecraft plugin ecosystem. It targets **Paper 1.21.11** and **Java 21**, requires **MiraCore 0.1.0+** and **LuckPerms**, and exposes the currently equipped tag as a LuckPerms suffix.

## Download

[**Download MiraTags v0.1.1**](https://github.com/FiveSOCE/Mira-Tags/releases/download/v0.1.1/MiraTags-0.1.1.jar)

Current release: **v0.1.1**

## v0.1.1 quick tag creation

Admins can create tags entirely in-game:

```text
/mtags add <Tag Name>
```

Example:

```text
/mtags add King
```

MiraTags then waits for that player's next chat message and privately captures the tag format. The message is cancelled and is not broadcast.

```text
&8[&eKing&8]
```

That creates a persistent `king` definition in `plugins/MiraTags/tags.yml` with:

- display name `King`
- suffix ` &8[&eKing&8]` (MiraTags adds the leading space automatically)
- `NAME_TAG` GUI icon
- LuckPerms backing permission `miratags.tag.king`
- default locked state
- immediate availability to `/tags`, `/mtags grant`, LuckPerms permissions and the public API

Type `cancel` instead of a tag format to abort creation. Duplicate ids are rejected safely.

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

Tags created with `/mtags add` automatically use `miratags.tag.<id>` as their permission backing.

## LuckPerms integration

MiraTags keeps ownership and the selected tag in its own `playerdata.yml`.

While a player is online, MiraTags manages exactly one LuckPerms suffix node at a reserved priority. Equipping or refreshing a tag clears the previous suffix at that reserved priority before applying the selected tag. The managed suffix is removed when the player leaves or MiraTags disables, then restored from `playerdata.yml` on the next join.

The default reserved suffix priority is:

```yaml
luckperms:
  suffix-priority: 500
```

Keep this priority reserved for MiraTags. A chat/tab formatting plugin must read LuckPerms metadata to display the suffix.

## Admin commands

```text
/mtags add <Tag Name>
/mtags grant <player> <tag>
/mtags revoke <player> <tag>
/mtags clear <player>
/mtags list
/mtags reload
/mtags test
/mtags help
```

`/mtags add` is player-only because its second step is captured through private chat input.

## Public API

MiraTags registers `MiraTagsApi` in MiraCore's service registry.

```java
MiraTagsApi tags = core.services().require(MiraTagsApi.class);
tags.grant(playerId, "king");
tags.equip(player, "king");
```

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `miratags.use` | Everyone | Open and use `/tags` |
| `miratags.admin` | OP | MiraTags admin commands |

Individual tag permissions are stored per definition in `tags.yml`.

## Data files

```text
plugins/MiraTags/
├── config.yml
├── tags.yml
└── playerdata.yml
```

## Quick v0.1.1 test

1. Install MiraCore, LuckPerms and MiraTags.
2. Run `/mtags add King`.
3. Enter `&8[&eKing&8]` in chat and confirm it is not broadcast.
4. Run `/mtags list` and confirm `king` exists.
5. Check `tags.yml` and confirm the persistent definition and `miratags.tag.king` permission.
6. Run `/mtags grant <yourname> king`.
7. Open `/tags`, equip King, and confirm your LuckPerms-aware chat format displays it.
8. Restart the server and confirm the created tag still exists.

## Building

```bash
gradle clean test build
```

Output:

```text
build/libs/MiraTags-0.1.1.jar
```
