# ChessCraft

ChessCraft is a Bukkit plugin that allows you to play chess on your CraftBukkit Minecraft server.
 
## Installation and Usage

Detailed documentation can be found in BukkitDev: http://dev.bukkit.org/server-mods/chesscraft

## Building

If you want to build ChessCraft yourself, you will need Maven.

1a) Download a copy of Vault.jar (1.1.1 minimum required, but get the latest) from http://dev.bukkit.org/server-mods/vault/

1b) Run 'mvn install:install-file -DgroupId=net.milkbowl -DartifactId=vault -Dversion=1.1.1 -Dpackaging=jar -Dfile=Vault.jar' (adjust version accordingly)

2a) Download a copy of ScrollingMenuSign.jar (0.6 minimum required, but get the latest) from http://dev.bukkit.org/server-mods/scrollingmenusign

2b) Run 'mvn install:install-file -DgroupId=me.desht -DartifactId=scrollingmenusign -Dversion=1.1 -Dpackaging=jar -Dfile=ScrollingMenuSign.jar' (adjust version accordingly)

2c) Alternatively, "git clone https://github.com/desht/ScrollingMenuSign" and build it from source (see https://github.com/desht/ScrollingMenuSign)

3) Run 'mvn clean install'

This should give you a copy of ChessCraft.jar under the target/ directory.

Use 'mvn eclipse:eclipse' to create the .project and .classpath files if you want to open the project in Eclipse.

## License

ChessCraft by Des Herriott is licensed under the [Gnu GPL v3](http://www.gnu.org/licenses/gpl-3.0.html). 
