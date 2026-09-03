# MiraTags

MiraTags is the GUI-first player-tag system for the Mira Paper server suite. It manages persistent tag definitions, player ownership and selection, exposes the active tag through LuckPerms metadata, and supports administrative, timed and programmatic grants.

## Download

[**Download MiraTags v0.1.3**](https://github.com/FiveSOCE/Mira-Tags/releases/download/v0.1.3/MiraTags-0.1.3.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0 or newer
- LuckPerms

## How MiraTags Works

Tags are persistent definitions stored in `plugins/MiraTags/tags.yml`. Players open `/tags` to browse enabled tags, equip an unlocked tag or clear the currently equipped tag. Only one MiraTag can be active at a time. Player ownership and active selection are stored in `playerdata.yml`.

A tag can be unlocked by `default-unlocked: true`, an internal MiraTags grant, a timed grant, or the configured LuckPerms/Bukkit permission. While a player is online, MiraTags manages one LuckPerms suffix node at its reserved priority so chat/tab plugins that read LuckPerms metadata can display the selected tag.

Administrators can create tags in-game with `/mtags create <Tag Name>`. MiraTags then captures that administrator's next chat message privately as the tag format, cancels the chat broadcast, generates a persistent ID and backing permission such as `miratags.tag.king`, and immediately makes the tag available to the GUI/API. Typing `cancel` aborts the creation flow. Deleting a tag removes its MiraTags definition, internal grants and active selections but intentionally does not remove external LuckPerms assignments.

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
