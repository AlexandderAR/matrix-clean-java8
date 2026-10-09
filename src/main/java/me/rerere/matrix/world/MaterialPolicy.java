package me.rerere.matrix.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import me.rerere.matrix.util.CollectionUtilities;
import me.rerere.matrix.util.CollectionsSupport;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

/** Server-version-aware material categories and block helpers. */
public final class MaterialPolicy {
    private static final Set<Material> fluids;
    private static final Set<Material> slabOrStepMaterials;
    private static final Set<Material> iceMaterials;
    private static final Set<Material> boatMaterials;
    public static final Set<Material> spearMaterials;
    private static final Set<Material> trapdoorMaterials;
    private static final Material legacyCobweb;
    private static final Set<Material> supportMaterials;
    private static final Set<Material> fenceAndWallMaterials;
    private static final Set<Material> pistonMaterials;
    private static final Set<Material> signMaterials;
    public static final Set<Material> bedMaterials;
    private static final Set<Material> shulkerBoxMaterials;
    public static final Material carrotOnAStick;
    private static final Material spawner;
    public static final Material lilyPad;
    @Nullable public static final Material potentSulfur;

    public MaterialPolicy() { }

    private static boolean before113() {
        return Matrix.getServerVersion().isOlderThan(ServerVersion.V_1_13);
    }

