# MittensHomes

A Bukkit/Spigot/Paper plugin for Minecraft **1.21.x**, built with **Java 21**. Players can save up to three homes, manage them through a menu, and teleport after standing still for five seconds. Successful teleports have a five-second cooldown.

## License and access

Copyright (c) 2026 Matt0k (PinStudio). All rights reserved.

This plugin is proprietary and **not open source**. Access is restricted to explicitly authorized Waked developers, solely to develop, maintain and operate Minecraft servers run by Waked. See [LICENSE](LICENSE) for the full terms, including restrictions on redistribution and reuse.

For access or permission, contact **matt0k on Discord**. No license key or activation is required.

## Open in IntelliJ IDEA

1. Choose **File → Open**, select this folder or `pom.xml`, and open it as a Maven project.
2. Under **Project Structure → Project SDK**, select or download **JDK 21**. Select the same JDK for Maven under **Settings → Build Tools → Maven → Runner**.
3. Let Maven import the dependencies. The Maven Wrapper is included, so no separate Maven installation is needed.
4. Run the shared **Build plugin** configuration. The JAR is generated at `target/mittenshomes-1.0.1.jar`.

Shared run configurations are stored in `.run/`. IntelliJ generates `.idea/` and `.iml` files locally; these should not be committed.

## Test on a local server

1. Download a **Paper 1.21.x** server from [PaperMC](https://papermc.io/downloads/paper), or use your Spigot 1.21.x JAR, and place it at `server/server.jar`.
2. Run **Install local plugin** to build, test and copy the plugin to `server/plugins/mittenshomes.jar`.
3. Run **Run local server**. On its first launch, the server creates `eula.txt` and stops. Read the EULA linked in that file and, if you accept it, change `eula=false` to `eula=true`, then restart.
4. Connect to `localhost` using a Minecraft client with the same version and run `/homes`.

After each change, stop the server using `stop` in its console, run **Install local plugin**, then run **Run local server** again. Avoid `/reload`. You can also launch the JAR configuration with **Debug** in IntelliJ.

The server and its worlds are ignored by Git. The plugin JAR cannot run on its own: it must be loaded by a Minecraft server. All use remains subject to the license above.

## Build from the terminal

With Java 21 installed (`JAVA_HOME` set or `java` available in `PATH`):

```sh
./mvnw clean verify
./mvnw -Plocal-server clean verify
```

On Windows, use `mvnw.cmd clean verify` or `mvnw.cmd -Plocal-server clean verify`.

## Commands and permissions

| Command | Action | Permission |
| --- | --- | --- |
| `/homes` | Open the menu, even without any saved homes | `mittenshomes.homes` |
| `/home [name]` | Open the menu or teleport to a home | `mittenshomes.home` |
| `/sethome <name>` | Create or update a home | `mittenshomes.sethome` |
| `/delhome <name>` | Delete a home | `mittenshomes.delhome` |

Permissions are granted by default. Each menu action also checks its own permission. Left-click to create a home or teleport; right-click to review a deletion, then confirm or go back.

Names accept 1–16 characters: letters, numbers, internal spaces, `_` and `-`. For example, use `/sethome My base`, then `/home My base`. Names are case-sensitive. The shortcuts `1`, `2` and `3` refer to `Home 1`, `Home 2` and `Home 3` when those homes exist, unless a home already has that exact numeric name. This rule applies consistently to all three commands.

## In-game menus

`/homes` and `/home` open a 54-slot home collection: charcoal borders, cyan accents,
three centered home cards, coordinates, world status and clear click hints. Empty slots
save your current location. Offline worlds stay visible with a muted bed icon.
Navigation uses short click sounds; saving a home plays a soft chime. No resource pack is needed.

`/homeadmin` opens the admin dashboard. Browse online/offline owners, open their homes,
travel or review a deletion, and use the back button to return to the same page.
Lists paginate at 21 entries, including legacy players with more than three homes.
The admin shortcut in the player menu only appears with `mittenshomes.admin.view`.

Menu deletion opens a separate **Keep home / Delete home** screen. Permissions are
checked again on confirmation. If the home changes while the screen is open, deletion
is refused so you can review the new state. Closing the screen keeps the home.
Direct `/delhome` and `/homeadmin delete` commands still delete immediately.

## Administration

Use `/homeadmin` to manage homes belonging to other players, including offline players.
These actions are available to server operators by default. Grant `mittenshomes.admin`
for all administrative actions, or assign the individual permissions below.
Ordinary home permissions do not grant administrative access.

| Command | Action | Permission |
| --- | --- | --- |
| `/homeadmin players [page]` | List players with saved homes, ten per page, with UUIDs and home counts | `mittenshomes.admin.view` |
| `/homeadmin list <player\|uuid>` | List a player's homes and coordinates | `mittenshomes.admin.view` |
| `/homeadmin info <player\|uuid> <home name>` | Inspect a home's world, coordinates and orientation | `mittenshomes.admin.view` |
| `/homeadmin tp <player\|uuid> <home name>` | Teleport yourself to that home immediately | `mittenshomes.admin.teleport` |
| `/homeadmin delete <player\|uuid> <home name>` | Permanently delete that home | `mittenshomes.admin.delete` |

For example: `/homeadmin list Alex`, `/homeadmin tp Alex My base`, or
`/homeadmin delete Alex My base`. Home names with spaces and numeric shortcuts work
as they do in player commands. Tab completion only exposes actions and homes the
sender is allowed to access.

Player names are resolved from online players and local server records; use the full
UUID if the name is unavailable. No remote player-profile lookup is performed.
The console can list, inspect and delete homes; teleportation requires an in-game player.
Admin teleports bypass the normal warmup and cooldown, but still respect teleport
cancellation by other plugins. Unavailable worlds are reported and their homes preserved.
Successful admin teleports and deletions are recorded in the server log. Deletion is
immediate and cancels any pending home teleport for the owner.

## Data and fixes

The plugin name remains `homes` to preserve the existing `plugins/homes/config.yml` location and data format. Back up this file before updating.

- Named homes appear in the menu. Legacy homes exceeding the limit remain accessible through the paginated menu and commands so players can delete them.
- The three-home limit is enforced by the storage layer, including menu actions. Existing homes can still be updated.
- Clicks in the player's inventory, drag operations and unrelated inventories no longer trigger menu actions.
- Names containing dots or color codes are rejected to protect YAML paths and messages.
- Unavailable worlds no longer cause homes to be lost. Invalid entries remain in the file for repair; an invalid player UUID no longer prevents other players' homes from loading.
- Saves use a temporary file and atomic replacement where supported. Failed writes restore the previous in-memory state.
- Movement, world changes, death and disconnection cancel pending teleports. Rotating the camera alone does not.
- Teleports rejected by the server no longer display a success message or apply a cooldown.

`./mvnw verify` runs unit tests with a mocked server API. In-game checks should cover creation through commands and the menu, reconnecting and restarting, denied permissions, movement during the delay, and teleport cancellation by another plugin.

References: [Spigot Maven API](https://www.spigotmc.org/wiki/spigot-maven/), [Paper setup and Java versions](https://docs.papermc.io/paper/getting-started/).
