# MiraTags

MiraTags is the GUI-first player-tag system for the Mira Minecraft plugin ecosystem. It targets **Paper 1.21.11** and **Java 21**, requires **MiraCore 0.1.0+** and **LuckPerms**, and exposes the currently equipped tag as a LuckPerms suffix.

## Download

[**Download MiraTags v0.1.3**](https://github.com/FiveSOCE/Mira-Tags/releases/download/v0.1.3/MiraTags-0.1.3.jar)

Current release: **v0.1.3**

## Create tags in-game

Preferred command:

```text
/mtags create <Tag Name>
```

Legacy alias retained:

```text
/mtags add <Tag Name>
```

Example:

```text
/mtags create King
```

MiraTags waits for that player's next chat message and privately captures the tag format. The message is cancelled and is not broadcast.

```text
&8[&eKing&8]
```

That creates a persistent `king` definition in `plugins/MiraTags/tags.yml` with display name `King`, suffix ` &8[&eKing&8]`, `NAME_TAG` GUI icon, LuckPerms backing permission `miratags.tag.king`, default locked state, and immediate availability to the GUI, grants and API.

Type `cancel` instead of a tag format to abort creation. Duplicate ids are rejected safely.

## Delete tags

Both admin aliases support deletion:

```text
/mtags delete <tag>
/mtag delete <tag>
```

Deletion removes the persistent tag definition, purges MiraTags internal grants and active selections, and refreshes online players immediately. External LuckPerms permission assignments are intentionally left untouched.

## Player workflow

```text
/tags
```

The selector shows enabled tags from `tags.yml`. Unlocked tags can be equipped, the active tag can be clicked again to clear it, locked tags are shown unless configured otherwise, and pagination supports more than 45 tags.

Only one MiraTag can be active at once.

## Ownership

A tag can be unlocked by `default-unlocked: true`, an internal MiraTags grant, or a configured LuckPerms/Bukkit permission. Tags created in-game automatically use `miratags.tag.<id>` as their permission backing.

## LuckPerms integration

MiraTags stores ownership and selection in `playerdata.yml`. While a player is online it manages one LuckPerms suffix node at a reserved priority. A chat/tab formatting plugin must read LuckPerms metadata to display the suffix.

Default priority:

```yaml
luckperms:
  suffix-priority: 500
```

## Admin commands

```text
/mtags create <Tag Name>
/mtags add <Tag Name>
/mtags delete <tag>
/mtag delete <tag>
/mtags grant <player> <tag>
/mtags revoke <player> <tag>
/mtags clear <player>
/mtags list
/mtags reload
/mtags test
/mtags help
```

Both `/mtags` and `/mtag` are aliases for the MiraTags admin command. Creation is player-only because the second step is captured through private chat input.

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

## Data files

```text
plugins/MiraTags/
├── config.yml
├── tags.yml
└── playerdata.yml
```

## Quick v0.1.3 test

1. Install MiraCore, LuckPerms and MiraTags.
2. Run `/version MiraTags` and confirm `0.1.3`.
3. Type `/mtags ` and confirm `create`, `add`, and `delete` appear in tab completion.
4. Run `/mtags create King` and enter `&8[&eKing&8]` in chat.
5. Confirm `king` appears in `/mtags list` and `/tags`.
6. Run `/mtags delete king` and confirm it disappears immediately.

## Building

```bash
gradle clean test build
```

Output:

```text
build/libs/MiraTags-0.1.3.jar
```
