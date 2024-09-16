package offsetclient

import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard
import skyblockclient.command.OffsetClientCommands
import skyblockclient.config.Config
import skyblockclient.config.ConfigManager.loadConfig
import skyblockclient.config.ConfigManager.parseData
import skyblockclient.config.ConfigManager.writeConfig
import skyblockclient.features.*
import skyblockclient.features.dungeons.*
import skyblockclient.features.macros.AutoBookCombine
import skyblockclient.features.macros.AutoRuneCombine
import skyblockclient.utils.GuiMacroUtils
import skyblockclient.utils.LocationUtils
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

@Mod(
    modid = OffsetClient.MOD_ID,
    name = OffsetClient.MOD_NAME,
    version = OffsetClient.MOD_VERSION,
    clientSideOnly = true
)
class OffsetClient {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        val directory = File(event.modConfigurationDirectory, "offsetclient")
        directory.mkdirs()
        configFile = File(directory, "config.json")
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        config.init()

        ClientCommandHandler.instance.registerCommand(OffsetClientCommands())

        listOf(
            this,
            AntiBlind,
            AutoBookCombine,
            AutoCloseChest,
            AutoRuneCombine,
            EnchantingExperiments,
            F7PreGhostBlocks,
            FastLeap,
            GemstoneESP,
            GhostBlock,
            GuiMacroUtils,
            HiddenMobs,
            HideServerID,
            LocationUtils,
            MobESP,
            NoBlockAnimation,
            NoWaterFOV,
            ThornStun,
            WormFishingLavaESP
        ).forEach(MinecraftForge.EVENT_BUS::register)

        keyBinds.forEach(ClientRegistry::registerKeyBinding)
    }

    @Mod.EventHandler
    fun postInit(event: FMLLoadCompleteEvent) = scope.launch(Dispatchers.IO) {
        configFile?.let {
            loadConfig(it)
            parseData()
            writeConfig(it)
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || display == null) return
        mc.displayGuiScreen(display)
        display = null
    }

    @SubscribeEvent
    fun onKey(event: KeyInputEvent) {
        if (keyBinds[0].isPressed) display = config.gui()
    }

    companion object {
        const val MOD_ID = "offsetclient"
        const val MOD_NAME = "OffsetClient"
        const val MOD_VERSION = "1.0"
        const val CHAT_PREFIX = "§b§l<§fOffsetClient§b§l>§r"

        val mc: Minecraft = Minecraft.getMinecraft()
        var config = Config
        val configData = HashMap<String, JsonElement>()
        var configFile: File? = null
        var display: GuiScreen? = null
        val scope = CoroutineScope(EmptyCoroutineContext)

        val keyBinds = arrayOf(
            KeyBinding("Open Settings", Keyboard.KEY_RSHIFT, "OffsetClient"),
            KeyBinding("Ghost Block", Keyboard.KEY_G, "OffsetClient"),
        )
    }
}
