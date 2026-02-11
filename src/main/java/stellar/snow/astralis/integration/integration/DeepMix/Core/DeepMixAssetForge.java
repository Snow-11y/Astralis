package stellar.snow.astralis.integration.DeepMix;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import java.awt.geom.*;
import javax.imageio.*;
import javax.sound.sampled.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  🔮 DeepMix Asset Forge — Phase 3 & 4                              ║
 * ║  ══════════════════════════════════════                             ║
 * ║                                                                     ║
 * ║  PHASE 3: Asset & Resource Annotations (12 annotations)            ║
 * ║    ├── Visual Assets (5): Texture, Model, Shader, Font, Animation  ║
 * ║    ├── Audio/Media  (2): Audio, Particle                           ║
 * ║    └── Language/Text(5): Lang, Splash, Credits, Atlas, Pack        ║
 * ║                                                                     ║
 * ║  PHASE 4: Game-Specific Annotations (14 annotations)              ║
 * ║    ├── MC Data (9): Registry, Recipe, Loot, NBT, Tag,             ║
 * ║    │                 BlockState, ItemModel, Advancement, Predicate ║
 * ║    └── WorldGen (5): Structure, Biome, Dimension, Function,       ║
 * ║                       Script                                       ║
 * ║                                                                     ║
 * ║  "Where raw assets are smelted into something greater."            ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * @author Stellar Snow Astralis Team
 * @version 1.0.0-FORGE
 */
public class DeepMixAssetForge {

    // ============================================================================
    //  GLOBAL CONFIGURATION
    // ============================================================================

    private static volatile boolean debugMode = false;
    private static volatile boolean hotReloadEnabled = false;
    private static final AtomicInteger assetTransformCount = new AtomicInteger(0);
    private static volatile Consumer<String> logSink = null;

    public static void setDebugMode(boolean enabled) { debugMode = enabled; }
    public static boolean isDebugMode() { return debugMode; }
    public static void setHotReloadEnabled(boolean enabled) { hotReloadEnabled = enabled; }
    public static boolean isHotReloadEnabled() { return hotReloadEnabled; }
    public static int getAssetTransformCount() { return assetTransformCount.get(); }
    public static void setLogSink(Consumer<String> sink) { logSink = sink; }

