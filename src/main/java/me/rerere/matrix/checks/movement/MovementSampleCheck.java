package me.rerere.matrix.checks.movement;

/** Collects movement samples and submits eligible windows to the inference task. */
public final class MovementSampleCheck extends me.rerere.matrix.checks.CheckRuntimeState {
    private double previousX, previousY, previousZ;
    private double previousDeltaX, previousDeltaZ;
    private float previousPitchDifference, previousYaw, previousPitch, previousYawDifference;
    private me.rerere.matrix.analysis.AnalysisRequest samples;
    private boolean markNextSampleAsAttack;

    public MovementSampleCheck(me.rerere.matrix.player.PlayerCheckContext config) {
        super(config, me.rerere.matrix.checks.CheckCategory.KILLAURA);
        this.samples = new me.rerere.matrix.analysis.AnalysisRequest(null, new java.util.ArrayList<>());
    }

    public boolean onPacketReceive(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent event,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper<?> wrapper) {
        me.rerere.matrix.config.MatrixConfigBridge heightService =
                me.rerere.matrix.Matrix.Companion;
        if (!heightService.matrixInstance().getInferenceTask().isEnabled()) return false;

        if (wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation movement =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper;
            double x = previousX;
            double y = previousY;
            double z = previousZ;
            float yaw = previousYaw;
            float pitch = previousPitch;
            if (movement.hasPositionChanged()) {
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d position = movement.getPosition();
                x = position.x;
                y = position.y;
                z = position.z;
            }
            if (movement.hasRotationChanged()) {
                yaw = movement.getYaw();
                pitch = movement.getPitch();
            }

            java.util.List<me.rerere.matrix.player.MovementSample> sampleWindow = sampleData();
            if (sampleWindow.size() >= 100) {
                int markedCount = 0;
                for (me.rerere.matrix.player.MovementSample sample : sampleWindow) {
                    if (sample.isAttack() && ++markedCount < 0) {
                        me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.throwCountOverflow();
                    }
                }
                double totalAbsYaw = 0.0D;
                for (me.rerere.matrix.player.MovementSample sample : sampleWindow) {
                    totalAbsYaw += Math.abs((double) sample.component3());
                }
                if (markedCount > 3 && totalAbsYaw > 3 * sampleWindow.size()
                        && !super.playerState().isInVehicle()
                        && !super.playerState().isGliding()) {
                    this.samples.setOwner(this.player().getUniqueId());
                    heightService.matrixInstance().getInferenceTask()
                            .add(this.samples);
                } else if (me.rerere.matrix.commands.DeveloperCommand.bedrockModeEnabled) {
                    String message = appendSample(org.bukkit.ChatColor.GRAY,
                            this.player().getName());
                    me.rerere.matrix.runtime.MatrixTasks.broadcast(message);
                }

                this.samples = new me.rerere.matrix.analysis.AnalysisRequest(null, new java.util.ArrayList<>());
            }

            float yawDifference = normalizedAngleDifference(yaw, previousYaw);
            float pitchDifference = normalizedAngleDifference(pitch, previousPitch);
            float yawDelta = yawDifference - previousYawDifference;
            float pitchDelta = pitchDifference - previousPitchDifference;
            double dx = x - previousX;
            double dy = y - previousY;
            double dz = z - previousZ;
            double rotatedX = dx;
            double rotatedZ = dz;
            if (dx != 0.0D || dz != 0.0D) {
                double radians = Math.toRadians((double) yaw);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                rotatedX = -sin * dx + cos * dz;
                rotatedZ = -cos * dx - sin * dz;
            }
            sampleData().add(new me.rerere.matrix.player.MovementSample(
                    yaw, pitch, yawDifference, pitchDifference, yawDelta, pitchDelta,
                    dx, dy, dz, rotatedX, rotatedZ, markNextSampleAsAttack));
            markNextSampleAsAttack = false;
            previousX = x;
            previousY = y;
            previousZ = z;
            previousDeltaX = dx;
            previousDeltaZ = dz;
            previousYaw = yaw;
            previousPitch = pitch;
            previousYawDifference = yawDifference;
            previousPitchDifference = pitchDifference;
        }

        if (wrapper == null || !me.rerere.matrix.network.packet.PacketEventSupport.isAttackWrapper(wrapper)) return false;
        int entityId = me.rerere.matrix.network.packet.PacketEventSupport.getAttackEntityId(wrapper);
        me.rerere.matrix.player.PlayerCheckContext playerData = super.playerState();
        me.rerere.matrix.entity.TrackedEntity entity = playerData.motionTracker().get(entityId);
        if (entity == null) return false;
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType =
                entity.entityType();
        boolean living = me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
                .isTypeInstanceOf(entityType,
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.LIVINGENTITY);
        java.util.Set<?> specialTypes = me.rerere.matrix.network.packet.NonstandardHitboxEntityTypes.getTypes();
        if (living && !specialTypes.contains(entityType)) {
            markNextSampleAsAttack = true;
            return false;
        }
        if (!specialTypes.contains(entityType)) return false;
        this.samples = new me.rerere.matrix.analysis.AnalysisRequest(null, new java.util.ArrayList<>());
        if (me.rerere.matrix.commands.DeveloperCommand.bedrockModeEnabled) {
            String message = appendEntitySample(org.bukkit.ChatColor.GRAY,
                    this.player().getName());
            me.rerere.matrix.runtime.MatrixTasks.broadcast(message);
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private java.util.List<me.rerere.matrix.player.MovementSample> sampleData() {
        return (java.util.List<me.rerere.matrix.player.MovementSample>)
                (java.util.List<?>) this.samples.data();
    }

    private static float normalizedAngleDifference(float first, float second) {
        float difference = (first - second) % 360.0f;
        if (difference > 180.0f) difference -= 360.0f;
        else if (difference < -180.0f) difference += 360.0f;
        return difference;
    }

    private static String appendEntitySample(org.bukkit.ChatColor color, String name) {
        return new StringBuilder().append((Object) color).append(name)
                .append("'s sample discarded due to attack weird entity").toString();
    }

    private static String appendSample(org.bukkit.ChatColor color, String name) {
        return new StringBuilder().append((Object) color).append(name)
                .append("'s sample discarded").toString();
    }

    public void onSample(me.rerere.matrix.inference.AnalysisResultItem sample) {
        int prediction = sample.componentPrediction();
        if (prediction == 1) {
            java.util.Map<String, Object> details = java.util.Collections.<String, Object>singletonMap(
                    "cheat score",
                    me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(sample.getConfidence() * 100.0D));
            super.flag("ka.ai", "acting like aimbot in combat", details,
                    me.rerere.matrix.util.GeometryMath.floorToInt(
                            sample.getConfidence() * 20.0D));
        }

        if (!me.rerere.matrix.commands.DeveloperCommand.bedrockModeEnabled) return;
        double confidenceScore = sample.getConfidence();
        String scoreColor;
        if (confidenceScore >= 0.0D && confidenceScore <= 0.25D) scoreColor = "&a";
        else if (confidenceScore >= 0.25D && confidenceScore <= 0.5D) scoreColor = "&2";
        else if (confidenceScore >= 0.5D && confidenceScore <= 0.75D) scoreColor = "&6";
        else scoreColor = "&c";
        String predictionName = prediction == 0 ? "legit" : prediction == 1 ? "cheat" : "unknown";
        String analysis = formatAnalysis(this.player().getName(),
                scoreColor, predictionName, scoreColor,
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(confidenceScore));
        String broadcast = me.rerere.matrix.util.text.ChatColorFormatter.formatString(analysis, 1);
        org.bukkit.Bukkit.broadcastMessage(broadcast);
    }
    private static String formatAnalysis(String player, String category, String detail,
                                         String confidence, String score) {
        return new StringBuilder("&f[Analysis/").append(player).append("] ")
                .append(category).append(detail).append(" &7(confidence: ")
                .append(confidence).append(score).append("&7)").toString();
    }

}
