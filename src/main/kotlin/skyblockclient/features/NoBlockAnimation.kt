package skyblockclient.features

import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import skyblockclient.SkyblockClient.Companion.config
import skyblockclient.SkyblockClient.Companion.data
import skyblockclient.SkyblockClient.Companion.inSkyblock
import skyblockclient.SkyblockClient.Companion.mc
import skyblockclient.utils.Utils.itemID
import skyblockclient.utils.Utils.lore

class NoBlockAnimation {

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !config.noBlockAnimation || !inSkyblock) return
        isRightClickKeyDown = mc.gameSettings.keyBindUseItem.isKeyDown
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (!config.noBlockAnimation || !inSkyblock) return
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            val item = mc.thePlayer.heldItem ?: return
            if (item.item !is ItemSword || data["Block Animation Blacklist"]?.asJsonArray?.any {
                    item.itemID == it.asString || item.displayName.contains(it.asString)
                } == true) return
            for (line in mc.thePlayer.heldItem.lore) if (line.contains("§6Ability: ") && line.endsWith("§e§lRIGHT CLICK")) {
                event.isCanceled = true
                if (!isRightClickKeyDown) {
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                }
            }
        }
    }

    companion object {
        private var isRightClickKeyDown = false
    }
}