package stellar.snow.astralis;

import stellar.snow.astralis.nexus.GLOptimizer;
import stellar.snow.astralis.api.opengl.managers.GLStateCache;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.EnumMap;
import java.util.Map;

public class CommandAstralis extends CommandBase {
    @Override
    public String getName() {
        return "astralis";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "/astralis <status|glinfo|stats|cache>";
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            args = new String[]{"status"};
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "status":
                showEntityStatus(sender);
                break;
                
            case "glinfo":
                showGLInfo(sender);
                break;
                
            case "stats":
                showGLStats(sender);
                break;
                
            case "cache":
                showCacheStats(sender);
                break;
                
            default:
                sender.sendMessage(new TextComponentString("§cUnknown subcommand. Use: status, glinfo, stats, or cache"));
        }
    }
    
    private void showEntityStatus(ICommandSender sender) {
        Map<CullingTier, Integer> counts = new EnumMap<>(CullingTier.class);
        for (CullingTier tier : CullingTier.values()) {
            counts.put(tier, 0);
        }
        
        // Count entities per tier
        for (Entity entity : sender.getEntityWorld().loadedEntityList) {
            if (entity instanceof net.minecraft.entity.EntityLiving) {
                CullingTier tier = CullingManager.getInstance()
                    .calculateTier(entity, sender.getEntityWorld());
                counts.put(tier, counts.get(tier) + 1);
            }
        }
        
        sender.sendMessage(new TextComponentString("§6=== Astralis Entity Distribution ==="));
        sender.sendMessage(new TextComponentString(
            String.format("§aFULL: %d  §eMINIMAL: %d  §6MODERATE: %d  §cAGGRESSIVE: %d",
                counts.get(CullingTier.FULL),
                counts.get(CullingTier.MINIMAL),
                counts.get(CullingTier.MODERATE),
                counts.get(CullingTier.AGGRESSIVE))
        ));
        
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        sender.sendMessage(new TextComponentString(String.format("§7Total entities tracked: %d", total)));
    }
    
    private void showGLInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== Astralis GL Information ==="));
        
        if (!GLOptimizer.isEnabled()) {
            sender.sendMessage(new TextComponentString("§cGL Optimizer: DISABLED"));
            sender.sendMessage(new TextComponentString("§7Use this command in the log to see why."));
            return;
        }
        
        sender.sendMessage(new TextComponentString("§aGL Optimizer: ENABLED"));
        sender.sendMessage(new TextComponentString(
            "§7Compatibility: §f" + GLOptimizer.getCompatibilityLevel()
        ));
        sender.sendMessage(new TextComponentString(
            "§7Implementation: §f" + GLOptimizer.getBufferOps().getName()
        ));
        
        // Print full report to console for detailed info
        String report = GLOptimizer.getDetailedReport();
        sender.sendMessage(new TextComponentString("§7Full report printed to console/log."));
        System.out.println(report);
    }
    
    private void showGLStats(ICommandSender sender) {
        if (!GLOptimizer.isEnabled()) {
            sender.sendMessage(new TextComponentString("§cGL Optimizer disabled - no stats available"));
            return;
        }
        
        sender.sendMessage(new TextComponentString("§6=== Astralis Performance Stats ==="));
        sender.sendMessage(new TextComponentString(
            String.format("§7State Cache Efficiency: §a%.1f%% §7calls skipped",
                GLStateCache.getSkipPercentage())
        ));
        
        // Print detailed stats to console
        GLOptimizer.printStats();
        sender.sendMessage(new TextComponentString("§7Detailed stats printed to console/log."));
    }
    
    private void showCacheStats(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== GL State Cache Statistics ==="));
        sender.sendMessage(new TextComponentString(
            String.format("§7Cache Hit Rate: §a%.1f%%",
                GLStateCache.getSkipPercentage())
        ));
        sender.sendMessage(new TextComponentString(
            "§7This shows how many redundant GL calls were eliminated."
        ));
        sender.sendMessage(new TextComponentString(
            "§7Higher is better (means more optimization)."
        ));
        
        // Reset metrics for next measurement period
        sender.sendMessage(new TextComponentString("§eMetrics reset for next measurement."));
        GLStateCache.resetMetrics();
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0; // no need for Cheats!!
    }
}