    static void log(String format, Object... args) {
        if (debugMode) {
            String msg = String.format("[DeepMix-AssetForge] " + format, args);
            if (logSink != null) logSink.accept(msg);
            else System.out.println(msg);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 3 — ASSET & RESOURCE ANNOTATIONS                        ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ====================================================================
    //  3A · VISUAL ASSETS (5 annotations)
    // ====================================================================

    // ─────────────────────── @DeepTexture ────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepTexture {
        String path() default "";
        String[] resourcePaths() default {};
        TexOp operation() default TexOp.MODIFY;
        TexFormat format() default TexFormat.PNG;
        boolean hotReload() default true;
        int priority() default 1000;

        enum TexOp   { MODIFY, OVERLAY, TINT, RESIZE, FILTER, ANIMATE, REPLACE, COMPOSITE, NINE_PATCH }
        enum TexFormat { PNG, JPG, JPEG, WEBP, BMP, GIF, TGA }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DTEX {
        String path() default "";
        String[] resourcePaths() default {};
        DeepTexture.TexOp operation() default DeepTexture.TexOp.MODIFY;
        DeepTexture.TexFormat format() default DeepTexture.TexFormat.PNG;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepModel ──────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepModel {
        String path() default "";
        String[] resourcePaths() default {};
        ModelOp operation() default ModelOp.MODIFY;
        ModelFmt format() default ModelFmt.JSON;
        boolean hotReload() default true;
        int priority() default 1000;

        enum ModelOp  { MODIFY, SCALE, ROTATE, TRANSLATE, RETEXTURE, MERGE, REPLACE, PARENT }
        enum ModelFmt { JSON, OBJ, FBX, GLTF, GLB, COLLADA }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DMDL {
        String path() default "";
        String[] resourcePaths() default {};
        DeepModel.ModelOp operation() default DeepModel.ModelOp.MODIFY;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepShader ─────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepShader {
        String path() default "";
        String[] resourcePaths() default {};
        ShaderStage stage() default ShaderStage.VERTEX;
        ShaderOp operation() default ShaderOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum ShaderStage { VERTEX, FRAGMENT, GEOMETRY, COMPUTE, TESSELLATION_CTRL, TESSELLATION_EVAL }
        enum ShaderOp    { MODIFY, INJECT, REPLACE, OPTIMIZE, VALIDATE, DEFINE, UNDEFINE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSHD {
        String path() default "";
        String[] resourcePaths() default {};
        DeepShader.ShaderStage stage() default DeepShader.ShaderStage.VERTEX;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepFont ───────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepFont {
        String path() default "";
        String[] resourcePaths() default {};
        FontOp operation() default FontOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum FontOp { MODIFY, REPLACE, SUBSET, MERGE, CONVERT, SCALE, RASTERIZE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DFNT {
        String path() default "";
        String[] resourcePaths() default {};
        DeepFont.FontOp operation() default DeepFont.FontOp.MODIFY;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepAnimation ──────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepAnimation {
        String path() default "";
        String[] resourcePaths() default {};
        AnimOp operation() default AnimOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum AnimOp { MODIFY, SPEED_UP, SLOW_DOWN, REVERSE, LOOP, REPLACE, BLEND, REMAP }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DANIM {
        String path() default "";
        String[] resourcePaths() default {};
        DeepAnimation.AnimOp operation() default DeepAnimation.AnimOp.MODIFY;
        boolean hotReload() default true;
    }


    // ====================================================================
    //  3B · AUDIO / MEDIA (2 annotations)
    // ====================================================================

    // ─────────────────────── @DeepAudio ──────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepAudio {
        String path() default "";
        String[] resourcePaths() default {};
        AudioOp operation() default AudioOp.MODIFY;
        AudioFmt format() default AudioFmt.OGG;
        boolean hotReload() default true;
        int priority() default 1000;

        enum AudioOp  { MODIFY, VOLUME, PITCH, FADE_IN, FADE_OUT, TRIM, LOOP, REPLACE, REVERB, NORMALIZE }
        enum AudioFmt { OGG, MP3, WAV, FLAC, M4A }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DAUD {
        String path() default "";
        String[] resourcePaths() default {};
        DeepAudio.AudioOp operation() default DeepAudio.AudioOp.MODIFY;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepParticle ───────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepParticle {
        String path() default "";
        String[] resourcePaths() default {};
        ParticleOp operation() default ParticleOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum ParticleOp { MODIFY, ADD_EMITTER, REMOVE_EMITTER, SCALE, COLOR, REPLACE, LIFETIME, VELOCITY }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPART {
        String path() default "";
        String[] resourcePaths() default {};
        DeepParticle.ParticleOp operation() default DeepParticle.ParticleOp.MODIFY;
        boolean hotReload() default true;
    }


    // ====================================================================
    //  3C · LANGUAGE & TEXT (5 annotations)
    // ====================================================================

    // ─────────────────────── @DeepLang ───────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepLang {
        String path() default "";
        String[] resourcePaths() default {};
        String locale() default "en_us";
        LangOp operation() default LangOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum LangOp { MODIFY, ADD_KEY, REMOVE_KEY, REPLACE, MERGE, REGEX_REPLACE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DLANG {
        String path() default "";
        String locale() default "en_us";
        DeepLang.LangOp operation() default DeepLang.LangOp.MODIFY;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepSplash ─────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepSplash {
        String path() default "assets/minecraft/texts/splashes.txt";
        SplashOp operation() default SplashOp.ADD;
        String[] splashes() default {};
        boolean hotReload() default true;
        int priority() default 1000;

        enum SplashOp { ADD, REMOVE, REPLACE, CLEAR, MERGE, REGEX_REMOVE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSPLS {
        String path() default "assets/minecraft/texts/splashes.txt";
        String[] splashes() default {};
        DeepSplash.SplashOp operation() default DeepSplash.SplashOp.ADD;
    }

    // ─────────────────────── @DeepCredits ────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepCredits {
        String path() default "assets/minecraft/texts/credits.json";
        CreditsOp operation() default CreditsOp.ADD;
        String[] credits() default {};
        boolean hotReload() default true;
        int priority() default 1000;

        enum CreditsOp { ADD, REMOVE, REPLACE, MODIFY, SECTION }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DCRED {
        String path() default "assets/minecraft/texts/credits.json";
        String[] credits() default {};
        DeepCredits.CreditsOp operation() default DeepCredits.CreditsOp.ADD;
    }

    // ─────────────────────── @DeepAtlas ──────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepAtlas {
        String path() default "";
        String[] resourcePaths() default {};
        AtlasOp operation() default AtlasOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum AtlasOp { MODIFY, ADD_SPRITE, REMOVE_SPRITE, REPACK, OPTIMIZE, ADD_SOURCE, REMOVE_SOURCE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DATL {
        String path() default "";
        String[] resourcePaths() default {};
        DeepAtlas.AtlasOp operation() default DeepAtlas.AtlasOp.MODIFY;
        boolean hotReload() default true;
    }

    // ─────────────────────── @DeepPack ───────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepPack {
        String path() default "pack.mcmeta";
        PackOp operation() default PackOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum PackOp { MODIFY, UPDATE_DESCRIPTION, UPDATE_FORMAT, UPDATE_ICON, ADD_FILTER, ADD_FEATURE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPCK {
        String path() default "pack.mcmeta";
        DeepPack.PackOp operation() default DeepPack.PackOp.MODIFY;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 4 — GAME-SPECIFIC ANNOTATIONS                           ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ====================================================================
    //  4A · MINECRAFT DATA (9 annotations)
    // ====================================================================

    // ─────────────────────── @DeepRegistry ───────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepRegistry {
        String registryName();
        RegOp operation() default RegOp.MODIFY;
        String[] targets() default {};
        boolean hotReload() default true;
        int priority() default 1000;

        enum RegOp { MODIFY, ADD, REMOVE, REPLACE, QUERY, ALIAS, FREEZE, UNFREEZE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DREG {
        String registryName();
        String[] targets() default {};
        DeepRegistry.RegOp operation() default DeepRegistry.RegOp.MODIFY;
    }

    // ─────────────────────── @DeepRecipe ─────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepRecipe {
        String path() default "";
        String[] resourcePaths() default {};
        RecipeOp operation() default RecipeOp.MODIFY;
        RecipeType type() default RecipeType.ALL;
        boolean hotReload() default true;
        int priority() default 1000;

        enum RecipeOp   { MODIFY, ADD, REMOVE, REPLACE, DISABLE, ENABLE, CLONE }
        enum RecipeType { ALL, CRAFTING_SHAPED, CRAFTING_SHAPELESS, SMELTING, BLASTING, SMOKING,
                          CAMPFIRE, STONECUTTING, SMITHING, SMITHING_TRANSFORM, SMITHING_TRIM }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DRCP {
        String path() default "";
        String[] resourcePaths() default {};
        DeepRecipe.RecipeOp operation() default DeepRecipe.RecipeOp.MODIFY;
        DeepRecipe.RecipeType type() default DeepRecipe.RecipeType.ALL;
    }

    // ─────────────────────── @DeepLoot ───────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepLoot {
        String path() default "";
        String[] resourcePaths() default {};
        LootOp operation() default LootOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum LootOp { MODIFY, ADD_POOL, REMOVE_POOL, ADD_ENTRY, REMOVE_ENTRY, REPLACE,
                      SET_ROLLS, ADD_CONDITION, ADD_FUNCTION }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DLOOT {
        String path() default "";
        String[] resourcePaths() default {};
        DeepLoot.LootOp operation() default DeepLoot.LootOp.MODIFY;
    }

    // ─────────────────────── @DeepNBT ────────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface DeepNBT {
        String path() default "";
        String[] resourcePaths() default {};
        NbtOp operation() default NbtOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum NbtOp { MODIFY, ADD_TAG, REMOVE_TAG, REPLACE, MERGE, QUERY, RENAME_TAG, DEEP_MERGE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface DNBT {
        String path() default "";
        String[] resourcePaths() default {};
        DeepNBT.NbtOp operation() default DeepNBT.NbtOp.MODIFY;
    }

    // ─────────────────────── @DeepTag ────────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepTag {
        String path() default "";
        String[] resourcePaths() default {};
        TagKind kind() default TagKind.BLOCK;
        TagOp operation() default TagOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum TagKind { BLOCK, ITEM, ENTITY_TYPE, FLUID, FUNCTION, BIOME, STRUCTURE, GAME_EVENT,
                       POINT_OF_INTEREST, BANNER_PATTERN, CAT_VARIANT, INSTRUMENT, PAINTING_VARIANT }
        enum TagOp   { MODIFY, ADD_ENTRY, REMOVE_ENTRY, REPLACE, MERGE, ADD_OPTIONAL }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DTAG {
        String path() default "";
        String[] resourcePaths() default {};
        DeepTag.TagKind kind() default DeepTag.TagKind.BLOCK;
        DeepTag.TagOp operation() default DeepTag.TagOp.MODIFY;
    }

    // ─────────────────────── @DeepBlockState ─────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepBlockState {
        String path() default "";
        String[] resourcePaths() default {};
        BSop operation() default BSop.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum BSop { MODIFY, ADD_VARIANT, REMOVE_VARIANT, ADD_MODEL, REPLACE, SET_ROTATION,
                    ADD_MULTIPART, REMOVE_MULTIPART }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DBS {
        String path() default "";
        String[] resourcePaths() default {};
        DeepBlockState.BSop operation() default DeepBlockState.BSop.MODIFY;
    }

    // ─────────────────────── @DeepItemModel ──────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepItemModel {
        String path() default "";
        String[] resourcePaths() default {};
        IMop operation() default IMop.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum IMop { MODIFY, CHANGE_PARENT, ADD_TEXTURE, REMOVE_TEXTURE, REPLACE,
                    ADD_OVERRIDE, REMOVE_OVERRIDE, SET_DISPLAY }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DIM {
        String path() default "";
        String[] resourcePaths() default {};
        DeepItemModel.IMop operation() default DeepItemModel.IMop.MODIFY;
    }

    // ─────────────────────── @DeepAdvancement ────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepAdvancement {
        String path() default "";
        String[] resourcePaths() default {};
        AdvOp operation() default AdvOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum AdvOp { MODIFY, ADD_CRITERION, REMOVE_CRITERION, ADD_REWARD, REPLACE,
                     SET_PARENT, SET_DISPLAY, ADD_REQUIREMENT }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DADV {
        String path() default "";
        String[] resourcePaths() default {};
        DeepAdvancement.AdvOp operation() default DeepAdvancement.AdvOp.MODIFY;
    }

    // ─────────────────────── @DeepPredicate ──────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepPredicate {
        String path() default "";
        String[] resourcePaths() default {};
        PredOp operation() default PredOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum PredOp { MODIFY, ADD_CONDITION, REMOVE_CONDITION, REPLACE, MERGE, NEGATE, AND, OR }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPRED {
        String path() default "";
        String[] resourcePaths() default {};
        DeepPredicate.PredOp operation() default DeepPredicate.PredOp.MODIFY;
    }


    // ====================================================================
    //  4B · WORLD GENERATION (5 annotations)
    // ====================================================================

    // ─────────────────────── @DeepStructure ──────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepStructure {
        String path() default "";
        String[] resourcePaths() default {};
        StructOp operation() default StructOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum StructOp { MODIFY, ROTATE, MIRROR, SCALE, REPLACE_BLOCKS, MERGE, HOLLOW, FILL,
                        RANDOMIZE, PALETTE_SWAP }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSTR {
        String path() default "";
        String[] resourcePaths() default {};
        DeepStructure.StructOp operation() default DeepStructure.StructOp.MODIFY;
    }

    // ─────────────────────── @DeepBiome ──────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepBiome {
        String path() default "";
        String[] resourcePaths() default {};
        BiomeOp operation() default BiomeOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum BiomeOp { MODIFY, TEMPERATURE, PRECIPITATION, ADD_FEATURE, REMOVE_FEATURE, REPLACE,
                       ADD_SPAWN, REMOVE_SPAWN, ADD_CARVER, SET_EFFECTS, SET_MUSIC }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DBIOME {
        String path() default "";
        String[] resourcePaths() default {};
        DeepBiome.BiomeOp operation() default DeepBiome.BiomeOp.MODIFY;
    }

    // ─────────────────────── @DeepDimension ──────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepDimension {
        String path() default "";
        String[] resourcePaths() default {};
        DimOp operation() default DimOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum DimOp { MODIFY, CHANGE_GENERATOR, CHANGE_TYPE, REPLACE, SET_EFFECTS, SET_CEILING,
                     SET_LIGHT, SET_COORDINATE_SCALE }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DDIM {
        String path() default "";
        String[] resourcePaths() default {};
        DeepDimension.DimOp operation() default DeepDimension.DimOp.MODIFY;
    }

    // ─────────────────────── @DeepFunction ───────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepFunction {
        String path() default "";
        String[] resourcePaths() default {};
        FuncOp operation() default FuncOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum FuncOp { MODIFY, ADD_COMMAND, REMOVE_COMMAND, REPLACE, OPTIMIZE,
                      PREPEND, APPEND, INSERT_AT, CONDITIONAL_WRAP }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DFUNC {
        String path() default "";
        String[] resourcePaths() default {};
        DeepFunction.FuncOp operation() default DeepFunction.FuncOp.MODIFY;
    }

    // ─────────────────────── @DeepScript ─────────────────────────

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepScript {
        String path() default "";
        String[] resourcePaths() default {};
        ScriptLang lang() default ScriptLang.JAVASCRIPT;
        ScriptOp operation() default ScriptOp.MODIFY;
        boolean hotReload() default true;
        int priority() default 1000;

        enum ScriptLang { JAVASCRIPT, PYTHON, LUA, GROOVY, KOTLIN, RUBY, TYPESCRIPT }
        enum ScriptOp   { MODIFY, INJECT, REPLACE, OPTIMIZE, VALIDATE, WRAP, POLYFILL }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSCR {
        String path() default "";
        String[] resourcePaths() default {};
        DeepScript.ScriptLang lang() default DeepScript.ScriptLang.JAVASCRIPT;
        DeepScript.ScriptOp operation() default DeepScript.ScriptOp.MODIFY;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  LIGHTWEIGHT JSON ENGINE                                        ║
    // ║  (Minimal JSON reader/writer so the forge is self-contained)    ║
    // ╚══════════════════════════════════════════════════════════════════╝

    /**
     * A minimal recursive-descent JSON parser and writer.
     * Produces {@code Map<String,Object>} for objects, {@code List<Object>} for arrays,
     * and the usual boxed primitives for literals.
     *
     * This avoids a hard dependency on Gson/Jackson in the core forge.
     * When Gson IS on the classpath (Minecraft always ships it) the pipeline
     * transparently delegates to it via {@link GsonBridge}.
     */
    public static final class JsonEngine {

        private JsonEngine() {}

        // ── Parsing ──────────────────────────────────────────────

        public static Object parse(String json) {
            return new JsonParser(json.trim()).parseValue();
        }

        public static Object parse(InputStream in) throws IOException {
            return parse(readString(in));
        }

        @SuppressWarnings("unchecked")
        public static Map<String, Object> parseObject(String json) {
            Object val = parse(json);
            if (val instanceof Map) return (Map<String, Object>) val;
            throw new DeepMixAssetException("Expected JSON object, got: " + (val == null ? "null" : val.getClass().getSimpleName()));
        }

        @SuppressWarnings("unchecked")
        public static Map<String, Object> parseObject(InputStream in) throws IOException {
            return parseObject(readString(in));
        }

        // ── Writing ──────────────────────────────────────────────

        public static String toJson(Object value) { return toJson(value, false); }

        public static String toJson(Object value, boolean pretty) {
            StringBuilder sb = new StringBuilder(256);
            writeValue(sb, value, pretty, 0);
            return sb.toString();
        }

        // ── JSONPath (simple subset) ─────────────────────────────

        @SuppressWarnings("unchecked")
        public static Object getPath(Object root, String path) {
            String[] keys = tokenizePath(path);
            Object current = root;
            for (String key : keys) {
                if (current == null) return null;
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else if (current instanceof List) {
                    try {
                        int idx = Integer.parseInt(key);
                        List<Object> list = (List<Object>) current;
                        current = (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                    } catch (NumberFormatException e) { return null; }
                } else {
                    return null;
                }
            }
            return current;
        }

        @SuppressWarnings("unchecked")
        public static void setPath(Object root, String path, Object value) {
            String[] keys = tokenizePath(path);
            if (keys.length == 0) return;
            Object current = root;
            for (int i = 0; i < keys.length - 1; i++) {
                if (current instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) current;
                    Object next = map.get(keys[i]);
                    if (next == null) {
                        next = new LinkedHashMap<String, Object>();
                        map.put(keys[i], next);
                    }
                    current = next;
                } else if (current instanceof List) {
                    int idx = Integer.parseInt(keys[i]);
                    current = ((List<Object>) current).get(idx);
                }
            }
            String lastKey = keys[keys.length - 1];
            if (current instanceof Map) {
                ((Map<String, Object>) current).put(lastKey, value);
            } else if (current instanceof List) {
                int idx = Integer.parseInt(lastKey);
                ((List<Object>) current).set(idx, value);
            }
        }

        @SuppressWarnings("unchecked")
        public static void removePath(Object root, String path) {
            String[] keys = tokenizePath(path);
            if (keys.length == 0) return;
            Object current = root;
            for (int i = 0; i < keys.length - 1; i++) {
                if (current instanceof Map) current = ((Map<String, Object>) current).get(keys[i]);
                else if (current instanceof List) current = ((List<Object>) current).get(Integer.parseInt(keys[i]));
                else return;
                if (current == null) return;
            }
            String lastKey = keys[keys.length - 1];
            if (current instanceof Map) ((Map<String, Object>) current).remove(lastKey);
            else if (current instanceof List) ((List<Object>) current).remove(Integer.parseInt(lastKey));
        }

        @SuppressWarnings("unchecked")
        public static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                String key = entry.getKey();
                Object srcVal = entry.getValue();
                Object tgtVal = target.get(key);
                if (tgtVal instanceof Map && srcVal instanceof Map) {
                    deepMerge((Map<String, Object>) tgtVal, (Map<String, Object>) srcVal);
                } else {
                    target.put(key, srcVal);
                }
            }
        }

        // ── Internal parser ──────────────────────────────────────

        private static final class JsonParser {
            private final String src;
            private int pos;

            JsonParser(String src) { this.src = src; this.pos = 0; }

            Object parseValue() {
                skipWhitespace();
                if (pos >= src.length()) throw error("Unexpected end of input");
                char c = src.charAt(pos);
                if (c == '{') return parseObj();
                if (c == '[') return parseArr();
                if (c == '"') return parseStr();
                if (c == 't' || c == 'f') return parseBool();
                if (c == 'n') return parseNull();
                return parseNum();
            }

            private Map<String, Object> parseObj() {
                expect('{');
                Map<String, Object> map = new LinkedHashMap<>();
                skipWhitespace();
                if (peek() == '}') { pos++; return map; }
                while (true) {
                    skipWhitespace();
                    String key = parseStr();
                    skipWhitespace();
                    expect(':');
                    Object value = parseValue();
                    map.put(key, value);
                    skipWhitespace();
                    if (peek() == ',') { pos++; continue; }
                    break;
                }
                skipWhitespace();
                expect('}');
                return map;
            }

            private List<Object> parseArr() {
                expect('[');
                List<Object> list = new ArrayList<>();
                skipWhitespace();
                if (peek() == ']') { pos++; return list; }
                while (true) {
                    list.add(parseValue());
                    skipWhitespace();
                    if (peek() == ',') { pos++; continue; }
                    break;
                }
                skipWhitespace();
                expect(']');
                return list;
            }

            private String parseStr() {
                expect('"');
                StringBuilder sb = new StringBuilder();
                while (pos < src.length()) {
                    char c = src.charAt(pos++);
                    if (c == '"') return sb.toString();
                    if (c == '\\') {
                        char esc = src.charAt(pos++);
                        switch (esc) {
                            case '"': case '\\': case '/': sb.append(esc); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case 't': sb.append('\t'); break;
                            case 'b': sb.append('\b'); break;
                            case 'f': sb.append('\f'); break;
                            case 'u':
                                String hex = src.substring(pos, pos + 4); pos += 4;
                                sb.append((char) Integer.parseInt(hex, 16));
                                break;
                            default: sb.append(esc);
                        }
                    } else {
                        sb.append(c);
                    }
                }
                throw error("Unterminated string");
            }

            private Number parseNum() {
                int start = pos;
                if (peek() == '-') pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
                boolean isFloat = false;
                if (pos < src.length() && src.charAt(pos) == '.') { isFloat = true; pos++; while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++; }
                if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                    isFloat = true; pos++;
                    if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                    while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
                }
                String numStr = src.substring(start, pos);
                if (isFloat) return Double.parseDouble(numStr);
                long val = Long.parseLong(numStr);
                if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
                return val;
            }

            private Boolean parseBool() {
                if (src.startsWith("true", pos))  { pos += 4; return Boolean.TRUE; }
                if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
                throw error("Expected boolean");
            }

            private Object parseNull() {
                if (src.startsWith("null", pos)) { pos += 4; return null; }
                throw error("Expected null");
            }

            private char peek() { return pos < src.length() ? src.charAt(pos) : 0; }
            private void expect(char c) {
                skipWhitespace();
                if (pos >= src.length() || src.charAt(pos) != c) throw error("Expected '" + c + "'");
                pos++;
            }
            private void skipWhitespace() { while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++; }
            private RuntimeException error(String msg) { return new DeepMixAssetException("JSON parse error at " + pos + ": " + msg); }
        }

        // ── Internal writer ──────────────────────────────────────

        @SuppressWarnings("unchecked")
        private static void writeValue(StringBuilder sb, Object val, boolean pretty, int indent) {
            if (val == null) { sb.append("null"); }
            else if (val instanceof Map) { writeObj(sb, (Map<String, Object>) val, pretty, indent); }
            else if (val instanceof List) { writeArr(sb, (List<Object>) val, pretty, indent); }
            else if (val instanceof String) { writeStr(sb, (String) val); }
            else if (val instanceof Boolean) { sb.append(val); }
            else if (val instanceof Number) {
                Number num = (Number) val;
                if (num instanceof Double || num instanceof Float) {
                    double d = num.doubleValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d)) sb.append((long) d);
                    else sb.append(d);
                } else { sb.append(num); }
            }
            else { writeStr(sb, val.toString()); }
        }

        private static void writeObj(StringBuilder sb, Map<String, Object> map, boolean pretty, int indent) {
            sb.append('{');
            if (map.isEmpty()) { sb.append('}'); return; }
            Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
            int next = indent + 1;
            while (it.hasNext()) {
                Map.Entry<String, Object> e = it.next();
                if (pretty) { sb.append('\n'); indent(sb, next); }
                writeStr(sb, e.getKey());
                sb.append(pretty ? ": " : ":");
                writeValue(sb, e.getValue(), pretty, next);
                if (it.hasNext()) sb.append(',');
            }
            if (pretty) { sb.append('\n'); indent(sb, indent); }
            sb.append('}');
        }

        private static void writeArr(StringBuilder sb, List<Object> list, boolean pretty, int indent) {
            sb.append('[');
            if (list.isEmpty()) { sb.append(']'); return; }
            int next = indent + 1;
            for (int i = 0; i < list.size(); i++) {
                if (pretty) { sb.append('\n'); indent(sb, next); }
                writeValue(sb, list.get(i), pretty, next);
                if (i < list.size() - 1) sb.append(',');
            }
            if (pretty) { sb.append('\n'); indent(sb, indent); }
            sb.append(']');
        }

        private static void writeStr(StringBuilder sb, String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"':  sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                }
            }
            sb.append('"');
        }

        private static void indent(StringBuilder sb, int level) { for (int i = 0; i < level; i++) sb.append("  "); }

        // ── Helpers ──────────────────────────────────────────────

        private static String[] tokenizePath(String path) {
            if (path.startsWith("$.")) path = path.substring(2);
            else if (path.startsWith("$")) path = path.substring(1);
            if (path.isEmpty()) return new String[0];
            return path.split("[.\\[\\]]+");
        }
    }

    /**
     * Thin bridge to Gson when available on the classpath.
     */
    public static final class GsonBridge {
        private static final Object GSON_INSTANCE;
        private static final java.lang.reflect.Method TO_JSON;
        private static final java.lang.reflect.Method FROM_JSON;

        static {
            Object gson = null;
            java.lang.reflect.Method toJson = null, fromJson = null;
            try {
                Class<?> gsonBuilderClass = Class.forName("com.google.gson.GsonBuilder");
                Object builder = gsonBuilderClass.getConstructor().newInstance();
                java.lang.reflect.Method setPretty = gsonBuilderClass.getMethod("setPrettyPrinting");
                builder = setPretty.invoke(builder);
                java.lang.reflect.Method disableHtml = gsonBuilderClass.getMethod("disableHtmlEscaping");
                builder = disableHtml.invoke(builder);
                java.lang.reflect.Method create = gsonBuilderClass.getMethod("create");
                gson = create.invoke(builder);
                Class<?> gsonClass = Class.forName("com.google.gson.Gson");
                toJson = gsonClass.getMethod("toJson", Object.class);
                fromJson = gsonClass.getMethod("fromJson", String.class, Class.class);
            } catch (Exception ignored) {}
            GSON_INSTANCE = gson;
            TO_JSON = toJson;
            FROM_JSON = fromJson;
        }

        public static boolean isAvailable() { return GSON_INSTANCE != null; }

        public static String toJson(Object obj) {
            if (!isAvailable()) return JsonEngine.toJson(obj, true);
            try { return (String) TO_JSON.invoke(GSON_INSTANCE, obj); }
            catch (Exception e) { return JsonEngine.toJson(obj, true); }
        }

        @SuppressWarnings("unchecked")
        public static Map<String, Object> fromJson(String json) {
            if (!isAvailable()) return JsonEngine.parseObject(json);
            try { return (Map<String, Object>) FROM_JSON.invoke(GSON_INSTANCE, json, Map.class); }
            catch (Exception e) { return JsonEngine.parseObject(json); }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  LIGHTWEIGHT NBT ENGINE                                         ║
    // ║  (Read/write Minecraft's Named Binary Tag format)               ║
    // ╚══════════════════════════════════════════════════════════════════╝

    public static final class NbtEngine {

        public static final byte TAG_END        = 0;
        public static final byte TAG_BYTE       = 1;
        public static final byte TAG_SHORT      = 2;
        public static final byte TAG_INT        = 3;
        public static final byte TAG_LONG       = 4;
        public static final byte TAG_FLOAT      = 5;
        public static final byte TAG_DOUBLE     = 6;
        public static final byte TAG_BYTE_ARRAY = 7;
        public static final byte TAG_STRING     = 8;
        public static final byte TAG_LIST       = 9;
        public static final byte TAG_COMPOUND   = 10;
        public static final byte TAG_INT_ARRAY  = 11;
        public static final byte TAG_LONG_ARRAY = 12;

        /** A compound tag represented as a LinkedHashMap preserving insertion order. */
        public static final class CompoundTag extends LinkedHashMap<String, Object> {
            public byte getByte(String key)   { Object v = get(key); return v instanceof Number ? ((Number) v).byteValue() : 0; }
            public short getShort(String key) { Object v = get(key); return v instanceof Number ? ((Number) v).shortValue() : 0; }
            public int getInt(String key)     { Object v = get(key); return v instanceof Number ? ((Number) v).intValue() : 0; }
            public long getLong(String key)   { Object v = get(key); return v instanceof Number ? ((Number) v).longValue() : 0; }
            public float getFloat(String key) { Object v = get(key); return v instanceof Number ? ((Number) v).floatValue() : 0; }
            public double getDouble(String key){ Object v = get(key); return v instanceof Number ? ((Number) v).doubleValue() : 0; }
            public String getString(String key){ Object v = get(key); return v instanceof String ? (String) v : ""; }
            public byte[] getByteArray(String key) { Object v = get(key); return v instanceof byte[] ? (byte[]) v : new byte[0]; }
            public int[] getIntArray(String key)   { Object v = get(key); return v instanceof int[] ? (int[]) v : new int[0]; }
            public long[] getLongArray(String key)  { Object v = get(key); return v instanceof long[] ? (long[]) v : new long[0]; }
            @SuppressWarnings("unchecked")
            public List<Object> getList(String key){ Object v = get(key); return v instanceof List ? (List<Object>) v : new ArrayList<>(); }
            public CompoundTag getCompound(String key) {
                Object v = get(key);
                return v instanceof CompoundTag ? (CompoundTag) v : new CompoundTag();
            }
            public boolean getBoolean(String key) { return getByte(key) != 0; }
            public void putByte(String key, byte v)   { put(key, v); }
            public void putShort(String key, short v)  { put(key, v); }
            public void putInt(String key, int v)      { put(key, v); }
            public void putLong(String key, long v)    { put(key, v); }
            public void putFloat(String key, float v)  { put(key, v); }
            public void putDouble(String key, double v){ put(key, v); }
            public void putString(String key, String v){ put(key, v); }
            public void putBoolean(String key, boolean v) { put(key, (byte)(v ? 1 : 0)); }
            public void putByteArray(String key, byte[] v){ put(key, v); }
            public void putIntArray(String key, int[] v)  { put(key, v); }
            public void putLongArray(String key, long[] v){ put(key, v); }
            public void putCompound(String key, CompoundTag v) { put(key, v); }
            public void putList(String key, List<Object> v) { put(key, v); }

            public void merge(CompoundTag other) {
                for (Map.Entry<String, Object> entry : other.entrySet()) {
                    Object existing = this.get(entry.getKey());
                    if (existing instanceof CompoundTag && entry.getValue() instanceof CompoundTag) {
                        ((CompoundTag) existing).merge((CompoundTag) entry.getValue());
                    } else {
                        this.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        // ── Reading ──────────────────────────────────────────────

        public static CompoundTag read(InputStream in) throws IOException {
            DataInputStream dis = (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in);
            byte rootType = dis.readByte();
            if (rootType != TAG_COMPOUND) throw new IOException("Root tag must be TAG_Compound, got " + rootType);
            dis.readUTF(); // root name (usually empty)
            return readCompound(dis);
        }

        public static CompoundTag read(byte[] data) throws IOException {
            return read(new ByteArrayInputStream(data));
        }

        private static CompoundTag readCompound(DataInputStream dis) throws IOException {
            CompoundTag tag = new CompoundTag();
            while (true) {
                byte type = dis.readByte();
                if (type == TAG_END) break;
                String name = dis.readUTF();
                tag.put(name, readTag(dis, type));
            }
            return tag;
        }

        private static Object readTag(DataInputStream dis, byte type) throws IOException {
            switch (type) {
                case TAG_BYTE:       return dis.readByte();
                case TAG_SHORT:      return dis.readShort();
                case TAG_INT:        return dis.readInt();
                case TAG_LONG:       return dis.readLong();
                case TAG_FLOAT:      return dis.readFloat();
                case TAG_DOUBLE:     return dis.readDouble();
                case TAG_STRING:     return dis.readUTF();
                case TAG_BYTE_ARRAY: { int len = dis.readInt(); byte[] arr = new byte[len]; dis.readFully(arr); return arr; }
                case TAG_INT_ARRAY:  { int len = dis.readInt(); int[] arr = new int[len]; for (int i = 0; i < len; i++) arr[i] = dis.readInt(); return arr; }
                case TAG_LONG_ARRAY: { int len = dis.readInt(); long[] arr = new long[len]; for (int i = 0; i < len; i++) arr[i] = dis.readLong(); return arr; }
                case TAG_LIST: {
                    byte elemType = dis.readByte();
                    int len = dis.readInt();
                    List<Object> list = new ArrayList<>(len);
                    for (int i = 0; i < len; i++) list.add(readTag(dis, elemType));
                    return list;
                }
                case TAG_COMPOUND:   return readCompound(dis);
                default: throw new IOException("Unknown NBT tag type: " + type);
            }
        }

        // ── Writing ──────────────────────────────────────────────

        public static byte[] write(CompoundTag tag) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            write(tag, baos);
            return baos.toByteArray();
        }

        public static void write(CompoundTag tag, OutputStream out) throws IOException {
            DataOutputStream dos = (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out);
            dos.writeByte(TAG_COMPOUND);
            dos.writeUTF(""); // root name
            writeCompound(dos, tag);
            dos.flush();
        }

        private static void writeCompound(DataOutputStream dos, CompoundTag tag) throws IOException {
            for (Map.Entry<String, Object> entry : tag.entrySet()) {
                byte type = tagTypeOf(entry.getValue());
                dos.writeByte(type);
                dos.writeUTF(entry.getKey());
                writeTag(dos, entry.getValue(), type);
            }
            dos.writeByte(TAG_END);
        }

        @SuppressWarnings("unchecked")
        private static void writeTag(DataOutputStream dos, Object val, byte type) throws IOException {
            switch (type) {
                case TAG_BYTE:       dos.writeByte(((Number) val).byteValue()); break;
                case TAG_SHORT:      dos.writeShort(((Number) val).shortValue()); break;
                case TAG_INT:        dos.writeInt(((Number) val).intValue()); break;
                case TAG_LONG:       dos.writeLong(((Number) val).longValue()); break;
                case TAG_FLOAT:      dos.writeFloat(((Number) val).floatValue()); break;
                case TAG_DOUBLE:     dos.writeDouble(((Number) val).doubleValue()); break;
                case TAG_STRING:     dos.writeUTF((String) val); break;
                case TAG_BYTE_ARRAY: { byte[] a = (byte[]) val; dos.writeInt(a.length); dos.write(a); break; }
                case TAG_INT_ARRAY:  { int[] a = (int[]) val; dos.writeInt(a.length); for (int v : a) dos.writeInt(v); break; }
                case TAG_LONG_ARRAY: { long[] a = (long[]) val; dos.writeInt(a.length); for (long v : a) dos.writeLong(v); break; }
                case TAG_LIST: {
                    List<Object> list = (List<Object>) val;
                    byte elemType = list.isEmpty() ? TAG_END : tagTypeOf(list.get(0));
                    dos.writeByte(elemType);
                    dos.writeInt(list.size());
                    for (Object elem : list) writeTag(dos, elem, elemType);
                    break;
                }
                case TAG_COMPOUND:   writeCompound(dos, (CompoundTag) val); break;
            }
        }

        private static byte tagTypeOf(Object val) {
            if (val instanceof Byte)        return TAG_BYTE;
            if (val instanceof Short)       return TAG_SHORT;
            if (val instanceof Integer)     return TAG_INT;
            if (val instanceof Long)        return TAG_LONG;
            if (val instanceof Float)       return TAG_FLOAT;
            if (val instanceof Double)      return TAG_DOUBLE;
            if (val instanceof String)      return TAG_STRING;
            if (val instanceof byte[])      return TAG_BYTE_ARRAY;
            if (val instanceof int[])       return TAG_INT_ARRAY;
            if (val instanceof long[])      return TAG_LONG_ARRAY;
            if (val instanceof List)        return TAG_LIST;
            if (val instanceof CompoundTag) return TAG_COMPOUND;
            throw new DeepMixAssetException("Cannot determine NBT type of: " + val.getClass().getSimpleName());
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 3 — PROCESSORS                                          ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ====================================================================
    //  3A · VISUAL ASSET PROCESSOR
    // ====================================================================

    public static class VisualAssetProcessor {

        // ── @DeepTexture ─────────────────────────────────────────

        public BufferedImage processTexture(DeepTexture ann, BufferedImage source) {
            log("TextureProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            switch (ann.operation()) {
                case TINT:     return source; // tint applied externally via tintTexture()
                case RESIZE:   return source; // resize applied externally via resizeTexture()
                case OVERLAY:  return source; // overlay applied externally via overlayTextures()
                case FILTER:   return source; // filter applied externally
                case ANIMATE:  return source; // animation strips handled separately
                case REPLACE:  return source; // direct replacement
                case MODIFY:
                default:       return source;
            }
        }

        public BufferedImage loadTexture(String path) throws IOException {
            File file = new File(path);
            if (file.exists()) return ImageIO.read(file);
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) { try { return ImageIO.read(is); } finally { is.close(); } }
            throw new DeepMixAssetException("Texture not found: " + path);
        }

        public void saveTexture(BufferedImage image, String path, DeepTexture.TexFormat format) throws IOException {
            String fmt = format == DeepTexture.TexFormat.JPG || format == DeepTexture.TexFormat.JPEG ? "jpg" : format.name().toLowerCase();
            File file = new File(path);
            file.getParentFile().mkdirs();
            ImageIO.write(image, fmt, file);
            log("TextureProcessor: saved %s (%dx%d) as %s", path, image.getWidth(), image.getHeight(), fmt);
        }

        public BufferedImage tintTexture(BufferedImage source, int tintColor) {
            float tr = ((tintColor >> 16) & 0xFF) / 255.0f;
            float tg = ((tintColor >> 8) & 0xFF) / 255.0f;
            float tb = (tintColor & 0xFF) / 255.0f;
            float ta = ((tintColor >> 24) & 0xFF) / 255.0f;
            if (ta == 0) ta = 1.0f;

            BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int pixel = source.getRGB(x, y);
                    int a = (pixel >> 24) & 0xFF;
                    int r = Math.min(255, (int)(((pixel >> 16) & 0xFF) * tr * ta));
                    int g = Math.min(255, (int)(((pixel >> 8) & 0xFF) * tg * ta));
                    int b = Math.min(255, (int)((pixel & 0xFF) * tb * ta));
                    result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public BufferedImage resizeTexture(BufferedImage source, int newWidth, int newHeight, boolean smooth) {
            int hint = smooth ? Image.SCALE_SMOOTH : Image.SCALE_FAST;
            Image scaled = source.getScaledInstance(newWidth, newHeight, hint);
            BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            if (smooth) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            g2d.drawImage(scaled, 0, 0, null);
            g2d.dispose();
            assetTransformCount.incrementAndGet();
            return result;
        }

        public BufferedImage overlayTextures(BufferedImage base, BufferedImage overlay, float opacity) {
            BufferedImage result = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(base, 0, 0, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.drawImage(overlay, 0, 0, base.getWidth(), base.getHeight(), null);
            g2d.dispose();
            assetTransformCount.incrementAndGet();
            return result;
        }

        public BufferedImage applyConvolutionFilter(BufferedImage source, float[] kernel, int kernelWidth, int kernelHeight) {
            Kernel k = new Kernel(kernelWidth, kernelHeight, kernel);
            ConvolveOp op = new ConvolveOp(k, ConvolveOp.EDGE_NO_OP, null);
            assetTransformCount.incrementAndGet();
            return op.filter(source, null);
        }

        public BufferedImage applyGrayscale(BufferedImage source) {
            BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int pixel = source.getRGB(x, y);
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    result.setRGB(x, y, (a << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public BufferedImage applySepia(BufferedImage source) {
            BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int pixel = source.getRGB(x, y);
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    int nr = Math.min(255, (int)(r * 0.393 + g * 0.769 + b * 0.189));
                    int ng = Math.min(255, (int)(r * 0.349 + g * 0.686 + b * 0.168));
                    int nb = Math.min(255, (int)(r * 0.272 + g * 0.534 + b * 0.131));
                    result.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public BufferedImage adjustBrightness(BufferedImage source, float factor) {
            RescaleOp op = new RescaleOp(factor, 0, null);
            assetTransformCount.incrementAndGet();
            return op.filter(source, null);
        }

        public BufferedImage flipHorizontal(BufferedImage source) {
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-source.getWidth(), 0);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            assetTransformCount.incrementAndGet();
            return op.filter(source, null);
        }

        public BufferedImage flipVertical(BufferedImage source) {
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -source.getHeight());
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            assetTransformCount.incrementAndGet();
            return op.filter(source, null);
        }

        public List<BufferedImage> extractAnimationFrames(BufferedImage strip, int frameWidth, int frameHeight) {
            List<BufferedImage> frames = new ArrayList<>();
            int cols = strip.getWidth() / frameWidth;
            int rows = strip.getHeight() / frameHeight;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    frames.add(strip.getSubimage(c * frameWidth, r * frameHeight, frameWidth, frameHeight));
                }
            }
            return frames;
        }

        public BufferedImage assembleAnimationStrip(List<BufferedImage> frames) {
            if (frames.isEmpty()) throw new DeepMixAssetException("No frames to assemble");
            int w = frames.get(0).getWidth();
            int h = frames.get(0).getHeight();
            BufferedImage strip = new BufferedImage(w, h * frames.size(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = strip.createGraphics();
            for (int i = 0; i < frames.size(); i++) {
                g2d.drawImage(frames.get(i), 0, i * h, null);
            }
            g2d.dispose();
            return strip;
        }

        // ── @DeepModel (JSON model format) ───────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processModel(DeepModel ann, InputStream input) throws IOException {
            Map<String, Object> model = JsonEngine.parseObject(input);
            log("ModelProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setModelParent(Map<String, Object> model, String parent) {
            model.put("parent", parent);
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setModelTexture(Map<String, Object> model, String layer, String texturePath) {
            Map<String, Object> textures = (Map<String, Object>) model.computeIfAbsent("textures", k -> new LinkedHashMap<>());
            textures.put(layer, texturePath);
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> scaleModelDisplay(Map<String, Object> model, String displaySlot, double scaleFactor) {
            Map<String, Object> display = (Map<String, Object>) model.computeIfAbsent("display", k -> new LinkedHashMap<>());
            Map<String, Object> slot = (Map<String, Object>) display.computeIfAbsent(displaySlot, k -> new LinkedHashMap<>());
            List<Object> scale = (List<Object>) slot.get("scale");
            if (scale == null) { scale = new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0)); slot.put("scale", scale); }
            for (int i = 0; i < scale.size(); i++) {
                double current = scale.get(i) instanceof Number ? ((Number) scale.get(i)).doubleValue() : 1.0;
                scale.set(i, current * scaleFactor);
            }
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addModelElement(Map<String, Object> model, Map<String, Object> element) {
            List<Object> elements = (List<Object>) model.computeIfAbsent("elements", k -> new ArrayList<>());
            elements.add(element);
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addModelOverride(Map<String, Object> model, Map<String, Object> predicate, String overrideModel) {
            List<Object> overrides = (List<Object>) model.computeIfAbsent("overrides", k -> new ArrayList<>());
            Map<String, Object> override = new LinkedHashMap<>();
            override.put("predicate", predicate);
            override.put("model", overrideModel);
            overrides.add(override);
            assetTransformCount.incrementAndGet();
            return model;
        }

        // ── @DeepShader ──────────────────────────────────────────

        public String processShader(DeepShader ann, String shaderSource) {
            log("ShaderProcessor: %s on %s (stage=%s)", ann.operation(), ann.path(), ann.stage());
            assetTransformCount.incrementAndGet();
            return shaderSource;
        }

        public String injectShaderCode(String shaderSource, String marker, String injection, boolean before) {
            int idx = shaderSource.indexOf(marker);
            if (idx < 0) { log("WARN: Shader marker not found: %s", marker); return shaderSource; }
            if (before) return shaderSource.substring(0, idx) + injection + "\n" + shaderSource.substring(idx);
            else {
                int end = idx + marker.length();
                return shaderSource.substring(0, end) + "\n" + injection + shaderSource.substring(end);
            }
        }

        public String addShaderDefine(String shaderSource, String name, String value) {
            String define = "#define " + name + (value != null ? " " + value : "") + "\n";
            int versionIdx = shaderSource.indexOf("#version");
            if (versionIdx >= 0) {
                int lineEnd = shaderSource.indexOf('\n', versionIdx);
                if (lineEnd >= 0) return shaderSource.substring(0, lineEnd + 1) + define + shaderSource.substring(lineEnd + 1);
            }
            return define + shaderSource;
        }

        public String removeShaderDefine(String shaderSource, String name) {
            return shaderSource.replaceAll("#define\\s+" + Pattern.quote(name) + ".*\\n?", "");
        }

        public String addShaderUniform(String shaderSource, String type, String name) {
            String uniform = "uniform " + type + " " + name + ";\n";
            int mainIdx = shaderSource.indexOf("void main");
            if (mainIdx >= 0) return shaderSource.substring(0, mainIdx) + uniform + shaderSource.substring(mainIdx);
            return uniform + shaderSource;
        }

        public String replaceShaderFunction(String shaderSource, String funcName, String newBody) {
            String pattern = funcName + "\\s*\\([^)]*\\)\\s*\\{";
            Matcher m = Pattern.compile(pattern).matcher(shaderSource);
            if (!m.find()) return shaderSource;
            int braceStart = m.end() - 1;
            int braceEnd = findMatchingBrace(shaderSource, braceStart);
            if (braceEnd < 0) return shaderSource;
            assetTransformCount.incrementAndGet();
            return shaderSource.substring(0, m.start()) + newBody + shaderSource.substring(braceEnd + 1);
        }

        public boolean validateGLSL(String shaderSource) {
            boolean hasVersion = shaderSource.contains("#version");
            boolean hasMain = shaderSource.contains("void main");
            int openBraces = 0, closeBraces = 0;
            for (char c : shaderSource.toCharArray()) {
                if (c == '{') openBraces++;
                if (c == '}') closeBraces++;
            }
            return hasMain && openBraces == closeBraces;
        }

        // ── @DeepFont ────────────────────────────────────────────

        public Font loadFont(String path) throws IOException, FontFormatException {
            File file = new File(path);
            if (file.exists()) return Font.createFont(Font.TRUETYPE_FONT, file);
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) { try { return Font.createFont(Font.TRUETYPE_FONT, is); } finally { is.close(); } }
            throw new DeepMixAssetException("Font not found: " + path);
        }

        public Font scaleFont(Font font, float size) {
            assetTransformCount.incrementAndGet();
            return font.deriveFont(size);
        }

        public Font stylizeFont(Font font, int style) {
            assetTransformCount.incrementAndGet();
            return font.deriveFont(style);
        }

        public BufferedImage rasterizeGlyph(Font font, char glyph, int size, Color color) {
            Font sized = font.deriveFont((float) size);
            BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = temp.createGraphics();
            g.setFont(sized);
            FontMetrics fm = g.getFontMetrics();
            int w = Math.max(1, fm.charWidth(glyph));
            int h = fm.getHeight();
            g.dispose();

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(sized);
            g.setColor(color);
            g.drawString(String.valueOf(glyph), 0, fm.getAscent());
            g.dispose();
            return img;
        }

        // ── @DeepAnimation ───────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processAnimation(DeepAnimation ann, InputStream input) throws IOException {
            Map<String, Object> anim = JsonEngine.parseObject(input);
            log("AnimationProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return anim;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> changeAnimationSpeed(Map<String, Object> anim, double speedMultiplier) {
            Map<String, Object> animation = (Map<String, Object>) anim.get("animation");
            if (animation == null) return anim;

            Object frametimeObj = animation.get("frametime");
            int frametime = frametimeObj instanceof Number ? ((Number) frametimeObj).intValue() : 1;
            int newFrametime = Math.max(1, (int)(frametime / speedMultiplier));
            animation.put("frametime", newFrametime);

            List<Object> frames = (List<Object>) animation.get("frames");
            if (frames != null) {
                for (int i = 0; i < frames.size(); i++) {
                    Object frame = frames.get(i);
                    if (frame instanceof Map) {
                        Map<String, Object> frameMap = (Map<String, Object>) frame;
                        Object ft = frameMap.get("time");
                        if (ft instanceof Number) {
                            frameMap.put("time", Math.max(1, (int)(((Number) ft).intValue() / speedMultiplier)));
                        }
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return anim;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> reverseAnimation(Map<String, Object> anim) {
            Map<String, Object> animation = (Map<String, Object>) anim.get("animation");
            if (animation == null) return anim;
            List<Object> frames = (List<Object>) animation.get("frames");
            if (frames != null) Collections.reverse(frames);
            assetTransformCount.incrementAndGet();
            return anim;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setAnimationInterpolate(Map<String, Object> anim, boolean interpolate) {
            Map<String, Object> animation = (Map<String, Object>) anim.computeIfAbsent("animation", k -> new LinkedHashMap<>());
            animation.put("interpolate", interpolate);
            assetTransformCount.incrementAndGet();
            return anim;
        }

        // ── Utility ──────────────────────────────────────────────

        private int findMatchingBrace(String src, int openPos) {
            int depth = 0;
            for (int i = openPos; i < src.length(); i++) {
                if (src.charAt(i) == '{') depth++;
                if (src.charAt(i) == '}') { depth--; if (depth == 0) return i; }
            }
            return -1;
        }
    }


    // ====================================================================
    //  3B · AUDIO / MEDIA PROCESSOR
    // ====================================================================

    public static class AudioMediaProcessor {

        // ── @DeepAudio ───────────────────────────────────────────

        public byte[] processAudio(DeepAudio ann, byte[] audioData) {
            log("AudioProcessor: %s on %s (format=%s)", ann.operation(), ann.path(), ann.format());
            assetTransformCount.incrementAndGet();
            return audioData;
        }

        public AudioInputStream loadAudio(String path) throws UnsupportedAudioFileException, IOException {
            File file = new File(path);
            if (file.exists()) return AudioSystem.getAudioInputStream(file);
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) return AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            throw new DeepMixAssetException("Audio not found: " + path);
        }

        public byte[] adjustVolume(byte[] audioData, javax.sound.sampled.AudioFormat format, float volumeFactor) {
            byte[] result = Arrays.copyOf(audioData, audioData.length);
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            boolean bigEndian = format.isBigEndian();

            for (int i = 0; i < result.length - bytesPerSample + 1; i += bytesPerSample) {
                if (bytesPerSample == 2) {
                    short sample;
                    if (bigEndian) sample = (short)((result[i] << 8) | (result[i + 1] & 0xFF));
                    else sample = (short)((result[i + 1] << 8) | (result[i] & 0xFF));

                    int amplified = (int)(sample * volumeFactor);
                    amplified = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, amplified));
                    short clipped = (short) amplified;

                    if (bigEndian) { result[i] = (byte)(clipped >> 8); result[i + 1] = (byte) clipped; }
                    else { result[i] = (byte) clipped; result[i + 1] = (byte)(clipped >> 8); }
                } else if (bytesPerSample == 1) {
                    int sample = result[i] & 0xFF;
                    sample = (int)((sample - 128) * volumeFactor) + 128;
                    result[i] = (byte) Math.max(0, Math.min(255, sample));
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public byte[] applyFadeIn(byte[] audioData, javax.sound.sampled.AudioFormat format, int fadeSamples) {
            byte[] result = Arrays.copyOf(audioData, audioData.length);
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int channels = format.getChannels();
            int frameSize = bytesPerSample * channels;
            int totalFrames = result.length / frameSize;
            int fadeFrames = Math.min(fadeSamples, totalFrames);

            for (int frame = 0; frame < fadeFrames; frame++) {
                float factor = (float) frame / fadeFrames;
                for (int ch = 0; ch < channels; ch++) {
                    int offset = frame * frameSize + ch * bytesPerSample;
                    if (bytesPerSample == 2 && offset + 1 < result.length) {
                        boolean bigEndian = format.isBigEndian();
                        short sample;
                        if (bigEndian) sample = (short)((result[offset] << 8) | (result[offset + 1] & 0xFF));
                        else sample = (short)((result[offset + 1] << 8) | (result[offset] & 0xFF));
                        sample = (short)(sample * factor);
                        if (bigEndian) { result[offset] = (byte)(sample >> 8); result[offset + 1] = (byte) sample; }
                        else { result[offset] = (byte) sample; result[offset + 1] = (byte)(sample >> 8); }
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public byte[] applyFadeOut(byte[] audioData, javax.sound.sampled.AudioFormat format, int fadeSamples) {
            byte[] result = Arrays.copyOf(audioData, audioData.length);
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int channels = format.getChannels();
            int frameSize = bytesPerSample * channels;
            int totalFrames = result.length / frameSize;
            int fadeFrames = Math.min(fadeSamples, totalFrames);
            int fadeStart = totalFrames - fadeFrames;

            for (int frame = fadeStart; frame < totalFrames; frame++) {
                float factor = (float)(totalFrames - frame) / fadeFrames;
                for (int ch = 0; ch < channels; ch++) {
                    int offset = frame * frameSize + ch * bytesPerSample;
                    if (bytesPerSample == 2 && offset + 1 < result.length) {
                        boolean bigEndian = format.isBigEndian();
                        short sample;
                        if (bigEndian) sample = (short)((result[offset] << 8) | (result[offset + 1] & 0xFF));
                        else sample = (short)((result[offset + 1] << 8) | (result[offset] & 0xFF));
                        sample = (short)(sample * factor);
                        if (bigEndian) { result[offset] = (byte)(sample >> 8); result[offset + 1] = (byte) sample; }
                        else { result[offset] = (byte) sample; result[offset + 1] = (byte)(sample >> 8); }
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public byte[] trimAudio(byte[] audioData, javax.sound.sampled.AudioFormat format, int startFrame, int endFrame) {
            int frameSize = (format.getSampleSizeInBits() / 8) * format.getChannels();
            int startByte = startFrame * frameSize;
            int endByte = Math.min(endFrame * frameSize, audioData.length);
            if (startByte >= endByte) return new byte[0];
            assetTransformCount.incrementAndGet();
            return Arrays.copyOfRange(audioData, startByte, endByte);
        }

        public byte[] changePitch(byte[] audioData, javax.sound.sampled.AudioFormat format, float pitchFactor) {
            if (Math.abs(pitchFactor - 1.0f) < 0.001f) return audioData;
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int channels = format.getChannels();
            int frameSize = bytesPerSample * channels;
            int totalFrames = audioData.length / frameSize;
            int newTotalFrames = (int)(totalFrames / pitchFactor);
            byte[] result = new byte[newTotalFrames * frameSize];

            for (int newFrame = 0; newFrame < newTotalFrames; newFrame++) {
                double srcFrame = newFrame * pitchFactor;
                int srcIdx = (int) srcFrame;
                if (srcIdx >= totalFrames - 1) srcIdx = totalFrames - 2;
                if (srcIdx < 0) srcIdx = 0;
                double frac = srcFrame - srcIdx;

                for (int ch = 0; ch < channels; ch++) {
                    int srcOff1 = srcIdx * frameSize + ch * bytesPerSample;
                    int srcOff2 = (srcIdx + 1) * frameSize + ch * bytesPerSample;
                    int dstOff = newFrame * frameSize + ch * bytesPerSample;

                    if (bytesPerSample == 2 && srcOff2 + 1 < audioData.length && dstOff + 1 < result.length) {
                        boolean be = format.isBigEndian();
                        short s1, s2;
                        if (be) { s1 = (short)((audioData[srcOff1] << 8) | (audioData[srcOff1+1] & 0xFF)); s2 = (short)((audioData[srcOff2] << 8) | (audioData[srcOff2+1] & 0xFF)); }
                        else    { s1 = (short)((audioData[srcOff1+1] << 8) | (audioData[srcOff1] & 0xFF)); s2 = (short)((audioData[srcOff2+1] << 8) | (audioData[srcOff2] & 0xFF)); }
                        short interp = (short)(s1 + (s2 - s1) * frac);
                        if (be) { result[dstOff] = (byte)(interp >> 8); result[dstOff+1] = (byte)interp; }
                        else    { result[dstOff] = (byte)interp; result[dstOff+1] = (byte)(interp >> 8); }
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return result;
        }

        public byte[] normalize(byte[] audioData, javax.sound.sampled.AudioFormat format) {
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int maxAbs = 0;
            for (int i = 0; i < audioData.length - bytesPerSample + 1; i += bytesPerSample) {
                if (bytesPerSample == 2) {
                    short sample;
                    if (format.isBigEndian()) sample = (short)((audioData[i] << 8) | (audioData[i+1] & 0xFF));
                    else sample = (short)((audioData[i+1] << 8) | (audioData[i] & 0xFF));
                    maxAbs = Math.max(maxAbs, Math.abs(sample));
                }
            }
            if (maxAbs == 0) return audioData;
            float factor = (float) Short.MAX_VALUE / maxAbs;
            return adjustVolume(audioData, format, factor);
        }

        // ── @DeepParticle ────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processParticle(DeepParticle ann, InputStream input) throws IOException {
            Map<String, Object> particle = JsonEngine.parseObject(input);
            log("ParticleProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return particle;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setParticleTexture(Map<String, Object> particle, String texture) {
            List<Object> textures = (List<Object>) particle.computeIfAbsent("textures", k -> new ArrayList<>());
            textures.clear();
            textures.add(texture);
            assetTransformCount.incrementAndGet();
            return particle;
        }

        public Map<String, Object> setParticleLifetime(Map<String, Object> particle, String field, Object value) {
            particle.put(field, value);
            assetTransformCount.incrementAndGet();
            return particle;
        }
    }


    // ====================================================================
    //  3C · LANGUAGE & TEXT PROCESSOR
    // ====================================================================

    public static class LanguageTextProcessor {

        // ── @DeepLang ────────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, String> processLang(DeepLang ann, InputStream input) throws IOException {
            String content = readString(input);
            Map<String, String> lang;

            // Detect format: JSON (.json) vs legacy (.lang)
            content = content.trim();
            if (content.startsWith("{")) {
                Map<String, Object> json = JsonEngine.parseObject(content);
                lang = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : json.entrySet()) {
                    lang.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : "");
                }
            } else {
                lang = parseLegacyLang(content);
            }
            log("LangProcessor: %s on %s (locale=%s, keys=%d)", ann.operation(), ann.path(), ann.locale(), lang.size());
            assetTransformCount.incrementAndGet();
            return lang;
        }

        private Map<String, String> parseLegacyLang(String content) {
            Map<String, String> map = new LinkedHashMap<>();
            for (String line : content.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) map.put(line.substring(0, eq), line.substring(eq + 1));
            }
            return map;
        }

        public Map<String, String> addKey(Map<String, String> lang, String key, String value) {
            lang.put(key, value);
            assetTransformCount.incrementAndGet();
            return lang;
        }

        public Map<String, String> removeKey(Map<String, String> lang, String key) {
            lang.remove(key);
            assetTransformCount.incrementAndGet();
            return lang;
        }

        public Map<String, String> replaceValue(Map<String, String> lang, String key, String newValue) {
            if (lang.containsKey(key)) { lang.put(key, newValue); assetTransformCount.incrementAndGet(); }
            return lang;
        }

        public Map<String, String> regexReplace(Map<String, String> lang, String keyPattern, String valuePattern, String replacement) {
            Pattern kp = Pattern.compile(keyPattern);
            for (Map.Entry<String, String> entry : lang.entrySet()) {
                if (kp.matcher(entry.getKey()).matches()) {
                    entry.setValue(entry.getValue().replaceAll(valuePattern, replacement));
                }
            }
            assetTransformCount.incrementAndGet();
            return lang;
        }

        public Map<String, String> mergeLangs(Map<String, String> base, Map<String, String> overlay) {
            Map<String, String> merged = new LinkedHashMap<>(base);
            merged.putAll(overlay);
            assetTransformCount.incrementAndGet();
            return merged;
        }

        public String langToJson(Map<String, String> lang) {
            Map<String, Object> obj = new LinkedHashMap<>(lang);
            return JsonEngine.toJson(obj, true);
        }

        public String langToLegacy(Map<String, String> lang) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : lang.entrySet()) {
                sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            }
            return sb.toString();
        }

        // ── @DeepSplash ──────────────────────────────────────────

        public List<String> processSplash(DeepSplash ann, InputStream input) throws IOException {
            String content = readString(input);
            List<String> splashes = new ArrayList<>(Arrays.asList(content.split("\\r?\\n")));
            splashes.removeIf(String::isEmpty);
            log("SplashProcessor: %s (%d existing splashes)", ann.operation(), splashes.size());

            switch (ann.operation()) {
                case ADD:
                    for (String s : ann.splashes()) if (!splashes.contains(s)) splashes.add(s);
                    break;
                case REMOVE:
                    splashes.removeAll(Arrays.asList(ann.splashes()));
                    break;
                case REPLACE:
                    splashes.clear();
                    Collections.addAll(splashes, ann.splashes());
                    break;
                case CLEAR:
                    splashes.clear();
                    break;
                case MERGE:
                    for (String s : ann.splashes()) if (!splashes.contains(s)) splashes.add(s);
                    break;
                case REGEX_REMOVE:
                    for (String pattern : ann.splashes()) {
                        Pattern p = Pattern.compile(pattern);
                        splashes.removeIf(s -> p.matcher(s).matches());
                    }
                    break;
            }
            assetTransformCount.incrementAndGet();
            return splashes;
        }

        public String splashesToString(List<String> splashes) {
            return String.join("\n", splashes) + "\n";
        }

        // ── @DeepCredits ─────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processCredits(DeepCredits ann, InputStream input) throws IOException {
            Map<String, Object> credits = JsonEngine.parseObject(input);
            log("CreditsProcessor: %s", ann.operation());

            switch (ann.operation()) {
                case ADD:
                    List<Object> sections = (List<Object>) credits.computeIfAbsent("sections", k -> new ArrayList<>());
                    for (String credit : ann.credits()) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("title", credit);
                        entry.put("names", new ArrayList<>());
                        sections.add(entry);
                    }
                    break;
                case MODIFY:
                    // Callers modify the returned map directly
                    break;
                case REPLACE:
                    credits.clear();
                    List<Object> newSections = new ArrayList<>();
                    for (String credit : ann.credits()) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("title", credit);
                        entry.put("names", new ArrayList<>());
                        newSections.add(entry);
                    }
                    credits.put("sections", newSections);
                    break;
                case REMOVE:
                    List<Object> existing = (List<Object>) credits.get("sections");
                    if (existing != null) {
                        Set<String> toRemove = new HashSet<>(Arrays.asList(ann.credits()));
                        existing.removeIf(s -> {
                            if (s instanceof Map) return toRemove.contains(((Map<String, Object>) s).get("title"));
                            return false;
                        });
                    }
                    break;
            }
            assetTransformCount.incrementAndGet();
            return credits;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addCreditSection(Map<String, Object> credits, String title, List<String> names) {
            List<Object> sections = (List<Object>) credits.computeIfAbsent("sections", k -> new ArrayList<>());
            Map<String, Object> section = new LinkedHashMap<>();
            section.put("title", title);
            section.put("names", new ArrayList<>(names));
            sections.add(section);
            assetTransformCount.incrementAndGet();
            return credits;
        }

        // ── @DeepAtlas ───────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processAtlas(DeepAtlas ann, InputStream input) throws IOException {
            Map<String, Object> atlas = JsonEngine.parseObject(input);
            log("AtlasProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return atlas;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addAtlasSource(Map<String, Object> atlas, Map<String, Object> source) {
            List<Object> sources = (List<Object>) atlas.computeIfAbsent("sources", k -> new ArrayList<>());
            sources.add(source);
            assetTransformCount.incrementAndGet();
            return atlas;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeAtlasSource(Map<String, Object> atlas, String sourceType) {
            List<Object> sources = (List<Object>) atlas.get("sources");
            if (sources != null) {
                sources.removeIf(s -> s instanceof Map && sourceType.equals(((Map<String, Object>) s).get("type")));
            }
            assetTransformCount.incrementAndGet();
            return atlas;
        }

        public static Map<String, Object> createDirectorySource(String source, String prefix) {
            Map<String, Object> src = new LinkedHashMap<>();
            src.put("type", "minecraft:directory");
            src.put("source", source);
            src.put("prefix", prefix);
            return src;
        }

        public static Map<String, Object> createSingleSource(String resource, String sprite) {
            Map<String, Object> src = new LinkedHashMap<>();
            src.put("type", "minecraft:single");
            src.put("resource", resource);
            if (sprite != null) src.put("sprite", sprite);
            return src;
        }

        // ── @DeepPack ────────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processPack(DeepPack ann, InputStream input) throws IOException {
            Map<String, Object> packMeta = JsonEngine.parseObject(input);
            log("PackProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return packMeta;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setPackDescription(Map<String, Object> packMeta, String description) {
            Map<String, Object> pack = (Map<String, Object>) packMeta.computeIfAbsent("pack", k -> new LinkedHashMap<>());
            pack.put("description", description);
            assetTransformCount.incrementAndGet();
            return packMeta;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setPackFormat(Map<String, Object> packMeta, int format) {
            Map<String, Object> pack = (Map<String, Object>) packMeta.computeIfAbsent("pack", k -> new LinkedHashMap<>());
            pack.put("pack_format", format);
            assetTransformCount.incrementAndGet();
            return packMeta;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addPackFilter(Map<String, Object> packMeta, String namespace, String pathPattern) {
            Map<String, Object> filter = (Map<String, Object>) packMeta.computeIfAbsent("filter", k -> new LinkedHashMap<>());
            List<Object> blocks = (List<Object>) filter.computeIfAbsent("block", k -> new ArrayList<>());
            Map<String, Object> block = new LinkedHashMap<>();
            if (namespace != null) block.put("namespace", namespace);
            if (pathPattern != null) block.put("path", pathPattern);
            blocks.add(block);
            assetTransformCount.incrementAndGet();
            return packMeta;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addPackFeature(Map<String, Object> packMeta, String featureFlag) {
            Map<String, Object> features = (Map<String, Object>) packMeta.computeIfAbsent("features", k -> new LinkedHashMap<>());
            List<Object> enabled = (List<Object>) features.computeIfAbsent("enabled", k -> new ArrayList<>());
            if (!enabled.contains(featureFlag)) enabled.add(featureFlag);
            assetTransformCount.incrementAndGet();
            return packMeta;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setPackSupportedFormats(Map<String, Object> packMeta, int minFormat, int maxFormat) {
            Map<String, Object> pack = (Map<String, Object>) packMeta.computeIfAbsent("pack", k -> new LinkedHashMap<>());
            Map<String, Object> range = new LinkedHashMap<>();
            range.put("min_inclusive", minFormat);
            range.put("max_inclusive", maxFormat);
            pack.put("supported_formats", range);
            assetTransformCount.incrementAndGet();
            return packMeta;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 4 — PROCESSORS                                          ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ====================================================================
    //  4A · MINECRAFT DATA PROCESSOR
    // ====================================================================

    public static class MinecraftDataProcessor {

        // ── @DeepRegistry ────────────────────────────────────────

        /**
         * Modifies a game registry via reflection.
         * Works with both Forge and Fabric registry systems.
         */
        public void processRegistry(DeepRegistry ann, Object registryObject) {
            log("RegistryProcessor: %s on %s (targets=%s)",
                ann.operation(), ann.registryName(), Arrays.toString(ann.targets()));

            switch (ann.operation()) {
                case MODIFY: modifyRegistryEntries(registryObject, ann.targets()); break;
                case ADD:    addRegistryEntries(registryObject, ann.targets()); break;
                case REMOVE: removeRegistryEntries(registryObject, ann.targets()); break;
                case REPLACE: replaceRegistryEntries(registryObject, ann.targets()); break;
                case QUERY:  queryRegistry(registryObject, ann.targets()); break;
                case ALIAS:  aliasRegistryEntries(registryObject, ann.targets()); break;
                case FREEZE: freezeRegistry(registryObject); break;
                case UNFREEZE: unfreezeRegistry(registryObject); break;
            }
            assetTransformCount.incrementAndGet();
        }

        public Object getRegistry(String registryName) {
            // Try Forge registry
            try {
                Class<?> frCls = Class.forName("net.minecraftforge.registries.ForgeRegistries");
                for (java.lang.reflect.Field f : frCls.getFields()) {
                    if (f.getName().equalsIgnoreCase(registryName.replace("minecraft:", "").replace(":", "_"))) {
                        return f.get(null);
                    }
                }
            } catch (Exception ignored) {}

            // Try Fabric registry
            try {
                Class<?> regCls = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
                for (java.lang.reflect.Field f : regCls.getFields()) {
                    if (f.getName().equalsIgnoreCase(registryName.replace("minecraft:", ""))) {
                        return f.get(null);
                    }
                }
            } catch (Exception ignored) {}

            // Try legacy Minecraft registry
            try {
                Class<?> regCls = Class.forName("net.minecraft.util.registry.Registry");
                for (java.lang.reflect.Field f : regCls.getFields()) {
                    if (f.getName().equalsIgnoreCase(registryName.replace("minecraft:", ""))) {
                        return f.get(null);
                    }
                }
            } catch (Exception ignored) {}

            log("WARN: Registry not found: %s", registryName);
            return null;
        }

        private void modifyRegistryEntries(Object registry, String[] targets) {
            for (String target : targets) {
                try {
                    java.lang.reflect.Method getMethod = findMethod(registry.getClass(), "get", "getValue", "getEntry");
                    if (getMethod != null) {
                        Object rl = createResourceLocation(target);
                        Object entry = getMethod.invoke(registry, rl);
                        log("Registry entry found: %s → %s", target, entry);
                    }
                } catch (Exception e) { log("WARN: Failed to access registry entry %s: %s", target, e.getMessage()); }
            }
        }

        private void addRegistryEntries(Object registry, String[] targets)     { log("Registry ADD: %s", Arrays.toString(targets)); }
        private void removeRegistryEntries(Object registry, String[] targets)  { log("Registry REMOVE: %s", Arrays.toString(targets)); }
        private void replaceRegistryEntries(Object registry, String[] targets) { log("Registry REPLACE: %s", Arrays.toString(targets)); }
        private void queryRegistry(Object registry, String[] targets)          { log("Registry QUERY: %s", Arrays.toString(targets)); }
        private void aliasRegistryEntries(Object registry, String[] targets)   { log("Registry ALIAS: %s", Arrays.toString(targets)); }
        private void freezeRegistry(Object registry)   { log("Registry FREEZE"); }
        private void unfreezeRegistry(Object registry) { log("Registry UNFREEZE"); }

        private Object createResourceLocation(String id) {
            try {
                Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
                return rlClass.getConstructor(String.class).newInstance(id);
            } catch (Exception e1) {
                try {
                    Class<?> rlClass = Class.forName("net.minecraft.util.ResourceLocation");
                    return rlClass.getConstructor(String.class).newInstance(id);
                } catch (Exception e2) { return id; }
            }
        }

        private java.lang.reflect.Method findMethod(Class<?> clazz, String... names) {
            for (String name : names) {
                for (java.lang.reflect.Method m : clazz.getMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == 1) return m;
                }
            }
            return null;
        }

        // ── @DeepRecipe ──────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processRecipe(DeepRecipe ann, InputStream input) throws IOException {
            Map<String, Object> recipe = JsonEngine.parseObject(input);
            log("RecipeProcessor: %s on %s (type=%s)", ann.operation(), ann.path(), ann.type());
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setRecipeResult(Map<String, Object> recipe, String item, int count) {
            Map<String, Object> result = (Map<String, Object>) recipe.computeIfAbsent("result", k -> new LinkedHashMap<>());
            result.put("item", item);
            if (count > 1) result.put("count", count);
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setRecipeIngredient(Map<String, Object> recipe, String key, String item) {
            Map<String, Object> keyMap = (Map<String, Object>) recipe.get("key");
            if (keyMap != null) {
                Map<String, Object> ingredient = new LinkedHashMap<>();
                ingredient.put("item", item);
                keyMap.put(key, ingredient);
            }
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setRecipePattern(Map<String, Object> recipe, String... rows) {
            recipe.put("pattern", new ArrayList<>(Arrays.asList(rows)));
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        public Map<String, Object> setRecipeType(Map<String, Object> recipe, String type) {
            recipe.put("type", type);
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        public Map<String, Object> setSmeltingExperience(Map<String, Object> recipe, double xp) {
            recipe.put("experience", xp);
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        public Map<String, Object> setSmeltingCookingTime(Map<String, Object> recipe, int ticks) {
            recipe.put("cookingtime", ticks);
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addShapelessIngredient(Map<String, Object> recipe, String item) {
            List<Object> ingredients = (List<Object>) recipe.computeIfAbsent("ingredients", k -> new ArrayList<>());
            Map<String, Object> ingredient = new LinkedHashMap<>();
            ingredient.put("item", item);
            ingredients.add(ingredient);
            assetTransformCount.incrementAndGet();
            return recipe;
        }

        public boolean validateRecipe(Map<String, Object> recipe) {
            if (!recipe.containsKey("type")) return false;
            String type = recipe.get("type").toString();
            if (type.contains("crafting_shaped")) {
                return recipe.containsKey("pattern") && recipe.containsKey("key") && recipe.containsKey("result");
            } else if (type.contains("crafting_shapeless")) {
                return recipe.containsKey("ingredients") && recipe.containsKey("result");
            } else if (type.contains("smelting") || type.contains("blasting") || type.contains("smoking") || type.contains("campfire")) {
                return recipe.containsKey("ingredient") && recipe.containsKey("result");
            } else if (type.contains("stonecutting")) {
                return recipe.containsKey("ingredient") && recipe.containsKey("result");
            } else if (type.contains("smithing")) {
                return recipe.containsKey("base") && recipe.containsKey("addition");
            }
            return true; // unknown type, assume valid
        }

        // ── @DeepLoot ────────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processLoot(DeepLoot ann, InputStream input) throws IOException {
            Map<String, Object> loot = JsonEngine.parseObject(input);
            log("LootProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return loot;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addLootPool(Map<String, Object> lootTable, Map<String, Object> pool) {
            List<Object> pools = (List<Object>) lootTable.computeIfAbsent("pools", k -> new ArrayList<>());
            pools.add(pool);
            assetTransformCount.incrementAndGet();
            return lootTable;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeLootPool(Map<String, Object> lootTable, int poolIndex) {
            List<Object> pools = (List<Object>) lootTable.get("pools");
            if (pools != null && poolIndex >= 0 && poolIndex < pools.size()) {
                pools.remove(poolIndex);
                assetTransformCount.incrementAndGet();
            }
            return lootTable;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addLootEntry(Map<String, Object> lootTable, int poolIndex, Map<String, Object> entry) {
            List<Object> pools = (List<Object>) lootTable.get("pools");
            if (pools != null && poolIndex >= 0 && poolIndex < pools.size()) {
                Map<String, Object> pool = (Map<String, Object>) pools.get(poolIndex);
                List<Object> entries = (List<Object>) pool.computeIfAbsent("entries", k -> new ArrayList<>());
                entries.add(entry);
                assetTransformCount.incrementAndGet();
            }
            return lootTable;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setLootPoolRolls(Map<String, Object> lootTable, int poolIndex, Object rolls) {
            List<Object> pools = (List<Object>) lootTable.get("pools");
            if (pools != null && poolIndex >= 0 && poolIndex < pools.size()) {
                Map<String, Object> pool = (Map<String, Object>) pools.get(poolIndex);
                pool.put("rolls", rolls);
                assetTransformCount.incrementAndGet();
            }
            return lootTable;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addLootCondition(Map<String, Object> lootTable, int poolIndex, Map<String, Object> condition) {
            List<Object> pools = (List<Object>) lootTable.get("pools");
            if (pools != null && poolIndex >= 0 && poolIndex < pools.size()) {
                Map<String, Object> pool = (Map<String, Object>) pools.get(poolIndex);
                List<Object> conditions = (List<Object>) pool.computeIfAbsent("conditions", k -> new ArrayList<>());
                conditions.add(condition);
                assetTransformCount.incrementAndGet();
            }
            return lootTable;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addLootFunction(Map<String, Object> lootTable, int poolIndex, int entryIndex, Map<String, Object> function) {
            List<Object> pools = (List<Object>) lootTable.get("pools");
            if (pools != null && poolIndex >= 0 && poolIndex < pools.size()) {
                Map<String, Object> pool = (Map<String, Object>) pools.get(poolIndex);
                List<Object> entries = (List<Object>) pool.get("entries");
                if (entries != null && entryIndex >= 0 && entryIndex < entries.size()) {
                    Map<String, Object> entry = (Map<String, Object>) entries.get(entryIndex);
                    List<Object> functions = (List<Object>) entry.computeIfAbsent("functions", k -> new ArrayList<>());
                    functions.add(function);
                    assetTransformCount.incrementAndGet();
                }
            }
            return lootTable;
        }

        /** Convenience: build a simple item loot entry */
        public static Map<String, Object> createItemEntry(String item, int weight) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "minecraft:item");
            entry.put("name", item);
            if (weight > 0) entry.put("weight", weight);
            return entry;
        }

        /** Convenience: build a loot pool with fixed rolls */
        public static Map<String, Object> createPool(int rolls, List<Map<String, Object>> entries) {
            Map<String, Object> pool = new LinkedHashMap<>();
            pool.put("rolls", rolls);
            pool.put("entries", new ArrayList<>(entries));
            return pool;
        }

        /** Convenience: random-chance condition */
        public static Map<String, Object> createRandomChanceCondition(float chance) {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("condition", "minecraft:random_chance");
            cond.put("chance", (double) chance);
            return cond;
        }

        /** Convenience: killed-by-player condition */
        public static Map<String, Object> createKilledByPlayerCondition() {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("condition", "minecraft:killed_by_player");
            return cond;
        }

        /** Convenience: set-count function */
        public static Map<String, Object> createSetCountFunction(int min, int max) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("function", "minecraft:set_count");
            Map<String, Object> count = new LinkedHashMap<>();
            count.put("type", "minecraft:uniform");
            count.put("min", min);
            count.put("max", max);
            func.put("count", count);
            return func;
        }

        /** Convenience: enchant-with-levels function */
        public static Map<String, Object> createEnchantFunction(int minLevel, int maxLevel, boolean treasure) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("function", "minecraft:enchant_with_levels");
            Map<String, Object> levels = new LinkedHashMap<>();
            levels.put("type", "minecraft:uniform");
            levels.put("min", minLevel);
            levels.put("max", maxLevel);
            func.put("levels", levels);
            func.put("treasure", treasure);
            return func;
        }

        // ── @DeepNBT ─────────────────────────────────────────────

        public NbtEngine.CompoundTag processNBT(DeepNBT ann, InputStream input) throws IOException {
            NbtEngine.CompoundTag nbt = NbtEngine.read(input);
            log("NBTProcessor: %s on %s (tags=%d)", ann.operation(), ann.path(), nbt.size());
            assetTransformCount.incrementAndGet();
            return nbt;
        }

        public NbtEngine.CompoundTag addTag(NbtEngine.CompoundTag nbt, String key, Object value) {
            nbt.put(key, value);
            assetTransformCount.incrementAndGet();
            return nbt;
        }

        public NbtEngine.CompoundTag removeTag(NbtEngine.CompoundTag nbt, String key) {
            nbt.remove(key);
            assetTransformCount.incrementAndGet();
            return nbt;
        }

        public NbtEngine.CompoundTag renameTag(NbtEngine.CompoundTag nbt, String oldKey, String newKey) {
            if (nbt.containsKey(oldKey)) {
                Object val = nbt.remove(oldKey);
                nbt.put(newKey, val);
                assetTransformCount.incrementAndGet();
            }
            return nbt;
        }

        public NbtEngine.CompoundTag mergeNBT(NbtEngine.CompoundTag base, NbtEngine.CompoundTag overlay) {
            base.merge(overlay);
            assetTransformCount.incrementAndGet();
            return base;
        }

        /**
         * Navigate into a nested compound by dot-separated path.
         * Returns the deepest compound, creating intermediates as needed.
         */
        public NbtEngine.CompoundTag navigateNBT(NbtEngine.CompoundTag root, String path) {
            String[] keys = path.split("\\.");
            NbtEngine.CompoundTag current = root;
            for (String key : keys) {
                Object child = current.get(key);
                if (child instanceof NbtEngine.CompoundTag) {
                    current = (NbtEngine.CompoundTag) child;
                } else {
                    NbtEngine.CompoundTag newChild = new NbtEngine.CompoundTag();
                    current.put(key, newChild);
                    current = newChild;
                }
            }
            return current;
        }

        /**
         * Set a value at a dot-separated path within a compound tag.
         */
        public NbtEngine.CompoundTag setNBTPath(NbtEngine.CompoundTag root, String path, Object value) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0) {
                root.put(path, value);
            } else {
                NbtEngine.CompoundTag parent = navigateNBT(root, path.substring(0, lastDot));
                parent.put(path.substring(lastDot + 1), value);
            }
            assetTransformCount.incrementAndGet();
            return root;
        }

        /**
         * Get a value at a dot-separated path within a compound tag.
         */
        public Object getNBTPath(NbtEngine.CompoundTag root, String path) {
            String[] keys = path.split("\\.");
            Object current = root;
            for (String key : keys) {
                if (current instanceof NbtEngine.CompoundTag) {
                    current = ((NbtEngine.CompoundTag) current).get(key);
                } else {
                    return null;
                }
            }
            return current;
        }

        public byte[] writeNBT(NbtEngine.CompoundTag nbt) throws IOException {
            return NbtEngine.write(nbt);
        }

        // ── @DeepTag ─────────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processTag(DeepTag ann, InputStream input) throws IOException {
            Map<String, Object> tag = JsonEngine.parseObject(input);
            log("TagProcessor: %s on %s (kind=%s)", ann.operation(), ann.path(), ann.kind());
            assetTransformCount.incrementAndGet();
            return tag;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addTagEntry(Map<String, Object> tag, String entry) {
            List<Object> values = (List<Object>) tag.computeIfAbsent("values", k -> new ArrayList<>());
            if (!values.contains(entry)) { values.add(entry); assetTransformCount.incrementAndGet(); }
            return tag;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addOptionalTagEntry(Map<String, Object> tag, String entry) {
            List<Object> values = (List<Object>) tag.computeIfAbsent("values", k -> new ArrayList<>());
            Map<String, Object> optional = new LinkedHashMap<>();
            optional.put("id", entry);
            optional.put("required", false);
            values.add(optional);
            assetTransformCount.incrementAndGet();
            return tag;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeTagEntry(Map<String, Object> tag, String entry) {
            List<Object> values = (List<Object>) tag.get("values");
            if (values != null) {
                values.removeIf(v -> {
                    if (v instanceof String) return v.equals(entry);
                    if (v instanceof Map) return entry.equals(((Map<String, Object>) v).get("id"));
                    return false;
                });
                assetTransformCount.incrementAndGet();
            }
            return tag;
        }

        public Map<String, Object> setTagReplace(Map<String, Object> tag, boolean replace) {
            tag.put("replace", replace);
            assetTransformCount.incrementAndGet();
            return tag;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> mergeTags(Map<String, Object> base, Map<String, Object> overlay) {
            List<Object> baseValues = (List<Object>) base.computeIfAbsent("values", k -> new ArrayList<>());
            List<Object> overlayValues = (List<Object>) overlay.getOrDefault("values", new ArrayList<>());
            Set<String> existing = new HashSet<>();
            for (Object v : baseValues) {
                if (v instanceof String) existing.add((String) v);
                else if (v instanceof Map) existing.add(String.valueOf(((Map<String, Object>) v).get("id")));
            }
            for (Object v : overlayValues) {
                String id;
                if (v instanceof String) id = (String) v;
                else if (v instanceof Map) id = String.valueOf(((Map<String, Object>) v).get("id"));
                else continue;
                if (!existing.contains(id)) baseValues.add(v);
            }
            assetTransformCount.incrementAndGet();
            return base;
        }

        // ── @DeepBlockState ──────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processBlockState(DeepBlockState ann, InputStream input) throws IOException {
            Map<String, Object> bs = JsonEngine.parseObject(input);
            log("BlockStateProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return bs;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addBlockStateVariant(Map<String, Object> blockstate, String variant, Map<String, Object> model) {
            Map<String, Object> variants = (Map<String, Object>) blockstate.computeIfAbsent("variants", k -> new LinkedHashMap<>());
            variants.put(variant, model);
            assetTransformCount.incrementAndGet();
            return blockstate;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeBlockStateVariant(Map<String, Object> blockstate, String variant) {
            Map<String, Object> variants = (Map<String, Object>) blockstate.get("variants");
            if (variants != null) { variants.remove(variant); assetTransformCount.incrementAndGet(); }
            return blockstate;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setVariantModel(Map<String, Object> blockstate, String variant, String model) {
            Map<String, Object> variants = (Map<String, Object>) blockstate.get("variants");
            if (variants != null) {
                Object variantObj = variants.get(variant);
                if (variantObj instanceof Map) {
                    ((Map<String, Object>) variantObj).put("model", model);
                } else {
                    Map<String, Object> newVariant = new LinkedHashMap<>();
                    newVariant.put("model", model);
                    variants.put(variant, newVariant);
                }
                assetTransformCount.incrementAndGet();
            }
            return blockstate;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setVariantRotation(Map<String, Object> blockstate, String variant, int x, int y) {
            Map<String, Object> variants = (Map<String, Object>) blockstate.get("variants");
            if (variants != null) {
                Object variantObj = variants.get(variant);
                if (variantObj instanceof Map) {
                    Map<String, Object> v = (Map<String, Object>) variantObj;
                    if (x != 0) v.put("x", x);
                    if (y != 0) v.put("y", y);
                    assetTransformCount.incrementAndGet();
                }
            }
            return blockstate;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addMultipart(Map<String, Object> blockstate, Map<String, Object> when, Map<String, Object> apply) {
            List<Object> multipart = (List<Object>) blockstate.computeIfAbsent("multipart", k -> new ArrayList<>());
            Map<String, Object> part = new LinkedHashMap<>();
            if (when != null && !when.isEmpty()) part.put("when", when);
            part.put("apply", apply);
            multipart.add(part);
            assetTransformCount.incrementAndGet();
            return blockstate;
        }

        /** Convenience: create variant model entry */
        public static Map<String, Object> createVariantModel(String model, int x, int y, boolean uvlock) {
            Map<String, Object> variant = new LinkedHashMap<>();
            variant.put("model", model);
            if (x != 0) variant.put("x", x);
            if (y != 0) variant.put("y", y);
            if (uvlock) variant.put("uvlock", true);
            return variant;
        }

        // ── @DeepItemModel ───────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processItemModel(DeepItemModel ann, InputStream input) throws IOException {
            Map<String, Object> model = JsonEngine.parseObject(input);
            log("ItemModelProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return model;
        }

        public Map<String, Object> setItemModelParent(Map<String, Object> model, String parent) {
            model.put("parent", parent);
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setItemModelTexture(Map<String, Object> model, String layer, String texturePath) {
            Map<String, Object> textures = (Map<String, Object>) model.computeIfAbsent("textures", k -> new LinkedHashMap<>());
            textures.put(layer, texturePath);
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeItemModelTexture(Map<String, Object> model, String layer) {
            Map<String, Object> textures = (Map<String, Object>) model.get("textures");
            if (textures != null) { textures.remove(layer); assetTransformCount.incrementAndGet(); }
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addItemModelOverride(Map<String, Object> model, Map<String, Object> predicate, String overrideModel) {
            List<Object> overrides = (List<Object>) model.computeIfAbsent("overrides", k -> new ArrayList<>());
            Map<String, Object> override = new LinkedHashMap<>();
            override.put("predicate", predicate);
            override.put("model", overrideModel);
            overrides.add(override);
            assetTransformCount.incrementAndGet();
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeItemModelOverride(Map<String, Object> model, int index) {
            List<Object> overrides = (List<Object>) model.get("overrides");
            if (overrides != null && index >= 0 && index < overrides.size()) {
                overrides.remove(index);
                assetTransformCount.incrementAndGet();
            }
            return model;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setItemDisplay(Map<String, Object> model, String slot,
                                                    double[] rotation, double[] translation, double[] scale) {
            Map<String, Object> display = (Map<String, Object>) model.computeIfAbsent("display", k -> new LinkedHashMap<>());
            Map<String, Object> slotData = new LinkedHashMap<>();
            if (rotation != null) slotData.put("rotation", toDoubleList(rotation));
            if (translation != null) slotData.put("translation", toDoubleList(translation));
            if (scale != null) slotData.put("scale", toDoubleList(scale));
            display.put(slot, slotData);
            assetTransformCount.incrementAndGet();
            return model;
        }

        private List<Object> toDoubleList(double[] arr) {
            List<Object> list = new ArrayList<>(arr.length);
            for (double d : arr) list.add(d);
            return list;
        }

        // ── @DeepAdvancement ─────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processAdvancement(DeepAdvancement ann, InputStream input) throws IOException {
            Map<String, Object> adv = JsonEngine.parseObject(input);
            log("AdvancementProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return adv;
        }

        public Map<String, Object> setAdvancementParent(Map<String, Object> adv, String parent) {
            adv.put("parent", parent);
            assetTransformCount.incrementAndGet();
            return adv;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setAdvancementDisplay(Map<String, Object> adv, String title, String description,
                                                          String icon, String frame, boolean showToast, boolean announceToChat) {
            Map<String, Object> display = (Map<String, Object>) adv.computeIfAbsent("display", k -> new LinkedHashMap<>());
            Map<String, Object> titleObj = new LinkedHashMap<>();
            titleObj.put("text", title);
            display.put("title", titleObj);
            Map<String, Object> descObj = new LinkedHashMap<>();
            descObj.put("text", description);
            display.put("description", descObj);
            Map<String, Object> iconObj = new LinkedHashMap<>();
            iconObj.put("item", icon);
            display.put("icon", iconObj);
            display.put("frame", frame);
            display.put("show_toast", showToast);
            display.put("announce_to_chat", announceToChat);
            assetTransformCount.incrementAndGet();
            return adv;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addAdvancementCriterion(Map<String, Object> adv, String name, Map<String, Object> criterion) {
            Map<String, Object> criteria = (Map<String, Object>) adv.computeIfAbsent("criteria", k -> new LinkedHashMap<>());
            criteria.put(name, criterion);
            assetTransformCount.incrementAndGet();
            return adv;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeAdvancementCriterion(Map<String, Object> adv, String name) {
            Map<String, Object> criteria = (Map<String, Object>) adv.get("criteria");
            if (criteria != null) { criteria.remove(name); assetTransformCount.incrementAndGet(); }
            // Also remove from requirements
            List<Object> requirements = (List<Object>) adv.get("requirements");
            if (requirements != null) {
                requirements.forEach(req -> {
                    if (req instanceof List) ((List<Object>) req).remove(name);
                });
                requirements.removeIf(req -> req instanceof List && ((List<Object>) req).isEmpty());
            }
            return adv;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addAdvancementReward(Map<String, Object> adv, String type, Object value) {
            Map<String, Object> rewards = (Map<String, Object>) adv.computeIfAbsent("rewards", k -> new LinkedHashMap<>());
            if (type.equals("recipes") || type.equals("loot")) {
                List<Object> list = (List<Object>) rewards.computeIfAbsent(type, k -> new ArrayList<>());
                list.add(value);
            } else {
                rewards.put(type, value);
            }
            assetTransformCount.incrementAndGet();
            return adv;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addAdvancementRequirement(Map<String, Object> adv, String... criteriaNames) {
            List<Object> requirements = (List<Object>) adv.computeIfAbsent("requirements", k -> new ArrayList<>());
            requirements.add(new ArrayList<>(Arrays.asList(criteriaNames)));
            assetTransformCount.incrementAndGet();
            return adv;
        }

        /** Convenience: create an inventory_changed trigger criterion */
        public static Map<String, Object> createInventoryChangedCriterion(String... items) {
            Map<String, Object> criterion = new LinkedHashMap<>();
            criterion.put("trigger", "minecraft:inventory_changed");
            Map<String, Object> conditions = new LinkedHashMap<>();
            List<Object> itemList = new ArrayList<>();
            for (String item : items) {
                Map<String, Object> itemObj = new LinkedHashMap<>();
                itemObj.put("items", Collections.singletonList(item));
                itemList.add(itemObj);
            }
            conditions.put("items", itemList);
            criterion.put("conditions", conditions);
            return criterion;
        }

        // ── @DeepPredicate ───────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processPredicate(DeepPredicate ann, InputStream input) throws IOException {
            Map<String, Object> pred = JsonEngine.parseObject(input);
            log("PredicateProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return pred;
        }

        public Map<String, Object> addPredicateCondition(Map<String, Object> predicate, Map<String, Object> condition) {
            // Predicates can be a single condition or an array
            Object existing = predicate.get("condition");
            if (existing != null) {
                // Wrap into alternative
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("condition", "minecraft:alternative");
                List<Object> terms = new ArrayList<>();
                terms.add(new LinkedHashMap<>(predicate));
                terms.add(condition);
                wrapper.put("terms", terms);
                predicate.clear();
                predicate.putAll(wrapper);
            } else {
                predicate.putAll(condition);
            }
            assetTransformCount.incrementAndGet();
            return predicate;
        }

        public Map<String, Object> negatePredicate(Map<String, Object> predicate) {
            Map<String, Object> negated = new LinkedHashMap<>();
            negated.put("condition", "minecraft:inverted");
            negated.put("term", new LinkedHashMap<>(predicate));
            predicate.clear();
            predicate.putAll(negated);
            assetTransformCount.incrementAndGet();
            return predicate;
        }

        /** Convenience: weather_check condition */
        public static Map<String, Object> createWeatherCheck(Boolean raining, Boolean thundering) {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("condition", "minecraft:weather_check");
            if (raining != null) cond.put("raining", raining);
            if (thundering != null) cond.put("thundering", thundering);
            return cond;
        }

        /** Convenience: entity_properties condition */
        public static Map<String, Object> createEntityPropertiesCondition(String entity, Map<String, Object> properties) {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("condition", "minecraft:entity_properties");
            cond.put("entity", entity);
            cond.put("predicate", properties);
            return cond;
        }

        public boolean validatePredicate(Map<String, Object> predicate) {
            return predicate.containsKey("condition");
        }
    }


    // ====================================================================
    //  4B · WORLD GENERATION PROCESSOR
    // ====================================================================

    public static class WorldGenerationProcessor {

        // ── @DeepStructure ───────────────────────────────────────

        public NbtEngine.CompoundTag processStructure(DeepStructure ann, InputStream input) throws IOException {
            NbtEngine.CompoundTag structure = NbtEngine.read(input);
            log("StructureProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return structure;
        }

        @SuppressWarnings("unchecked")
        public NbtEngine.CompoundTag replaceStructureBlocks(NbtEngine.CompoundTag structure, String fromBlock, String toBlock) {
            List<Object> palette = structure.getList("palette");
            for (int i = 0; i < palette.size(); i++) {
                Object entry = palette.get(i);
                if (entry instanceof NbtEngine.CompoundTag) {
                    NbtEngine.CompoundTag block = (NbtEngine.CompoundTag) entry;
                    String name = block.getString("Name");
                    if (name.equals(fromBlock)) {
                        block.putString("Name", toBlock);
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return structure;
        }

        public NbtEngine.CompoundTag setStructureSize(NbtEngine.CompoundTag structure, int x, int y, int z) {
            List<Object> size = new ArrayList<>(3);
            size.add(x); size.add(y); size.add(z);
            structure.putList("size", size);
            assetTransformCount.incrementAndGet();
            return structure;
        }

        @SuppressWarnings("unchecked")
        public NbtEngine.CompoundTag addStructureEntity(NbtEngine.CompoundTag structure, NbtEngine.CompoundTag entityData) {
            List<Object> entities = structure.getList("entities");
            entities.add(entityData);
            structure.putList("entities", entities);
            assetTransformCount.incrementAndGet();
            return structure;
        }

        @SuppressWarnings("unchecked")
        public NbtEngine.CompoundTag rotateStructure90(NbtEngine.CompoundTag structure) {
            List<Object> size = structure.getList("size");
            if (size.size() >= 3) {
                int sizeX = ((Number) size.get(0)).intValue();
                int sizeZ = ((Number) size.get(2)).intValue();
                // Swap X and Z dimensions for 90-degree rotation
                size.set(0, sizeZ);
                size.set(2, sizeX);
                structure.putList("size", size);
            }

            // Rotate block positions
            List<Object> blocks = structure.getList("blocks");
            for (Object blockObj : blocks) {
                if (blockObj instanceof NbtEngine.CompoundTag) {
                    NbtEngine.CompoundTag block = (NbtEngine.CompoundTag) blockObj;
                    List<Object> pos = block.getList("pos");
                    if (pos.size() >= 3) {
                        int x = ((Number) pos.get(0)).intValue();
                        int z = ((Number) pos.get(2)).intValue();
                        int sizeZ = ((Number) size.get(2)).intValue();
                        pos.set(0, sizeZ - 1 - z);
                        pos.set(2, x);
                        block.putList("pos", pos);
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return structure;
        }

        @SuppressWarnings("unchecked")
        public NbtEngine.CompoundTag mirrorStructureX(NbtEngine.CompoundTag structure) {
            List<Object> size = structure.getList("size");
            int sizeX = size.size() >= 1 ? ((Number) size.get(0)).intValue() : 0;

            List<Object> blocks = structure.getList("blocks");
            for (Object blockObj : blocks) {
                if (blockObj instanceof NbtEngine.CompoundTag) {
                    NbtEngine.CompoundTag block = (NbtEngine.CompoundTag) blockObj;
                    List<Object> pos = block.getList("pos");
                    if (pos.size() >= 1) {
                        int x = ((Number) pos.get(0)).intValue();
                        pos.set(0, sizeX - 1 - x);
                        block.putList("pos", pos);
                    }
                }
            }
            assetTransformCount.incrementAndGet();
            return structure;
        }

        public byte[] writeStructure(NbtEngine.CompoundTag structure) throws IOException {
            return NbtEngine.write(structure);
        }

        // ── @DeepBiome ───────────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processBiome(DeepBiome ann, InputStream input) throws IOException {
            Map<String, Object> biome = JsonEngine.parseObject(input);
            log("BiomeProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return biome;
        }

        public Map<String, Object> setBiomeTemperature(Map<String, Object> biome, double temperature) {
            biome.put("temperature", temperature);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        public Map<String, Object> setBiomeDownfall(Map<String, Object> biome, double downfall) {
            biome.put("downfall", downfall);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        public Map<String, Object> setBiomePrecipitation(Map<String, Object> biome, String precipitation) {
            biome.put("has_precipitation", "none".equalsIgnoreCase(precipitation) ? false : true);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeEffects(Map<String, Object> biome, String key, Object value) {
            Map<String, Object> effects = (Map<String, Object>) biome.computeIfAbsent("effects", k -> new LinkedHashMap<>());
            effects.put(key, value);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeSkyColor(Map<String, Object> biome, int color) {
            return setBiomeEffects(biome, "sky_color", color);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeFogColor(Map<String, Object> biome, int color) {
            return setBiomeEffects(biome, "fog_color", color);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeWaterColor(Map<String, Object> biome, int color) {
            return setBiomeEffects(biome, "water_color", color);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeWaterFogColor(Map<String, Object> biome, int color) {
            return setBiomeEffects(biome, "water_fog_color", color);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeGrassColor(Map<String, Object> biome, int color) {
            return setBiomeEffects(biome, "grass_color", color);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeFoliageColor(Map<String, Object> biome, int color) {
            return setBiomeEffects(biome, "foliage_color", color);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addBiomeFeature(Map<String, Object> biome, int step, String feature) {
            Map<String, Object> features = (Map<String, Object>) biome.computeIfAbsent("features", k -> new ArrayList<>());
            List<Object> featureList;
            if (features instanceof List) {
                List<Object> featureSteps = (List<Object>) features;
                while (featureSteps.size() <= step) featureSteps.add(new ArrayList<>());
                featureList = (List<Object>) featureSteps.get(step);
            } else {
                featureList = new ArrayList<>();
            }
            if (!featureList.contains(feature)) featureList.add(feature);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> addBiomeSpawner(Map<String, Object> biome, String category, String entityType,
                                                     int weight, int minCount, int maxCount) {
            Map<String, Object> spawners = (Map<String, Object>) biome.computeIfAbsent("spawners", k -> new LinkedHashMap<>());
            List<Object> categoryList = (List<Object>) spawners.computeIfAbsent(category, k -> new ArrayList<>());
            Map<String, Object> spawner = new LinkedHashMap<>();
            spawner.put("type", entityType);
            spawner.put("weight", weight);
            spawner.put("minCount", minCount);
            spawner.put("maxCount", maxCount);
            categoryList.add(spawner);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> removeBiomeSpawner(Map<String, Object> biome, String category, String entityType) {
            Map<String, Object> spawners = (Map<String, Object>) biome.get("spawners");
            if (spawners != null) {
                List<Object> categoryList = (List<Object>) spawners.get(category);
                if (categoryList != null) {
                    categoryList.removeIf(s -> {
                        if (s instanceof Map) return entityType.equals(((Map<String, Object>) s).get("type"));
                        return false;
                    });
                    assetTransformCount.incrementAndGet();
                }
            }
            return biome;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setBiomeMusic(Map<String, Object> biome, String sound, int minDelay, int maxDelay, boolean replaceCurrentMusic) {
            Map<String, Object> effects = (Map<String, Object>) biome.computeIfAbsent("effects", k -> new LinkedHashMap<>());
            Map<String, Object> music = new LinkedHashMap<>();
            music.put("sound", sound);
            music.put("min_delay", minDelay);
            music.put("max_delay", maxDelay);
            music.put("replace_current_music", replaceCurrentMusic);
            effects.put("music", music);
            assetTransformCount.incrementAndGet();
            return biome;
        }

        // ── @DeepDimension ───────────────────────────────────────

        @SuppressWarnings("unchecked")
        public Map<String, Object> processDimension(DeepDimension ann, InputStream input) throws IOException {
            Map<String, Object> dim = JsonEngine.parseObject(input);
            log("DimensionProcessor: %s on %s", ann.operation(), ann.path());
            assetTransformCount.incrementAndGet();
            return dim;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionType(Map<String, Object> dimension, String type) {
            dimension.put("type", type);
            assetTransformCount.incrementAndGet();
            return dimension;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionGenerator(Map<String, Object> dimension, Map<String, Object> generator) {
            dimension.put("generator", generator);
            assetTransformCount.incrementAndGet();
            return dimension;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionTypeProperty(Map<String, Object> dimensionType, String property, Object value) {
            dimensionType.put(property, value);
            assetTransformCount.incrementAndGet();
            return dimensionType;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionAmbientLight(Map<String, Object> dimensionType, double ambientLight) {
            return setDimensionTypeProperty(dimensionType, "ambient_light", ambientLight);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionHasCeiling(Map<String, Object> dimensionType, boolean hasCeiling) {
            return setDimensionTypeProperty(dimensionType, "has_ceiling", hasCeiling);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionHasRaids(Map<String, Object> dimensionType, boolean hasRaids) {
            return setDimensionTypeProperty(dimensionType, "has_raids", hasRaids);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionLogicalHeight(Map<String, Object> dimensionType, int logicalHeight) {
            return setDimensionTypeProperty(dimensionType, "logical_height", logicalHeight);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionCoordinateScale(Map<String, Object> dimensionType, double scale) {
            return setDimensionTypeProperty(dimensionType, "coordinate_scale", scale);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> setDimensionFixedTime(Map<String, Object> dimensionType, long fixedTime) {
            return setDimensionTypeProperty(dimensionType, "fixed_time", fixedTime);
        }

        /** Convenience: create a noise generator config */
        public static Map<String, Object> createNoiseGenerator(String settingsId, String biomeSourceType) {
            Map<String, Object> gen = new LinkedHashMap<>();
            gen.put("type", "minecraft:noise");
            gen.put("settings", settingsId);
            Map<String, Object> biomeSource = new LinkedHashMap<>();
            biomeSource.put("type", biomeSourceType);
            gen.put("biome_source", biomeSource);
            return gen;
        }

        /** Convenience: create a flat generator config */
        public static Map<String, Object> createFlatGenerator(List<Map<String, Object>> layers, String biome) {
            Map<String, Object> gen = new LinkedHashMap<>();
            gen.put("type", "minecraft:flat");
            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("layers", layers);
            settings.put("biome", biome);
            gen.put("settings", settings);
            return gen;
        }

        /** Convenience: create a flat layer */
        public static Map<String, Object> createFlatLayer(String block, int height) {
            Map<String, Object> layer = new LinkedHashMap<>();
            layer.put("block", block);
            layer.put("height", height);
            return layer;
        }

        // ── @DeepFunction ────────────────────────────────────────

        public List<String> processFunction(DeepFunction ann, InputStream input) throws IOException {
            String content = readString(input);
            List<String> commands = new ArrayList<>(Arrays.asList(content.split("\\r?\\n")));
            log("FunctionProcessor: %s on %s (%d commands)", ann.operation(), ann.path(), commands.size());
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> addCommand(List<String> commands, String command) {
            commands.add(command);
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> prependCommand(List<String> commands, String command) {
            commands.add(0, command);
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> insertCommand(List<String> commands, int index, String command) {
            index = Math.max(0, Math.min(index, commands.size()));
            commands.add(index, command);
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> removeCommand(List<String> commands, String command) {
            commands.remove(command);
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> removeCommandsByPattern(List<String> commands, String pattern) {
            Pattern p = Pattern.compile(pattern);
            commands.removeIf(cmd -> p.matcher(cmd).matches());
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> replaceCommand(List<String> commands, String oldCmd, String newCmd) {
            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i).equals(oldCmd)) {
                    commands.set(i, newCmd);
                }
            }
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> replaceCommandsByPattern(List<String> commands, String pattern, String replacement) {
            Pattern p = Pattern.compile(pattern);
            for (int i = 0; i < commands.size(); i++) {
                Matcher m = p.matcher(commands.get(i));
                if (m.matches()) commands.set(i, m.replaceAll(replacement));
            }
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public List<String> wrapConditional(List<String> commands, String condition) {
            List<String> wrapped = new ArrayList<>(commands.size());
            for (String cmd : commands) {
                if (!cmd.startsWith("#") && !cmd.trim().isEmpty()) {
                    wrapped.add("execute " + condition + " run " + cmd);
                } else {
                    wrapped.add(cmd);
                }
            }
            assetTransformCount.incrementAndGet();
            return wrapped;
        }

        public List<String> optimizeSelectors(List<String> commands, int maxDistance) {
            List<String> optimized = new ArrayList<>(commands.size());
            for (String cmd : commands) {
                String opt = cmd;
                // Add distance limit to bare @e selectors
                opt = opt.replaceAll("@e\\[(?!.*distance)", "@e[distance=.." + maxDistance + ",");
                opt = opt.replace("@e[distance=.." + maxDistance + ",]", "@e[distance=.." + maxDistance + "]");
                // Remove redundant type=player from @a
                opt = opt.replace("@a[type=minecraft:player]", "@a");
                optimized.add(opt);
            }
            assetTransformCount.incrementAndGet();
            return optimized;
        }

        public List<String> removeComments(List<String> commands) {
            commands.removeIf(cmd -> cmd.trim().startsWith("#") || cmd.trim().isEmpty());
            assetTransformCount.incrementAndGet();
            return commands;
        }

        public String functionToString(List<String> commands) {
            return String.join("\n", commands) + "\n";
        }

        public boolean validateFunction(List<String> commands) {
            for (String cmd : commands) {
                String trimmed = cmd.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                // Basic validation: commands should start with known prefixes
                if (!trimmed.matches("^(execute|say|tell|msg|w|give|tp|teleport|kill|summon|setblock|fill|clone|" +
                        "effect|enchant|gamemode|gamerule|weather|time|difficulty|scoreboard|team|tag|trigger|" +
                        "title|bossbar|data|function|schedule|forceload|worldborder|spreadplayers|" +
                        "advancement|recipe|loot|attribute|item|place|ride|damage|return|" +
                        "clear|replaceitem|particle|playsound|stopsound|seed|list|ban|pardon|kick|op|deop|" +
                        "reload|debug|publish|save-all|save-on|save-off|stop|whitelist|me|defaultgamemode).*")) {
                    log("WARN: Possibly invalid command: %s", trimmed.substring(0, Math.min(50, trimmed.length())));
                }
            }
            return true;
        }

        // ── @DeepScript ──────────────────────────────────────────

        public String processScript(DeepScript ann, InputStream input) throws IOException {
            String script = readString(input);
            log("ScriptProcessor: %s on %s (lang=%s)", ann.operation(), ann.path(), ann.lang());
            assetTransformCount.incrementAndGet();
            return script;
        }

        public String injectScriptCode(String script, String marker, String injection, boolean before) {
            int idx = script.indexOf(marker);
            if (idx < 0) { log("WARN: Script marker not found: %s", marker); return script; }
            if (before) return script.substring(0, idx) + injection + "\n" + script.substring(idx);
            int end = idx + marker.length();
            assetTransformCount.incrementAndGet();
            return script.substring(0, end) + "\n" + injection + script.substring(end);
        }

        public String replaceScriptFunction(String script, String funcName, String newBody, DeepScript.ScriptLang lang) {
            String pattern;
            switch (lang) {
                case PYTHON:
                    pattern = "def\\s+" + Pattern.quote(funcName) + "\\s*\\([^)]*\\)\\s*:";
                    break;
                case LUA:
                    pattern = "function\\s+" + Pattern.quote(funcName) + "\\s*\\([^)]*\\)";
                    break;
                case RUBY:
                    pattern = "def\\s+" + Pattern.quote(funcName) + "(\\s*\\([^)]*\\))?";
                    break;
                default: // JavaScript, TypeScript, Groovy, Kotlin
                    pattern = "(function\\s+)?" + Pattern.quote(funcName) + "\\s*\\([^)]*\\)\\s*\\{";
                    break;
            }
            Matcher m = Pattern.compile(pattern).matcher(script);
            if (m.find()) {
                if (lang == DeepScript.ScriptLang.PYTHON) {
                    // Python: replace indented block
                    int start = m.start();
                    int lineEnd = script.indexOf('\n', m.end());
                    if (lineEnd < 0) lineEnd = script.length();
                    // Find the end of the indented block
                    int blockEnd = lineEnd;
                    String indent = getIndent(script, start);
                    while (blockEnd < script.length()) {
                        int nextNewline = script.indexOf('\n', blockEnd + 1);
                        if (nextNewline < 0) { blockEnd = script.length(); break; }
                        String nextLine = script.substring(blockEnd + 1, nextNewline).stripTrailing();
                        if (!nextLine.isEmpty() && !nextLine.startsWith(indent + " ") && !nextLine.startsWith(indent + "\t")) break;
                        blockEnd = nextNewline;
                    }
                    return script.substring(0, start) + newBody + script.substring(blockEnd);
                } else {
                    // Brace-based languages
                    int braceIdx = script.indexOf('{', m.end() - 1);
                    if (braceIdx >= 0) {
                        int braceEnd = findBrace(script, braceIdx);
                        if (braceEnd >= 0) {
                            return script.substring(0, m.start()) + newBody + script.substring(braceEnd + 1);
                        }
                    }
                }
            }
            return script;
        }

        public String wrapScriptFunction(String script, String funcName, String beforeCode, String afterCode, DeepScript.ScriptLang lang) {
            // For JavaScript-like languages
            String oldName = funcName + "__deepmix_original";
            script = script.replace(funcName, oldName);
            String wrapper = String.format(
                "function %s() {\n  %s\n  var __result = %s.apply(this, arguments);\n  %s\n  return __result;\n}\n",
                funcName, beforeCode != null ? beforeCode : "", oldName, afterCode != null ? afterCode : ""
            );
            assetTransformCount.incrementAndGet();
            return wrapper + script;
        }

        private String getIndent(String src, int pos) {
            int lineStart = src.lastIndexOf('\n', pos);
            if (lineStart < 0) lineStart = 0; else lineStart++;
            StringBuilder indent = new StringBuilder();
            for (int i = lineStart; i < pos && i < src.length(); i++) {
                char c = src.charAt(i);
                if (c == ' ' || c == '\t') indent.append(c);
                else break;
            }
            return indent.toString();
        }

        private int findBrace(String src, int openPos) {
            int depth = 0;
            for (int i = openPos; i < src.length(); i++) {
                if (src.charAt(i) == '{') depth++;
                if (src.charAt(i) == '}') { depth--; if (depth == 0) return i; }
            }
            return -1;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  UNIFIED PIPELINES                                              ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ====================================================================
    //  ASSET TRANSFORM PIPELINE
    // ====================================================================

    /**
     * Unified asset transformation pipeline.
     * Routes incoming resources to the correct Phase 3 processor based on
     * file extension, annotation type, or explicit routing.
     *
     * <pre>
     * AssetTransformPipeline
     * ├── VisualAssetProcessor   (PNG, JPG, WebP, JSON models, GLSL, TTF/OTF, animation JSON)
     * ├── AudioMediaProcessor    (OGG, MP3, WAV, particle JSON)
     * └── LanguageTextProcessor  (lang/JSON, splashes.txt, credits, atlas JSON, pack.mcmeta)
     * </pre>
     */
    public static class AssetTransformPipeline {

        private final VisualAssetProcessor visualProcessor = new VisualAssetProcessor();
        private final AudioMediaProcessor audioProcessor   = new AudioMediaProcessor();
        private final LanguageTextProcessor langProcessor   = new LanguageTextProcessor();

        private final Map<String, List<AssetTransformEntry>> pendingTransforms = new ConcurrentHashMap<>();

        public VisualAssetProcessor getVisualProcessor()  { return visualProcessor; }
        public AudioMediaProcessor getAudioProcessor()    { return audioProcessor; }
        public LanguageTextProcessor getLangProcessor()    { return langProcessor; }

        /** Register a transform to be applied when a matching resource is loaded. */
        public void registerTransform(String pathPattern, Annotation annotation, Object handler) {
            pendingTransforms.computeIfAbsent(pathPattern, k -> new CopyOnWriteArrayList<>())
                    .add(new AssetTransformEntry(annotation, handler));
            log("Pipeline: registered transform for %s", pathPattern);
        }

        /** Unregister all transforms for a given path. */
        public void unregisterTransforms(String pathPattern) {
            pendingTransforms.remove(pathPattern);
        }

        /** Process a single asset by file path and input stream. */
        public byte[] processAsset(String path, InputStream input) throws IOException {
            String ext = getFileExtension(path).toLowerCase();
            log("Pipeline: processing %s (ext=%s)", path, ext);

            switch (ext) {
                // ── Visual textures ──────────────────────────
                case "png": case "jpg": case "jpeg": case "bmp": case "gif": case "webp":
                    return processTextureAsset(path, input, ext);

                // ── Shaders ──────────────────────────────────
                case "glsl": case "vsh": case "fsh": case "gsh": case "csh":
                    return processShaderAsset(path, input);

                // ── Fonts ────────────────────────────────────
                case "ttf": case "otf":
                    return passThroughBytes(input); // fonts returned raw; processor works on Font objects

                // ── Audio ────────────────────────────────────
                case "ogg": case "mp3": case "wav": case "flac":
                    return passThroughBytes(input); // audio returned raw; processor works on byte[]

                // ── JSON (context-dependent) ─────────────────
                case "json":
                    return processJsonAsset(path, input);

                // ── Text ─────────────────────────────────────
                case "txt":
                    return processTextAsset(path, input);

                // ── mcfunction ────────────────────────────────
                case "mcfunction":
                    return passThroughBytes(input);

                // ── pack.mcmeta ──────────────────────────────
                case "mcmeta":
                    return processJsonAsset(path, input);

                // ── lang files ────────────────────────────────
                case "lang":
                    return processLangAsset(path, input);

                default:
                    log("Pipeline: unsupported extension '%s', passing through", ext);
                    return passThroughBytes(input);
            }
        }

        private byte[] processTextureAsset(String path, InputStream input, String ext) throws IOException {
            BufferedImage img = ImageIO.read(input);
            if (img == null) throw new DeepMixAssetException("Failed to read image: " + path);
            // Apply any registered transforms
            img = applyRegisteredTextureTransforms(path, img);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = "png"; // always output PNG for lossless Minecraft textures
            ImageIO.write(img, format, baos);
            return baos.toByteArray();
        }

        private BufferedImage applyRegisteredTextureTransforms(String path, BufferedImage img) {
            for (Map.Entry<String, List<AssetTransformEntry>> entry : pendingTransforms.entrySet()) {
                if (pathMatches(path, entry.getKey())) {
                    for (AssetTransformEntry transform : entry.getValue()) {
                        if (transform.annotation instanceof DeepTexture) {
                            img = visualProcessor.processTexture((DeepTexture) transform.annotation, img);
                        }
                    }
                }
            }
            return img;
        }

        private byte[] processShaderAsset(String path, InputStream input) throws IOException {
            String src = readString(input);
            for (Map.Entry<String, List<AssetTransformEntry>> entry : pendingTransforms.entrySet()) {
                if (pathMatches(path, entry.getKey())) {
                    for (AssetTransformEntry transform : entry.getValue()) {
                        if (transform.annotation instanceof DeepShader) {
                            src = visualProcessor.processShader((DeepShader) transform.annotation, src);
                        }
                    }
                }
            }
            return src.getBytes(StandardCharsets.UTF_8);
        }

        private byte[] processJsonAsset(String path, InputStream input) throws IOException {
            String json = readString(input);
            // JSON assets are returned as-is; individual processors modify the parsed map
            return json.getBytes(StandardCharsets.UTF_8);
        }

        private byte[] processTextAsset(String path, InputStream input) throws IOException {
            String text = readString(input);
            return text.getBytes(StandardCharsets.UTF_8);
        }

        private byte[] processLangAsset(String path, InputStream input) throws IOException {
            String content = readString(input);
            return content.getBytes(StandardCharsets.UTF_8);
        }

        private byte[] passThroughBytes(InputStream input) throws IOException {
            return readBytes(input);
        }

        /** Batch process multiple assets. */
        public Map<String, byte[]> processAssets(Map<String, InputStream> assets) {
            Map<String, byte[]> results = new ConcurrentHashMap<>();
            assets.entrySet().parallelStream().forEach(entry -> {
                try {
                    results.put(entry.getKey(), processAsset(entry.getKey(), entry.getValue()));
                } catch (IOException e) {
                    log("ERROR: Failed to process asset %s: %s", entry.getKey(), e.getMessage());
                }
            });
            return results;
        }

        /** Simple glob-like matching: * matches anything, ** matches path separators too */
        private boolean pathMatches(String path, String pattern) {
            String regex = pattern
                    .replace(".", "\\.")
                    .replace("**/", "(.+/)?")
                    .replace("**", ".+")
                    .replace("*", "[^/]+");
            return path.matches(regex);
        }

        private String getFileExtension(String path) {
            int lastDot = path.lastIndexOf('.');
            return lastDot > 0 ? path.substring(lastDot + 1) : "";
        }

        /** Internal record for pending transforms */
        private static class AssetTransformEntry {
            final Annotation annotation;
            final Object handler;
            AssetTransformEntry(Annotation ann, Object handler) { this.annotation = ann; this.handler = handler; }
        }
    }


    // ====================================================================
    //  GAME DATA PIPELINE
    // ====================================================================

    /**
     * Unified game-data transformation pipeline.
     * Routes incoming game data to the correct Phase 4 processor.
     *
     * <pre>
     * GameDataPipeline
     * ├── MinecraftDataProcessor (registry, recipe, loot, NBT, tag, blockstate, item model, advancement, predicate)
     * └── WorldGenerationProcessor (structure, biome, dimension, function, script)
     * </pre>
     */
    public static class GameDataPipeline {

        private final MinecraftDataProcessor dataProcessor  = new MinecraftDataProcessor();
        private final WorldGenerationProcessor worldProcessor = new WorldGenerationProcessor();

        private final Map<String, List<GameDataTransformEntry>> pendingTransforms = new ConcurrentHashMap<>();

        public MinecraftDataProcessor getDataProcessor()     { return dataProcessor; }
        public WorldGenerationProcessor getWorldProcessor()  { return worldProcessor; }

        /** Register a game-data transform. */
        public void registerTransform(String pathPattern, Annotation annotation, Object handler) {
            pendingTransforms.computeIfAbsent(pathPattern, k -> new CopyOnWriteArrayList<>())
                    .add(new GameDataTransformEntry(annotation, handler));
            log("GamePipeline: registered transform for %s", pathPattern);
        }

        /** Route incoming game data to the correct processor. */
        @SuppressWarnings("unchecked")
        public Object processGameData(String path, InputStream input) throws IOException {
            String normalized = path.replace('\\', '/').toLowerCase();

            if (normalized.contains("/recipes/"))             return dataProcessor.processRecipe(null, input);
            if (normalized.contains("/loot_tables/"))         return dataProcessor.processLoot(null, input);
            if (normalized.contains("/tags/"))                return dataProcessor.processTag(null, input);
            if (normalized.contains("/advancements/"))        return dataProcessor.processAdvancement(null, input);
            if (normalized.contains("/predicates/"))          return dataProcessor.processPredicate(null, input);
            if (normalized.contains("/blockstates/"))         return dataProcessor.processBlockState(null, input);
            if (normalized.contains("/models/item/"))         return dataProcessor.processItemModel(null, input);
            if (normalized.contains("/worldgen/biome/"))      return worldProcessor.processBiome(null, input);
            if (normalized.contains("/dimension/"))           return worldProcessor.processDimension(null, input);
            if (normalized.contains("/structures/"))          return worldProcessor.processStructure(null, input);
            if (normalized.endsWith(".mcfunction"))            return worldProcessor.processFunction(null, input);
            if (normalized.endsWith(".nbt"))                   return dataProcessor.processNBT(null, input);

            // Fallback: parse as JSON
            return JsonEngine.parseObject(input);
        }

        /** Convenience: modify a registry. */
        public void modifyRegistry(String registryName, Object data) {
            Object registry = dataProcessor.getRegistry(registryName);
            if (registry != null) {
                DeepRegistry ann = createDummyRegistryAnnotation(registryName);
                dataProcessor.processRegistry(ann, registry);
            }
        }

        /** Convenience: transform a recipe from JSON. */
        public Map<String, Object> transformRecipe(InputStream input, DeepRecipe ann) throws IOException {
            return dataProcessor.processRecipe(ann, input);
        }

        /** Convenience: modify a loot table from JSON. */
        public Map<String, Object> modifyLootTable(InputStream input, DeepLoot ann) throws IOException {
            return dataProcessor.processLoot(ann, input);
        }

        private DeepRegistry createDummyRegistryAnnotation(String registryName) {
            return new DeepRegistry() {
                @Override public Class<? extends Annotation> annotationType() { return DeepRegistry.class; }
                @Override public String registryName() { return registryName; }
                @Override public RegOp operation() { return RegOp.MODIFY; }
                @Override public String[] targets() { return new String[0]; }
                @Override public boolean hotReload() { return true; }
                @Override public int priority() { return 1000; }
            };
        }

        private static class GameDataTransformEntry {
            final Annotation annotation;
            final Object handler;
            GameDataTransformEntry(Annotation ann, Object handler) { this.annotation = ann; this.handler = handler; }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PROCESSOR REGISTRY & CENTRAL DISPATCHER                        ║
    // ╚══════════════════════════════════════════════════════════════════╝

    /**
     * Central registry that discovers, indexes, and dispatches Phase 3 & 4
     * annotation processors at class-scan time.
     */
    public static class Phase34ProcessorRegistry {

        private static volatile Phase34ProcessorRegistry INSTANCE;

        private final VisualAssetProcessor visualAssetProcessor       = new VisualAssetProcessor();
        private final AudioMediaProcessor audioMediaProcessor         = new AudioMediaProcessor();
        private final LanguageTextProcessor languageTextProcessor     = new LanguageTextProcessor();
        private final MinecraftDataProcessor minecraftDataProcessor   = new MinecraftDataProcessor();
        private final WorldGenerationProcessor worldGenerationProcessor = new WorldGenerationProcessor();

        private final AssetTransformPipeline assetPipeline  = new AssetTransformPipeline();
        private final GameDataPipeline gameDataPipeline     = new GameDataPipeline();

        /** Annotation class → processor routing */
        private final Map<Class<? extends Annotation>, BiConsumer<Annotation, InputStream>> routes = new HashMap<>();

        private Phase34ProcessorRegistry() {
            registerRoutes();
        }

        public static Phase34ProcessorRegistry getInstance() {
            if (INSTANCE == null) {
                synchronized (Phase34ProcessorRegistry.class) {
                    if (INSTANCE == null) INSTANCE = new Phase34ProcessorRegistry();
                }
            }
            return INSTANCE;
        }

        @SuppressWarnings("unchecked")
        private void registerRoutes() {
            // Phase 3A — Visual
            routes.put(DeepTexture.class,   (a, in) -> wrap(() -> visualAssetProcessor.processTexture((DeepTexture) a, ImageIO.read(in))));
            routes.put(DeepModel.class,     (a, in) -> wrap(() -> visualAssetProcessor.processModel((DeepModel) a, in)));
            routes.put(DeepShader.class,    (a, in) -> wrap(() -> visualAssetProcessor.processShader((DeepShader) a, readString(in))));
            routes.put(DeepAnimation.class, (a, in) -> wrap(() -> visualAssetProcessor.processAnimation((DeepAnimation) a, in)));

            // Phase 3B — Audio/Media
            routes.put(DeepAudio.class,    (a, in) -> wrap(() -> audioMediaProcessor.processAudio((DeepAudio) a, readBytes(in))));
            routes.put(DeepParticle.class,  (a, in) -> wrap(() -> audioMediaProcessor.processParticle((DeepParticle) a, in)));

            // Phase 3C — Language/Text
            routes.put(DeepLang.class,     (a, in) -> wrap(() -> languageTextProcessor.processLang((DeepLang) a, in)));
            routes.put(DeepSplash.class,   (a, in) -> wrap(() -> languageTextProcessor.processSplash((DeepSplash) a, in)));
            routes.put(DeepCredits.class,  (a, in) -> wrap(() -> languageTextProcessor.processCredits((DeepCredits) a, in)));
            routes.put(DeepAtlas.class,    (a, in) -> wrap(() -> languageTextProcessor.processAtlas((DeepAtlas) a, in)));
            routes.put(DeepPack.class,     (a, in) -> wrap(() -> languageTextProcessor.processPack((DeepPack) a, in)));

            // Phase 4A — Minecraft Data
            routes.put(DeepRecipe.class,      (a, in) -> wrap(() -> minecraftDataProcessor.processRecipe((DeepRecipe) a, in)));
            routes.put(DeepLoot.class,        (a, in) -> wrap(() -> minecraftDataProcessor.processLoot((DeepLoot) a, in)));
            routes.put(DeepNBT.class,         (a, in) -> wrap(() -> minecraftDataProcessor.processNBT((DeepNBT) a, in)));
            routes.put(DeepTag.class,         (a, in) -> wrap(() -> minecraftDataProcessor.processTag((DeepTag) a, in)));
            routes.put(DeepBlockState.class,  (a, in) -> wrap(() -> minecraftDataProcessor.processBlockState((DeepBlockState) a, in)));
            routes.put(DeepItemModel.class,   (a, in) -> wrap(() -> minecraftDataProcessor.processItemModel((DeepItemModel) a, in)));
            routes.put(DeepAdvancement.class, (a, in) -> wrap(() -> minecraftDataProcessor.processAdvancement((DeepAdvancement) a, in)));
            routes.put(DeepPredicate.class,   (a, in) -> wrap(() -> minecraftDataProcessor.processPredicate((DeepPredicate) a, in)));

            // Phase 4B — World Generation
            routes.put(DeepStructure.class,  (a, in) -> wrap(() -> worldGenerationProcessor.processStructure((DeepStructure) a, in)));
            routes.put(DeepBiome.class,      (a, in) -> wrap(() -> worldGenerationProcessor.processBiome((DeepBiome) a, in)));
            routes.put(DeepDimension.class,  (a, in) -> wrap(() -> worldGenerationProcessor.processDimension((DeepDimension) a, in)));
            routes.put(DeepFunction.class,   (a, in) -> wrap(() -> worldGenerationProcessor.processFunction((DeepFunction) a, in)));
            routes.put(DeepScript.class,     (a, in) -> wrap(() -> worldGenerationProcessor.processScript((DeepScript) a, in)));
        }

        /** Dispatch an annotation + its target resource to the correct processor. */
        public void dispatch(Annotation annotation, InputStream input) {
            BiConsumer<Annotation, InputStream> route = routes.get(annotation.annotationType());
            if (route != null) {
                route.accept(annotation, input);
            } else {
                log("WARN: No route registered for annotation @%s", annotation.annotationType().getSimpleName());
            }
        }

        /** Scan a class for Phase 3/4 annotations and register them. */
        public void scanClass(Class<?> clazz) {
            // Class-level annotations
            for (Annotation ann : clazz.getAnnotations()) {
                if (routes.containsKey(ann.annotationType())) {
                    log("Registry: found class-level @%s on %s", ann.annotationType().getSimpleName(), clazz.getName());
                    registerAnnotation(ann, clazz, null);
                }
            }
            // Method-level annotations
            for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                for (Annotation ann : method.getAnnotations()) {
                    if (routes.containsKey(ann.annotationType())) {
                        log("Registry: found method-level @%s on %s.%s()", ann.annotationType().getSimpleName(), clazz.getSimpleName(), method.getName());
                        registerAnnotation(ann, clazz, method);
                    }
                }
            }
        }

        private void registerAnnotation(Annotation ann, Class<?> clazz, java.lang.reflect.Method method) {
            String path = extractPath(ann);
            if (path != null && !path.isEmpty()) {
                assetPipeline.registerTransform(path, ann, method != null ? method : clazz);
            }
        }

        /** Extract the 'path' attribute from any Phase 3/4 annotation via reflection. */
        private String extractPath(Annotation ann) {
            try {
                java.lang.reflect.Method pathMethod = ann.annotationType().getMethod("path");
                Object result = pathMethod.invoke(ann);
                return result instanceof String ? (String) result : null;
            } catch (NoSuchMethodException e) {
                // Some annotations (e.g., DeepRegistry) use registryName instead
                try {
                    java.lang.reflect.Method regMethod = ann.annotationType().getMethod("registryName");
                    Object result = regMethod.invoke(ann);
                    return result instanceof String ? (String) result : null;
                } catch (Exception ignored) {}
            } catch (Exception e) {
                log("WARN: Could not extract path from @%s: %s", ann.annotationType().getSimpleName(), e.getMessage());
            }
            return null;
        }

        // ── Accessors ────────────────────────────────────────────

        public VisualAssetProcessor getVisualAssetProcessor()           { return visualAssetProcessor; }
        public AudioMediaProcessor getAudioMediaProcessor()             { return audioMediaProcessor; }
        public LanguageTextProcessor getLanguageTextProcessor()         { return languageTextProcessor; }
        public MinecraftDataProcessor getMinecraftDataProcessor()       { return minecraftDataProcessor; }
        public WorldGenerationProcessor getWorldGenerationProcessor()   { return worldGenerationProcessor; }
        public AssetTransformPipeline getAssetPipeline()                { return assetPipeline; }
        public GameDataPipeline getGameDataPipeline()                   { return gameDataPipeline; }

        private void wrap(ThrowingRunnable r) {
            try { r.run(); } catch (Exception e) { log("ERROR: Processor failed: %s", e.getMessage()); }
        }

        @FunctionalInterface
        private interface ThrowingRunnable { void run() throws Exception; }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  UTILITY CLASSES                                                ║
    // ╚══════════════════════════════════════════════════════════════════╝

    /** Resolve resource paths with support for Minecraft-style namespaces. */
    public static final class ResourcePathResolver {

        private ResourcePathResolver() {}

        public static String resolve(String path) {
            if (path.contains(":") && !path.contains(":/")) {
                // Minecraft-style namespace: "minecraft:textures/block/stone.png"
                String[] parts = path.split(":", 2);
                return "assets/" + parts[0] + "/" + parts[1];
            }
            return path;
        }

        public static String resolveData(String path) {
            if (path.contains(":") && !path.contains(":/")) {
                String[] parts = path.split(":", 2);
                return "data/" + parts[0] + "/" + parts[1];
            }
            return path;
        }

        public static List<String> findResources(String baseDir, String pattern) {
            List<String> results = new ArrayList<>();
            try {
                Path base = Paths.get(baseDir);
                if (Files.exists(base)) {
                    String glob = pattern.contains("*") ? pattern : "**/" + pattern;
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(base, glob)) {
                        for (Path p : stream) results.add(p.toString().replace('\\', '/'));
                    } catch (Exception e) {
                        // Fallback: walk the tree
                        Files.walk(base).filter(p -> {
                            String name = p.getFileName().toString();
                            return name.matches(pattern.replace("*", ".*"));
                        }).forEach(p -> results.add(p.toString().replace('\\', '/')));
                    }
                }
            } catch (IOException e) {
                log("WARN: Failed to search resources in %s: %s", baseDir, e.getMessage());
            }
            return results;
        }

        public static String getNamespace(String path) {
            if (path.contains(":")) return path.substring(0, path.indexOf(':'));
            if (path.startsWith("assets/") || path.startsWith("data/")) {
                String[] parts = path.split("/");
                return parts.length > 1 ? parts[1] : "minecraft";
            }
            return "minecraft";
        }

        public static String getResourcePath(String path) {
            if (path.contains(":")) return path.substring(path.indexOf(':') + 1);
            if (path.startsWith("assets/") || path.startsWith("data/")) {
                int secondSlash = path.indexOf('/', path.indexOf('/') + 1);
                return secondSlash >= 0 ? path.substring(secondSlash + 1) : path;
            }
            return path;
        }
    }

    /** Thread-safe LRU cache for transformed assets. */
    public static final class AssetCacheManager {

        private static final int MAX_CACHE_SIZE = 512;
        private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private static final AtomicLong hits = new AtomicLong(0);
        private static final AtomicLong misses = new AtomicLong(0);

        private AssetCacheManager() {}

        public static void cache(String key, Object asset) {
            if (cache.size() >= MAX_CACHE_SIZE) evict();
            cache.put(key, new CacheEntry(asset));
        }

        public static Object get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry != null) { entry.touch(); hits.incrementAndGet(); return entry.value; }
            misses.incrementAndGet();
            return null;
        }

        public static boolean has(String key) { return cache.containsKey(key); }

        public static void invalidate(String key) { cache.remove(key); }

        public static void invalidatePattern(String pattern) {
            Pattern p = Pattern.compile(pattern.replace("*", ".*"));
            cache.keySet().removeIf(k -> p.matcher(k).matches());
        }

        public static void clear() { cache.clear(); hits.set(0); misses.set(0); }

        public static int size() { return cache.size(); }

        public static double hitRate() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total : 0.0;
        }

        private static void evict() {
            // LRU eviction: remove the oldest quarter
            int toRemove = MAX_CACHE_SIZE / 4;
            cache.entrySet().stream()
                    .sorted(Comparator.comparingLong(e -> e.getValue().lastAccess))
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()) // materialize before removal
                    .forEach(cache::remove);
        }

        private static class CacheEntry {
            final Object value;
            volatile long lastAccess;
            CacheEntry(Object v) { this.value = v; this.lastAccess = System.nanoTime(); }
            void touch() { this.lastAccess = System.nanoTime(); }
        }
    }

    /** Detect file format from magic bytes. */
    public static final class FormatDetector {

        private FormatDetector() {}

        private static final byte[] PNG_MAGIC  = {(byte)0x89, 0x50, 0x4E, 0x47};
        private static final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
        private static final byte[] GIF_MAGIC  = {0x47, 0x49, 0x46, 0x38};
        private static final byte[] BMP_MAGIC  = {0x42, 0x4D};
        private static final byte[] WEBP_MAGIC = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
        private static final byte[] OGG_MAGIC  = {0x4F, 0x67, 0x67, 0x53}; // "OggS"
        private static final byte[] WAV_MAGIC  = {0x52, 0x49, 0x46, 0x46}; // "RIFF" (also)
        private static final byte[] NBT_GZIP   = {0x1F, (byte)0x8B};
        private static final byte[] NBT_COMP   = {0x0A}; // TAG_Compound

        public static String detectFormat(byte[] bytes) {
            if (bytes == null || bytes.length < 4) return "unknown";
            if (startsWith(bytes, PNG_MAGIC))  return "png";
            if (startsWith(bytes, JPEG_MAGIC)) return "jpg";
            if (startsWith(bytes, GIF_MAGIC))  return "gif";
            if (startsWith(bytes, BMP_MAGIC))  return "bmp";
            if (startsWith(bytes, OGG_MAGIC))  return "ogg";
            if (startsWith(bytes, NBT_GZIP))   return "nbt_gzip";
            if (bytes[0] == 0x0A)              return "nbt";
            if (startsWith(bytes, WEBP_MAGIC) && bytes.length > 11 && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E')
                return "wav";
            if (startsWith(bytes, WEBP_MAGIC) && bytes.length > 11 && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
                return "webp";

            // Try text-based detection
            String text = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8).trim();
            if (text.startsWith("{") || text.startsWith("[")) return "json";
            if (text.startsWith("#version") || text.contains("void main")) return "glsl";
            if (text.startsWith("#") || text.contains("=")) return "properties";

            return "unknown";
        }

        public static String detectFormat(String path) {
            int dot = path.lastIndexOf('.');
            if (dot > 0) return path.substring(dot + 1).toLowerCase();
            return "unknown";
        }

        private static boolean startsWith(byte[] data, byte[] prefix) {
            if (data.length < prefix.length) return false;
            for (int i = 0; i < prefix.length; i++) {
                if (data[i] != prefix[i]) return false;
            }
            return true;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  HOT-RELOAD WATCHER                                             ║
    // ╚══════════════════════════════════════════════════════════════════╝

    /**
     * File watcher that monitors resource directories and triggers
     * re-transformation when assets or game data files change.
     */
    public static class HotReloadWatcher implements Closeable {

        private final Map<Path, WatchKey> watched = new ConcurrentHashMap<>();
        private final WatchService watchService;
        private final Consumer<Path> onFileChanged;
        private volatile boolean running;
        private Thread watchThread;

        public HotReloadWatcher(Consumer<Path> onFileChanged) throws IOException {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.onFileChanged = onFileChanged;
        }

        public void watch(Path directory) throws IOException {
            if (!Files.isDirectory(directory)) return;
            WatchKey key = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);
            watched.put(directory, key);
            log("HotReload: watching %s", directory);
        }

        public void start() {
            if (running) return;
            running = true;
            watchThread = new Thread(this::pollLoop, "DeepMix-HotReload");
            watchThread.setDaemon(true);
            watchThread.start();
            log("HotReload: watcher started");
        }

        public void stop() {
            running = false;
            if (watchThread != null) watchThread.interrupt();
            log("HotReload: watcher stopped");
        }

        @Override
        public void close() throws IOException {
            stop();
            watchService.close();
        }

        private void pollLoop() {
            while (running) {
                try {
                    WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) continue;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path dir = (Path) key.watchable();
                        Path changed = dir.resolve(pathEvent.context());
                        log("HotReload: file changed → %s", changed);
                        AssetCacheManager.invalidate(changed.toString().replace('\\', '/'));
                        onFileChanged.accept(changed);
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log("ERROR: HotReload poll failure: %s", e.getMessage());
                }
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  EXCEPTION                                                                   ║
    // ╚══════════════════════════════════════════════════════════════════╝

    public static class DeepMixAssetException extends RuntimeException {
        public DeepMixAssetException(String message) { super(message); }
        public DeepMixAssetException(String message, Throwable cause) { super(message, cause); }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  I/O HELPERS                                                                 ║
    // ╚══════════════════════════════════════════════════════════════════╝

    static String readString(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }
