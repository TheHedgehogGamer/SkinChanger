package de.setlex.rank;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class SkinChanger {

    private final Map<UUID, GameProfile> defaultGameProfile = new HashMap<>();

    public void setSkin(Player player, String name) {
        EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();

        if (!defaultGameProfile.containsKey(player.getUniqueId())) {
            defaultGameProfile.put(player.getUniqueId(), entityPlayer.getProfile());
        }

        // In dem GameProfile wird die Texture gespeichert
        GameProfile gameProfile;

        try {

            /**
             * Hier wird die UUID von dem Spieler angefragt, von dem wir die Texture haben wollen
             * (Das direkte anfragen bei der Mojang api erlaubt das fetchen von offline Spielern, die noch nie auf dem Server waren)
             */
            URL skinPlayerURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader skinPlayerReader = new InputStreamReader(skinPlayerURL.openStream());
            JsonObject asJsonObject = new JsonParser().parse(skinPlayerReader).getAsJsonObject();
            String uuid = asJsonObject.get("id").getAsString();
            name = asJsonObject.get("name").getAsString();

            // Umwandlung von einer "short uuid" zu einer "normalen uuid"
            StringBuilder builder = new StringBuilder(uuid);
            builder.insert(20, "-");
            builder.insert(16, "-");
            builder.insert(12, "-");
            builder.insert(8, "-");

            // Neues GameProfile erstellen
            gameProfile = new GameProfile(player.getUniqueId(), player.getName()); // hier kann auch anstelle von player.getName() ein anderer name angegeben werden (der name wird dann ingame angezeigt, wenn der name zum skin passen soll, kann man hier die name variable einfügen, die oben gefetcht wurde)
            URL skinPlayerTextureURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader skinPlayerTextureReader = new InputStreamReader(skinPlayerTextureURL.openStream());
            JsonObject textureProperty = new JsonParser().parse(skinPlayerTextureReader).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString(); // texture des spielers
            String signature = textureProperty.get("signature").getAsString(); // signature der texture (sehr wichtig!!!)
            gameProfile.getProperties().put("textures", new Property("textures", texture, signature)); // speichern von texture in gameprofile
        } catch (Exception e) {
            return;
        }

        try {
            Field field = entityPlayer.getClass().getSuperclass().getDeclaredField("bH"); // reflection (das Bearbeiten von Feldern, die eigentlich privat sind)
            field.setAccessible(true); // hier wird das feld bearbeitbar gemacht
            field.set(entityPlayer, gameProfile); // hier wird es bearbeitet
            field.setAccessible(false); // und hier wird es wieder geschlossen (optional, meines wissens nach)
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return;
        }

        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection; // hier werden jetzt die packets gesendet

        new BukkitRunnable() { // man dar das nicht direkt machen, sonst kommt der server nicht hinterher
            @Override
            public void run() {

                // das MUSS alles in dieser reihenfolge sein !!!

                // die kontrolle über die spieler entität wird entzogen
                playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));
                // der spieler wird gelöscht
                playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
                // es wird ein neuer spieler gespawnt
                playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(entityPlayer));
                // der spieler bekommt die kontrolle über die neue spieler entität
                playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer));

                // Der spieler muss die dimension wechseln, damit er seinen skin updated (der client checkt das sonst nicht)
                DedicatedPlayerList craftServer = ((CraftServer) Bukkit.getServer()).getHandle();
                CraftWorld cw = (CraftWorld) player.getWorld();
                WorldServer ws = cw.getHandle();
                int dimension = ws.dimension;
                craftServer.moveToWorld(entityPlayer, dimension, true, player.getLocation(), true);

                // das inventar muss auch geupdated werden
                player.updateInventory();
                ((CraftPlayer) player).updateScaledHealth();
            }
        }.runTaskLater(Main.getInstance(), 2);

        // damit andere spieler den neuen skin sehen, macht der server den spieler, der seinen skin wechselt einmal unsichtbar und wieder sichtbar, dadurch updaten alle anderen clients die Texture (man selber aber nicht, deshalb das dimensions wechsel zeug)
        for(Player op : Bukkit.getServer().getOnlinePlayers()) {
            op.hidePlayer(player);
            op.showPlayer(player);
        }
    }
}

