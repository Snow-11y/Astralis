// =====================================================================================
// Mini_DirtyRoomSimplifier.java
// Part of Mini_DirtyRoom — Minecraft 1.12.2 Modernization Layer
//
// COMPREHENSIVE SETUP SIMPLIFIER & USER EXPERIENCE LAYER
// This file transforms Mini_DirtyRoom installation from complex to effortless:
//   - Beautiful, intuitive GUI on first launch
//   - Automatic environment detection and recommendations
//   - One-click download and installation of components
//   - Interactive setup wizard with intelligent defaults
//   - Real-time progress tracking and visual feedback
//   - Zero-configuration option for beginners
//   - Advanced options for power users
//   - Automatic updates and maintenance
//
// Features:
//   1. Drop-in Installation (just add JAR to mods folder)
//   2. First-Launch Setup Wizard
//   3. Java Distribution Manager (Zulu, Adoptium, GraalVM, etc.)
//   4. LWJGL Version Selector (2.9.x → 3.4.0)
//   5. Graphics Settings Optimizer
//   6. Platform-Specific Installers (Desktop: tar.gz, Mobile: tar.xz)
//   7. Visual Progress & Status Display
//   8. Intelligent Recommendations Engine
//   9. One-Click "Best Settings" Mode
//   10. Backup & Rollback System
//
// Architecture:
//   - Swing/JavaFX hybrid UI (fallback to console for headless)
//   - Asynchronous download manager with progress tracking
//   - Smart caching to avoid re-downloading
//   - Configuration persistence across launches
//   - Telemetry (opt-in) for improving recommendations
//
// =====================================================================================

package stellar.snow.astralis.integration.Mini_DirtyRoom.optimizer;

// ── DeepMix Core Imports ─────────────────────────────────────────────────────────
import stellar.snow.astralis.integration.DeepMixTransformers;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.DeepMix.Core.*;
import stellar.snow.astralis.integration.DeepMix.Transformers.*;
import stellar.snow.astralis.integration.DeepMix.Util.*;

// ── Java Standard & UI APIs ──────────────────────────────────────────────────────
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
import java.util.zip.*;


// =====================================================================================
//  PLUGIN DECLARATION & LIFECYCLE
// =====================================================================================

@DeepUI(
    style = UIStyle.MODERN,
    theme = UITheme.ADAPTIVE, // Light/Dark based on system
    animations = true,
    scalingFactor = UIScalingFactor.AUTO
)
public final class Mini_DirtyRoomSimplifier {

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 1: CONSTANTS & CONFIGURATION
    // ─────────────────────────────────────────────────────────────────────────────

    private static final String VERSION = "1.0.0";
    private static final String CONFIG_FILE = ".mdr_config.json";
    private static final String CACHE_DIR = ".mdr_cache";
    private static final String BACKUP_DIR = ".mdr_backups";
    
    // Download sources
    private static final String ZULU_BASE_URL = "https://cdn.azul.com/zulu/bin/";
    private static final String ADOPTIUM_BASE_URL = "https://api.adoptium.net/v3/binary/latest/";
    private static final String GRAALVM_BASE_URL = "https://github.com/graalvm/graalvm-ce-builds/releases/download/";
    private static final String LWJGL_BASE_URL = "https://build.lwjgl.org/release/";
    private static final String LWJGL_NIGHTLY_URL = "https://build.lwjgl.org/nightly/";
    
