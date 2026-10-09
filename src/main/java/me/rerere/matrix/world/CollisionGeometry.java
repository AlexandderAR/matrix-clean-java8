package me.rerere.matrix.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.compat.ProtocolTranslationSupport;
import me.rerere.matrix.entity.MovementBlockResult;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.logging.DebugLogger;
/** Collision geometry, material filtering, and movement-envelope queries. */
public class CollisionGeometry {
    private static final double MOVEMENT_WIDTH = 0x1.99999ap-2;
    private static final double ROUNDING_EPSILON = 0x1.0624dd2f1a9fcp-10;
    private static final double PLAYER_WIDTH = 0x1.333333fffbea1p-2;
    private static Set<Material> EXCLUDED_MATERIALS;

    public CollisionGeometry() {}

    public static Boolean hasCollisionWithinPlayerHeight(Location location) {
        AxisAlignedBox box = new AxisAlignedBox(location.toVector(), 0x1.3d70a4p-2, 0x1.ccccccp0);
        for (int x = GeometryMath.floorToInt(box.minX()); x < GeometryMath.ceilToInt(box.maxX()); x++)
            for (int z = GeometryMath.floorToInt(box.minZ()); z < GeometryMath.ceilToInt(box.maxZ()); z++)
                for (int y = GeometryMath.floorToInt(box.minY()); y < GeometryMath.ceilToInt(box.maxY()); y++) {
                    Block block = BlockEnvironment.getBlockAt(Objects.requireNonNull(location.getWorld()), x, y, z);
                    AxisAlignedBox[] shapes = BlockCollisionGeometry.getBlockShapes(block, BlockShapeMode.COLLISION, block.getType());
                    if (shapes.length != 0) {
                        double shapeTop = BlockCollisionGeometry.maximumTopY(shapes);
                        if (shapeTop > location.getY() && shapeTop <= location.getY() + 2.8)
                            return Boolean.TRUE;
                    }
                }
        return Boolean.FALSE;
    }

    private static String formatExcludedType(String materialNames) { return "(Phase#Debug) Exclude: " + materialNames; }

    private static boolean isNonLegacyMaterial(Material material) { return !material.name().startsWith("LEGACY_"); }
    private static boolean isLavaMaterial(Material material) { return material.name().contains("LAVA"); }
    private static boolean sameLavaCategory(Material firstType, Material secondType) { return isLavaMaterial(firstType) == isLavaMaterial(secondType); }
    private static int getFluidLevel(Block block) {
        BlockData data = block.getBlockData();
        return data instanceof Levelled ? ((Levelled) data).getLevel() : 0;
    }

    private static double getFluidSurfaceHeight(Block block) {
        if (!MaterialPolicy.isWaterloggedOrFluidBlock(block, block.getType())) return 0;
        byte fluidLevel = block.getData();
        if (fluidLevel >= 8) fluidLevel = 0;
        return (fluidLevel + 1.0) / 9.0;
    }

    private static double getSolidSurfaceHeight(Block block) {
        Material type = block.getType();
        if (!MaterialPolicy.isWaterloggedOrFluidBlock(block, type)) return 0;
        Block above = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        if (MaterialPolicy.isWaterloggedOrFluidBlock(above, above.getType()) && sameLavaCategory(type, above.getType())) return 1;
        int fluidLevel = getFluidLevel(block);
        return fluidLevel >= 8 ? 0x1.c71c71c71c71cp-1 : (8.0 - fluidLevel) / 9.0;
    }

    private static boolean hasBlockCollision(World world, AxisAlignedBox box, boolean includeLiquids) {
        return includeLiquids ? hasSolidGround(world, box) : hasAnyCollision(world, box);
    }