    private static boolean atLeast113() {
        return Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13);
    }

    private static Set<Material> immutableEnumSet(Collection<Material> materials) {
        @SuppressWarnings("unchecked")
        Set<Material> enumSet = (Set<Material>) CollectionsSupport.enumSetOf(materials);
        return enumSet;
    }

    private static Set<Material> aliasSet(Material[] materials) {
        @SuppressWarnings("unchecked")
        Set<Material> aliases = CollectionUtilities.<Material>setOfValues(materials);
        return aliases;
    }

    private static boolean blockHasFluidType(Block block) {
        return fluids.contains(BlockEnvironment.getMaterial(block));
    }

    private static boolean materialHasFluidType(Material material) {
        return fluids.contains(material);
    }

    public static boolean isWaterloggedOrFluidBlock(Block block, Material material) {
        if (before113()) return fluids.contains(material);
        if (isEarthlike(material)) return false;
        BlockData data = BlockEnvironment.getBlockData(block);
        if (blockHasFluidType(block)) return true;
        if (data instanceof Waterlogged && ((Waterlogged)data).isWaterlogged()) return true;
        return material==Material.SEAGRASS || material==Material.BUBBLE_COLUMN || material==Material.TALL_SEAGRASS
                || material==Material.KELP || material==Material.KELP_PLANT;
    }

    private static boolean isModernMaterial(Material material) {
        return !material.name().startsWith("LEGACY_");
    }

    private static boolean isTrapdoor(Material material) {
        return !material.name().startsWith("LEGACY_") && material.name().endsWith("TRAPDOOR");
    }
    private static boolean hasSlabOrStepName(Material material) {
        String materialName = material.name();
        return materialName.contains("SLAB") || materialName.contains("STEP");
    }
    private static boolean hasIceName(Material material) {
        return material.name().contains("ICE");
    }
    private static boolean hasBoatName(Material material) {
        return material.name().contains("BOAT");
    }
    private static boolean hasSpearSuffix(Material material) {
        return material.name().endsWith("_SPEAR");
    }

    /** Returns whether the material is treated as air by the server version. */
    public static boolean isAirMaterial(Material material) {
        if (before113()) return material==Material.AIR;
        return material==Material.AIR || material==Material.CAVE_AIR || material==Material.VOID_AIR;
    }

    private static boolean isCobweb(Material material) {
        if (atLeast113()) return material==Material.COBWEB;
        return material==legacyCobweb;
    }

    public static Material cobwebMaterial() {
        if (atLeast113()) return Material.COBWEB;
        return legacyCobweb;
    }

    public static boolean isWaterOrLavaAt(Block block) {
        if (before113()) return blockHasFluidType(block);
        BlockData data = BlockEnvironment.getBlockData(block);
        Material material = BlockEnvironment.getMaterial(block);
        if (blockHasFluidType(block)) return true;
        if (data instanceof Waterlogged && ((Waterlogged)data).isWaterlogged()) return true;
        return material == Material.SEAGRASS || material == Material.BUBBLE_COLUMN
                || material == Material.TALL_SEAGRASS || material == Material.KELP
                || material == Material.KELP_PLANT;
    }

    private static boolean hasTrapdoorType(Material material) { return trapdoorMaterials.contains(material); }
    private static boolean hasSlabOrStepType(Material material) { return slabOrStepMaterials.contains(material); }
    private static boolean hasFluidBlockType(Block block) {
        return fluids.contains(BlockEnvironment.getMaterial(block));
    }
    private static boolean hasFenceType(Material material) { return fenceAndWallMaterials.contains(material); }
    private static boolean hasPistonType(Material material) { return pistonMaterials.contains(material); }
    private static boolean hasSignType(Material material) { return signMaterials.contains(material); }
    private static boolean hasBedType(Material material) { return bedMaterials.contains(material); }
    private static boolean hasShulkerBoxType(Material material) { return shulkerBoxMaterials.contains(material); }
    public static boolean hasIceType(Material material) { return iceMaterials.contains(material); }
    private static boolean hasBoatType(Material material) { return boatMaterials.contains(material); }
    private static boolean hasSupportType(Material material) { return supportMaterials.contains(material); }
    private static boolean isEarthlike(Material material) {
        return material==Material.GRASS_BLOCK || material==Material.SAND || material==Material.STONE
                || material==Material.NETHERRACK || material==Material.END_STONE;
    }
    private static boolean isBlockNonAir(Block block) {
        // Retain the historical API name; the original bridge returns the air test.
        return isAirMaterial(BlockEnvironment.getMaterial(block));
    }
    private static Set<Material> getShulkerBoxes() { return shulkerBoxMaterials; }
    private static Material getSpawner() { return spawner; }

    public static boolean containsFluidMaterial(Material material) { return materialHasFluidType(material); }
    public static boolean containsShulkerBox(Material material) { return hasShulkerBoxType(material); }
    public static boolean containsFluidBlock(Block block) { return hasFluidBlockType(block); }
    public static boolean containsSlabOrStep(Material material) { return hasSlabOrStepType(material); }
    public static boolean containsTrapdoor(Material material) { return hasTrapdoorType(material); }
    public static Material spawnerBridge() { return getSpawner(); }
    public static boolean containsFenceOrWall(Material material) { return hasFenceType(material); }
    public static boolean containsEarthlike(Material material) { return isEarthlike(material); }
    public static boolean containsPiston(Material material) { return hasPistonType(material); }
    public static boolean containsSign(Material material) { return hasSignType(material); }
    public static boolean containsSupport(Material material) { return hasSupportType(material); }
    public static boolean containsBoat(Material material) { return hasBoatType(material); }
    public static boolean nonAirBlockBridge(Block block) { return isBlockNonAir(block); }
    public static boolean cobwebBridge(Material material) { return isCobweb(material); }
    public static Set<Material> shulkerBoxBridge() { return getShulkerBoxes(); }
    public static boolean containsBedBridge(Material material) { return hasBedType(material); }

    public static Material materialByVersionNames(String oldName, String currentName) {
        if (before113()) return Material.valueOf(oldName);
        return Material.valueOf(currentName);
    }

    public static Optional<Material> findMaterial(String materialName) {
        Material found = null;
        try {
            found = Material.valueOf(materialName);
        } catch (Exception ignored) {
            for (Material material : Material.values()) {
                String enumName = material.name();
                if (!enumName.startsWith("LEGACY_") && enumName.equalsIgnoreCase(materialName)) {
                    found = material;
                    break;
                }
            }
        }
        return Optional.ofNullable(found);
    }

    static {
        carrotOnAStick=materialByVersionNames("CARROT_STICK", "CARROT_ON_A_STICK");
        lilyPad=materialByVersionNames("WATER_LILY", "LILY_PAD");
        potentSulfur=Material.matchMaterial("POTENT_SULFUR");

        slabOrStepMaterials = immutableEnumSet(Stream.of(Material.values())
                .filter(MaterialPolicy::isModernMaterial)
                .filter(MaterialPolicy::hasSlabOrStepName)
                .collect(Collectors.toSet()));
        iceMaterials = immutableEnumSet(Stream.of(Material.values())
                .filter(MaterialPolicy::isModernMaterial)
                .filter(MaterialPolicy::hasIceName)
                .collect(Collectors.toSet()));
        boatMaterials = immutableEnumSet(Stream.of(Material.values())
                .filter(MaterialPolicy::isModernMaterial)
                .filter(MaterialPolicy::hasBoatName)
                .collect(Collectors.toSet()));
        spearMaterials = immutableEnumSet(Stream.of(Material.values())
                .filter(MaterialPolicy::hasSpearSuffix)
                .collect(Collectors.toSet()));

        if (before113()) {
            fluids = EnumSet.copyOf(aliasSet(new Material[] {
                    Material.valueOf("STATIONARY_WATER"), Material.WATER,
                    Material.valueOf("STATIONARY_LAVA"), Material.LAVA
            }));
        } else {
            fluids = EnumSet.copyOf(aliasSet(new Material[] {Material.WATER, Material.LAVA}));
        }

        if (before113()) {
            trapdoorMaterials = EnumSet.of(
                    Material.valueOf("TRAP_DOOR"), Material.valueOf("IRON_TRAPDOOR"));
        } else {
            Collection<Material> trapdoors = Arrays.stream(Material.values())
                    .filter(MaterialPolicy::isTrapdoor)
                    .collect(Collectors.toSet());
            trapdoorMaterials = EnumSet.copyOf(trapdoors);
        }
        legacyCobweb = before113() ? Material.valueOf("WEB") : null;

        List<Material> support = new ArrayList<>();
        List<Material> fences = new ArrayList<>();
        List<Material> pistons = new ArrayList<>();
        List<Material> signs = new ArrayList<>();
        List<Material> beds = new ArrayList<>();
        List<Material> shulkers = new ArrayList<>();
        support.add(Material.SNOW);
        if (before113()) support.add(Material.valueOf("CAKE_BLOCK")); else support.add(Material.CAKE);
        if (before113()) beds.add(Material.valueOf("BED_BLOCK"));

        for (Material material : Material.values()) {
            String name = material.name();
            if (name.startsWith("LEGACY_")) continue;
            if (name.contains("SHULKER_BOX")) shulkers.add(material);
            if (atLeast113() && name.endsWith("_BED")) beds.add(material);
            if (name.contains("SIGN")) signs.add(material);
            if (name.endsWith("STAIRS") || name.endsWith("SLAB") || name.endsWith("SKULL")) support.add(material);

            if (before113()) {
                if (name.endsWith("FENCE") || name.toLowerCase().contains("fence")) fences.add(material);
                if (name.endsWith("_WALL")) fences.add(material);
                if (name.startsWith("PISTON")) pistons.add(material);
                if (name.endsWith("STEP")) support.add(material);
                support.add(Material.valueOf("BED_BLOCK"));
            } else {
                if (name.endsWith("FENCE") || name.toLowerCase().contains("fence")) fences.add(material);
                if (name.endsWith("_WALL")) fences.add(material);
                if (name.endsWith("PISTON")) pistons.add(material);
                if (name.endsWith("BED")) support.add(material);
            }
        }

        supportMaterials=immutableEnumSet(support);
        fenceAndWallMaterials=immutableEnumSet(fences);
        pistonMaterials=immutableEnumSet(pistons);
        signMaterials=immutableEnumSet(signs);
        bedMaterials=immutableEnumSet(beds);
        shulkerBoxMaterials=immutableEnumSet(shulkers);

        spawner=materialByVersionNames("MOB_SPAWNER", "SPAWNER");
    }
}