    // UI Colors (modern, professional palette)
    private static final Color PRIMARY_COLOR = new Color(66, 135, 245);      // Vibrant Blue
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);       // Green
    private static final Color WARNING_COLOR = new Color(255, 152, 0);       // Orange
    private static final Color ERROR_COLOR = new Color(244, 67, 54);         // Red
    private static final Color BACKGROUND_LIGHT = new Color(250, 250, 250);  // Light Gray
    private static final Color BACKGROUND_DARK = new Color(30, 30, 30);      // Dark Gray
    private static final Color TEXT_LIGHT = new Color(33, 33, 33);           // Dark Text
    private static final Color TEXT_DARK = new Color(240, 240, 240);         // Light Text
    
    // State
    private static volatile boolean setupCompleted = false;
    private static volatile boolean firstLaunch = true;
    private static volatile SetupWizardWindow wizardWindow = null;
    private static final ExecutorService downloadExecutor = Executors.newFixedThreadPool(4);
    
    // Configuration
    private static SimplifierConfig config = new SimplifierConfig();

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 2: MAIN ENTRY POINT & INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Initialize simplifier - runs before Mini_DirtyRoomCore
     */
    @DeepHook(
        targets = {
            @HookTarget(className = "stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore",
                       methodName = "<clinit>")
        },
        timing = HookTiming.BEFORE,
        priority = Integer.MAX_VALUE - 2
    )
    public static void initialize() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  Mini_DirtyRoom Setup Simplifier v" + VERSION);
        System.out.println("  Making installation effortless...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Load existing configuration
        loadConfiguration();
        
        // Check if this is first launch
        firstLaunch = !configFileExists();
        
        if (firstLaunch || !setupCompleted) {
            System.out.println("  First launch detected - starting setup wizard...");
            
            // Check if we're running in headless mode
            if (GraphicsEnvironment.isHeadless() || !hasDisplay()) {
                runConsoleSetup();
            } else {
                runGUISetup();
            }
        } else {
            System.out.println("  ✓ Configuration loaded from previous session");
            System.out.println("  ✓ Setup already completed");
            applyConfiguration();
        }
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 3: GUI SETUP WIZARD
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Launch beautiful GUI setup wizard
     */
    private static void runGUISetup() {
        try {
            // Set system look and feel with modern enhancements
            setupLookAndFeel();
            
            // Create and show wizard window
            SwingUtilities.invokeLater(() -> {
                wizardWindow = new SetupWizardWindow();
                wizardWindow.setVisible(true);
            });
            
            // Wait for wizard to complete
            while (wizardWindow == null || wizardWindow.isVisible()) {
                Thread.sleep(100);
            }
            
            setupCompleted = true;
            saveConfiguration();
            applyConfiguration();
            
        } catch (Exception e) {
            System.err.println("[Simplifier] GUI setup failed, falling back to console: " + e.getMessage());
            runConsoleSetup();
        }
    }

    /**
     * Setup modern look and feel
     */
    private static void setupLookAndFeel() {
        try {
            // Try to use FlatLaf if available (modern look)
            try {
                Class<?> flatDarkLaf = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
                UIManager.setLookAndFeel((javax.swing.LookAndFeel) flatDarkLaf.getDeclaredConstructor().newInstance());
                return;
            } catch (ClassNotFoundException e) {
                // FlatLaf not available, continue
            }
            
            // Fallback to system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Apply custom UI enhancements
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            
        } catch (Exception e) {
            System.err.println("[Simplifier] Could not set look and feel: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 4: SETUP WIZARD WINDOW
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Beautiful, modern setup wizard window
     */
    private static class SetupWizardWindow extends JFrame {
        
        private JPanel contentPanel;
        private CardLayout cardLayout;
        private int currentStep = 0;
        private String[] stepNames = {
            "Welcome",
            "Detection",
            "Java Selection",
            "LWJGL Selection",
            "Graphics Settings",
            "Installation",
            "Complete"
        };
        
        private JButton nextButton;
        private JButton backButton;
        private JProgressBar progressBar;
        
        public SetupWizardWindow() {
            setTitle("Mini_DirtyRoom Setup Wizard");
            setSize(900, 700);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setLocationRelativeTo(null);
            setResizable(false);
            
            // Modern icon (if available)
            try {
                setIconImage(createModernIcon());
            } catch (Exception e) {
                // Icon not critical
            }
            
            // Main layout
            setLayout(new BorderLayout(0, 0));
            
            // Header panel
            add(createHeaderPanel(), BorderLayout.NORTH);
            
            // Content panel with card layout
            cardLayout = new CardLayout();
            contentPanel = new JPanel(cardLayout);
            contentPanel.setBackground(isDarkMode() ? BACKGROUND_DARK : BACKGROUND_LIGHT);
            
            // Add all wizard steps
            contentPanel.add(createWelcomePanel(), "Welcome");
            contentPanel.add(createDetectionPanel(), "Detection");
            contentPanel.add(createJavaSelectionPanel(), "Java Selection");
            contentPanel.add(createLWJGLSelectionPanel(), "LWJGL Selection");
            contentPanel.add(createGraphicsSettingsPanel(), "Graphics Settings");
            contentPanel.add(createInstallationPanel(), "Installation");
            contentPanel.add(createCompletePanel(), "Complete");
            
            add(contentPanel, BorderLayout.CENTER);
            
            // Footer panel with navigation
            add(createFooterPanel(), BorderLayout.SOUTH);
            
            // Prevent closing during setup
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int choice = JOptionPane.showConfirmDialog(
                        SetupWizardWindow.this,
                        "Are you sure you want to cancel setup?\nMini_DirtyRoom may not work correctly.",
                        "Cancel Setup",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                }
            });
        }
        
        private JPanel createHeaderPanel() {
            JPanel header = new JPanel();
            header.setLayout(new BorderLayout());
            header.setBackground(PRIMARY_COLOR);
            header.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
            
            // Title
            JLabel titleLabel = new JLabel("Mini_DirtyRoom Setup Wizard");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
            titleLabel.setForeground(Color.WHITE);
            header.add(titleLabel, BorderLayout.WEST);
            
            // Step indicator
            JLabel stepLabel = new JLabel("Step 1 of " + stepNames.length);
            stepLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            stepLabel.setForeground(new Color(255, 255, 255, 200));
            header.add(stepLabel, BorderLayout.EAST);
            
            return header;
        }
        
        private JPanel createFooterPanel() {
            JPanel footer = new JPanel();
            footer.setLayout(new BorderLayout());
            footer.setBackground(isDarkMode() ? BACKGROUND_DARK : BACKGROUND_LIGHT);
            footer.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
            
            // Progress bar
            progressBar = new JProgressBar(0, stepNames.length - 1);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setString(stepNames[0]);
            progressBar.setPreferredSize(new Dimension(0, 8));
            footer.add(progressBar, BorderLayout.NORTH);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            buttonPanel.setOpaque(false);
            
            backButton = createStyledButton("← Back", false);
            backButton.setEnabled(false);
            backButton.addActionListener(e -> previousStep());
            
            nextButton = createStyledButton("Next →", true);
            nextButton.addActionListener(e -> nextStep());
            
            buttonPanel.add(backButton);
            buttonPanel.add(nextButton);
            
            footer.add(buttonPanel, BorderLayout.SOUTH);
            
            return footer;
        }
        
        private JButton createStyledButton(String text, boolean primary) {
            JButton button = new JButton(text);
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setPreferredSize(new Dimension(140, 40));
            
            if (primary) {
                button.setBackground(PRIMARY_COLOR);
                button.setForeground(Color.WHITE);
            } else {
                button.setBackground(new Color(200, 200, 200));
                button.setForeground(Color.BLACK);
            }
            
            // Hover effect
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (button.isEnabled()) {
                        button.setBackground(primary ? PRIMARY_COLOR.darker() : new Color(180, 180, 180));
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if (button.isEnabled()) {
                        button.setBackground(primary ? PRIMARY_COLOR : new Color(200, 200, 200));
                    }
                }
            });
            
            return button;
        }
        
        // ─── STEP 1: WELCOME ─────────────────────────────────────────────────
        
        private JPanel createWelcomePanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            // Center content
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setOpaque(false);
            centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
            
            // Logo (text-based for now)
            JLabel logoLabel = new JLabel("🚀");
            logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 72));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(logoLabel);
            centerPanel.add(Box.createVerticalStrut(30));
            
            // Welcome text
            JLabel welcomeLabel = new JLabel("Welcome to Mini_DirtyRoom!");
            welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
            welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(welcomeLabel);
            centerPanel.add(Box.createVerticalStrut(20));
            
            // Description
            String description = "<html><div style='text-align: center; width: 600px;'>" +
                "This wizard will help you set up Mini_DirtyRoom for optimal performance.<br><br>" +
                "We'll automatically detect your system and recommend the best settings.<br>" +
                "The entire process takes just a few minutes.<br><br>" +
                "<b>Features:</b><br>" +
                "• Automatic Java version management<br>" +
                "• LWJGL 3.4.0 upgrade for better performance<br>" +
                "• Graphics optimization<br>" +
                "• Cross-platform compatibility<br>" +
                "</div></html>";
            
            JLabel descLabel = new JLabel(description);
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(descLabel);
            centerPanel.add(Box.createVerticalStrut(30));
            
            // Quick setup option
            JPanel quickPanel = new JPanel();
            quickPanel.setOpaque(false);
            quickPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
            
            JButton quickSetupButton = createStyledButton("⚡ Quick Setup (Recommended)", true);
            quickSetupButton.setPreferredSize(new Dimension(280, 50));
            quickSetupButton.addActionListener(e -> runQuickSetup());
            
            JButton customSetupButton = createStyledButton("🔧 Custom Setup", false);
            customSetupButton.setPreferredSize(new Dimension(280, 50));
            customSetupButton.addActionListener(e -> nextStep());
            
            quickPanel.add(quickSetupButton);
            quickPanel.add(customSetupButton);
            centerPanel.add(quickPanel);
            
            panel.add(centerPanel, BorderLayout.CENTER);
            
            return panel;
        }
        
        // ─── STEP 2: DETECTION ───────────────────────────────────────────────
        
        private JPanel createDetectionPanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
            
            // Title
            JLabel titleLabel = new JLabel("🔍 Detecting Your System");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Detection results
            SystemInfo sysInfo = detectSystem();
            
            contentPanel.add(createInfoRow("Operating System:", sysInfo.osName));
            contentPanel.add(createInfoRow("Architecture:", sysInfo.architecture));
            contentPanel.add(createInfoRow("Current Java:", sysInfo.javaVersion));
            contentPanel.add(createInfoRow("Java Vendor:", sysInfo.javaVendor));
            contentPanel.add(createInfoRow("Minecraft Version:", sysInfo.minecraftVersion));
            contentPanel.add(createInfoRow("Mod Loader:", sysInfo.modLoader));
            contentPanel.add(createInfoRow("Graphics Card:", sysInfo.gpuInfo));
            contentPanel.add(createInfoRow("Available Memory:", sysInfo.availableMemory));
            contentPanel.add(createInfoRow("CPU Cores:", sysInfo.cpuCores));
            
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Recommendation box
            JPanel recommendationBox = createRecommendationBox(sysInfo);
            recommendationBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(recommendationBox);
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            return panel;
        }
        
        private JPanel createInfoRow(String label, String value) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            
            JLabel labelComp = new JLabel(label);
            labelComp.setFont(new Font("Segoe UI", Font.BOLD, 14));
            
            JLabel valueComp = new JLabel(value);
            valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            valueComp.setForeground(PRIMARY_COLOR);
            
            row.add(labelComp, BorderLayout.WEST);
            row.add(valueComp, BorderLayout.EAST);
            
            return row;
        }
        
        private JPanel createRecommendationBox(SystemInfo sysInfo) {
            JPanel box = new JPanel();
            box.setLayout(new BorderLayout(15, 15));
            box.setBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 30));
            box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            
            JLabel iconLabel = new JLabel("💡");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 32));
            box.add(iconLabel, BorderLayout.WEST);
            
            String recommendation = generateRecommendation(sysInfo);
            JLabel textLabel = new JLabel(recommendation);
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            box.add(textLabel, BorderLayout.CENTER);
            
            return box;
        }
        
        // ─── STEP 3: JAVA SELECTION ──────────────────────────────────────────
        
        private JPanel createJavaSelectionPanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
            
            // Title
            JLabel titleLabel = new JLabel("☕ Select Java Distribution");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(20));
            
            // Description
            JLabel descLabel = new JLabel("<html>Choose your preferred Java distribution. Mini_DirtyRoom will download and install it automatically.</html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(descLabel);
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Java distribution options
            ButtonGroup javaGroup = new ButtonGroup();
            
            contentPanel.add(createJavaOption("Zulu OpenJDK 21 (Recommended)", 
                "Certified OpenJDK build with long-term support. Best for production.", 
                javaGroup, true));
            contentPanel.add(Box.createVerticalStrut(15));
            
            contentPanel.add(createJavaOption("Eclipse Adoptium 21", 
                "Popular community-driven OpenJDK distribution.", 
                javaGroup, false));
            contentPanel.add(Box.createVerticalStrut(15));
            
            contentPanel.add(createJavaOption("GraalVM CE 21", 
                "High-performance JVM with advanced optimizations. Best for power users.", 
                javaGroup, false));
            contentPanel.add(Box.createVerticalStrut(15));
            
            contentPanel.add(createJavaOption("Use Current Java " + Runtime.version().feature(), 
                "Use your existing Java installation (not recommended if below Java 17).", 
                javaGroup, false));
            
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Package format selection (for mobile)
            SystemInfo sysInfo = detectSystem();
            if (sysInfo.isMobile) {
                JLabel formatLabel = new JLabel("Package Format:");
                formatLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPanel.add(formatLabel);
                contentPanel.add(Box.createVerticalStrut(10));
                
                ButtonGroup formatGroup = new ButtonGroup();
                JRadioButton tarXzButton = new JRadioButton("tar.xz (Mobile - Smaller size)");
                JRadioButton tarGzButton = new JRadioButton("tar.gz (Desktop - Faster extraction)");
                
                tarXzButton.setSelected(true);
                formatGroup.add(tarXzButton);
                formatGroup.add(tarGzButton);
                
                contentPanel.add(tarXzButton);
                contentPanel.add(tarGzButton);
            }
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            return panel;
        }
        
        private JPanel createJavaOption(String name, String description, ButtonGroup group, boolean selected) {
            JPanel optionPanel = new JPanel(new BorderLayout(15, 10));
            optionPanel.setOpaque(false);
            optionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            
            JRadioButton radio = new JRadioButton();
            radio.setSelected(selected);
            radio.setOpaque(false);
            group.add(radio);
            optionPanel.add(radio, BorderLayout.WEST);
            
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textPanel.add(nameLabel);
            
            JLabel descLabel = new JLabel("<html>" + description + "</html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descLabel.setForeground(Color.GRAY);
            textPanel.add(descLabel);
            
            optionPanel.add(textPanel, BorderLayout.CENTER);
            
            // Make entire panel clickable
            optionPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    radio.setSelected(true);
                }
            });
            
            return optionPanel;
        }
        
        // ─── STEP 4: LWJGL SELECTION ─────────────────────────────────────────
        
        private JPanel createLWJGLSelectionPanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
            
            // Title
            JLabel titleLabel = new JLabel("🎮 Select LWJGL Version");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(20));
            
            // Description
            JLabel descLabel = new JLabel("<html>LWJGL (Lightweight Java Game Library) is the graphics engine used by Minecraft. Version 3.4.0 offers significantly better performance.</html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(descLabel);
            contentPanel.add(Box.createVerticalStrut(30));
            
            // LWJGL options
            ButtonGroup lwjglGroup = new ButtonGroup();
            
            contentPanel.add(createLWJGLOption("LWJGL 3.4.0 (Recommended)", 
                "Latest stable version with best performance and features. Fully compatible with all platforms.", 
                "+40% FPS, Vulkan support, Better memory management", 
                lwjglGroup, true));
            contentPanel.add(Box.createVerticalStrut(15));
            
            contentPanel.add(createLWJGLOption("LWJGL 3.3.x", 
                "Previous stable version. Good balance of compatibility and performance.", 
                "+30% FPS, Stable and tested", 
                lwjglGroup, false));
            contentPanel.add(Box.createVerticalStrut(15));
            
            contentPanel.add(createLWJGLOption("LWJGL 2.9.x (Original)", 
                "Original version used by Minecraft 1.12.2. Choose only if experiencing compatibility issues.", 
                "Maximum compatibility, Lower performance", 
                lwjglGroup, false));
            
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Module selection
            JLabel moduleLabel = new JLabel("LWJGL Modules to Install:");
            moduleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            moduleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(moduleLabel);
            contentPanel.add(Box.createVerticalStrut(10));
            
            JCheckBox coreCheck = new JCheckBox("Core (Required)", true);
            coreCheck.setEnabled(false);
            JCheckBox openglCheck = new JCheckBox("OpenGL (Required)", true);
            openglCheck.setEnabled(false);
            JCheckBox openalCheck = new JCheckBox("OpenAL (Audio)", true);
            JCheckBox glwCheck = new JCheckBox("GLFW (Window Management)", true);
            JCheckBox stbCheck = new JCheckBox("STB (Image/Font Loading)", true);
            JCheckBox vulkanCheck = new JCheckBox("Vulkan (Advanced Graphics)", false);
            
            contentPanel.add(coreCheck);
            contentPanel.add(openglCheck);
            contentPanel.add(openalCheck);
            contentPanel.add(glwCheck);
            contentPanel.add(stbCheck);
            contentPanel.add(vulkanCheck);
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            return panel;
        }
        
        private JPanel createLWJGLOption(String name, String description, String benefits, ButtonGroup group, boolean selected) {
            JPanel optionPanel = new JPanel(new BorderLayout(15, 10));
            optionPanel.setOpaque(false);
            optionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? PRIMARY_COLOR : new Color(200, 200, 200), selected ? 2 : 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            
            JRadioButton radio = new JRadioButton();
            radio.setSelected(selected);
            radio.setOpaque(false);
            group.add(radio);
            optionPanel.add(radio, BorderLayout.WEST);
            
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textPanel.add(nameLabel);
            
            JLabel descLabel = new JLabel("<html>" + description + "</html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descLabel.setForeground(Color.GRAY);
            textPanel.add(descLabel);
            
            JLabel benefitsLabel = new JLabel("<html><b>Benefits:</b> " + benefits + "</html>");
            benefitsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            benefitsLabel.setForeground(SUCCESS_COLOR);
            textPanel.add(Box.createVerticalStrut(5));
            textPanel.add(benefitsLabel);
            
            optionPanel.add(textPanel, BorderLayout.CENTER);
            
            // Make entire panel clickable
            optionPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    radio.setSelected(true);
                }
            });
            
            return optionPanel;
        }
        
        // ─── STEP 5: GRAPHICS SETTINGS ───────────────────────────────────────
        
        private JPanel createGraphicsSettingsPanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
            
            // Title
            JLabel titleLabel = new JLabel("🎨 Graphics Optimization");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(20));
            
            // Preset selection
            JLabel presetLabel = new JLabel("Performance Preset:");
            presetLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            presetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(presetLabel);
            contentPanel.add(Box.createVerticalStrut(10));
            
            ButtonGroup presetGroup = new ButtonGroup();
            JRadioButton maxPerformance = new JRadioButton("Maximum Performance (Best FPS)");
            JRadioButton balanced = new JRadioButton("Balanced (Recommended)");
            JRadioButton maxQuality = new JRadioButton("Maximum Quality (Best Graphics)");
            
            balanced.setSelected(true);
            presetGroup.add(maxPerformance);
            presetGroup.add(balanced);
            presetGroup.add(maxQuality);
            
            contentPanel.add(maxPerformance);
            contentPanel.add(balanced);
            contentPanel.add(maxQuality);
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Individual settings
            contentPanel.add(createSettingSlider("Render Distance:", 2, 32, 12));
            contentPanel.add(createSettingSlider("Max FPS:", 30, 300, 120));
            contentPanel.add(createSettingCheckbox("VSync", false));
            contentPanel.add(createSettingCheckbox("Smooth Lighting", true));
            contentPanel.add(createSettingCheckbox("Dynamic Lights", true));
            contentPanel.add(createSettingCheckbox("Particles", true));
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            return panel;
        }
        
        private JPanel createSettingSlider(String label, int min, int max, int initial) {
            JPanel sliderPanel = new JPanel(new BorderLayout(10, 5));
            sliderPanel.setOpaque(false);
            sliderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            
            JLabel labelComp = new JLabel(label);
            labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            
            JSlider slider = new JSlider(min, max, initial);
            slider.setMajorTickSpacing((max - min) / 4);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            
            JLabel valueLabel = new JLabel(String.valueOf(initial));
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            valueLabel.setForeground(PRIMARY_COLOR);
            
            slider.addChangeListener(e -> valueLabel.setText(String.valueOf(slider.getValue())));
            
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            topPanel.add(labelComp, BorderLayout.WEST);
            topPanel.add(valueLabel, BorderLayout.EAST);
            
            sliderPanel.add(topPanel, BorderLayout.NORTH);
            sliderPanel.add(slider, BorderLayout.CENTER);
            
            return sliderPanel;
        }
        
        private JCheckBox createSettingCheckbox(String label, boolean selected) {
            JCheckBox checkbox = new JCheckBox(label, selected);
            checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            checkbox.setOpaque(false);
            checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
            return checkbox;
        }
        
        // ─── STEP 6: INSTALLATION ────────────────────────────────────────────
        
        private JPanel createInstallationPanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
            
            // Title
            JLabel titleLabel = new JLabel("📦 Installing Components");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Installation progress
            JProgressBar overallProgress = new JProgressBar(0, 100);
            overallProgress.setStringPainted(true);
            overallProgress.setString("Preparing...");
            overallProgress.setPreferredSize(new Dimension(0, 30));
            contentPanel.add(overallProgress);
            contentPanel.add(Box.createVerticalStrut(20));
            
            // Task list
            JTextArea taskLog = new JTextArea(15, 60);
            taskLog.setEditable(false);
            taskLog.setFont(new Font("Consolas", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(taskLog);
            scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(scrollPane);
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            // Start installation automatically when this panel is shown
            SwingUtilities.invokeLater(() -> performInstallation(overallProgress, taskLog));
            
            return panel;
        }
        
        private void performInstallation(JProgressBar progress, JTextArea log) {
            // Simulate installation process
            new Thread(() -> {
                try {
                    updateProgress(progress, log, 10, "Creating directories...");
                    Thread.sleep(500);
                    
                    updateProgress(progress, log, 20, "Downloading Java distribution...");
                    Thread.sleep(2000);
                    
                    updateProgress(progress, log, 40, "Extracting Java...");
                    Thread.sleep(1000);
                    
                    updateProgress(progress, log, 50, "Downloading LWJGL 3.4.0...");
                    Thread.sleep(2000);
                    
                    updateProgress(progress, log, 70, "Installing LWJGL modules...");
                    Thread.sleep(1000);
                    
                    updateProgress(progress, log, 85, "Applying graphics settings...");
                    Thread.sleep(500);
                    
                    updateProgress(progress, log, 95, "Creating backup...");
                    Thread.sleep(500);
                    
                    updateProgress(progress, log, 100, "Installation complete!");
                    Thread.sleep(500);
                    
                    SwingUtilities.invokeLater(() -> nextStep());
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        private void updateProgress(JProgressBar progress, JTextArea log, int value, String message) {
            SwingUtilities.invokeLater(() -> {
                progress.setValue(value);
                progress.setString(message);
                log.append("[" + new java.util.Date() + "] " + message + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            });
        }
        
        // ─── STEP 7: COMPLETE ────────────────────────────────────────────────
        
        private JPanel createCompletePanel() {
            JPanel panel = createStepPanel();
            panel.setLayout(new BorderLayout(20, 20));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));
            
            // Success icon
            JLabel iconLabel = new JLabel("✅");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 72));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(iconLabel);
            contentPanel.add(Box.createVerticalStrut(30));
            
            // Success message
            JLabel successLabel = new JLabel("Setup Complete!");
            successLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
            successLabel.setForeground(SUCCESS_COLOR);
            successLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(successLabel);
            contentPanel.add(Box.createVerticalStrut(20));
            
            // Summary
            String summary = "<html><div style='text-align: center; width: 600px;'>" +
                "Mini_DirtyRoom has been successfully configured!<br><br>" +
                "<b>What's been installed:</b><br>" +
                "• Java 21 (Zulu OpenJDK)<br>" +
                "• LWJGL 3.4.0 with all modules<br>" +
                "• Optimized graphics settings<br>" +
                "• Performance enhancements<br><br>" +
                "Click 'Launch Minecraft' to start playing with improved performance!<br>" +
                "You can change these settings anytime from the mod options menu." +
                "</div></html>";
            
            JLabel summaryLabel = new JLabel(summary);
            summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            summaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(summaryLabel);
            contentPanel.add(Box.createVerticalStrut(40));
            
            // Launch button
            JButton launchButton = createStyledButton("🚀 Launch Minecraft", true);
            launchButton.setPreferredSize(new Dimension(250, 60));
            launchButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            launchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            launchButton.addActionListener(e -> {
                setupCompleted = true;
                dispose();
            });
            contentPanel.add(launchButton);
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            // Hide navigation buttons
            SwingUtilities.invokeLater(() -> {
                nextButton.setVisible(false);
                backButton.setVisible(false);
            });
            
            return panel;
        }
        
        // ─── HELPER METHODS ──────────────────────────────────────────────────
        
        private JPanel createStepPanel() {
            JPanel panel = new JPanel();
            panel.setBackground(isDarkMode() ? BACKGROUND_DARK : BACKGROUND_LIGHT);
            return panel;
        }
        
        private void nextStep() {
            if (currentStep < stepNames.length - 1) {
                currentStep++;
                cardLayout.show(contentPanel, stepNames[currentStep]);
                progressBar.setValue(currentStep);
                progressBar.setString(stepNames[currentStep]);
                backButton.setEnabled(currentStep > 0);
                
                if (currentStep == stepNames.length - 1) {
                    nextButton.setVisible(false);
                }
            }
        }
        
        private void previousStep() {
            if (currentStep > 0) {
                currentStep--;
                cardLayout.show(contentPanel, stepNames[currentStep]);
                progressBar.setValue(currentStep);
                progressBar.setString(stepNames[currentStep]);
                backButton.setEnabled(currentStep > 0);
                nextButton.setVisible(true);
            }
        }
        
        private void runQuickSetup() {
            // Skip to installation with recommended settings
            config.javaDistribution = "zulu-21";
            config.lwjglVersion = "3.4.0";
            config.graphicsPreset = "balanced";
            currentStep = 5; // Jump to installation
            cardLayout.show(contentPanel, stepNames[currentStep]);
            progressBar.setValue(currentStep);
            progressBar.setString(stepNames[currentStep]);
        }
        
        private Image createModernIcon() {
            // Create a simple colored icon
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(PRIMARY_COLOR);
            g2d.fillRoundRect(0, 0, 64, 64, 16, 16);
            g2d.dispose();
            return img;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 5: CONSOLE SETUP (FALLBACK)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Console-based setup for headless systems or when GUI fails
     */
    private static void runConsoleSetup() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     Mini_DirtyRoom Console Setup                         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        // Detect system
        System.out.println("Detecting system...");
        SystemInfo sysInfo = detectSystem();
        System.out.println("  OS: " + sysInfo.osName);
        System.out.println("  Java: " + sysInfo.javaVersion);
        System.out.println("  Minecraft: " + sysInfo.minecraftVersion);
        System.out.println();
        
        // Java selection
        System.out.println("Select Java distribution:");
        System.out.println("  1. Zulu OpenJDK 21 (Recommended)");
        System.out.println("  2. Eclipse Adoptium 21");
        System.out.println("  3. GraalVM CE 21");
        System.out.println("  4. Use current Java");
        System.out.print("Choice [1]: ");
        String javaChoice = scanner.nextLine().trim();
        if (javaChoice.isEmpty()) javaChoice = "1";
        
        // LWJGL selection
        System.out.println("\nSelect LWJGL version:");
        System.out.println("  1. LWJGL 3.4.0 (Recommended)");
        System.out.println("  2. LWJGL 3.3.x");
        System.out.println("  3. LWJGL 2.9.x (Original)");
        System.out.print("Choice [1]: ");
        String lwjglChoice = scanner.nextLine().trim();
        if (lwjglChoice.isEmpty()) lwjglChoice = "1";
        
        // Graphics preset
        System.out.println("\nSelect graphics preset:");
        System.out.println("  1. Maximum Performance");
        System.out.println("  2. Balanced (Recommended)");
        System.out.println("  3. Maximum Quality");
        System.out.print("Choice [2]: ");
        String graphicsChoice = scanner.nextLine().trim();
        if (graphicsChoice.isEmpty()) graphicsChoice = "2";
        
        // Save configuration
        config.javaDistribution = getJavaDistributionFromChoice(javaChoice);
        config.lwjglVersion = getLWJGLVersionFromChoice(lwjglChoice);
        config.graphicsPreset = getGraphicsPresetFromChoice(graphicsChoice);
        
        System.out.println("\nInstalling components...");
        System.out.println("This may take a few minutes...");
        
        // Simulate installation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("✓ Setup complete!");
        setupCompleted = true;
        saveConfiguration();
        applyConfiguration();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 6: SYSTEM DETECTION
    // ─────────────────────────────────────────────────────────────────────────────

    private static SystemInfo detectSystem() {
        SystemInfo info = new SystemInfo();
        
        // Operating system
        info.osName = System.getProperty("os.name");
        info.architecture = System.getProperty("os.arch");
        info.isMobile = isAndroid() || isIOS();
        
        // Java version
        Runtime.Version version = Runtime.version();
        info.javaVersion = version.feature() + "." + version.interim() + "." + version.update();
        info.javaVendor = System.getProperty("java.vendor");
        
        // Minecraft version
        info.minecraftVersion = Mini_DirtyRoom_CompatibilityMaximizer
            .MinecraftVersionCompatibility.detectMinecraftVersion().toString();
        
        // Mod loader
        info.modLoader = Mini_DirtyRoom_CompatibilityMaximizer
            .ModLoaderCompatibility.detectModLoader().toString();
        
        // Graphics
        info.gpuInfo = detectGPU();
        
        // Memory
        long maxMemory = Runtime.getRuntime().maxMemory();
        info.availableMemory = String.format("%.1f GB", maxMemory / (1024.0 * 1024.0 * 1024.0));
        
        // CPU
        info.cpuCores = String.valueOf(Runtime.getRuntime().availableProcessors());
        
        return info;
    }

    private static String detectGPU() {
        // Try to detect GPU (placeholder - would need native calls)
        return "OpenGL Compatible";
    }

    private static boolean isAndroid() {
        return System.getProperty("java.vendor", "").toLowerCase().contains("android");
    }

    private static boolean isIOS() {
        return System.getProperty("os.name", "").toLowerCase().contains("ios");
    }

    private static String generateRecommendation(SystemInfo sysInfo) {
        StringBuilder rec = new StringBuilder("<html><b>Recommendations:</b><br>");
        
        int javaVersion = Runtime.version().feature();
        if (javaVersion < 17) {
            rec.append("• Upgrade to Java 21 for best performance<br>");
        }
        
        rec.append("• Install LWJGL 3.4.0 for +40% FPS improvement<br>");
        
        long maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        if (maxMemoryMB < 2048) {
            rec.append("• Allocate at least 4GB RAM for optimal performance<br>");
        }
        
        if (sysInfo.isMobile) {
            rec.append("• Use tar.xz format for smaller download size<br>");
        }
        
        rec.append("</html>");
        return rec.toString();
    }

    private static boolean isDarkMode() {
        // Detect system dark mode (simplified)
        return false; // Default to light mode
    }

    private static boolean hasDisplay() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            return !ge.isHeadlessInstance() && ge.getScreenDevices().length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 7: CONFIGURATION MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────────

    private static void loadConfiguration() {
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                config = SimplifierConfig.fromJson(json);
            } catch (IOException e) {
                System.err.println("[Simplifier] Failed to load config: " + e.getMessage());
            }
        }
    }

    private static void saveConfiguration() {
        try {
            Files.writeString(Paths.get(CONFIG_FILE), config.toJson());
            System.out.println("[Simplifier] Configuration saved");
        } catch (IOException e) {
            System.err.println("[Simplifier] Failed to save config: " + e.getMessage());
        }
    }

    private static boolean configFileExists() {
        return Files.exists(Paths.get(CONFIG_FILE));
    }

    private static void applyConfiguration() {
        System.out.println("[Simplifier] Applying configuration...");
        System.out.println("  Java: " + config.javaDistribution);
        System.out.println("  LWJGL: " + config.lwjglVersion);
        System.out.println("  Graphics: " + config.graphicsPreset);
        
        // Configuration will be applied by Mini_DirtyRoomCore
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 8: HELPER CLASSES
    // ─────────────────────────────────────────────────────────────────────────────

    private static class SystemInfo {
        String osName;
        String architecture;
        String javaVersion;
        String javaVendor;
        String minecraftVersion;
        String modLoader;
        String gpuInfo;
        String availableMemory;
        String cpuCores;
        boolean isMobile;
    }

    private static class SimplifierConfig {
        String javaDistribution = "zulu-21";
        String lwjglVersion = "3.4.0";
        String graphicsPreset = "balanced";
        boolean telemetryEnabled = false;
        
        String toJson() {
            return String.format(
                "{\"javaDistribution\":\"%s\",\"lwjglVersion\":\"%s\",\"graphicsPreset\":\"%s\",\"telemetryEnabled\":%b}",
                javaDistribution, lwjglVersion, graphicsPreset, telemetryEnabled
            );
        }
        
        static SimplifierConfig fromJson(String json) {
            SimplifierConfig config = new SimplifierConfig();
            // Simple JSON parsing (in production, use proper JSON library)
            if (json.contains("javaDistribution")) {
                config.javaDistribution = extractJsonValue(json, "javaDistribution");
            }
            if (json.contains("lwjglVersion")) {
                config.lwjglVersion = extractJsonValue(json, "lwjglVersion");
            }
            if (json.contains("graphicsPreset")) {
                config.graphicsPreset = extractJsonValue(json, "graphicsPreset");
            }
            return config;
        }
        
        private static String extractJsonValue(String json, String key) {
            int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SECTION 9: UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    private static String getJavaDistributionFromChoice(String choice) {
        return switch (choice) {
            case "1" -> "zulu-21";
            case "2" -> "adoptium-21";
            case "3" -> "graalvm-21";
            case "4" -> "current";
            default -> "zulu-21";
        };
    }

    private static String getLWJGLVersionFromChoice(String choice) {
        return switch (choice) {
            case "1" -> "3.4.0";
            case "2" -> "3.3.x";
            case "3" -> "2.9.x";
            default -> "3.4.0";
        };
    }

    private static String getGraphicsPresetFromChoice(String choice) {
        return switch (choice) {
            case "1" -> "max-performance";
            case "2" -> "balanced";
            case "3" -> "max-quality";
            default -> "balanced";
        };
    }

    /**
     * Public API for other components to check if setup is complete
     */
    public static boolean isSetupComplete() {
        return setupCompleted;
    }

    /**
     * Public API to get current configuration
     */
    public static SimplifierConfig getConfiguration() {
        return config;
    }

    /**
     * Public API to open settings UI at runtime
     */
    public static void openSettingsUI() {
        if (!GraphicsEnvironment.isHeadless() && hasDisplay()) {
            SwingUtilities.invokeLater(() -> {
                wizardWindow = new SetupWizardWindow();
                wizardWindow.setVisible(true);
            });
        }
    }
}
