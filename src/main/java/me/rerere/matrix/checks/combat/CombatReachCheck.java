package me.rerere.matrix.checks.combat;

import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.SecurityMode;
import me.rerere.matrix.checks.rotation.TrackingWindow;
import me.rerere.matrix.config.DefaultSettingProperty;
import me.rerere.matrix.entity.EntityStateSnapshots;
import me.rerere.matrix.entity.EntityTypeGroups;
import me.rerere.matrix.entity.TrackedEntity;
import me.rerere.matrix.listeners.CallbackEventFactory;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.Ray;
import me.rerere.matrix.interaction.InteractionData;
import me.rerere.matrix.license.HitboxLicenseState;
import me.rerere.matrix.network.InteractEntityObservation;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.TargetTrackingSnapshot;
import me.rerere.matrix.replay.AttackReplayFrame;
import me.rerere.matrix.replay.PacketStateSnapshot;
import me.rerere.matrix.util.GeometryMath;

/** Reach check that evaluates recorded interactions against candidate hitboxes. */
public final class CombatReachCheck extends CheckRuntimeState {
    private static boolean checksEnabled;
    private static String[] encryptedStrings;
    public static final me.rerere.matrix.thirdparty.kotlin.reflect.KProperty[] PROPERTY_REFERENCES = createPropertyReferences();
    public static final HitboxLicenseState volume = new HitboxLicenseState();
    private final ConcurrentHashMap<Integer, InteractionData> pendingTransactions;
    private final DefaultSettingProperty cancelThreshold;
    private final DefaultSettingProperty cancelMode;
    private final DefaultSettingProperty raycastEnabled;
    private boolean damageResponsePending;
    private long lastCheckTime;
    private final double movementLimit;

    public CombatReachCheck(PlayerCheckContext playerState) {
        super(playerState, CheckCategory.HITBOX);
        this.pendingTransactions = new ConcurrentHashMap<>();
        this.cancelThreshold = CallbackEventFactory.createSecondary(this, "cancel_vl", Integer.valueOf(5));
        this.cancelMode = CallbackEventFactory.createSecondary(this, "cancel_way", "silent");
        this.raycastEnabled = CallbackEventFactory.createSecondary(this, "raycast", Boolean.FALSE);
        this.movementLimit = 3.0;
    }

    public static boolean isCheckEnabled() { return checksEnabled; }

