package me.rerere.matrix.checks.player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;

public class ScaffoldPlacementCheck extends me.rerere.matrix.checks.CheckRuntimeState {
    private static int violationWeight;
    private long lastHeldSlotAt;
    private long lastYawAnomalyAt;
    private long lastTeleportAt;
    private long lastMoveAt;
    private org.bukkit.block.BlockFace previousFacing;
    private static int cancelViolationThreshold;
    private final me.rerere.matrix.network.PacketOrderBuffer packetHistory;
    private long lastPitchAnomalyAt;
    private static int delayViolationWeight;
    public static boolean cancelTypeKCheck;
    private long lastSneakAlertAt;
    private int heldItemSlot;
    private static boolean cancelDelayFlag;
    private static final Set<org.bukkit.block.BlockFace> horizontalFaces;
    private org.bukkit.util.Vector lastMoveVector;
    private static int minimumDiagonalDelay;
    private int flyingPacketCount;
    private long lastRotationPacketAt;
    private final Queue<Integer> placeTicks;
    private final Queue<Integer> alternatePlaceTicks;
    private long lastSuspiciousPacketAt;
    public static boolean volumeCheckEnabled;
    private long lastPlacementAt;
    private static boolean placeCheckEnabled;
    private static int minimumStraightDelayCount;
    private long lastInvalidCursorAt;
    private static boolean delayCheckEnabled;
    private long lastPacketPlacementAt;
    private long previousTrackedPacketAt;
    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon previousPlacementPacket;
    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon currentPlacementPacket;
    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon latestPacketType;

    private static boolean isEdgeFraction(double distance) {
        return distance > 0x1.8a3d70a3d70a4p-1 && distance < 0x1.999999999999ap-1;
    }

    private static String formatCombinedDelay(int currentDelay, int averageDelay) {
        return "(delay) bridge too fast: " + currentDelay + "," + averageDelay;
    }

    private static String formatDiagonalDelay(int currentDelay, int averageDelay) {
        return "(delay) bridge too fast(diagonal): " + currentDelay + "," + averageDelay;
    }

    private static String buildOffsetMessage(double offset, long elapsed) {
        return "scaffold-like placement | type C | offset: " + offset + ", delay: " + elapsed;
    }

