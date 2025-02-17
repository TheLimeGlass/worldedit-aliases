# worldedit-aliases

Allows builders to add aliases for worldedit patterns Spigot server side.

Reducing the time it takes to type out long block names, even with tab complete.

Example being "d" alias can reference "minecraft:diamond_block" for faster pattern parsing in any WE command.
In this example d = diamond_block and e = emerald_block.
```
//gmask d
//replace d e
//move 5 -m e
//set #l2d[d,e]
```

### Usage
- `/wealiases add <block> <alias>` - Adds an alias for a pattern.
- `/wealiases remove <alias>` - Removes an alias.
- `/wealiases list` - Lists all aliases.

### Permissions
- `worldeditaliases.command.remove` - Allows the player to remove aliases.
- `worldeditaliases.command.list` - Allows the player to list aliases.
- `worldeditaliases.command.add` - Allows the player to add aliases.