    private TrackingWindow getRaycastVersionMode() {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion client=super.playerState().clientVersion();
        return client.isOlderThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_12_2)
            ? TrackingWindow.CURRENT_AND_LAST_YAW
            : TrackingWindow.CURRENT_ONLY;
    }

    public static void setCheckEnabled(boolean enabled) { checksEnabled=enabled; }

    public static void setEncryptedStrings(String[] strings) { encryptedStrings=strings; }

    public static String[] getEncryptedStrings() { return encryptedStrings; }

    private boolean usesLegacyTransactionProtocol() {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion client=super.playerState().clientVersion();
        return client.isOlderThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_8)
            && Matrix.Companion.serverVersion().isOlderThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_8_8);
    }

    private void reportReachResult(InteractEntityObservation result, boolean estimated) {
        if(!result.cancelHit()) return;
        java.util.Map<String,String> estimatedDetails=estimated
            ? java.util.Collections.singletonMap("estimated","true") : java.util.Collections.emptyMap();
        java.util.Map<String,String> vehicleDetails=result.vehicle()
            ? java.util.Collections.singletonMap("vehicle","true") : java.util.Collections.emptyMap();
        String category=result.component();
        if("hb.reach".equals(category)) {
            java.util.Map<String,Object> details=new java.util.LinkedHashMap<>();
            details.put("dist",DiagnosticFormat.formatDouble(result.reach()));
            details.put("max",DiagnosticFormat.formatDouble(result.allowedReach()));
            TargetTrackingSnapshot target=result.target();
            details.put("type",target.getType().getName().toString());
            details.put("pose",target.getPose().name());
            String action=result.action().name().toLowerCase(java.util.Locale.ROOT);
            details.put("action",action);
            details.putAll(estimatedDetails);
            details.putAll(vehicleDetails);
            int severity=!estimated && !result.zeroVl() ? 1 : 0;
            super.flag("hb.reach","reach further than normal",details,severity);
            return;
        }
        if("hb.miss".equals(category)) {
            java.util.Map<String,Object> details=new java.util.LinkedHashMap<>();
            TargetTrackingSnapshot target=result.target();
            details.put("type",target.getType().getName().toString());
            details.put("pose",target.getPose().name());
            AxisAlignedBox box=result.boundingBox();
            String boxText=formatLabelTriple(
                DiagnosticFormat.formatDouble(box.width()),
                DiagnosticFormat.formatDouble(box.height()),
                DiagnosticFormat.formatDouble(box.depth()));
            details.put("box",boxText);
            String action=result.action().name().toLowerCase(java.util.Locale.ROOT);
            details.put("action",action);
            details.putAll(estimatedDetails);
            details.putAll(vehicleDetails);
            int severity=!estimated && !result.zeroVl() ? 2 : 0;
            super.flag("hb.miss","hit without any intersection",details,severity);
        }
    }

    private int getCancelVl() {
        return ((Number) cancelThreshold.getValue(this, PROPERTY_REFERENCES[0])).intValue();
    }

    private String getCancelWay() {
        return (String) cancelMode.getValue(this, PROPERTY_REFERENCES[1]);
    }

    @Override
    public void onAttackWithContext(EntityDamageByEntityEvent event) {
        if (!damageResponsePending) return;
        damageResponsePending=false;
        int violationLevel=super.violationCount();
        int threshold=getCancelVl();
        if (!(0 <= threshold && threshold <= violationLevel)) return;
        SecurityMode outcome = SecurityMode.auxiliaryMarker.resolve(getCancelWay());
        if (outcome == SecurityMode.NONE) return;
        if (outcome == SecurityMode.SILENT) {
            event.setDamage(0.0);
            return;
        }
        if (outcome == SecurityMode.CANCEL) {
            event.setCancelled(true);
            return;
        }
        throw new me.rerere.matrix.thirdparty.kotlin.NoWhenBranchMatchedException();
    }
    private boolean getCheckRaycast() {
        return ((Boolean) raycastEnabled.getValue(this, PROPERTY_REFERENCES[2])).booleanValue();
    }

    private static void reportIfEnabled(CombatReachCheck check,
            InteractEntityObservation result, boolean estimated) {
        check.reportReachResult(result, estimated);
    }

    private void updateLastCheckTime(InteractEntityObservation result) {
        if("hb.reach".equals(result.component()) || "hb.miss".equals(result.component()))
            lastCheckTime=System.currentTimeMillis();
    }

    private static boolean checkInteractions(CombatReachCheck check, int entityId,
            java.util.List<AttackReplayFrame> interactions,
            boolean estimated, Double requestedReach, Double reachAllowance,
            boolean reportBest, boolean useCurrentPosition,
            TrackingWindow action, boolean useCurrentStateOnly,
            boolean useHistoricalBounds) {
        return check.selectClosestInteraction(entityId,interactions,estimated,requestedReach,
            reachAllowance,reportBest,useCurrentPosition,action,
            useCurrentStateOnly,useHistoricalBounds);
    }

    public boolean checkBlockInteraction(int entityId, PacketStateSnapshot targetState) {
        AttackReplayFrame interaction=
            new AttackReplayFrame(targetState,null);
        return checkInteractions(this,entityId,java.util.Collections.singletonList(interaction),
            false,null,null,true,false,getRaycastVersionMode(),false,false);
    }

    private boolean selectClosestInteraction(int entityId,
            java.util.List<AttackReplayFrame> interactions,
            boolean estimated, Double requestedReach, Double reachAllowance,
            boolean reportBest, boolean useCurrentPosition,
            TrackingWindow action,
            boolean useCurrentStateOnly, boolean useHistoricalBounds) {
        InteractEntityObservation best=null;

        if(!checksEnabled) return true;
        for(AttackReplayFrame candidate:interactions) {
            PacketStateSnapshot targetState=candidate.getPlayerStateAlias();
            java.util.List<Vector> origins;
            if(useCurrentPosition) {
                origins=java.util.Collections.singletonList(new Vector(
                    targetState.getX(),
                    targetState.getY(),
                    targetState.getZ()));
            } else {
                origins=targetState.getPositionCandidates();
            }
            InteractionData attack=new InteractionData(
                origins,null,
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity$InteractAction.ATTACK);
            TargetTrackingSnapshot target=candidate.getTargetState();
            if(target==null) {
                TrackedEntity entity=playerState().motionTracker()
                    .get(entityId);
                target=entity==null?null:EntityStateSnapshots.fromUpdateSnapshot(entity);
            }
            if(target==null) continue;

            InteractEntityObservation result=evaluateReachCandidate(
                target,attack,targetState,requestedReach,reachAllowance,
                action,useCurrentStateOnly,useHistoricalBounds);
            if(result.hit()
                    && !result.cancelHit()) {
                damageResponsePending=false;
                return true;
            }
            if(best==null || result.score()
                    < best.score()) best=result;
        }
        if(best==null) return true;
        updateLastCheckTime(best);
        if(reportBest) reportIfEnabled(this,best,estimated);
        damageResponsePending=best.cancelHit();
        boolean accepted=!best.cancelHit();
        damageResponsePending=false;
        return accepted;
    }

    public boolean checkTrackedEntity(int entityId, me.rerere.matrix.checks.combat.AttackReplaySnapshot entityTracking) {
        if(!checksEnabled) return true;
        if(entityTracking.getTargetState()==null) return false;
        return selectClosestInteraction(entityId,
            entityTracking.getTargetStates(),
            true,Double.valueOf(getReachCompensation()),Double.valueOf(movementLimit),
            false,true,TrackingWindow.CURRENT_ONLY,true,false);
    }

    public boolean checkAttackEntity(int entityId, me.rerere.matrix.checks.combat.AttackReplaySnapshot entityTracking) {
        boolean forceSpecialChecks=!entityTracking.isCancelledOriginal()
            && !usesLegacyTransactionProtocol();
        java.util.List<AttackReplayFrame> interactions=
            entityTracking.getTargetStates();
        if(entityTracking.isCancelledOriginal()) {
            return checkInteractions(this,entityId,interactions,false,
                Double.valueOf(getReachCompensation()),Double.valueOf(movementLimit),
                true,true,TrackingWindow.CURRENT_ONLY,
                true,true);
        }
        return checkInteractions(this,entityId,interactions,forceSpecialChecks,null,null,
            true,false,getRaycastVersionMode(),false,false);
    }

    public boolean onPacketReceive(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent event,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper wrapper) {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon received=event.getPacketType();
        boolean tracked=me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(received);
        boolean pong=received==me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PONG;
        boolean confirmation=received==me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.WINDOW_CONFIRMATION;
        if(tracked || pong || confirmation) {
            PacketStateSnapshot targetState=playerState()
                    .movementState().snapshot();
            processPendingInteractions(targetState,null,true,
                    null,null,getRaycastVersionMode(),false,false);
        }
        return false;
    }

    private double getReachDistance() {
        if (Matrix.Companion
                .serverVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_20_5)) {
            org.bukkit.attribute.AttributeInstance attribute=super.player().getAttribute(
                me.rerere.matrix.world.BukkitAttributes.INDEX.interactionRangeValue());
            if(attribute!=null) return attribute.getValue();
        }
        return movementLimit;
    }

    private double adjustReachForTarget(TargetTrackingSnapshot target, double baseReach) {
        double adjusted=baseReach;
        if(super.playerState().isInVehicle()) adjusted+=0.1;
        if(target.getVehicleId()!=null) adjusted+=0.2;
        return adjusted;
    }

    private double getTargetReachAllowance(TargetTrackingSnapshot target) {
        boolean playerIsInVehicle=super.playerState().isInVehicle();
        if(playerIsInVehicle && target.getVehicleId()!=null) return 1.0;
        if(target.getVehicleId()!=null) return 0.8;
        return 0.5;
    }

    private AxisAlignedBox getEntityHitbox(TargetTrackingSnapshot target, Double expansion, boolean useCurrentBounds) {
        AxisAlignedBox bounds=useCurrentBounds
            ? target.copyCertainCollisionBox()
            : target.copyCollisionBox();
        return buildTargetHitbox(target,bounds,expansion);
    }

    public static AxisAlignedBox getEntityHitboxForFlags(
            CombatReachCheck check, TargetTrackingSnapshot target,
            Double expansion, boolean useCurrentBounds, int flags) {
        if((flags & 2)!=0) expansion=Double.valueOf(check.getRayTraceTolerance());
        if((flags & 4)!=0) useCurrentBounds=false;
        return check.getEntityHitbox(target,expansion,useCurrentBounds);
    }

    private AxisAlignedBox buildTargetHitbox(TargetTrackingSnapshot target, AxisAlignedBox originalBounds, Double expansion) {
        AxisAlignedBox hitbox=originalBounds.copy();
        if(EntityTypeGroups.endCrystalTypes().contains(target.getType())) {
            hitbox=AxisAlignedBox.aroundVector(target.getClientPos(), 2.0f, 2.0f);
        }
        if(expansion!=null) hitbox.grow(expansion.doubleValue(),expansion.doubleValue(),expansion.doubleValue());
        return hitbox;
    }

    public static java.util.List<AxisAlignedBox> getHitboxesForFlags(CombatReachCheck check, TargetTrackingSnapshot target, Double expansion, boolean useHistory, int flags) {
        if((flags & 2)!=0) expansion=Double.valueOf(check.getRayTraceTolerance());
        if((flags & 4)!=0) useHistory=false;
        return check.getCandidateHitboxes(target,expansion,useHistory);
    }



    private java.util.List<AxisAlignedBox> getCandidateHitboxes(TargetTrackingSnapshot target, Double expansion, boolean useHistoricalBounds) {
        java.util.List<AxisAlignedBox> sourceBounds;
        if(useHistoricalBounds) {
            sourceBounds=target.copyDiscreteCollisionBoxes();
        } else {
            sourceBounds=java.util.Collections.singletonList(target.copyCollisionBox());
        }
        java.util.ArrayList<AxisAlignedBox> adjusted=new java.util.ArrayList<>(sourceBounds.size());
        for(AxisAlignedBox bounds:sourceBounds)
            adjusted.add(buildTargetHitbox(target,bounds,expansion));
        return adjusted;
    }

    private java.util.List<Vector> getDistinctTargetPositions(InteractionData interaction,
            PacketStateSnapshot targetState) {
        @SuppressWarnings("unchecked")
        java.util.List<Vector> origins=(java.util.List<Vector>)(java.util.List<?>)interaction.getOrigins();
        java.util.ArrayList<Vector> positions=new java.util.ArrayList<>(origins);
        positions.addAll(targetState.getPositionCandidates());
        positions.addAll(playerState().movementState().getServerPositionCandidates());
        return new java.util.ArrayList<>(new java.util.LinkedHashSet<>(positions));
    }

    private java.util.List<Double> getCandidateHeights(PacketStateSnapshot targetState, boolean targetOnly) {
        if(targetOnly) return java.util.Collections.singletonList(Double.valueOf(targetState.getEyeHeight()));
        java.util.ArrayList<Double> heights=new java.util.ArrayList<>(playerState().possibleEyeHeights());
        heights.add(Double.valueOf(targetState.getEyeHeight()));
        heights.add(Double.valueOf(targetState.getLastEyeHeight()));
        return new java.util.ArrayList<>(new java.util.LinkedHashSet<>(heights));
    }

    private java.util.ArrayList<Double> getRayIntersectionDistances(java.util.List<Vector> queryOrigins, Vector basePosition, AxisAlignedBox collisionBox, java.util.List<Double> verticalOffsets) {
        java.util.ArrayList<Double> distances=new java.util.ArrayList<>();
        for(Vector origin:queryOrigins) {
            for(Double verticalOffset:verticalOffsets) {
                Vector queryPoint=new Vector(basePosition.getX(),basePosition.getY()+verticalOffset.doubleValue(),basePosition.getZ());
                Ray ray=new Ray(queryPoint,origin);
                Vector intersection=collisionBox.clipSegmentAgainstBox(ray,0.0f,5.0f);
                distances.add(intersection==null?null:intersection.distance(queryPoint));
            }
        }
        return distances;
    }

    private static Double minimumRayDistance(java.util.List<Double> distances) {
        Double minimum=null;
        for(Double distance:distances) {
            if(distance!=null)
                minimum=minimum==null?distance:Double.valueOf(Math.min(minimum.doubleValue(),distance.doubleValue()));
        }
        return minimum;
    }

    private InteractEntityObservation processPendingInteractions(
            PacketStateSnapshot targetState,
            TargetTrackingSnapshot knownTarget,
            boolean reportBest, Double requestedReach, Double reachAllowance,
            TrackingWindow action,
            boolean useCurrentStateOnly, boolean useHistoricalBounds) {
        InteractEntityObservation lastResult=null;

        for (java.util.Map.Entry<Integer,InteractionData> entry :
                new java.util.ArrayList<java.util.Map.Entry<Integer,InteractionData>>(pendingTransactions.entrySet())) {
            Integer entityId=entry.getKey();
            InteractionData interaction=entry.getValue();
            pendingTransactions.remove(entityId,interaction);

            TargetTrackingSnapshot target=knownTarget;
            if(target==null) target=interaction.getTarget();
            if(target==null) {
                TrackedEntity entity=playerState().motionTracker()
                    .get(entityId.intValue());
                target=entity==null?null:EntityStateSnapshots.fromUpdateSnapshot(entity);
            }
            if(target==null) continue;

            InteractEntityObservation candidate=evaluateReachCandidate(
                target,interaction,targetState,requestedReach,reachAllowance,
                action,useCurrentStateOnly,useHistoricalBounds);
            lastResult=candidate;
        }

        if(reportBest && lastResult!=null) reportIfEnabled(this,lastResult,false);
        return lastResult;
    }

    private InteractEntityObservation evaluateReachCandidate(
            TargetTrackingSnapshot target,
            InteractionData interaction,
            PacketStateSnapshot targetState,
            Double requestedReach, Double reachAllowance,
            TrackingWindow action,
            boolean useCurrentStateOnly, boolean useHistoricalBounds) {
        CollisionResultFactory factory=InteractEntityObservation.companion;

        org.bukkit.GameMode gameMode=playerState().gameMode();
        if(gameMode!=org.bukkit.GameMode.SURVIVAL && gameMode!=org.bukkit.GameMode.ADVENTURE)
            return factory.defaultResult(target,interaction);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType=target.getType();
        boolean supported=me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.isTypeInstanceOf(
            entityType,me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.LIVINGENTITY)
            || entityType==me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.END_CRYSTAL;
        if(!supported || EntityTypeGroups.armorStandTypes().contains(entityType)
                || target.getPose()
                    ==me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.DYING)
            return factory.defaultResult(target,interaction);

        if(playerState().isInVehicle()
                || target.getVehicleId()!=null)
            return evaluateAlternateReachCandidate(target,interaction,targetState,requestedReach,
                reachAllowance,action,useCurrentStateOnly,useHistoricalBounds);

        Double expansion=requestedReach==null?Double.valueOf(getRayTraceTolerance()):requestedReach;
        java.util.List<AxisAlignedBox> hitboxes=
            getCandidateHitboxes(target,expansion,useHistoricalBounds);
        java.util.List<me.rerere.matrix.thirdparty.kotlin.Pair<Float,Float>> rotationCandidates=targetState.getRotationCandidates(action);
        java.util.ArrayList<Vector> rayOrigins=new java.util.ArrayList<>(rotationCandidates.size());
        for(me.rerere.matrix.thirdparty.kotlin.Pair<Float,Float> rotation:rotationCandidates)
            rayOrigins.add(GeometryMath.directionFromClientVersion(
                playerState(),rotation.getFirst().floatValue(),rotation.getSecond().floatValue()));
        java.util.List<Double> verticalOffsets=useCurrentStateOnly
            ? java.util.Collections.singletonList(Double.valueOf(targetState.getEyeHeight()))
            : playerState().possibleEyeHeights();
        @SuppressWarnings("unchecked")
        java.util.List<Vector> candidatePositions=(java.util.List<Vector>)(java.util.List<?>)interaction.getOrigins();
        boolean found=false;
        double minimumDistance=0.0;
        Vector bestPosition=null;
        if(hitboxes.isEmpty()) throw new java.util.NoSuchElementException("List is empty.");
        AxisAlignedBox bestHitbox=hitboxes.get(0);
        for(Vector candidatePosition:candidatePositions) {
            for(AxisAlignedBox hitbox:hitboxes) {
                java.util.ArrayList<Double> distances=getRayIntersectionDistances(
                    rayOrigins,candidatePosition,hitbox,verticalOffsets);
                Double distance=minimumRayDistance(distances);
                if(distance!=null) {
                    double value=distance.doubleValue();
                    if(!found || value<minimumDistance) {
                        found=true;
                        minimumDistance=value;
                        bestPosition=candidatePosition;
                        bestHitbox=hitbox;
                    }
                }
            }
        }

        if(target.getBacktrackTicks()>1)
            return factory.acceptedResult(target,interaction,bestHitbox);
        if(minimumDistance>0.0 && getCheckRaycast() && bestPosition!=null)
            inspectNearbyEntities(target,bestPosition,bestHitbox,rayOrigins,verticalOffsets,
                minimumDistance,useCurrentStateOnly);

        if(!found) return factory.missResult(target,interaction,bestHitbox);
        double configuredReach=reachAllowance==null?getReachDistance():reachAllowance.doubleValue();
        return factory.reachResult(target,interaction,bestHitbox,
            minimumDistance,configuredReach);
    }

    private InteractEntityObservation evaluateAlternateReachCandidate(
            TargetTrackingSnapshot target,
            InteractionData interaction,
            PacketStateSnapshot targetState,
            Double requestedReach, Double reachAllowance,
            TrackingWindow action,
            boolean targetOnlyHeights, boolean useHistoricalBounds) {
        CollisionResultFactory factory=InteractEntityObservation.companion;

        if(target.getBacktrackTicks()>1)
            return factory.defaultResult(target,interaction);
        double baseReach=requestedReach==null?getRayTraceTolerance():requestedReach.doubleValue();
        Double expansion=Double.valueOf(adjustReachForTarget(target,baseReach));
        java.util.List<AxisAlignedBox> hitboxes=
            getCandidateHitboxes(target,expansion,useHistoricalBounds);
        java.util.List<me.rerere.matrix.thirdparty.kotlin.Pair<Float,Float>> rotationCandidates=
            targetState.getRotationCandidates(action);
        java.util.ArrayList<Vector> rayOrigins=new java.util.ArrayList<>(rotationCandidates.size());
        for(me.rerere.matrix.thirdparty.kotlin.Pair<Float,Float> rotation:rotationCandidates)
            rayOrigins.add(GeometryMath.directionFromClientVersion(
                playerState(),rotation.getFirst().floatValue(),rotation.getSecond().floatValue()));
        java.util.List<Double> verticalOffsets=getCandidateHeights(targetState,targetOnlyHeights);
        java.util.List<Vector> positions=getDistinctTargetPositions(interaction,targetState);
        boolean found=false;
        double minimumDistance=0.0;
        if(hitboxes.isEmpty()) throw new java.util.NoSuchElementException("List is empty.");
        AxisAlignedBox bestHitbox=hitboxes.get(0);
        for(Vector position:positions) {
            for(AxisAlignedBox hitbox:hitboxes) {
                java.util.ArrayList<Double> distances=getRayIntersectionDistances(
                    rayOrigins,position,hitbox,verticalOffsets);
                Double distance=minimumRayDistance(distances);
                if(distance!=null) {
                    double value=distance.doubleValue();
                    if(!found || value<minimumDistance) {
                        found=true;
                        minimumDistance=value;
                        bestHitbox=hitbox;
                    }
                }
            }
        }
        if(!found) return factory.missResultWithCollisionCheck(target,interaction,bestHitbox);

        double maximumReach=(reachAllowance==null?getReachDistance():reachAllowance.doubleValue())
            +getTargetReachAllowance(target);
        if(minimumDistance>maximumReach)
            return factory.reachResultWithReferenceDistance(target,interaction,bestHitbox,minimumDistance,maximumReach);
        return factory.reachResult(target,interaction,bestHitbox,
            minimumDistance,maximumReach);
    }

    private void inspectNearbyEntities(TargetTrackingSnapshot target,
            Vector targetPosition, AxisAlignedBox targetHitbox,
            java.util.List<Vector> rayOrigins, java.util.List<Double> verticalOffsets,
            double maximumDistance, boolean useCurrentBounds) {
        if(!target.isConstant()) return;

        java.util.ArrayList<TrackedEntity> nearby=new java.util.ArrayList<>();
        for(TrackedEntity entity:
                playerState().motionTracker().values()) {
            double separation=GeometryMath.horizontalManhattanDistance(
                entity.position(),target.getClientPos());
            if(separation<3.0) nearby.add(entity);
        }

        TrackedEntity closestOther=null;
        double closestDistance=maximumDistance;
        for(TrackedEntity entity:nearby) {
            if(entity.entityId()==target.getId()) continue;
            if(!entity.isWithinCoordinateTolerance()) continue;
            if(!EntityTypeGroups.targetEntityTypes().contains(
                    entity.entityType())) continue;

            TargetTrackingSnapshot otherTarget=
                EntityStateSnapshots.fromUpdateSnapshot(entity);
            AxisAlignedBox otherHitbox=getEntityHitbox(otherTarget,null,useCurrentBounds);
            java.util.ArrayList<Double> rayDistances=getRayIntersectionDistances(
                rayOrigins,targetPosition,otherHitbox,verticalOffsets);
            boolean intersectsBeforeTarget=false;
            for(Double distance:rayDistances)
                if(distance!=null && distance.doubleValue()<maximumDistance) {
                    intersectsBeforeTarget=true;
                    break;
                }
            if(!intersectsBeforeTarget) continue;
            Double minimum=minimumRayDistance(rayDistances);

            boolean containsCandidatePoint=false;
            for(Double verticalOffset:verticalOffsets) {
                Vector candidatePoint=new Vector(targetPosition.getX(),
                    targetPosition.getY()+verticalOffset.doubleValue(),targetPosition.getZ());
                if(otherHitbox.contains(candidatePoint)) {
                    containsCandidatePoint=true;
                    break;
                }
            }
            if(containsCandidatePoint) continue;
            if(closestOther!=null && !(minimum.doubleValue()<=closestDistance)) continue;
            closestDistance=minimum.doubleValue();
            closestOther=entity;
        }

        if(closestOther==null) return;
        java.util.Map<String,Object> details=new java.util.LinkedHashMap<>();
        details.put("type",target.getType().getName().toString());
        details.put("target",Integer.toString(target.getId()));
        details.put("passed",formatResourceIndex(
            closestOther.entityType().getName(),closestOther.entityId()));
        details.put("pose",target.getPose().name());
        details.put("box",formatLabelTriple(
            DiagnosticFormat.formatDouble(targetHitbox.width()),
            DiagnosticFormat.formatDouble(targetHitbox.height()),
            DiagnosticFormat.formatDouble(targetHitbox.depth())));
        super.flag("hb.raycast","attack entity passed through other entity",details,1);
        damageResponsePending=true;
    }

    private double getReachCompensation() {
        return usesLegacyTransactionProtocol() ? 0.1 : 0.03;
    }

    private double getRayTraceTolerance() {
        return usesLegacyTransactionProtocol() ? 0.1 : 0.13;
    }

    public static String formatResourceIndex(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.resources.ResourceLocation resource, int index) {
        return resource + "#" + index;
    }

    private static String formatLabelTriple(String first, String second, String third) {
        return new StringBuilder(first).append(", ").append(second).append(", ")
                .append(third).toString();
    }

    private static me.rerere.matrix.thirdparty.kotlin.reflect.KProperty[] createPropertyReferences() {
        me.rerere.matrix.thirdparty.kotlin.reflect.KProperty[] properties=new me.rerere.matrix.thirdparty.kotlin.reflect.KProperty[3];
        properties[0]=me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection.property1(
            new me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl(CombatReachCheck.class,"cancelVl","getCancelVl()I",0));
        properties[1]=me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection.property1(
            new me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl(CombatReachCheck.class,"cancelWay","getCancelWay()Ljava/lang/String;",0));
        properties[2]=me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection.property1(
            new me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl(CombatReachCheck.class,"checkRaycast","getCheckRaycast()Z",0));
        return properties;
    }
}
