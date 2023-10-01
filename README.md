# SkinChanger

This Java Class is made for a minecraft spigot plugin.
It was tested in version 1.8

# Can't find some librarys?

if you can't find librarys like "org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;"
run the [build tools](https://hub.spigotmc.org/jenkins/job/BuildTools/) from bukkit and change the dependency from:
```
<dependency>
  <groupId>org.spigotmc</groupId>
  <artifactId>spigot-api</artifactId> <-------------- "spigot-api"
  <version>1.8.8-R0.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```
to
```
<dependency>
  <groupId>org.spigotmc</groupId>
  <artifactId>spigot</artifactId> <-------------- "spigot"
  <version>1.8.8-R0.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```
