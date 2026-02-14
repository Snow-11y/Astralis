package stellar.snow.astralis.engine.render.debug;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * DebugHUDManager - In-Game Debug HUD System
 * 
 * Provides a comprehensive debug overlay with:
 * - Nested context menus
 * - Real-time performance graphs (FPS, frame time, memory)
 * - Custom metrics and watches
 * - Interactive toggles and sliders
 * - Text annotations
 * 
 * Features:
 * - Hierarchical organization
 * - Hot-reloading values
 * - Performance history tracking
 * - Color-coded warnings
 * - Customizable layout
 */
public final class DebugHUDManager {
    
    /**
     * HUD element types
     */
    public enum ElementType {
        TEXT,
        VALUE_WATCH,
        TOGGLE,
        SLIDER,
        BUTTON,
        GRAPH,
        SEPARATOR,
        SUBMENU
    }
    
    /**
     * Base HUD element
     */
    public static abstract class HUDElement {
        protected final String id;
        protected String label;
        protected ElementType type;
        protected boolean visible = true;
        protected HUDContext parent;
        
        public HUDElement(String id, String label, ElementType type) {
            this.id = id;
            this.label = label;
            this.type = type;
        }
        
        public String getId() { return id; }
        public String getLabel() { return label; }
        public ElementType getType() { return type; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean v) { visible = v; }
        
        public abstract String render();
    }
    
    /**
     * Text element
     */
    public static class TextElement extends HUDElement {
        private String text;
        private Supplier<String> textSupplier;
        
        public TextElement(String id, String label, String text) {
            super(id, label, ElementType.TEXT);
            this.text = text;
        }
        
        public TextElement(String id, String label, Supplier<String> supplier) {
            super(id, label, ElementType.TEXT);
            this.textSupplier = supplier;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        @Override
        public String render() {
            String value = textSupplier != null ? textSupplier.get() : text;
            return String.format("%s: %s", label, value);
        }
    }
    
    /**
     * Value watch element (monitors a changing value)
     */
    public static class ValueWatch<T> extends HUDElement {
        private T value;
        private Supplier<T> valueSupplier;
        private String format;
        private Predicate<T> warningCondition;
        
        public ValueWatch(String id, String label, Supplier<T> supplier, String format) {
            super(id, label, ElementType.VALUE_WATCH);
            this.valueSupplier = supplier;
            this.format = format;
        }
        
        public void setWarningCondition(Predicate<T> condition) {
            this.warningCondition = condition;
        }
        
        @Override
        public String render() {
            T val = valueSupplier.get();
            String formatted = String.format(format, val);
            
            boolean warning = warningCondition != null && warningCondition.test(val);
            String prefix = warning ? "[!] " : "";
            
            return String.format("%s%s: %s", prefix, label, formatted);
        }
    }
    
    /**
     * Toggle element
     */
    public static class ToggleElement extends HUDElement {
        private boolean state;
        private Consumer<Boolean> onChange;
        
        public ToggleElement(String id, String label, boolean initialState, Consumer<Boolean> onChange) {
            super(id, label, ElementType.TOGGLE);
            this.state = initialState;
            this.onChange = onChange;
        }
        
        public void toggle() {
            state = !state;
            if (onChange != null) {
                onChange.accept(state);
            }
        }
        
        public boolean getState() {
            return state;
        }
        
        @Override
        public String render() {
            return String.format("[%s] %s", state ? "X" : " ", label);
        }
    }
    
    /**
     * Slider element
     */
    public static class SliderElement extends HUDElement {
        private float value;
        private final float min, max, step;
        private Consumer<Float> onChange;
        
        public SliderElement(String id, String label, float initialValue, 
                           float min, float max, float step, Consumer<Float> onChange) {
            super(id, label, ElementType.SLIDER);
            this.value = Math.max(min, Math.min(max, initialValue));
            this.min = min;
            this.max = max;
            this.step = step;
            this.onChange = onChange;
        }
        
        public void increment() {
            setValue(value + step);
        }
        
        public void decrement() {
            setValue(value - step);
        }
        
        public void setValue(float newValue) {
            value = Math.max(min, Math.min(max, newValue));
            if (onChange != null) {
                onChange.accept(value);
            }
        }
        
        public float getValue() {
            return value;
        }
        
        @Override
        public String render() {
            float normalized = (value - min) / (max - min);
            int barLength = 20;
            int filled = (int)(normalized * barLength);
            
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < barLength; i++) {
                bar.append(i < filled ? "=" : " ");
            }
            bar.append("]");
            
            return String.format("%s: %s %.2f", label, bar.toString(), value);
        }
    }
    
    /**
     * Button element
     */
    public static class ButtonElement extends HUDElement {
        private Runnable onClick;
        
        public ButtonElement(String id, String label, Runnable onClick) {
            super(id, label, ElementType.BUTTON);
            this.onClick = onClick;
        }
        
        public void click() {
            if (onClick != null) {
                onClick.run();
            }
        }
        
