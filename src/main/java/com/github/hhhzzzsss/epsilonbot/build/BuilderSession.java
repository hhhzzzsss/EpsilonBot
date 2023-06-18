package com.github.hhhzzzsss.epsilonbot.build;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.block.World;
import com.github.hhhzzzsss.epsilonbot.build.action.*;
import com.github.hhhzzzsss.epsilonbot.modules.PositionManager;
import com.github.hhhzzzsss.epsilonbot.util.BlockUtils;
import com.github.hhhzzzsss.epsilonbot.util.Vec3d;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class BuilderSession {
    public static final Pattern WE_PROGRESS_PATTERN = Pattern.compile("\\s*You have \\S+ blocks queued. Placing speed: \\S+, \\S+ left.\\s*");
    public static final Pattern WE_DONE_PATTERN = Pattern.compile("\\s*Job \\[\\S+\\] \\S+ - done\\s*");

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
        if (WE_PROGRESS_PATTERN.matcher(message).matches()) {
            WECooldown = 10;
        } else if (WE_DONE_PATTERN.matcher(message).matches()) {
            WEFinished = true;
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
        Vec3d actionPos = new Vec3d(action.x, action.y, action.z);
        Vec3d currentPos = new Vec3d(posManager.getX(), posManager.getY(), posManager.getZ());
        Vec3d targetPos;
        if (action.yPriority && currentPos.y != action.y) {
            if (Math.abs(action.y - currentPos.y) <= 2.5) {
                targetPos = new Vec3d(currentPos.x, action.y, currentPos.z);
            } else if (action.y > currentPos.y) {
                targetPos = new Vec3d(currentPos.x, currentPos.y+2.5, currentPos.z);
            } else {
                targetPos = new Vec3d(currentPos.x, currentPos.y-2.5, currentPos.z);
            }
        } else {
            Vec3d difVec = actionPos.subtract(currentPos);
            if (difVec.lengthSquared() > 9) {
                targetPos = currentPos.add(difVec.normalize().multiply(3));
            } else {
                targetPos = actionPos;
            }
        }
        // Block intersection bounds
        World world = bot.getWorld();
        int minIX = (int) Math.floor(targetPos.x - 0.3);
        int maxIX = (int) Math.floor(targetPos.x + 0.3);
        int minIZ = (int) Math.floor(targetPos.z - 0.3);
        int maxIZ = (int) Math.floor(targetPos.z + 0.3);
        int minIY = (int) Math.floor(targetPos.y);
        int maxIY = (int) Math.floor(targetPos.y + 1.8);
        for (int y = minIY; y <= maxIY && y < 256; y++) {
            for (int z = minIZ; z <= maxIZ; z++) {
                for (int x = minIX; x <= maxIX; x++) {
                    if (!BlockUtils.isAir(world.getBlock(x, y, z))) {
                        if (action.breaking) {
                            world.breakBlock(x, y, z);
                        } else if (actionPos.subtract(currentPos).length() > 1) {
                            tryTeleport((int) Math.floor(action.x), (int) Math.floor(action.y), (int) Math.floor(action.z));
                        } else {
                            actionQueue.poll();
                        }
                        return;
                    }
                }
            }
        }
        posManager.move(targetPos.x, targetPos.y, targetPos.z);
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
            if (!action.awaitWE) {
                WECooldown = 1; // Set to 1 because CommandHandler will already guarantee a minimum 1000 delay
                actionQueue.poll();
            } else {
                WECooldown = 10;
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