    private boolean checkEdgePlacement(
            org.bukkit.entity.Player player,
            org.bukkit.block.Block placed,
            org.bukkit.block.Block against) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return false;
        }
        if (placed.getY() != player.getLocation().getY() - 1.0D) {
            return false;
        }
        if (!player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).equals(placed)) {
            return false;
        }
        if (placed.getY() != against.getY()) {
            return false;
        }
        if (!me.rerere.matrix.world.MaterialPolicy.isAirMaterial(placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType())) {
            return false;
        }
        if (!me.rerere.matrix.world.MaterialPolicy.isAirMaterial(against.getRelative(org.bukkit.block.BlockFace.DOWN).getType())) {
            return false;
        }
        double dx = Math.abs(against.getX() + 0.5D - player.getLocation().getX());
        double dz = Math.abs(against.getZ() + 0.5D - player.getLocation().getZ());
        if (!(isEdgeFraction(dx) || isEdgeFraction(dz) && volumeCheckEnabled)) {
            this.lastSneakAlertAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            return false;
        }
        if (player.isSneaking()) {
            this.lastSneakAlertAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            return false;
        }
        if (!(dx < 0.1D || dz < 0.1D)) {
            this.lastSneakAlertAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            return false;
        }
        if (me.rerere.matrix.world.BlockEnvironment.countAdjacentNonPredicateMaterials(placed) > 1) {
            this.lastSneakAlertAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            return false;
        }
        long elapsed = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt;
        if (elapsed >= 500L) {
            this.lastSneakAlertAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            return false;
        }
        double offset = isEdgeFraction(dx) ? dx : dz;
        this.flag("sfd.place.c", buildOffsetMessage(
                me.rerere.matrix.util.NumberUtils.round3(offset), elapsed), violationWeight);
        return true;
    }

    @Override
    public void reloadConfiguration() {
        placeCheckEnabled = this.configuration().getBoolean("modules.place.enable", true);
        violationWeight = this.configuration().getInt("modules.place.vl_weight", 5);
        cancelViolationThreshold = this.configuration().getInt("modules.cancel_vl", 15);
        delayCheckEnabled = this.configuration().getBoolean("modules.delay.enable", true);
        delayViolationWeight = this.configuration().getInt("modules.delay.vl_weight", 5);
        cancelDelayFlag = this.configuration().getBoolean("modules.delay.cancel_place", true);
        minimumStraightDelayCount = this.configuration().getInt("modules.delay.min_delay", 6);
        minimumDiagonalDelay = this.configuration().getInt("modules.delay.min_delay_diagonal", 3);
    }

    public ScaffoldPlacementCheck(me.rerere.matrix.player.PlayerCheckContext playerData) {
        super(playerData);
        this.packetHistory = new me.rerere.matrix.network.PacketOrderBuffer(4);
        this.flyingPacketCount = 0;
        this.placeTicks = new ArrayDeque<Integer>();
        this.alternatePlaceTicks = new ArrayDeque<Integer>();
        this.setCheckType(me.rerere.matrix.checks.CheckCategory.SCAFFOLD);
    }

    @Override
    public boolean onPacketReceive(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent event,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper packet) {
        if (me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(packetType)
                && this.flyingPacketCount < 20) {
            this.flyingPacketCount++;
        }

        if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.HELD_ITEM_CHANGE) {
            int slot = ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange) packet).getSlot();
            if (slot == this.heldItemSlot) {
                if (me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt <= 1000L
                        && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastHeldSlotAt <= 1000L) {
                    this.flag("sfd.place.i", "scaffold-like placement | type I", violationWeight);
                }
                this.lastHeldSlotAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            }
            this.heldItemSlot = slot;
        }

        if (me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(packetType)) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement placement =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement) packet;
            float cursorX = placement.getCursorPosition().x;
            float cursorY = placement.getCursorPosition().y;
            float cursorZ = placement.getCursorPosition().z;
            if (!(cursorX >= 0.0F) || cursorX > 1.0F || !(cursorY >= 0.0F) || cursorY > 1.0F
                    || !(cursorZ >= 0.0F) || cursorZ > 1.0F) {
                this.lastInvalidCursorAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            }
        }

        if (me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(packetType)
                || packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.HELD_ITEM_CHANGE
                || me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(packetType)
                || packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION) {
            me.rerere.matrix.util.MutableValue packetState = new me.rerere.matrix.util.MutableValue();
            this.packetHistory.addPacket(
                    packetType,
                    me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.previousTrackedPacketAt,
                    packetState);
            this.previousTrackedPacketAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();

            if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION) {
                packetState.set(
                        me.rerere.matrix.network.packet.PacketEventSupport.getEntityAction(packet));
            }
            if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.HELD_ITEM_CHANGE) {
                packetState.set(Integer.valueOf(
                        me.rerere.matrix.network.packet.PacketEventSupport.getHeldItemSlot(packet)));
            }

            if (this.packetHistory.hasAnyPacket()) {
                if (me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(
                        packetAt(3))) {
                    if (packetAge(3) < 5L
                            && me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(packetAt(2))
                            && packetAge(2) > 40L
                            && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt < 1000L) {
                        if (me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastRotationPacketAt <= 1000L) {
                            this.lastSuspiciousPacketAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
                        }
                        this.lastRotationPacketAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
                    }
                }

                if (me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(packetAt(0))
                        && packetAt(1)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION
                        && packetAction(1)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.START_SNEAKING
                        && packetAge(1) < 5L
                        && me.rerere.matrix.network.packet.PacketEventSupport.isPlayerRotationPacket(packetAt(2))
                        && packetAge(2) < 5L
                        && packetAt(3)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION
                        && packetAction(3)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.STOP_SNEAKING
                        && packetAge(3) > 40L
                        && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt < 1000L
                        && volumeCheckEnabled) {
                    this.flag("sfd.place.g", "scaffold-like placement | type G", violationWeight);
                }

                if (packetAt(0)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION
                        && packetAction(0)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.START_SNEAKING
                        && me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(
                                packetAt(1))
                        && packetAge(1) < 5L
                        && me.rerere.matrix.network.packet.PacketEventSupport.isPlayerRotationPacket(packetAt(2))
                        && packetAge(2) < 5L
                        && packetAt(3)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION
                        && packetAction(3)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.STOP_SNEAKING
                        && packetAge(3) > 40L
                        && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt < 1000L
                        && volumeCheckEnabled) {
                    this.flag("sfd.place.f", "scaffold-like placement | type F", violationWeight);
                }

                if (me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(
                                packetAt(3))
                        && packetAge(3) < 5L
                        && packetAt(2)
                                == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.HELD_ITEM_CHANGE
                        && packetAge(2) < 5L
                        && me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(packetAt(1))
                        && packetAge(1) > 40L
                        && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt < 1200L) {
                    this.flag("sfd.place.l", "scaffold-like placement | type L", violationWeight);
                }
            }
            if (packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.ENTITY_ACTION) {
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action action =
                        me.rerere.matrix.network.packet.PacketEventSupport.getEntityAction(packet);
                if (action != me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.START_SNEAKING
                        && action != me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.STOP_SNEAKING) {
                    return false;
                }
            }

            if (placeCheckEnabled
                    && !me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(packetType)
                    && this.previousPlacementPacket != null && this.latestPacketType != null
                    && this.currentPlacementPacket != null
                    && packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.HELD_ITEM_CHANGE
                    && me.rerere.matrix.network.packet.PacketEventSupport.isBlockPlacementPacket(this.latestPacketType)
                    && this.previousPlacementPacket
                            == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.HELD_ITEM_CHANGE
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSneakAlertAt < 1500L
                    && volumeCheckEnabled) {
                this.flag("sfd.place.j", "scaffold-like placement | type J", violationWeight);
            }

            this.currentPlacementPacket = this.previousPlacementPacket;
            this.previousPlacementPacket = this.latestPacketType;
            this.latestPacketType = packetType;
        }
        return false;
    }

    private me.rerere.matrix.network.packet.PacketSnapshot packetHistoryEntry(int index) {
        return this.packetHistory.packetAt(index);
    }

    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetAt(
            int index) {
        return packetHistoryEntry(index).getType();
    }

    private long packetAge(int index) {
        return packetHistoryEntry(index).getTimestamp();
    }

    private me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action packetAction(
            int index) {
        return (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action)
                packetHistoryEntry(index).getPayload()
                        .get(
                                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action.class);
    }

    @Override
    public void onPlayerMove(
            org.bukkit.event.player.PlayerMoveEvent event,
            org.bukkit.Location from,
            org.bukkit.Location to) {
        float yawChange = me.rerere.matrix.util.GeometryMath.angularDistance(to.getYaw(), from.getYaw());
        if (yawChange >= 0x1.64p7F && yawChange <= 0x1.68p7F) {
            this.lastYawAnomalyAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
        }
        if (yawChange > 0x1.4cp6F && to.getPitch() > 0x1.18p6F
                && me.rerere.matrix.checks.movement.MovementMetrics.locationDistance(from, to)
                        > 0x1.999999999999ap-3) {
            this.lastPitchAnomalyAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
        }
        this.lastMoveVector = from.toVector();
        this.lastMoveAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
    }

    private static boolean hasIntegralAngle(float yaw, float pitch) {
        if (yaw % 1.0F == 0.0F && yaw != 0.0F && Math.abs(yaw) != 0x1.68p6F) {
            return true;
        }
        return pitch % 1.0F == 0.0F && pitch != 0.0F && Math.abs(pitch) != 0x1.68p6F;
    }

    private static double distance(org.bukkit.Location from, org.bukkit.Location to) {
        if (from.getWorld() != to.getWorld()) {
            return 0.0D;
        }
        return from.distance(to);
    }

    private static boolean containsHorizontal(org.bukkit.block.Block block, org.bukkit.util.Vector position) {
        return position.getX() > block.getX() && position.getZ() > block.getZ()
                && position.getX() < (double) (block.getX() + 1) && position.getZ() < (double) (block.getZ() + 1);
    }

    @Override
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        org.bukkit.block.Block placed = event.getBlockPlaced();
        org.bukkit.block.Block against = event.getBlockAgainst();
        org.bukkit.Location playerLocation = player.getLocation();
        boolean flagged = false;
        if (against.getLocation().equals(placed.getLocation())) {
            return;
        }
        if (placeCheckEnabled && !event.isCancelled()) {
            if (hasIntegralAngle(playerLocation.getYaw(), playerLocation.getPitch())) {
                this.flag("sfd.place.a", "scaffold-like placement | type A", 1);
                flagged = true;
            }
            if (checkEdgePlacement(player, placed, against)) {
                flagged = true;
            }
            if (detectScaffoldPlacement(placed, against, player)) {
                flagged = true;
            }
            if (checkPlacementGeometry(player, placed, against)) {
                flagged = true;
            }

            if (me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastYawAnomalyAt <= 1200L
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastTeleportAt > 1200L
                    && placed.getType().isSolid() && cancelTypeKCheck
                    && placed.getY() == against.getY()
                    && placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == org.bukkit.Material.AIR
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)) {
                this.flag("sfd.place.k", "scaffold-like placement | type K", violationWeight);
                flagged = true;
                event.setCancelled(true);
            }

            if (me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastPitchAnomalyAt < 75L
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastTeleportAt > 1200L
                    && placed.getType().isSolid() && volumeCheckEnabled && placed.getY() == against.getY()
                    && placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == org.bukkit.Material.AIR
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)) {
                this.flag("sfd.place.m", "scaffold-like placement | type M", violationWeight);
                flagged = true;
                event.setCancelled(true);
                this.lastPlacementAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            }

            if (me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastPlacementAt <= 1500L
                    && placed.getY() == against.getY()
                    && placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == org.bukkit.Material.AIR
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)) {
                flagged = true;
                event.setCancelled(true);
            }

            if (me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastSuspiciousPacketAt <= 400L
                    && placed.getY() == against.getY()
                    && placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == org.bukkit.Material.AIR
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)
                    && me.rerere.matrix.metrics.RollingStatistics.metric() > 18.0D) {
                this.flag("sfd.place.n", "scaffold-like placement | type N", 2);
                flagged = true;
            }

            if (this.lastMoveVector != null && player.getLocation().getY() == this.lastMoveVector.getY()
                    && containsHorizontal(placed, player.getLocation().toVector())
                    && containsHorizontal(against, this.lastMoveVector)
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastMoveAt <= 60L
                    && placed.getY() == against.getY()
                    && placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == org.bukkit.Material.AIR
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)
                    && me.rerere.matrix.world.BlockCollisionGeometry.isUnitCube(
                            me.rerere.matrix.world.BlockCollisionGeometry.getBlockShapes(
                                    against, me.rerere.matrix.geometry.BlockShapeMode.INTERACT))
                    && me.rerere.matrix.metrics.RollingStatistics.metric() > 19.0D
                    && volumeCheckEnabled) {
                long elapsed = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastPacketPlacementAt;
                this.lastPacketPlacementAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
                if (elapsed <= 300L) {
                    this.flag("sfd.place.o", "scaffold-like placement | type O", violationWeight);
                }
            }

            if (isAirBlock(against.getRelative(org.bukkit.block.BlockFace.DOWN))
                    && isAirBlock(placed.getRelative(org.bukkit.block.BlockFace.DOWN))
                    && against.getType() == placed.getType() && against.getY() == placed.getY()
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)
                    && placed.getType().isSolid() && volumeCheckEnabled
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastInvalidCursorAt <= 100L) {
                this.lastInvalidCursorAt = 0L;
                this.flag("sfd.place.q", "scaffold-like placement | type Q", violationWeight);
                flagged = true;
                event.setCancelled(true);
            }

            if (placed.getY() == against.getY() + 1
                    && placed.getX() == against.getX() && placed.getZ() == against.getZ()
                    && placed.getType().isSolid()
                    && player.getLocation().getY() >= (double) (placed.getY() + 1)
                    && me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis() - this.lastInvalidCursorAt <= 100L
                    && me.rerere.matrix.world.BlockCollisionGeometry.isUnitCube(
                            me.rerere.matrix.world.BlockCollisionGeometry.getBlockShapes(
                                    event.getBlockPlaced(), me.rerere.matrix.geometry.BlockShapeMode.INTERACT))
                    && volumeCheckEnabled) {
                this.lastInvalidCursorAt = 0L;
                this.flag("sfd.place.r", "scaffold-like placement | type R", violationWeight);
                flagged = true;
                event.setCancelled(true);
            }
        }

        if (delayCheckEnabled
                && isAirBlock(against.getRelative(org.bukkit.block.BlockFace.DOWN))
                && isAirBlock(placed.getRelative(org.bukkit.block.BlockFace.DOWN))
                && against.getType() == placed.getType() && against.getY() == placed.getY()
                && player.getLocation().getY() >= (double) (placed.getY() + 1)
                && me.rerere.matrix.util.GeometryMath.blockCenterDistance(
                        player.getLocation(), against.getLocation()) < 0x1.3ae147ae147aep0
                && me.rerere.matrix.util.GeometryMath.blockCenterDistance(
                        player.getLocation(), placed.getLocation()) < 0x1.3ae147ae147aep0
                && placed.getType().isSolid()
                && me.rerere.matrix.world.BlockCollisionGeometry.isUnitCube(
                        me.rerere.matrix.world.BlockCollisionGeometry.getBlockShapes(
                                against, me.rerere.matrix.geometry.BlockShapeMode.INTERACT))
                && !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)
                && nonSolidNeighborCount(placed) <= 1
                && me.rerere.matrix.world.BlockEnvironment.isPredicateMaterialAtAndAbove(player)
                && !me.rerere.matrix.player.PlayerUtilities.hasSwiftSneak(player)
                && volumeCheckEnabled) {
            org.bukkit.block.BlockFace currentFace = me.rerere.matrix.world.BlockEnvironment.findNeighborFace(against, placed);
            if (currentFace == this.previousFacing && volumeCheckEnabled) {
                this.placeTicks.add(Integer.valueOf(this.flyingPacketCount));
                if (this.placeTicks.size() >= 5) {
                    int averageDelay = me.rerere.matrix.checks.movement.MovementMetrics.integerAverage(
                            new ArrayList<Integer>(this.placeTicks));
                    if (averageDelay < minimumStraightDelayCount && this.flyingPacketCount < minimumStraightDelayCount) {
                        this.flag("sfd.dly",
                                formatCombinedDelay(this.flyingPacketCount, averageDelay), delayViolationWeight);
                        if (cancelDelayFlag) flagged = true;
                    }
                    this.placeTicks.poll();
                }
            } else {
                this.alternatePlaceTicks.add(Integer.valueOf(this.flyingPacketCount));
                if (this.alternatePlaceTicks.size() >= 5 && volumeCheckEnabled) {
                    int averageDelay = me.rerere.matrix.checks.movement.MovementMetrics.integerAverage(
                            new ArrayList<Integer>(this.alternatePlaceTicks));
                    if (averageDelay < minimumDiagonalDelay && this.flyingPacketCount < minimumDiagonalDelay) {
                        this.flag("sfd.dly",
                                formatDiagonalDelay(this.flyingPacketCount, averageDelay), delayViolationWeight);
                        if (cancelDelayFlag) flagged = true;
                    }
                    this.alternatePlaceTicks.poll();
                }
            }
            this.flyingPacketCount = 0;
            this.previousFacing = currentFace;
        }
        if (flagged && this.violationCount() >= cancelViolationThreshold && cancelViolationThreshold != -1) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        this.lastTeleportAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
    }

    private boolean detectScaffoldPlacement(org.bukkit.block.Block placed, org.bukkit.block.Block against,
            org.bukkit.entity.Player player) {
        if (against.getY() != placed.getY() + 1
                || against.getX() != placed.getX() || against.getZ() != placed.getZ()) {
            return false;
        }
        if (!(player.getLocation().getY() >= against.getY() - 1.2D)) {
            return false;
        }
        if (placed.getRelative(org.bukkit.block.BlockFace.DOWN).getType() != org.bukkit.Material.AIR) {
            return false;
        }
        me.rerere.matrix.geometry.AxisAlignedBox bounds = me.rerere.matrix.world.BlockCollisionGeometry.getBlockBounds(against);
        if (!(bounds.width() >= 1.0D) || !(bounds.depth() >= 1.0D)) {
            return false;
        }
        if (!me.rerere.matrix.world.MaterialPolicy.isAirMaterial(
                player.getEyeLocation().getBlock().getType())) {
            return false;
        }
        if (me.rerere.matrix.Matrix.getServerVersion()
                .isNewerThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_14)
                && player.getPose() != org.bukkit.entity.Pose.STANDING
                && player.getPose() != org.bukkit.entity.Pose.SNEAKING) {
            return false;
        }
        this.flag("sfd.place.d", "scaffold-like placement | type D", violationWeight);
        return true;
    }

    private boolean checkPlacementGeometry(org.bukkit.entity.Player player,
            org.bukkit.block.Block placed, org.bukkit.block.Block against) {
        if (me.rerere.matrix.Matrix.getServerVersion()
                .isNewerThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_14)
                && placed.getType() == org.bukkit.Material.SCAFFOLDING) {
            return false;
        }
        if (against == null || against.getType() == org.bukkit.Material.AIR
                || placed.getY() != against.getY()
                || player.getLocation().getY() < (double) (placed.getY() + 1)
                || against.getRelative(org.bukkit.block.BlockFace.DOWN).getType() != org.bukkit.Material.AIR
                || !me.rerere.matrix.world.BlockEnvironment.isPredicateMaterialAtAndAbove(player.getLocation())) {
            return false;
        }
        me.rerere.matrix.geometry.AxisAlignedBox bounds = me.rerere.matrix.world.BlockCollisionGeometry.getBlockBounds(against);
        if (!(bounds.width() >= 1.0D) || !(bounds.depth() >= 1.0D)) {
            return false;
        }
        double dx = placed.getX() - against.getX();
        double dz = placed.getZ() - against.getZ();
        if (Math.abs(dx) + Math.abs(dz) > 1.0D) {
            return false;
        }
        dx *= 0.5D;
        dz *= 0.5D;
        if (Math.abs(dx) > 0.0D) {
            double relative = player.getLocation().getX() - (against.getX() + 0.5D + dx);
            if (relative * dx < 0.0D) {
                this.flag("sfd.place.e", "scaffold-like placement | type E", violationWeight);
                return true;
            }
        }
        if (Math.abs(dz) > 0.0D) {
            double relative = player.getLocation().getZ() - (against.getZ() + 0.5D + dz);
            if (relative * dz < 0.0D) {
                this.flag("sfd.place.e", "scaffold-like placement | type E", violationWeight);
                return true;
            }
        }
        return false;
    }

    private static int nonSolidNeighborCount(org.bukkit.block.Block block) {
        int count = 0;
        for (org.bukkit.block.BlockFace face : horizontalFaces) {
            if (!isAirBlock(block.getRelative(face))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isAirBlock(org.bukkit.block.Block block) {
        return me.rerere.matrix.world.MaterialPolicy.isAirMaterial(
                me.rerere.matrix.world.BlockEnvironment.getMaterial(block));
    }

    static {
        horizontalFaces = me.rerere.matrix.util.CollectionUtilities.<org.bukkit.block.BlockFace>setOfValues(
                new org.bukkit.block.BlockFace[] {org.bukkit.block.BlockFace.NORTH,
                        org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST,
                        org.bukkit.block.BlockFace.SOUTH});
    }
}