    private static boolean isWorldBoundsValid(PlayerCheckContext state, AxisAlignedBox box) {
        if (box.width() <= 8 && box.depth() <= 8 && box.height() <= 8) return true;
        double allowedHorizontalExtent = Math.max(state.velocityTracker().sumRecentVerticalValues() + 0.5, 8);
        double allowedVerticalExtent = Math.max(state.velocityTracker().maximumRecentMagnitude() + 0.5, 8);
        return box.width() <= allowedHorizontalExtent
                && box.depth() <= allowedHorizontalExtent
                && box.height() <= allowedVerticalExtent;
    }

    private static boolean hasSolidGround(World world, AxisAlignedBox box) {
        AxisAlignedBox scan = box.copy().grow(0, -MOVEMENT_WIDTH, 0)
                .grow(-ROUNDING_EPSILON, -ROUNDING_EPSILON, -ROUNDING_EPSILON);
        int maxY = GeometryMath.floorToInt(scan.maxY() + 1);
        for (int x = GeometryMath.floorToInt(scan.minX()); x < GeometryMath.floorToInt(scan.maxX() + 1); x++) {
            for (int z = GeometryMath.floorToInt(scan.minZ()); z < GeometryMath.floorToInt(scan.maxZ() + 1); z++) {
                for (int y = GeometryMath.floorToInt(scan.minY()); y < maxY; y++) {
                    double surfaceHeight = getFluidSurfaceHeight(BlockEnvironment.getBlockAt(world, x, y, z));
                    if (surfaceHeight > 0 && maxY >= y + 1.0 - surfaceHeight) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAnyCollision(World world, AxisAlignedBox box) {
        AxisAlignedBox scan = box.inset(ROUNDING_EPSILON);
        for (int x = GeometryMath.floorToInt(scan.minX()); x < GeometryMath.ceilToInt(scan.maxX()); x++) {
            for (int z = GeometryMath.floorToInt(scan.minZ()); z < GeometryMath.ceilToInt(scan.maxZ()); z++) {
                for (int y = GeometryMath.floorToInt(scan.minY()); y < GeometryMath.ceilToInt(scan.maxY()); y++) {
                    double surfaceHeight = getSolidSurfaceHeight(BlockEnvironment.getBlockAt(world, x, y, z));
                    if (surfaceHeight > 0 && y + surfaceHeight >= scan.minY() && scan.maxY() >= y) return true;
                }
            }
        }
        return false;
    }

    public static boolean hasSupportingShapeAt(Player player, Location location, PlayerCheckContext state) {
        AxisAlignedBox box = new AxisAlignedBox(location.toVector(), 0x1.333334p-2, 1.5).expand(0,-1,0);
        for (AxisAlignedBox shape : collectCollisionShapes(player.getWorld(), box, state))
            if (shape.maxY() == location.getY()) return true;
        return false;
    }

    public static double getStandingHeight(Player player) { return getPlayerHeight(player, false); }
    public static double getPoseHeight(Player player) { return getPlayerHeight(player, true); }

    private static double getPlayerHeight(Player player, Boolean forceSneak) {
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
            org.bukkit.entity.Pose pose = player.getPose();
            if (pose == org.bukkit.entity.Pose.STANDING && forceSneak) pose = org.bukkit.entity.Pose.SNEAKING;
            if (pose == org.bukkit.entity.Pose.STANDING) return 0x1.ccccccp0;
            Objects.requireNonNull(pose);
            if (pose == org.bukkit.entity.Pose.SNEAKING) {
                if(ProtocolTranslationSupport.isAvailable()) {
                    PlayerCheckContext state=Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
                    if (state.protocolVersion() < 107) return 0x1.ccccccp0;
                    if (state.protocolVersion() >= 335 && state.protocolVersion() <= 404) return 0x1.a66666p0;
                    if (state.protocolVersion() >= 477) return 1.5;
                } else return 1.5;
                return 0x1.333334p-1;
            }
            if(pose==org.bukkit.entity.Pose.SWIMMING) return 0x1.333334p-1;
            return 0x1.ccccccp0;
        }
        boolean sneaking = forceSneak || player.isSneaking();
        if (!sneaking) return 0x1.ccccccp0;
        if(ProtocolTranslationSupport.isAvailable()) {
            PlayerCheckContext state=Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
            if (state.protocolVersion() < 107) return 0x1.ccccccp0;
            if (state.protocolVersion() >= 335 && state.protocolVersion() <= 404) return 0x1.a66666p0;
            if (state.protocolVersion() >= 477) return 1.5;
            return 0x1.ccccccp0;
        }
        if(Matrix.getServerVersion()==ServerVersion.V_1_8_8) return 0x1.ccccccp0;
        if(Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) return 0x1.a66666p0;
        return 0x1.ccccccp0;
    }

    private static String formatBadCollisionRequest(String request, double x, double y, double z) {
        return "Detected a bad collision request(" + request + "): " + x + "/" + y + "/" + z;
    }

    private static boolean isSpecialCollisionMaterial(Material material) {
        String name = material.name();
        if (name.contains("STAIRS")) return true;
        if (name.contains("LILY")) return true;
        if (name.contains("ANVIL")) return true;
        if (name.contains("BREWING_STAND")) return true;
        if (name.contains("SLIME_BLOCK")) return true;
        if (name.contains("SWEET_BERRY_BUSH")) return true;
        if (name.contains("BAMBOO")) return true;
        if (name.contains("PISTON")) return true;
        if (name.contains("SHULKER")) return true;
        if (material.name().toLowerCase().contains("trap")) return true;
        if (name.contains("SCAFFOLDING")) return true;
        return name.equals("CAULDRON");
    }

    public static List<AxisAlignedBox> collectCollisionShapes(World world, AxisAlignedBox bounds, PlayerCheckContext state) {
        List<AxisAlignedBox> result = new ArrayList<>();
        int minX=MovementMetrics.toInt(bounds.minX()), minY=MovementMetrics.toInt(bounds.minY()), minZ=MovementMetrics.toInt(bounds.minZ());
        int maxX=MovementMetrics.toInt(bounds.maxX()+1), maxY=MovementMetrics.toInt(bounds.maxY()+1), maxZ=MovementMetrics.toInt(bounds.maxZ()+1);
        for (int x=minX; x<=maxX; x++) for (int z=minZ; z<=maxZ; z++) for (int y=minY; y<=maxY; y++) {
            Block block=BlockEnvironment.getBlockAt(world,x,y,z);
            result.addAll(Arrays.asList(BlockCollisionGeometry.getBlockShapes(block, BlockShapeMode.COLLISION, state)));
        }
        return result;
    }

    public static boolean isLocationCollisionFree(Location location, PlayerCheckContext state) {
        if (!FoliaSupport.isLocationOwnedByCurrentRegion(location)) return false;
        AxisAlignedBox bounds=new AxisAlignedBox(location.toVector(),0x1.99999b332b075p-3,1.5);
        for (AxisAlignedBox shape:collectCollisionShapes(location.getWorld(),bounds,state))
            if (shape.intersects(bounds)) return false;
        return true;
    }

    public static double resolveVerticalMovement(Player player, PlayerCheckContext state, World world, Vector from, Vector to) {
        double speed=getStandingHeight(player);
        AxisAlignedBox swept=new AxisAlignedBox(from,0x1.333334p-2,speed);
        swept.expand(to.getX()-from.getX(),to.getY()-from.getY(),to.getZ()-from.getZ());
        swept.expand(0,-0.5,0);
        if (!isWorldBoundsValid(state,swept)) {
            DebugLogger.printDebug(formatBadCollisionRequest(
                    state.playerName(), swept.width(), swept.depth(), swept.height()));
            return to.getY()-from.getY();
        }
        AxisAlignedBox playerBox=new AxisAlignedBox(from,PLAYER_WIDTH,speed);
        double targetY=to.getY();
        double delta=targetY-from.getY();
        if (delta>0) {
            double ceiling=targetY+speed;
            for(AxisAlignedBox shape:collectCollisionShapes(world,swept,state))
                if(shape.intersects(swept)&&!shape.intersects(playerBox)&&shape.minY()>=playerBox.maxY()) ceiling=Math.min(ceiling,shape.minY());
            return ceiling-speed-from.getY();
        }
        if(delta<0) {
            double floor=targetY;
            for(AxisAlignedBox shape:collectCollisionShapes(world,swept,state))
                if(shape.intersects(swept)&&!shape.intersects(playerBox)&&shape.maxY()<=playerBox.minY()) floor=Math.max(floor,shape.maxY());
            return floor-from.getY();
        }
        return delta;
    }

    public static EnumSet<Material> collectTouchedMaterials(Player player, Location from, Location to) {
        double speed=getStandingHeight(player);
        Set<Material> materials=new HashSet<>();
        Vector movement=to.toVector().subtract(from.toVector());
        AxisAlignedBox bounds=new AxisAlignedBox(from.toVector(),PLAYER_WIDTH,speed);
        bounds.expand(movement.getX(),movement.getY(),movement.getZ());
        World world=player.getWorld();
        for(int x=MovementMetrics.toInt(bounds.minX()+ROUNDING_EPSILON);x<=MovementMetrics.toInt(bounds.maxX()-ROUNDING_EPSILON);x++)
            for(int z=MovementMetrics.toInt(bounds.minZ()+ROUNDING_EPSILON);z<=MovementMetrics.toInt(bounds.maxZ()-ROUNDING_EPSILON);z++)
                for(int y=MovementMetrics.toInt(bounds.minY()+ROUNDING_EPSILON);y<=MovementMetrics.toInt(bounds.maxY()-ROUNDING_EPSILON);y++)
                    materials.add(world.getBlockAt(x,y,z).getType());
        return EnumSet.copyOf(materials);
    }

    public static void loadExcludedMaterials() {
        EnumSet<Material> excluded=EnumSet.copyOf(Stream.of(Material.values())
                .filter(CollisionGeometry::isNonLegacyMaterial)
                .filter(CollisionGeometry::isSpecialCollisionMaterial)
                .collect(Collectors.toSet()));
        if(Matrix.getServerVersion().isOlderThan(ServerVersion.V_1_13)) {
            excluded.add(Material.valueOf("THIN_GLASS"));
            excluded.add(Material.valueOf("IRON_FENCE"));
            excluded.add(Material.valueOf("STAINED_GLASS_PANE"));
        }
        YamlFile config = Matrix.getInstance().getConfigManager().getChecksConfiguration().configuration();
        List<String> configured=config.getStringList("phase.exclude_types");
        Set<Material> configuredMaterials=new HashSet<>();
        for(String entry:configured) {
            Optional<Material> parsed=MaterialPolicy.findMaterial(entry);
            if(parsed.isPresent()) {
                Material material=parsed.get();
                if(!excluded.contains(material)) {
                    excluded.add(material);
                    configuredMaterials.add(material);
                }
            }
        }
        EXCLUDED_MATERIALS=excluded;
        if(!configuredMaterials.isEmpty()) Matrix.logMessage(formatExcludedType(String.join(",",configuredMaterials.stream().map(Enum::name).toArray(String[]::new))));
    }

    public static Pair<Set<Material>, Boolean> collectIntersectedMaterials(World world, AxisAlignedBox bounds, boolean includeLiquids) {
        boolean collision=hasBlockCollision(world,bounds,includeLiquids);
        Set<Material> materials=new HashSet<>();
        int minX=GeometryMath.floorToInt(bounds.minX()), minY=GeometryMath.floorToInt(bounds.minY()), minZ=GeometryMath.floorToInt(bounds.minZ());
        int maxX=GeometryMath.ceilToInt(bounds.maxX()), maxY=GeometryMath.ceilToInt(bounds.maxY()), maxZ=GeometryMath.ceilToInt(bounds.maxZ());
        for(int x=minX;x<maxX;x++) for(int z=minZ;z<maxZ;z++) for(int y=minY;y<maxY;y++) {
            Block block=BlockEnvironment.getBlockAt(world,x,y,z);
            Material material=block.getType();
            if(material!=Material.AIR) {
                AxisAlignedBox[] shapes=BlockCollisionGeometry.getBlockShapes(block,BlockShapeMode.COLLISION,material);
                if(shapes.length==0 || bounds.intersectsAny(shapes)) materials.add(BlockEnvironment.getMaterial(block));
            }
        }
        return new Pair<>(materials,collision);
    }

    public static CollisionRegion buildMovementEnvelope(Player player, World world, Location from, Location to, ClientVersion version) {
        if(GeometryMath.horizontalManhattanDistance(from,to)>12) {
            CollisionRegion first=buildMovementEnvelope(player,world,to,to,version);
            first.mergeMaterials(buildMovementEnvelope(player,world,from,from,version));
            return first;
        }
        double speed=getStandingHeight(player);
        boolean oldClient=version.isOlderThan(ClientVersion.V_1_13);
        AxisAlignedBox fromBox=new AxisAlignedBox(from.toVector(),0x1.353f7dba5a1f5p-2,speed);
        AxisAlignedBox toBox=new AxisAlignedBox(to.toVector(),0x1.353f7dba5a1f5p-2,speed);
        Pair<Set<Material>,Boolean> fromHits=collectIntersectedMaterials(world,fromBox,oldClient);
        Pair<Set<Material>,Boolean> toHits=collectIntersectedMaterials(world,toBox,oldClient);
        fromBox.setMinY(fromBox.minY()-0x1.c01a36e2eb1c4p-2);
        Pair<Set<Material>,Boolean> extra=collectIntersectedMaterials(world,fromBox,oldClient);
        fromHits.getFirst().addAll(toHits.getFirst());
        return new CollisionRegion(from.toVector(),to.toVector(),fromHits.getFirst(),fromHits.getSecond()||toHits.getSecond(),fromHits.getSecond(),toHits.getSecond(),extra.getFirst());
    }

    public static MovementBlockResult findFirstBlockingBlock(Player player, Vector from, Vector to, World world, PlayerCheckContext state) {
        Optional<Block> blocking=Optional.empty();
        boolean foundSolid=false;
        double height=getPoseHeight(player);
        Vector movement=GeometryMath.copy(to).subtract(from);
        AxisAlignedBox search=new AxisAlignedBox(from,0x1.333333fffbea1p-2,height);
        search.expand(movement.getX(),movement.getY(),movement.getZ());
        AxisAlignedBox initial=new AxisAlignedBox(from,0x1.333333fffbea1p-2,height);
        AxisAlignedBox destination=new AxisAlignedBox(to,0x1.333333fffbea1p-2,height);
        searchBlocks: for(int x=(int)search.minX()-2;x<=search.maxX()+1;x++)
            for(int y=(int)search.minY()-2;y<=search.maxY()+1;y++)
                for(int z=(int)search.minZ()-2;z<=search.maxZ()+1;z++) {
                    Block block=BlockEnvironment.getBlockAt(world,x,y,z);
                    Material type=BlockEnvironment.getMaterial(block);
                    if(MaterialPolicy.isAirMaterial(type)||EXCLUDED_MATERIALS.contains(type)) continue;
                    AxisAlignedBox[] shapes=BlockCollisionGeometry.getBlockShapes(block,BlockShapeMode.COLLISION,state);
                    for(AxisAlignedBox shape:shapes) {
                        if(shape.height()==0 || !shape.intersects(search) || !shape.intersects(destination)) continue;
                        foundSolid=true;
                        if(!shape.intersects(initial)) { blocking=Optional.of(block); break; }
                    }
                    if(blocking.isPresent()) break searchBlocks;
                }
        return new MovementBlockResult(blocking,foundSolid);
    }
}
