package offsetclient.features.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import offsetclient.OffsetClient.Companion.config
import offsetclient.OffsetClient.Companion.mc
import offsetclient.events.ReceivePacketEvent
import offsetclient.utils.LocationUtils.inDungeons

object AutoCloseChest {
    @SubscribeEvent
    fun onPacket(event: ReceivePacketEvent) {
        if (event.packet !is S2DPacketOpenWindow || !inDungeons) return
        if (config.autoCloseSecretChests) {
            if (event.packet.windowTitle.unformattedText == "Chest") {
                event.isCanceled = true
                mc.netHandler.networkManager.sendPacket(C0DPacketCloseWindow((event.packet.windowId)))
            }
        }
    }
}
