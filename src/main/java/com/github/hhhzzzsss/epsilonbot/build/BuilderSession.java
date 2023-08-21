package com.github.hhhzzzsss.epsilonbot.build;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.World;
import com.github.hhhzzzsss.epsilonbot.build.action.*;
import com.github.hhhzzzsss.epsilonbot.modules.PositionManager;
import com.github.hhhzzzsss.epsilonbot.util.BlockUtils;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class BuilderSession {
//    public static final Pattern WE_PROGRESS_PATTERN = Pattern.compile("\\s*You have \\S+ blocks queued. Placing speed: \\S+, \\S+ left.\\s*");
    public static final Pattern WE_CONFIRM_PATTERN = Pattern.compile("");
    public static final Pattern WE_DONE_PATTERN = Pattern.compile("\\s*(Job \\[\\S+\\] \\S+ - done|\\(FAWE\\) Operation completed \\(\\d+\\)\\.)\\s*|Operation completed \\(\\d+ blocks affected\\)\\.");

    public final EpsilonBot bot;
    @Getter private boolean stopped = false;

    protected Queue<Action> actionQueue = new LinkedList<>();

    int WECooldown = 0;
    boolean WEFinished = false;
    int waitTime = 0;
    boolean waiting = true;

    public BuilderSession(EpsilonBot bot) {
        this.bot = bot;
    }

    public void onAction() {
        if (!actionQueue.isEmpty()) {
            processAction();
        }
    }

    int teleportCooldown = 0;
    public void onTimePacket() {
        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
        if (WECooldown > 0) {
            WECooldown--;
        }
        if (waiting) {
            waitTime++;
        }
    }

    public void onChat(String message) {
        if (WE_DONE_PATTERN.matcher(message).matches()) {
            WEFinished = true;
        }
    }

    public void onBossbar(String message) {
        if (message.startsWith("ETA: ")) {
            WECooldown = 10;
        }
    }

    protected void processAction() {
        Action action = actionQueue.peek();
        if (action.getActionType() == ActionType.MOVE) {
            processMove((MoveAction) action);
        } else if (action.getActionType() == ActionType.HOLD) {
            processHold((HoldAction) action);
        } else if (action.getActionType() == ActionType.PLACE) {
            processPlace((PlaceAction) action);
        } else if (action.getActionType() == ActionType.COMMAND) {
            processCommand((CommandAction) action);
        } else if (action.getActionType() == ActionType.WAIT) {
            processWait((WaitAction) action);
        }
    }

    protected void processMove(MoveAction action) {
        PositionManager posManager = bot.getPosManager();
        Vector3d actionPos = Vector3d.from(action.x, action.y, action.z);
        Vector3d currentPos = Vector3d.from(posManager.getX(), posManager.getY(), posManager.getZ());
        Vector3d targetPos;
        if (action.yPriority && currentPos.getY() != action.y) {
            if (Math.abs(action.y - currentPos.getY()) <= 2.5) {
                targetPos = Vector3d.from(currentPos.getX(), action.y, currentPos.getZ());
            } else if (action.y > currentPos.getY()) {
                targetPos = Vector3d.from(currentPos.getX(), currentPos.getY()+2.5, currentPos.getZ());
            } else {
                targetPos = Vector3d.from(currentPos.getX(), currentPos.getY()-2.5, currentPos.getZ());
            }
        } else {
            Vector3d difVec = actionPos.sub(currentPos);
            if (difVec.lengthSquared() > 9) {
                targetPos = currentPos.add(difVec.normalize().mul(3));
            } else {
                targetPos = actionPos;
            }
        }
        // Block intersection bounds
        World world = bot.getWorld();
        int minIX = (int) Math.floor(targetPos.getX() - 0.3);
        int maxIX = (int) Math.floor(targetPos.getX() + 0.3);
        int minIZ = (int) Math.floor(targetPos.getZ() - 0.3);
        int maxIZ = (int) Math.floor(targetPos.getZ() + 0.3);
        int minIY = (int) Math.floor(targetPos.getY());
        int maxIY = (int) Math.floor(targetPos.getY() + 1.8);
        for (int y = minIY; y <= maxIY && y < 256; y++) {
            for (int z = minIZ; z <= maxIZ; z++) {
                for (int x = minIX; x <= maxIX; x++) {
                    if (!BlockUtils.isAir(world.getBlock(x, y, z))) {
                        if (action.breaking) {
                            world.breakBlock(x, y, z);
                        } else if (actionPos.sub(currentPos).length() > 1) {
                            tryTeleport((int) Math.floor(action.x), (int) Math.floor(action.y), (int) Math.floor(action.z));
                        } else {
                            actionQueue.poll();
                        }
                        return;
                    }
                }
            }
        }
        posManager.move(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        if (targetPos.equals(actionPos)) {
            actionQueue.poll();
        }
    }

    protected void processHold(HoldAction action) {
        bot.sendPacket(new ServerboundSetCreativeModeSlotPacket(36, new ItemStack(action.itemId)));
        actionQueue.poll();
    }

    protected void processPlace(PlaceAction action) {
        if (bot.getWorld().inLoadedChunk(action.x, action.z)) {
            if (action.stateId == 0) {
                bot.getWorld().breakBlock(action.x, action.y, action.z);
            } else {
                bot.getWorld().placeBlock(action.x, action.y, action.z, action.stateId);
            }
            actionQueue.poll();
        }
    }

    protected void processCommand(CommandAction action) {
        // If awaitWE, don't immediately poll command and wait for WEFinished flag
        // WECooldown will cause it to re-run the awaited command if it doesn't detect any updates in 10 time ticks
        if (action.awaitWE && WEFinished) {
            WECooldown = 1; // Set to 1 because CommandHandler will already guarantee a minimum 1000 delay
            WEFinished = false;
            actionQueue.poll();
        } else if (WECooldown <= 0) {
            bot.sendCommand(action.command);
            WECooldown = 1; // Set to 1 because CommandHandler will already guarantee a minimum 1000 delay
            if (!action.awaitWE) {
                actionQueue.poll();
            } else {
                WECooldown = 20;
            }
        }
    }

    protected void processWait(WaitAction action) {
        if (!waiting) {
            waiting = true;
            waitTime = 0;
        } else if (waitTime >= action.duration) {
            waiting = false;
            waitTime = 0;
            actionQueue.poll();
        }
    }

    protected void tryTeleport(int x, int y, int z) {
        tryTeleport(x, y, z, 3);
    }

    protected void tryTeleport(int x, int y, int z, int cooldown) {
        if (teleportCooldown <= 0) {
            bot.sendCommand(String.format("/tp %d %d %d", x, y, z));
            teleportCooldown = cooldown;
        }
    }

    public abstract void sendStatusMessage(Consumer<? super String> sendFunc);

    public void stop() {
        this.stopped = true;
    }
}