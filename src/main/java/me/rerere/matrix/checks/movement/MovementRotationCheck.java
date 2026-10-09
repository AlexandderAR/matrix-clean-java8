package me.rerere.matrix.checks.movement;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.entity.FloatSampleWindow;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.time.MillisecondSampler;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;

/** Tracks suspicious aim patterns across movement and rotation events. */
public final class MovementRotationCheck extends CheckRuntimeState {
    private int remainingDamageWindow;
    private int shakeScore;
    private int sensitivityCount;
    private int accelerationCount;
    private int snapSampleCount;
    private final FloatSampleWindow yawDeltaWindow;
    private final FloatSampleWindow pitchDeltaWindow;
    private int badYawStreak;
    private int movementObservationTicks;
    private static int aimViolationWeight;
    private static boolean checkSynchronizedAim;
    private static boolean checkSnapAim;
    private static boolean checkAimShake;
    private static boolean checkSensitivity;
    private static boolean checkAcceleration;
    private static boolean checkBadRotation;
    public static boolean offset;
    private static boolean cancelDamageDuringWindow;
    private long lastLargePitchChangeAt;
    private long lastPositionLookPacketAt;
    private float previousRawYaw;
    private float previousRawPitch;
    private float previousYawDelta;
    private float previousPitchDelta;
    private float previousNormalizedYaw;
    private float previousNormalizedPitch;
    private float synchronizationScore;
    public static final double movementScale;
    private static int guardCode;

    static {
        guardCode = 15;
        movementScale = 16777216.0d;
    }

    public static void setGuardCode(int value) { guardCode = value; }
    public static int rawGuardCode() { return guardCode; }
    public static int normalizedGuardCode() { return rawGuardCode() == 0 ? 121 : 0; }

    public MovementRotationCheck(PlayerCheckContext context) {
        super(context);
        remainingDamageWindow = 0;
        shakeScore = 0;
        snapSampleCount = 3;
        yawDeltaWindow = new FloatSampleWindow(20);
        pitchDeltaWindow = new FloatSampleWindow(20);
        badYawStreak = 0;
        accelerationCount = 0;
        sensitivityCount = 0;
        synchronizationScore = 0.0f;
        setCheckType(CheckCategory.KILLAURA);
    }

    public void onAttackWithContext(EntityDamageByEntityEvent event) {
        if (remainingDamageWindow > 0) {
            remainingDamageWindow--;
            if (cancelDamageDuringWindow) event.setCancelled(true);
        }
    }