        @Override
        public String render() {
            return String.format("[ %s ]", label);
        }
    }
    
    /**
     * Graph element for time-series data
     */
    public static class GraphElement extends HUDElement {
        private final int maxSamples;
        private final LinkedList<Float> samples = new LinkedList<>();
        private Supplier<Float> valueSupplier;
        private float minValue = Float.MAX_VALUE;
        private float maxValue = Float.MIN_VALUE;
        
        public GraphElement(String id, String label, int maxSamples, Supplier<Float> supplier) {
            super(id, label, ElementType.GRAPH);
            this.maxSamples = maxSamples;
            this.valueSupplier = supplier;
        }
        
        public void update() {
            float value = valueSupplier.get();
            samples.addLast(value);
            
            if (samples.size() > maxSamples) {
                samples.removeFirst();
            }
            
            // Update min/max
            minValue = Float.MAX_VALUE;
            maxValue = Float.MIN_VALUE;
            for (float sample : samples) {
                minValue = Math.min(minValue, sample);
                maxValue = Math.max(maxValue, sample);
            }
        }
        
        @Override
        public String render() {
            if (samples.isEmpty()) {
                return label + ": [No data]";
            }
            
            int height = 5;
            int width = Math.min(samples.size(), 50);
            
            StringBuilder sb = new StringBuilder();
            sb.append(label).append(String.format(" (min=%.2f, max=%.2f, avg=%.2f)\n",
                minValue, maxValue, getAverage()));
            
            // Simple ASCII graph
            for (int row = height - 1; row >= 0; row--) {
                float threshold = minValue + (maxValue - minValue) * row / (height - 1);
                for (int col = 0; col < width; col++) {
                    int sampleIdx = samples.size() - width + col;
                    if (sampleIdx >= 0 && sampleIdx < samples.size()) {
                        float value = samples.get(sampleIdx);
                        sb.append(value >= threshold ? "█" : " ");
                    } else {
                        sb.append(" ");
                    }
                }
                sb.append("\n");
            }
            
            return sb.toString();
        }
        
        private float getAverage() {
            if (samples.isEmpty()) return 0;
            float sum = 0;
            for (float s : samples) sum += s;
            return sum / samples.size();
        }
        
        public List<Float> getSamples() {
            return new ArrayList<>(samples);
        }
    }
    
    /**
     * Separator element
     */
    public static class SeparatorElement extends HUDElement {
        private final int length;
        
        public SeparatorElement(String id, int length) {
            super(id, "separator", ElementType.SEPARATOR);
            this.length = length;
        }
        
        @Override
        public String render() {
            return "─".repeat(length);
        }
    }
    
    /**
     * HUD Context (container for elements)
     */
    public static class HUDContext {
        private final String name;
        private final Map<String, HUDElement> elements = new LinkedHashMap<>();
        private final Map<String, HUDContext> subcontexts = new LinkedHashMap<>();
        private boolean expanded = true;
        
