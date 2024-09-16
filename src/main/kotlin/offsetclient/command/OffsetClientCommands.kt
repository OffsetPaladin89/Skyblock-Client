package offsetclient.command

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import offsetclient.OffsetClient.Companion.config
import offsetclient.OffsetClient.Companion.display

class OffsetClientCommands : CommandBase() {
    override fun getCommandName(): String {
        return "offsetclient"
    }

    override fun getCommandAliases(): List<String> {
        return listOf(
            "offsetclient",
            "oc"
        )
    }

    override fun getCommandUsage(sender: ICommandSender): String {
        return "/$commandName"
    }

    override fun getRequiredPermissionLevel(): Int {
        return 0
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            display = config.gui()
            return
        }
    }
}