    public void onTick() { if (movementObservationTicks > 0) movementObservationTicks--; }

    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection section =
                configuration();
        aimViolationWeight = section.getInt("modules.aimbot.vl_weight", 7);
        checkSynchronizedAim = section.getBoolean("modules.aimbot.check_aimbot_sync", true);
        checkSnapAim = section.getBoolean("modules.aimbot.check_aimbot_snap", true);
        checkAimShake = section.getBoolean("modules.aimbot.check_aimbot_shake", true);
        checkSensitivity = section.getBoolean("modules.aimbot.check_aimbot_sensitivity", true);
        checkAcceleration = section.getBoolean("modules.aimbot.check_aimbot_acceleration", true);
        checkBadRotation = section.getBoolean("modules.aimbot.check_aimbot_bad_rotation", true);
        cancelDamageDuringWindow = section.getBoolean("modules.aimbot.cancel_hit", true);
    }
    private static String formatAim(float deltaX, float deltaY, float yaw, float pitch) {
        return "\u00a77[\u00a76Aim\u00a77] delta: " + deltaX + "," + deltaY
                + " | yaw: " + yaw + " | pitch: " + pitch;
    }

    private static String accelerationMessage(int acceleration) {
        return "exhibiting an aiming pattern similar to aimbot // acceleration (" + acceleration + ")";
    }

    private boolean isScientificFraction(Number value) {
        return value.doubleValue() < 1.0d
                && Double.toString(value.doubleValue()).contains("E");
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        float pitchDelta = Math.abs(to.getPitch() - from.getPitch());
        if (pitchDelta > 20.0f)
            lastLargePitchChangeAt = MillisecondSampler.currentTimeMillis();
    }

    public boolean onPacketSend(PacketTypeCommon type, PacketSendEvent event, PacketWrapper wrapper) {
        if (type == PacketType$Play$Server.PLAYER_POSITION_AND_LOOK)
            lastPositionLookPacketAt = MillisecondSampler.currentTimeMillis();
        return false;
    }

    public void onCheckPulse(int entityId) {
        onCheckPulse();
    }

    private void onCheckPulse() {
        if (!offset) return;
        snapSampleCount = 0;
        movementObservationTicks = 15;
        long elapsed = MillisecondSampler.currentTimeMillis()
                - lastLargePitchChangeAt;
        if (elapsed <= 350L) {
            shakeScore += 8 - Math.round((float) elapsed / 50.0f);
            if (shakeScore > 45) {
                if (checkAimShake) {
                    flag("ka.abt.ske",
                            "exhibiting an aiming pattern similar to aimbot // shake", aimViolationWeight);
                    remainingDamageWindow = 3;
                }
                shakeScore = Math.max(0, shakeScore - 30);
            }
        } else {
            shakeScore = Math.max(0, shakeScore - 6);
        }
    }

    private long recursiveWindow(long first, long divisor) {
        if (divisor <= 16384L) return first;
        return recursiveWindow(divisor, first % divisor);
    }

    public void onPacketRotation(PacketTypeCommon type, float yaw, float pitch) {
        if (!offset) return;

        float normalizedYaw = GeometryMath.angularDistance(yaw, previousRawYaw);
        float normalizedPitch = GeometryMath.angularDistance(pitch, previousRawPitch);

        if (DeveloperCommand.debugAim) {
            org.bukkit.entity.Player player = player();
            player.sendMessage(formatAim(normalizedYaw, normalizedPitch, yaw, pitch));
        }

        if (movementObservationTicks > 0 && checkBadRotation) {
            if (normalizedYaw > 0.0f && normalizedYaw % 1.0f == 0.0f
                    && MillisecondSampler.currentTimeMillis()
                    - lastPositionLookPacketAt >= 1500L) {
                badYawStreak++;
                if (badYawStreak > 3) {
                    badYawStreak = 1;
                    flag("ka.abt.bdr",
                            "exhibiting an aiming pattern similar to aimbot // bad rotation (yaw)", 1);
                }
            } else {
                badYawStreak = 0;
            }
        }

        float yawDelta = Math.abs(normalizedYaw - previousYawDelta);
        float pitchDelta = Math.abs(normalizedPitch - previousPitchDelta);
        yawDeltaWindow.add(yawDelta);
        pitchDeltaWindow.add(pitchDelta);

        if (yawDeltaWindow.isFull()) {
            if (pitchDeltaWindow.isFull()) {
                if (movementObservationTicks > 0 && checkAcceleration) {
                    float yawWindow = yawDeltaWindow.mean();
                    float pitchWindow = pitchDeltaWindow.mean();
                    float yawRecent = yawDeltaWindow.standardDeviation();
                    float pitchRecent = pitchDeltaWindow.standardDeviation();
                    boolean lowYaw = yawWindow < 1.0f
                            || (pitchWindow < 1.0f && !(normalizedYaw < 1.5f));
                    boolean asymmetricPitch = yawRecent < 5.0f && pitchRecent > 5.0f
                            && !(normalizedYaw < 1.5f);
                    if (lowYaw && asymmetricPitch) {
                        accelerationCount++;
                        if (accelerationCount > 8) {
                            String message = accelerationMessage(accelerationCount);
                            flag("ka.abt.acl", message, aimViolationWeight / 2);
                            if (accelerationCount > 10) accelerationCount = 10;
                        }
                    } else {
                        accelerationCount = Math.max(0, accelerationCount - 1);
                    }
                }
            }
        }

        if (snapSampleCount < 3) {
            snapSampleCount++;
            if (snapSampleCount == 2 && normalizedYaw > 20.0f && previousNormalizedYaw > 20.0f
                    && Math.abs(normalizedYaw - previousNormalizedYaw) <= 3.0f
                    && Math.abs(normalizedPitch - previousNormalizedPitch) <= 3.0f
                    && Math.abs(pitch) <= 60.0f && checkSnapAim) {
                flag("ka.abt.snp",
                        "exhibiting an aiming pattern similar to aimbot // snap", aimViolationWeight);
            }
            previousNormalizedYaw = normalizedYaw;
            previousNormalizedPitch = normalizedPitch;
        }

        if (movementObservationTicks > 0) {
            if (Math.abs(pitch) <= 75.0f) {
                if (normalizedYaw > 0.0f
                        && (normalizedPitch == 0.0f || isScientificFraction(Float.valueOf(normalizedPitch)))) {
                    synchronizationScore += Math.min(normalizedYaw, 5.0f);
                    if (normalizedPitch == 0.0f && normalizedYaw > 10.0f)
                        synchronizationScore += 5.0f;
                    if (synchronizationScore > 50.0f) {
                        if (checkSynchronizedAim) {
                            flag("ka.abt.syn",
                                    "exhibiting an aiming pattern similar to aimbot // sync", 2);
                            remainingDamageWindow = 5;
                        }
                        synchronizationScore = 30.0f;
                    }
                } else {
                    synchronizationScore = Math.max(0.0f,
                            synchronizationScore - (synchronizationScore > 50.0f ? 10.0f : 4.0f));
                }
            }

            long elapsedPitchWindow = recursiveWindow(
                    (long) ((double) normalizedPitch * movementScale),
                    (long) ((double) previousPitchDelta * movementScale));
            if (normalizedYaw > 0.0f && normalizedPitch > 0.0f && previousPitchDelta > 0.0f) {
                if (elapsedPitchWindow < 131072L) {
                    sensitivityCount++;
                    if (sensitivityCount > 10) {
                        sensitivityCount = 10;
                        if (checkSensitivity && remainingDamageWindow <= 0) {
                            flag("ka.abt.ses",
                                    "exhibiting an aiming pattern similar to aimbot // sensitivity", 0);
                            remainingDamageWindow = 5;
                        }
                    }
                } else {
                    sensitivityCount = Math.max(0, sensitivityCount - 1);
                }
            }
        }

        previousRawYaw = yaw;
        previousRawPitch = pitch;
        previousYawDelta = normalizedYaw;
        previousPitchDelta = normalizedPitch;
    }
}