        public HUDContext(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public TextElement addText(String id, String label, String text) {
            TextElement elem = new TextElement(id, label, text);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public TextElement addText(String id, String label, Supplier<String> supplier) {
            TextElement elem = new TextElement(id, label, supplier);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public <T> ValueWatch<T> addWatch(String id, String label, Supplier<T> supplier, String format) {
            ValueWatch<T> elem = new ValueWatch<>(id, label, supplier, format);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public ToggleElement addToggle(String id, String label, boolean initialState, Consumer<Boolean> onChange) {
            ToggleElement elem = new ToggleElement(id, label, initialState, onChange);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public SliderElement addSlider(String id, String label, float value, float min, float max, 
                                      float step, Consumer<Float> onChange) {
            SliderElement elem = new SliderElement(id, label, value, min, max, step, onChange);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public ButtonElement addButton(String id, String label, Runnable onClick) {
            ButtonElement elem = new ButtonElement(id, label, onClick);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public GraphElement addGraph(String id, String label, int maxSamples, Supplier<Float> supplier) {
            GraphElement elem = new GraphElement(id, label, maxSamples, supplier);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public SeparatorElement addSeparator(String id, int length) {
            SeparatorElement elem = new SeparatorElement(id, length);
            elem.parent = this;
            elements.put(id, elem);
            return elem;
        }
        
        public HUDContext addSubcontext(String name) {
            HUDContext ctx = new HUDContext(name);
            subcontexts.put(name, ctx);
            return ctx;
        }
        
        public HUDElement getElement(String id) {
            return elements.get(id);
        }
        
        public HUDContext getSubcontext(String name) {
            return subcontexts.get(name);
        }
        
        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }
        
        public boolean isExpanded() {
            return expanded;
        }
        
        public String render(int indent) {
            StringBuilder sb = new StringBuilder();
            String indentStr = "  ".repeat(indent);
            
            // Render context header
            String expandSymbol = expanded ? "▼" : "▶";
            sb.append(indentStr).append(expandSymbol).append(" ").append(name).append("\n");
            
            if (expanded) {
                // Render elements
                for (HUDElement elem : elements.values()) {
                    if (elem.isVisible()) {
                        sb.append(indentStr).append("  ").append(elem.render()).append("\n");
                    }
                }
                
                // Render subcontexts
                for (HUDContext ctx : subcontexts.values()) {
                    sb.append(ctx.render(indent + 1));
                }
            }
            
            return sb.toString();
        }
        
        public void updateGraphs() {
            for (HUDElement elem : elements.values()) {
                if (elem instanceof GraphElement) {
                    ((GraphElement) elem).update();
                }
            }
            for (HUDContext ctx : subcontexts.values()) {
                ctx.updateGraphs();
            }
        }
    }
    
    // Root contexts
    private final Map<String, HUDContext> rootContexts = new LinkedHashMap<>();
    
    // Global visibility
    private boolean hudVisible = true;
    
    // FPS history service (built-in)
    private final FpsHistory fpsHistory = new FpsHistory();
    
    public DebugHUDManager() {
        // Create default contexts
        HUDContext performance = createContext("Performance");
        performance.addGraph("fps", "FPS", 100, fpsHistory::getCurrentFps);
        performance.addGraph("frametime", "Frame Time (ms)", 100, fpsHistory::getCurrentFrameTime);
        performance.addWatch("avg_fps", "Avg FPS", fpsHistory::getAverageFps, "%.1f");
        performance.addWatch("min_fps", "Min FPS", fpsHistory::getMinFps, "%.1f");
        performance.addWatch("max_fps", "Max FPS", fpsHistory::getMaxFps, "%.1f");
        
        HUDContext memory = createContext("Memory");
        Runtime runtime = Runtime.getRuntime();
        memory.addWatch("used", "Used Memory", 
            () -> (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024), "%d MB");
        memory.addWatch("total", "Total Memory", 
            () -> runtime.totalMemory() / (1024 * 1024), "%d MB");
        memory.addWatch("max", "Max Memory", 
            () -> runtime.maxMemory() / (1024 * 1024), "%d MB");
    }
    
    /**
     * Create a root context
     */
    public HUDContext createContext(String name) {
        HUDContext ctx = new HUDContext(name);
        rootContexts.put(name, ctx);
        return ctx;
    }
    
    /**
     * Get a context by name
     */
    public HUDContext getContext(String name) {
        return rootContexts.get(name);
    }
    
    /**
     * Update FPS counter
     */
    public void recordFrame(float deltaTime) {
        fpsHistory.recordFrame(deltaTime);
    }
    
    /**
     * Update all graphs
     */
    public void update() {
        for (HUDContext ctx : rootContexts.values()) {
            ctx.updateGraphs();
        }
    }
    
    /**
     * Render the entire HUD
     */
    public String render() {
        if (!hudVisible) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("═══ DEBUG HUD ═══\n");
        
        for (HUDContext ctx : rootContexts.values()) {
            sb.append(ctx.render(0));
        }
        
        sb.append("═════════════════\n");
        return sb.toString();
    }
    
    /**
     * Set HUD visibility
     */
    public void setVisible(boolean visible) {
        hudVisible = visible;
    }
    
    /**
     * Toggle HUD visibility
     */
    public void toggleVisible() {
        hudVisible = !hudVisible;
    }
    
    /**
     * Get FPS history service
     */
    public FpsHistory getFpsHistory() {
        return fpsHistory;
    }
    
    /**
     * FPS History tracking service
     */
    public static class FpsHistory {
        private final int maxSamples = 300; // 5 seconds at 60 FPS
        private final LinkedList<Float> frameTimeSamples = new LinkedList<>();
        private float currentFps = 0;
        private float currentFrameTime = 0;
        
        public void recordFrame(float deltaTime) {
            currentFrameTime = deltaTime * 1000; // Convert to ms
            currentFps = 1.0f / deltaTime;
            
            frameTimeSamples.addLast(currentFrameTime);
            if (frameTimeSamples.size() > maxSamples) {
                frameTimeSamples.removeFirst();
            }
        }
        
        public float getCurrentFps() {
            return currentFps;
        }
        
        public float getCurrentFrameTime() {
            return currentFrameTime;
        }
        
        public float getAverageFps() {
            if (frameTimeSamples.isEmpty()) return 0;
            float avgFrameTime = 0;
            for (float ft : frameTimeSamples) {
                avgFrameTime += ft;
            }
            avgFrameTime /= frameTimeSamples.size();
            return 1000.0f / avgFrameTime;
        }
        
        public float getMinFps() {
            if (frameTimeSamples.isEmpty()) return 0;
            float maxFrameTime = Collections.max(frameTimeSamples);
            return 1000.0f / maxFrameTime;
        }
        
        public float getMaxFps() {
            if (frameTimeSamples.isEmpty()) return 0;
            float minFrameTime = Collections.min(frameTimeSamples);
            return 1000.0f / minFrameTime;
        }
        
        public List<Float> getFrameTimeSamples() {
            return new ArrayList<>(frameTimeSamples);
        }
    }
}
