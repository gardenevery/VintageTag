# VintageTag

VintageTag is a lightweight and extensible tag system mod for Minecraft Forge (primarily for 1.12.2), bringing robust "tag" functionality inspired by Minecraft 1.13+'s official tag system to older game versions.

## Features

- **Flexible Tag Structure:**
    - Supports creating and loading tags for items, blocks, and fluids via both configuration files and resource packs/mod JARs.
    - Directory convention:
        - `config/tags/item/`, `config/tags/block/`, `config/tags/fluid/`
        - Tags can use uppercase, numbers, and custom namespaces (e.g., `item/Abc_1.json` -> tag "Abc_1").

- **Additive or Replacing Mode:**
    - Each tag file can use `"replace": true` to override/replace previous content or default to additively merging entries.

- **Deep Tag Hierarchy:**
    - Supports multi-level subfolders for expressive tag naming, i.e., `item/a/b/c.json` is parsed as tag `a:b/c`.
    - Directory nesting is validated to prevent excessive depth.

- **Dynamic Tag Management & Sync:**
    - Tag scanning supports both static resources and on-disk config without requiring Minecraft restart.
    - Efficient client/server sync for tag data with limit safeguards.

- **Command Support:**
    - In-game `/tag` commands for info and reloading tags.
    - Command permissions can be managed per action.

- **Constants & Helpers:**
    - Helper methods for interoperability with Minecraft Forge and vanilla classes, OreDictionary compatibility, and flexible lookups.

## Usage

1. **Define Tags:**
    - Place JSON files in the appropriate `config/tags/<type>/` folder or package them in your mod under `data/tags/<type>/`.
    - File naming: `[A-Za-z0-9_]+.json`
    - Each tag file format:

      ```json
      {
        "replace": true,
        "values": [
          "minecraft:iron_ingot",
          "mod:item_name"
        ]
      }
      ```

2. **Reload Tags:**
    - Use `/tag reload` in-game or via server console to refresh tags without restarting the game.

3. **Information:**
    - Use `/tag info` to view info about the current tags.

## Example

- Example tag file: `config/tags/item/myMetals.json`
    ```json
    {
      "values": [
        "minecraft:iron_ingot",
        "minecraft:gold_ingot"
      ]
    }
    ```
- This creates a tag called `myMetals` containing two items.

## Credit

- [ForgeDevEnv](https://github.com/CleanroomMC/ForgeDevEnv) ([MIT License](https://github.com/CleanroomMC/ForgeDevEnv/blob/master/LICENSE)).

## License

MIT License — see [LICENSE](LICENSE).