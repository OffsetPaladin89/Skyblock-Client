package skyblockclient.features.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import skyblockclient.SkyblockClient;
import skyblockclient.events.GuiContainerEvent;
import skyblockclient.utils.ColorSlot;
import skyblockclient.utils.SkyblockCheck;
import skyblockclient.utils.TextRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Terminals {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ArrayList<Slot> clickQueue = new ArrayList<>(28);
    private static final int[] mazeSlotDirection = {-9, -1, 1, 9};
    private static TerminalType currentTerminal = TerminalType.NONE;
    private static char letterNeeded;
    private static String colorNeeded;
    private static int displayTime;
    private static long lastClickTime;
    private static int ticksSinceTerminal;
    private static int windowId;
    private static int windowClicks;
    private static int mouseButton;
    private static boolean recalculate = false;

    @SubscribeEvent
    public void guiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!SkyblockCheck.inDungeons) return;
        if (event.gui instanceof GuiChest) {
            Container container = ((GuiChest) event.gui).inventorySlots;
            if (container instanceof ContainerChest) {
                List<Slot> invSlots = container.inventorySlots;
                if (currentTerminal == TerminalType.NONE) {
                    String chestName = ((ContainerChest) container).getLowerChestInventory().getDisplayName().getUnformattedText();
                    if (chestName.equals("Navigate the maze!")) {
                        currentTerminal = TerminalType.MAZE;
                    } else if (chestName.equals("Click in order!")) {
                        currentTerminal = TerminalType.NUMBERS;
                    } else if (chestName.equals("Correct all the panes!")) {
                        currentTerminal = TerminalType.CORRECTALL;
                    } else if (chestName.startsWith("What starts with:")) {
                        currentTerminal = TerminalType.LETTER;
                    } else if (chestName.startsWith("Select all the")) {
                        currentTerminal = TerminalType.COLOR;
                    }
                }
                if (currentTerminal != TerminalType.NONE) {
                    if (clickQueue.isEmpty() || recalculate) {
                        recalculate = getClicks((ContainerChest) container);
                    } else {
                        switch (currentTerminal) {
                            case MAZE:
                            case NUMBERS:
                            case CORRECTALL:
                                clickQueue.removeIf(slot -> invSlots.get(slot.slotNumber).getHasStack() &&
                                        invSlots.get(slot.slotNumber).getStack().getItemDamage() == 5);
                                break;
                            case LETTER:
                            case COLOR:
                                clickQueue.removeIf(slot -> invSlots.get(slot.slotNumber).getHasStack() &&
                                        invSlots.get(slot.slotNumber).getStack().isItemEnchanted());
                                break;
                        }
                    }
                    if (!clickQueue.isEmpty()) {
                        int size = invSlots.size();
                        switch (currentTerminal) {
                            case NUMBERS:
                                if (SkyblockClient.config.terminalHighlightNumbers) {
                                    new ColorSlot(clickQueue.get(0), size, SkyblockClient.config.terminalColorNumberFirst.getRGB());
                                    if (clickQueue.size() > 2) {
                                        new ColorSlot(clickQueue.get(2), size, SkyblockClient.config.terminalColorNumberThird.getRGB());
                                    }
                                    if (clickQueue.size() > 1) {
                                        new ColorSlot(clickQueue.get(1), size, SkyblockClient.config.terminalColorNumberSecond.getRGB());
                                    }
                                }
                                break;
                            case LETTER:
                                if (SkyblockClient.config.terminalHighlightLetter) {
                                    clickQueue.forEach(slot -> new ColorSlot(slot, size, SkyblockClient.config.terminalColorHighlight.getRGB()));
                                }
                                break;
                            case COLOR:
                                if (SkyblockClient.config.terminalHighlightColor) {
                                    clickQueue.forEach(slot -> new ColorSlot(slot, size, SkyblockClient.config.terminalColorHighlight.getRGB()));
                                }
                                break;
                        }
                        if (SkyblockClient.config.terminalAuto && System.currentTimeMillis() - lastClickTime > SkyblockClient.config.terminalClickDelay) {
                            switch (currentTerminal) {
                                case MAZE:
                                    if (SkyblockClient.config.terminalMaze) clickSlot(clickQueue.get(0));
                                    break;
                                case NUMBERS:
                                    if (SkyblockClient.config.terminalNumbers) clickSlot(clickQueue.get(0));
                                    break;
                                case CORRECTALL:
                                    if (SkyblockClient.config.terminalCorrectAll) clickSlot(clickQueue.get(0));
                                    break;
                                case LETTER:
                                    if (SkyblockClient.config.terminalLetter) clickSlot(clickQueue.get(0));
                                    break;
                                case COLOR:
                                    if (SkyblockClient.config.terminalColor) clickSlot(clickQueue.get(0));
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void drawSlot(GuiContainerEvent.DrawSlotEvent.Pre event) {
        if (!SkyblockClient.config.terminalHighlightNumbers) return;
        if (event.container instanceof ContainerChest && currentTerminal == TerminalType.NUMBERS) {
            Slot slot = event.slot;
            if (slot.inventory != mc.thePlayer.inventory && slot.getHasStack()) {
                ItemStack item = slot.getStack();
                if (item.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane) && item.getItemDamage() == 14) {
                    GlStateManager.pushMatrix();
                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.disableBlend();
                    mc.fontRendererObj.drawStringWithShadow(String.valueOf(item.stackSize),
                            (float) (slot.xDisplayPosition + 9 - mc.fontRendererObj.getStringWidth(String.valueOf(item.stackSize)) / 2),
                            (float) (slot.yDisplayPosition + 4), 16777215);
                    GlStateManager.popMatrix();
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void slotClick(GuiContainerEvent.SlotClickEvent event) {
        if (!SkyblockCheck.inDungeons || clickQueue.isEmpty()) return;
        if (!SkyblockClient.config.terminalPingless && !SkyblockClient.config.terminalBlockClicks &&
                !SkyblockClient.config.terminalMiddleClick) return;
        event.setCanceled(true);
        if (currentTerminal == TerminalType.MAZE || currentTerminal == TerminalType.NUMBERS) {
            if (SkyblockClient.config.terminalBlockClicks && clickQueue.get(0).slotNumber != event.slotId) return;
            clickSlot(event.slot);
        } else if (currentTerminal == TerminalType.CORRECTALL) {
            if (event.slot.getHasStack() && event.slot.getStack().getItemDamage() == 5) {
                if (SkyblockClient.config.terminalBlockClicks) {
                    return;
                } else {
                    clickQueue.add(event.slot);
                }
            }
            clickSlot(event.slot);
        } else if (currentTerminal != TerminalType.NONE) {
            if (SkyblockClient.config.terminalBlockClicks && clickQueue.stream().noneMatch(slot -> slot.slotNumber == event.slotId)) {
                return;
            }
            clickSlot(event.slot);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen instanceof GuiChest) {
            if (currentTerminal != TerminalType.NONE) ticksSinceTerminal = 0;
        } else {
            currentTerminal = TerminalType.NONE;
            if (ticksSinceTerminal == 1) {
                if (!clickQueue.isEmpty() && SkyblockClient.config.terminalKickNotify) displayTime = 40;
                clickQueue.clear();
                letterNeeded = 0;
                colorNeeded = null;
                windowClicks = 0;
            }
            if (ticksSinceTerminal <= 1) ticksSinceTerminal++;
        }
        if (displayTime > 0) displayTime--;
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (mc.ingameGUI == null || event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE &&
                event.type != RenderGameOverlayEvent.ElementType.JUMPBAR)
            return;
        if (displayTime > 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            int width = sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth("Terminal Kicked");
            int height = sr.getScaledHeight() / 2 - 50;
            new TextRenderer(mc, "§cTerminal Kicked", width, height, 2);
        }
    }

    @SubscribeEvent
    public void guiOpen(GuiOpenEvent event) {
        if (SkyblockClient.config.terminalMiddleClick) mouseButton = 2;
        else mouseButton = 0;
    }

    @SubscribeEvent
    public void tooltip(ItemTooltipEvent event) {
        if (!SkyblockCheck.inDungeons || !SkyblockClient.config.terminalHideTooltip || event.toolTip == null)
            return;
        if (mc.currentScreen instanceof GuiChest) {
            if (currentTerminal != TerminalType.NONE) {
                event.toolTip.clear();
            }
        }
    }

    private boolean getClicks(ContainerChest container) {
        List<Slot> invSlots = container.inventorySlots;
        String chestName = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        clickQueue.clear();
        switch (currentTerminal) {
            case MAZE:
                int endSlot = -1;
                boolean[] mazeVisited = new boolean[54];
                boolean[] isStartSlot = new boolean[54];
                for (Slot slot : invSlots) {
                    if (slot.inventory == mc.thePlayer.inventory) continue;
                    ItemStack itemStack = slot.getStack();
                    if (itemStack == null) continue;
                    if (itemStack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                        if (itemStack.getItemDamage() == 5) {
                            isStartSlot[slot.slotNumber] = true;
                        } else if (itemStack.getItemDamage() == 14) {
                            endSlot = slot.slotNumber;
                        }
                    }
                }
                for (int j = 0; j < 54; j++) {
                    if (isStartSlot[j]) {
                        int startSlot = j;
                        boolean newSlotChosen;
                        while (startSlot != endSlot) {
                            newSlotChosen = false;
                            for (int i = 0; i < 4; i++) {
                                int slotNumber = startSlot + mazeSlotDirection[i];
                                if (slotNumber == endSlot) return false;
                                if (slotNumber < 0 || slotNumber > 53 || i == 1 && slotNumber % 9 == 8 || i == 2 && slotNumber % 9 == 0)
                                    continue;
                                if (mazeVisited[slotNumber]) continue;
                                ItemStack itemStack = invSlots.get(slotNumber).getStack();
                                if (itemStack == null) continue;
                                if (itemStack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane) && itemStack.getItemDamage() == 0) {
                                    clickQueue.add(invSlots.get(slotNumber));
                                    startSlot = slotNumber;
                                    mazeVisited[slotNumber] = true;
                                    newSlotChosen = true;
                                    break;
                                }
                            }
                            if (!newSlotChosen) {
                                System.out.println("Maze calculation aborted");
                                break;
                            }
                        }
                    }
                }
                return true;
            case NUMBERS:
                while (clickQueue.size() < 14) clickQueue.add(null);
                int min = 0;
                for (int i = 10; i <= 25; i++) {
                    if (i == 17 || i == 18) continue;
                    ItemStack item = invSlots.get(i).getStack();
                    if (item == null) continue;
                    if (item.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane) && item.stackSize < 15) {
                        if (item.getItemDamage() == 14) {
                            clickQueue.set(item.stackSize - 1, invSlots.get(i));
                        } else if (item.getItemDamage() == 5) {
                            min = Math.max(0, item.stackSize - 1);
                        }
                    }
                }
                for (int i = min; i < 14; i++) {
                    if (clickQueue.get(i) == null) {
                        clickQueue.removeIf(Objects::isNull);
                        return true;
                    }
                }

                break;
            case CORRECTALL:
                for (Slot slot : invSlots) {
                    if (slot.inventory == mc.thePlayer.inventory) continue;
                    if (slot.slotNumber < 9 || slot.slotNumber > 35 || slot.slotNumber % 9 <= 1 || slot.slotNumber % 9 >= 7)
                        continue;
                    ItemStack item = slot.getStack();
                    if (item == null) return true;
                    if (item.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane) && item.getItemDamage() == 14) {
                        clickQueue.add(slot);
                    }
                }
                break;
            case LETTER:
                letterNeeded = chestName.charAt(chestName.indexOf("'") + 1);
                if (letterNeeded != 0) {
                    for (Slot slot : invSlots) {
                        if (slot.inventory == mc.thePlayer.inventory) continue;
                        if (slot.slotNumber < 9 || slot.slotNumber > 44 || slot.slotNumber % 9 == 0 || slot.slotNumber % 9 == 8)
                            continue;
                        ItemStack item = slot.getStack();
                        if (item == null) return true;
                        if (item.isItemEnchanted()) continue;
                        if (StringUtils.stripControlCodes(item.getDisplayName()).charAt(0) == letterNeeded) {
                            clickQueue.add(slot);
                        }
                    }
                }
                break;
            case COLOR:
                for (EnumDyeColor color : EnumDyeColor.values()) {
                    String colorName = color.getName().replaceAll("_", " ").toUpperCase();
                    if (chestName.contains(colorName)) {
                        colorNeeded = color.getUnlocalizedName();
                        break;
                    }
                }
                if (colorNeeded != null) {
                    for (Slot slot : invSlots) {
                        if (slot.inventory == mc.thePlayer.inventory) continue;
                        if (slot.slotNumber < 9 || slot.slotNumber > 44 || slot.slotNumber % 9 == 0 || slot.slotNumber % 9 == 8)
                            continue;
                        ItemStack item = slot.getStack();
                        if (item == null) return true;
                        if (item.isItemEnchanted()) continue;
                        if (item.getUnlocalizedName().contains(colorNeeded)) {
                            clickQueue.add(slot);
                        }
                    }
                }
                break;
        }
        return false;
    }

    private void clickSlot(Slot slot) {
        if (windowClicks == 0) windowId = mc.thePlayer.openContainer.windowId;
        if (SkyblockClient.config.terminalShiftClick) {
            mc.playerController.windowClick(windowId + windowClicks,
                    slot.slotNumber, 0, 1, mc.thePlayer);
        } else {
            mc.playerController.windowClick(windowId + windowClicks,
                    slot.slotNumber, mouseButton, 0, mc.thePlayer);
        }
        lastClickTime = System.currentTimeMillis();
        if (SkyblockClient.config.terminalPingless) {
            windowClicks++;
            clickQueue.remove(slot);
        }
        if (clickQueue.size() == 1 && clickQueue.get(0).slotNumber == slot.slotNumber) {
            clickQueue.clear();
        }
        System.out.println("Terminal clicked: " + currentTerminal + " Slot clicked: " + slot.slotNumber + " Window ID: " + windowId + "Window Clicks: " + windowClicks);
    }

    private enum TerminalType {
        MAZE, NUMBERS, CORRECTALL, LETTER, COLOR, NONE
    }


}
