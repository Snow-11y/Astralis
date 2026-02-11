package stellar.snow.astralis.api.shaders.Optimizers;

import java.nio.*;
import java.util.*;
import java.util.regex.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * SHADER STABILIZER SYSTEM
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * Prevents common shader errors and instabilities:
 * • Numerical stability (epsilon values, safe divisions)
 * • NaN/Inf protection and detection
 * • Denormal number flushing
 * • Convergence guards for iterative algorithms
 * • Precision loss prevention
 * • Floating-point rounding error mitigation
 * • Range clamping and bounds checking
 * • Catastrophic cancellation prevention
 * 
 * Usage:
 *   var stabilizer = new ShaderStabilizer()
 *       .enableNaNProtection()
 *       .enableDenormalFlushing()
 *       .enableConvergenceGuards()
 *       .setEpsilon(1e-7f);
 *   
 *   String stableShader = stabilizer.stabilize(shaderSource);
 */
public class ShaderStabilizer {
    
    private float epsilon = 1e-7f;
    private boolean nanProtection = true;
    private boolean denormalFlushing = true;
    private boolean convergenceGuards = true;
    private boolean rangeChecking = true;
    private boolean precisionGuards = true;
    
    // Track which helpers are actually needed to avoid injecting unused code
    private final Set<String> requiredHelpers = new HashSet<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public ShaderStabilizer setEpsilon(float epsilon) {
        this.epsilon = epsilon;
        return this;
    }
    
    public ShaderStabilizer enableNaNProtection() {
        this.nanProtection = true;
        return this;
    }
    
    public ShaderStabilizer enableDenormalFlushing() {
        this.denormalFlushing = true;
        return this;
    }
    
    public ShaderStabilizer enableConvergenceGuards() {
        this.convergenceGuards = true;
        return this;
    }
    
    public ShaderStabilizer enableRangeChecking() {
        this.rangeChecking = true;
        return this;
    }
    
    public ShaderStabilizer enablePrecisionGuards() {
        this.precisionGuards = true;
        return this;
    }
    
    public ShaderStabilizer enableAll() {
        return enableNaNProtection()
            .enableDenormalFlushing()
            .enableConvergenceGuards()
            .enableRangeChecking()
            .enablePrecisionGuards();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MAIN STABILIZATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public String stabilize(String shaderSource) {
        requiredHelpers.clear();
        String result = shaderSource;
        
        // First pass: apply transformations to determine which helpers are needed
        // We work on a copy to scan, then apply to the real source
        
        if (nanProtection) {
            result = addNaNProtection(result);
        }
        
        if (denormalFlushing) {
            result = addDenormalFlushing(result);
        }
        
        if (convergenceGuards) {
            result = addConvergenceGuards(result);
        }
        
        if (rangeChecking) {
            result = addRangeChecking(result);
        }
        
        if (precisionGuards) {
            result = addPrecisionGuards(result);
        }
        
        // Inject only the helpers that are actually referenced
        result = injectHelperFunctions(result);
        
        return result;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // HELPER FUNCTION INJECTION
    // ═══════════════════════════════════════════════════════════════════════
    
    private String injectHelperFunctions(String shader) {
        StringBuilder helpers = new StringBuilder();
        
        helpers.append("\n// ═══════════════════════════════════════════════════════════\n");
        helpers.append("// STABILIZER HELPER FUNCTIONS\n");
        helpers.append("// ═══════════════════════════════════════════════════════════\n\n");
        
        String eps = formatFloat(epsilon);
        
        boolean anyInjected = false;
        
        // NaN/Inf checking — needed by sanitize
        if (requiredHelpers.contains("isNaNOrInf") || requiredHelpers.contains("sanitize")) {
            helpers.append("bool isNaNOrInf(float x) {\n");
            helpers.append("    return isinf(x) || isnan(x);\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        if (requiredHelpers.contains("sanitize")) {
            helpers.append("float sanitize(float x) {\n");
            helpers.append("    return isNaNOrInf(x) ? 0.0 : x;\n");
            helpers.append("}\n\n");
            
            helpers.append("vec2 sanitize(vec2 v) {\n");
            helpers.append("    return vec2(sanitize(v.x), sanitize(v.y));\n");
            helpers.append("}\n\n");
            
            helpers.append("vec3 sanitize(vec3 v) {\n");
            helpers.append("    return vec3(sanitize(v.x), sanitize(v.y), sanitize(v.z));\n");
            helpers.append("}\n\n");
            
            helpers.append("vec4 sanitize(vec4 v) {\n");
            helpers.append("    return vec4(sanitize(v.x), sanitize(v.y), sanitize(v.z), sanitize(v.w));\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        // Safe division
        if (requiredHelpers.contains("safeDivide")) {
            helpers.append("float safeDivide(float a, float b) {\n");
            helpers.append("    return abs(b) < ").append(eps).append(" ? 0.0 : a / b;\n");
            helpers.append("}\n\n");
            
            helpers.append("vec2 safeDivide(vec2 a, vec2 b) {\n");
            helpers.append("    return vec2(safeDivide(a.x, b.x), safeDivide(a.y, b.y));\n");
            helpers.append("}\n\n");
            
            helpers.append("vec3 safeDivide(vec3 a, vec3 b) {\n");
            helpers.append("    return vec3(safeDivide(a.x, b.x), safeDivide(a.y, b.y), safeDivide(a.z, b.z));\n");
            helpers.append("}\n\n");
            
            helpers.append("vec4 safeDivide(vec4 a, vec4 b) {\n");
            helpers.append("    return vec4(safeDivide(a.x, b.x), safeDivide(a.y, b.y), safeDivide(a.z, b.z), safeDivide(a.w, b.w));\n");
            helpers.append("}\n\n");
            
            // Scalar divisor overloads
            helpers.append("vec2 safeDivide(vec2 a, float b) {\n");
            helpers.append("    return abs(b) < ").append(eps).append(" ? vec2(0.0) : a / b;\n");
            helpers.append("}\n\n");
            
            helpers.append("vec3 safeDivide(vec3 a, float b) {\n");
            helpers.append("    return abs(b) < ").append(eps).append(" ? vec3(0.0) : a / b;\n");
            helpers.append("}\n\n");
            
            helpers.append("vec4 safeDivide(vec4 a, float b) {\n");
            helpers.append("    return abs(b) < ").append(eps).append(" ? vec4(0.0) : a / b;\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        // Safe sqrt
        if (requiredHelpers.contains("safeSqrt")) {
            helpers.append("float safeSqrt(float x) {\n");
            helpers.append("    return sqrt(max(x, 0.0));\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        // Safe normalize
        if (requiredHelpers.contains("safeNormalize")) {
            helpers.append("vec2 safeNormalize(vec2 v) {\n");
            helpers.append("    float len = length(v);\n");
            helpers.append("    return len > ").append(eps).append(" ? v / len : vec2(0.0);\n");
            helpers.append("}\n\n");
            
            helpers.append("vec3 safeNormalize(vec3 v) {\n");
            helpers.append("    float len = length(v);\n");
            helpers.append("    return len > ").append(eps).append(" ? v / len : vec3(0.0, 0.0, 1.0);\n");
            helpers.append("}\n\n");
            
            helpers.append("vec4 safeNormalize(vec4 v) {\n");
            helpers.append("    float len = length(v);\n");
            helpers.append("    return len > ").append(eps).append(" ? v / len : vec4(0.0, 0.0, 0.0, 1.0);\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        // Clamp to safe range
        if (requiredHelpers.contains("clampSafe")) {
            helpers.append("float clampSafe(float x) {\n");
            helpers.append("    return clamp(x, -1e10, 1e10);\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        // Denormal flush
        if (requiredHelpers.contains("flushDenormal")) {
            helpers.append("float flushDenormal(float x) {\n");
            helpers.append("    return abs(x) < ").append(eps).append(" ? 0.0 : x;\n");
            helpers.append("}\n\n");
            
            helpers.append("vec2 flushDenormal(vec2 v) {\n");
            helpers.append("    return vec2(flushDenormal(v.x), flushDenormal(v.y));\n");
            helpers.append("}\n\n");
            
            helpers.append("vec3 flushDenormal(vec3 v) {\n");
            helpers.append("    return vec3(flushDenormal(v.x), flushDenormal(v.y), flushDenormal(v.z));\n");
            helpers.append("}\n\n");
            
            helpers.append("vec4 flushDenormal(vec4 v) {\n");
            helpers.append("    return vec4(flushDenormal(v.x), flushDenormal(v.y), flushDenormal(v.z), flushDenormal(v.w));\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        // Safe pow
        if (requiredHelpers.contains("safePow")) {
            helpers.append("float safePow(float base, float exp) {\n");
            helpers.append("    return pow(max(base, 0.0), exp);\n");
            helpers.append("}\n\n");
            anyInjected = true;
        }
        
        if (!anyInjected) {
            return shader;
        }
        
        // Find insertion point (after #version and #extension directives, and after precision qualifiers)
        int insertPos = findInsertionPoint(shader);
        return shader.substring(0, insertPos) + helpers.toString() + shader.substring(insertPos);
    }
    
    private int findInsertionPoint(String shader) {
        // Find position after all preprocessor directives (#version, #extension, #define at the top)
        // and precision qualifiers so that helper functions are placed in a valid location
        String[] lines = shader.split("\n");
        int pos = 0;
        int lastDirectiveLine = -1;
        
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                // Skip blank lines and comments at the top
                pos += lines[i].length() + 1;
                continue;
            }
            if (trimmed.startsWith("#") || trimmed.startsWith("precision ")) {
                lastDirectiveLine = i;
                pos += lines[i].length() + 1;
                continue;
            }
            // First non-directive, non-comment line — stop here
            break;
        }
        
        // If we found directives, insert after the last one
        if (lastDirectiveLine >= 0) {
            int result = 0;
            for (int i = 0; i <= lastDirectiveLine; i++) {
                result += lines[i].length() + 1;
            }
            return result;
        }
        
        return 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // NaN PROTECTION
    // ═══════════════════════════════════════════════════════════════════════
    
    private String addNaNProtection(String shader) {
        shader = protectDivisions(shader);
        shader = protectSqrt(shader);
        shader = protectNormalize(shader);
        shader = protectArcTrig(shader);
        shader = protectLog(shader);
        shader = protectPow(shader);
        return shader;
    }
    
    private String protectDivisions(String shader) {
        // Tokenize to properly handle nested expressions, comments, and strings
        // We use a more careful approach: find division operators that are not inside
        // comments or preprocessor directives, and wrap them
        
        StringBuilder result = new StringBuilder();
        String[] lines = shader.split("\n", -1);
        boolean inBlockComment = false;
        
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];
            
            // Track block comments
            if (inBlockComment) {
                int endIdx = line.indexOf("*/");
                if (endIdx >= 0) {
                    inBlockComment = false;
                    result.append(line, 0, endIdx + 2);
                    String rest = line.substring(endIdx + 2);
                    result.append(protectDivisionsInExpression(rest));
                } else {
                    result.append(line);
                }
            } else {
                String trimmed = line.trim();
                // Skip preprocessor directives entirely
                if (trimmed.startsWith("#")) {
                    result.append(line);
                } else {
                    // Handle line comments: only process the part before //
                    int lineCommentIdx = findLineCommentStart(line);
                    int blockCommentIdx = line.indexOf("/*");
                    
                    if (lineCommentIdx >= 0 && (blockCommentIdx < 0 || lineCommentIdx < blockCommentIdx)) {
                        String code = line.substring(0, lineCommentIdx);
                        String comment = line.substring(lineCommentIdx);
                        result.append(protectDivisionsInExpression(code));
                        result.append(comment);
                    } else if (blockCommentIdx >= 0) {
                        String before = line.substring(0, blockCommentIdx);
                        result.append(protectDivisionsInExpression(before));
                        
                        int endIdx = line.indexOf("*/", blockCommentIdx + 2);
                        if (endIdx >= 0) {
                            result.append(line, blockCommentIdx, endIdx + 2);
                            String after = line.substring(endIdx + 2);
                            result.append(protectDivisionsInExpression(after));
                        } else {
                            inBlockComment = true;
                            result.append(line.substring(blockCommentIdx));
                        }
                    } else {
                        result.append(protectDivisionsInExpression(line));
                    }
                }
            }
            
            if (lineIdx < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Find the start of a line comment (//), skipping occurrences inside string literals
     * or URLs (e.g., in comments that are already present). For GLSL, string literals
     * are not common, so this is primarily about correct detection.
     */
    private int findLineCommentStart(String line) {
        int idx = line.indexOf("//");
        // Simple: GLSL doesn't have string literals, so first // is always a comment
        return idx;
    }
    
    /**
     * Protect divisions in a single expression/line of code (known to not contain comments).
     * Uses a balanced-parenthesis-aware approach so we can handle expressions like:
     *   result = (a + b) / (c + d);
     *   color.rgb / max(color.a, 0.001);
     */
    private String protectDivisionsInExpression(String code) {
        if (!code.contains("/")) {
            return code;
        }
        
        // Don't replace /= compound assignment or // comments
        // Walk through character by character
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < code.length()) {
            char c = code.charAt(i);
            
            if (c == '/') {
                // Check for // (shouldn't happen here but safety)
                if (i + 1 < code.length() && code.charAt(i + 1) == '/') {
                    result.append(code.substring(i));
                    return result.toString();
                }
                // Check for /= (compound assignment — leave alone)
                if (i + 1 < code.length() && code.charAt(i + 1) == '=') {
                    result.append("/=");
                    i += 2;
                    continue;
                }
                // Check for /* (block comment start — shouldn't be here but safety)
                if (i + 1 < code.length() && code.charAt(i + 1) == '*') {
                    result.append(code.substring(i));
                    return result.toString();
                }
                
                // This is a division operator
                // Extract left operand (walk backwards from current position)
                String left = result.toString();
                String leftOperand = extractLeftOperand(left);
                
                if (leftOperand == null || leftOperand.isEmpty()) {
                    // Can't determine left operand, leave as-is
                    result.append(c);
                    i++;
                    continue;
                }
                
                // Extract right operand (walk forwards)
                String remaining = code.substring(i + 1);
                String rightOperand = extractRightOperand(remaining);
                
                if (rightOperand == null || rightOperand.isEmpty()) {
                    result.append(c);
                    i++;
                    continue;
                }
                
                // Skip if dividing by a known-safe constant (literal number)
                // but still wrap for safety
                
                // Remove the left operand from result and replace with safeDivide
                String prefix = left.substring(0, left.length() - leftOperand.length());
                result.setLength(0);
                result.append(prefix);
                result.append("safeDivide(").append(leftOperand.trim()).append(", ").append(rightOperand.trim()).append(")");
                requiredHelpers.add("safeDivide");
                
                i += 1 + rightOperand.length();
            } else {
                result.append(c);
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Extract the left operand of a division from the end of the accumulated string.
     * Handles: identifiers, member access (a.x), function calls (foo(...)), 
     * array access (a[i]), parenthesized expressions ((a+b)), and numeric literals.
     */
    private String extractLeftOperand(String left) {
        if (left.isEmpty()) return null;
        
        String trimmed = left.stripTrailing();
        if (trimmed.isEmpty()) return null;
        
        int end = trimmed.length();
        int pos = end - 1;
        char lastChar = trimmed.charAt(pos);
        
        // If ends with ')' — find matching '('
        if (lastChar == ')') {
            int depth = 1;
            pos--;
            while (pos >= 0 && depth > 0) {
                if (trimmed.charAt(pos) == ')') depth++;
                else if (trimmed.charAt(pos) == '(') depth--;
                pos--;
            }
            pos++; // points to '('
            
            // Check if preceded by a function name
            int funcStart = pos;
            if (pos > 0) {
                int nameEnd = pos;
                pos--;
                while (pos >= 0 && (Character.isLetterOrDigit(trimmed.charAt(pos)) || trimmed.charAt(pos) == '_' || trimmed.charAt(pos) == '.')) {
                    pos--;
                }
                funcStart = pos + 1;
            }
            
            // Return whitespace from original left string properly
            int trailingSpaces = left.length() - trimmed.length();
            String operand = trimmed.substring(funcStart) ;
            return " ".repeat(trailingSpaces) + operand;
        }
        
        // If ends with ']' — find matching '['
        if (lastChar == ']') {
            int depth = 1;
            pos--;
            while (pos >= 0 && depth > 0) {
                if (trimmed.charAt(pos) == ']') depth++;
                else if (trimmed.charAt(pos) == '[') depth--;
                pos--;
            }
            // Continue backwards for the array name
            while (pos >= 0 && (Character.isLetterOrDigit(trimmed.charAt(pos)) || trimmed.charAt(pos) == '_' || trimmed.charAt(pos) == '.')) {
                pos--;
            }
            return trimmed.substring(pos + 1);
        }
        
        // Identifier, member access, or numeric literal
        if (Character.isLetterOrDigit(lastChar) || lastChar == '_' || lastChar == '.') {
            while (pos >= 0 && (Character.isLetterOrDigit(trimmed.charAt(pos)) || trimmed.charAt(pos) == '_' || trimmed.charAt(pos) == '.')) {
                pos--;
            }
            return trimmed.substring(pos + 1);
        }
        
        return null;
    }
    
    /**
     * Extract the right operand of a division from the beginning of the remaining string.
     * Handles: identifiers, member access, function calls, parenthesized expressions,
     * unary minus, and numeric literals.
     */
    private String extractRightOperand(String right) {
        if (right.isEmpty()) return null;
        
        String trimmed = right.stripLeading();
        if (trimmed.isEmpty()) return null;
        
        int leadingSpaces = right.length() - trimmed.length();
        int pos = 0;
        
        // Handle unary minus/plus
        if (pos < trimmed.length() && (trimmed.charAt(pos) == '-' || trimmed.charAt(pos) == '+')) {
            pos++;
        }
        
        // Skip whitespace after unary
        while (pos < trimmed.length() && trimmed.charAt(pos) == ' ') pos++;
        
        if (pos >= trimmed.length()) return null;
        
        char firstChar = trimmed.charAt(pos);
        
        // Parenthesized expression
        if (firstChar == '(') {
            int depth = 1;
            pos++;
            while (pos < trimmed.length() && depth > 0) {
                if (trimmed.charAt(pos) == '(') depth++;
                else if (trimmed.charAt(pos) == ')') depth--;
                pos++;
            }
            return right.substring(0, leadingSpaces + pos);
        }
        
        // Identifier or function call or numeric literal
        if (Character.isLetterOrDigit(firstChar) || firstChar == '_') {
            while (pos < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(pos)) || trimmed.charAt(pos) == '_' || trimmed.charAt(pos) == '.')) {
                pos++;
            }
            // Check for function call
            if (pos < trimmed.length() && trimmed.charAt(pos) == '(') {
                int depth = 1;
                pos++;
                while (pos < trimmed.length() && depth > 0) {
                    if (trimmed.charAt(pos) == '(') depth++;
                    else if (trimmed.charAt(pos) == ')') depth--;
                    pos++;
                }
            }
            // Check for array access
            if (pos < trimmed.length() && trimmed.charAt(pos) == '[') {
                int depth = 1;
                pos++;
                while (pos < trimmed.length() && depth > 0) {
                    if (trimmed.charAt(pos) == '[') depth++;
                    else if (trimmed.charAt(pos) == ']') depth--;
                    pos++;
                }
            }
            // Check for swizzle/member access after function call or array
            while (pos < trimmed.length() && trimmed.charAt(pos) == '.') {
                pos++;
                while (pos < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(pos)) || trimmed.charAt(pos) == '_')) {
                    pos++;
                }
            }
            return right.substring(0, leadingSpaces + pos);
        }
        
        return null;
    }
    
    private String protectSqrt(String shader) {
        if (shader.contains("sqrt(") && !shader.contains("safeSqrt(")) {
            // Only replace bare sqrt, not safeSqrt references
            shader = replaceOutsideComments(shader, "\\bsqrt\\(", "safeSqrt(");
            requiredHelpers.add("safeSqrt");
        }
        return shader;
    }
    
    private String protectNormalize(String shader) {
        if (shader.contains("normalize(") && !shader.contains("safeNormalize(")) {
            shader = replaceOutsideComments(shader, "\\bnormalize\\(", "safeNormalize(");
            requiredHelpers.add("safeNormalize");
        }
        return shader;
    }
    
    private String protectArcTrig(String shader) {
        // Protect asin and acos — handle nested parentheses properly
        shader = wrapFunctionArgument(shader, "asin", arg -> "asin(clamp(" + arg + ", -1.0, 1.0))");
        shader = wrapFunctionArgument(shader, "acos", arg -> "acos(clamp(" + arg + ", -1.0, 1.0))");
        return shader;
    }
    
    private String protectLog(String shader) {
        String eps = formatFloat(epsilon);
        shader = wrapFunctionArgument(shader, "log", arg -> "log(max(" + arg + ", " + eps + "))");
        shader = wrapFunctionArgument(shader, "log2", arg -> "log2(max(" + arg + ", " + eps + "))");
        return shader;
    }
    
    private String protectPow(String shader) {
        // pow(x, y) with negative base can produce NaN
        if (shader.contains("pow(")) {
            shader = replaceOutsideComments(shader, "\\bpow\\(", "safePow(");
            requiredHelpers.add("safePow");
        }
        return shader;
    }
    
    /**
     * Wraps a GLSL function call's argument(s) with a transformation.
     * Properly handles nested parentheses so that the full argument list is captured.
     */
    private String wrapFunctionArgument(String shader, String funcName, java.util.function.Function<String, String> wrapper) {
        // Use a word-boundary-aware search
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(funcName) + "\\(");
        
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        Matcher matcher = pattern.matcher(shader);
        
        while (matcher.find()) {
            int matchStart = matcher.start();
            
            // Check if inside a comment
            if (isInComment(shader, matchStart)) {
                continue;
            }
            
            // Check it's not part of a larger identifier (e.g., "log2" when searching "log")
            // The \b handles this, but double check for safety with function names like log vs log2
            if (funcName.equals("log") && matchStart + 3 < shader.length() && shader.charAt(matchStart + 3) == '2') {
                continue;
            }
            
            int openParen = matcher.end() - 1; // position of '('
            int closeParen = findMatchingParen(shader, openParen);
            if (closeParen < 0) continue;
            
            String argument = shader.substring(openParen + 1, closeParen);
            String replacement = wrapper.apply(argument);
            
            result.append(shader, lastEnd, matchStart);
            result.append(replacement);
            lastEnd = closeParen + 1;
        }
        
        result.append(shader.substring(lastEnd));
        return result.toString();
    }
    
    /**
     * Find the matching closing parenthesis for an opening one, handling nesting.
     */
    private int findMatchingParen(String source, int openPos) {
        if (openPos >= source.length() || source.charAt(openPos) != '(') return -1;
        
        int depth = 1;
        int pos = openPos + 1;
        while (pos < source.length() && depth > 0) {
            char c = source.charAt(pos);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            pos++;
        }
        return depth == 0 ? pos - 1 : -1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DENORMAL FLUSHING
    // ═══════════════════════════════════════════════════════════════════════
    
    private String addDenormalFlushing(String shader) {
        // Build a set of known float-type variables by scanning declarations
        Map<String, String> floatVars = new LinkedHashMap<>();
        
        // Match declarations: float x = ...; or vec3 x = ...;
        Pattern declPattern = Pattern.compile("\\b(float|vec[234])\\s+(\\w+)\\s*=\\s*([^;]+);");
        Matcher declMatcher = declPattern.matcher(shader);
        
        while (declMatcher.find()) {
            String type = declMatcher.group(1);
            String name = declMatcher.group(2);
            floatVars.put(name, type);
        }
        
        if (floatVars.isEmpty()) {
            return shader;
        }
        
        // Only flush assignments to known float variables, avoiding declarations 
        // of helper functions and loop guards
        StringBuilder result = new StringBuilder();
        String[] lines = shader.split("\n", -1);
        boolean inBlockComment = false;
        
        // Track which variables we've already seen declared so we can flush assignments
        Set<String> declaredVars = new HashSet<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            
            // Handle block comments
            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                result.append(line);
            } else if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*")) {
                if (trimmed.startsWith("/*") && !trimmed.contains("*/")) {
                    inBlockComment = true;
                }
                result.append(line);
            } else {
                // Check for float variable assignments (not declarations) and wrap with flushDenormal
                boolean replaced = false;
                for (Map.Entry<String, String> entry : floatVars.entrySet()) {
                    String varName = entry.getKey();
                    String varType = entry.getValue();
                    
                    // Skip guard variables and loop counters
                    if (varName.startsWith("_guard_") || varName.equals("i") || varName.equals("j")) {
                        continue;
                    }
                    
                    // Match: varName = expression; (assignment, not declaration)
                    Pattern assignPattern = Pattern.compile(
                        "^(\\s*)" + Pattern.quote(varName) + "\\s*=\\s*([^;]+);(.*)$"
                    );
                    Matcher assignMatcher = assignPattern.matcher(line);
                    
                    if (assignMatcher.matches() && declaredVars.contains(varName)) {
                        String indent = assignMatcher.group(1);
                        String expr = assignMatcher.group(2).trim();
                        String trailing = assignMatcher.group(3);
                        
                        // Don't double-wrap
                        if (!expr.startsWith("flushDenormal(")) {
                            result.append(indent).append(varName).append(" = flushDenormal(").append(expr).append(");").append(trailing);
                            requiredHelpers.add("flushDenormal");
                            replaced = true;
                            break;
                        }
                    }
                    
                    // Track declarations
                    Pattern declCheckPattern = Pattern.compile("\\b(float|vec[234])\\s+" + Pattern.quote(varName) + "\\b");
                    if (declCheckPattern.matcher(line).find()) {
                        declaredVars.add(varName);
                    }
                }
                
                if (!replaced) {
                    result.append(line);
                }
            }
            
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONVERGENCE GUARDS
    // ═══════════════════════════════════════════════════════════════════════
    
    private String addConvergenceGuards(String shader) {
        // Properly handle while loops by:
        // 1. Declaring the guard variable before the while
        // 2. Adding the guard check to the condition
        // 3. Incrementing the guard inside the loop body
        
        StringBuilder result = new StringBuilder();
        int guardCounter = 0;
        int pos = 0;
        
        // Pattern to find while loops — matches "while" followed by "(" with balanced parens
        Pattern whilePattern = Pattern.compile("\\bwhile\\s*\\(");
        Matcher matcher = whilePattern.matcher(shader);
        
        while (matcher.find()) {
            int whileStart = matcher.start();
            
            // Check if inside a comment
            if (isInComment(shader, whileStart)) {
                continue;
            }
            
            int conditionStart = matcher.end() - 1; // position of '('
            int conditionEnd = findMatchingParen(shader, conditionStart);
            if (conditionEnd < 0) continue;
            
            String condition = shader.substring(conditionStart + 1, conditionEnd);
            
            // Find the opening brace of the loop body
            int braceStart = -1;
            for (int i = conditionEnd + 1; i < shader.length(); i++) {
                char c = shader.charAt(i);
                if (c == '{') {
                    braceStart = i;
                    break;
                } else if (!Character.isWhitespace(c)) {
                    // Single-statement while loop — more complex to handle
                    break;
                }
            }
            
            String guardVar = "_guard_" + guardCounter++;
            
            // Append everything before this while loop
            result.append(shader, pos, whileStart);
            
            if (braceStart >= 0) {
                // Multi-statement while loop with braces
                result.append("int ").append(guardVar).append(" = 0;\n");
                // Preserve indentation
                int lineStart = shader.lastIndexOf('\n', whileStart);
                if (lineStart >= 0 && lineStart < whileStart) {
                    String indent = shader.substring(lineStart + 1, whileStart);
                    if (indent.isBlank()) {
                        result.append(indent);
                    }
                }
                result.append("while ((").append(condition).append(") && ").append(guardVar).append(" < 1000)");
                result.append(shader, conditionEnd + 1, braceStart + 1);
                result.append(" ").append(guardVar).append("++;");
                pos = braceStart + 1;
            } else {
                // Single-statement while — wrap in braces
                // Find the end of the statement (next semicolon)
                int stmtEnd = shader.indexOf(';', conditionEnd + 1);
                if (stmtEnd < 0) {
                    // Malformed, skip
                    result.append(shader, whileStart, conditionEnd + 1);
                    pos = conditionEnd + 1;
                    continue;
                }
                
                String statement = shader.substring(conditionEnd + 1, stmtEnd + 1).trim();
                result.append("int ").append(guardVar).append(" = 0;\n");
                int lineStart = shader.lastIndexOf('\n', whileStart);
                if (lineStart >= 0 && lineStart < whileStart) {
                    String indent = shader.substring(lineStart + 1, whileStart);
                    if (indent.isBlank()) {
                        result.append(indent);
                    }
                }
                result.append("while ((").append(condition).append(") && ").append(guardVar).append(" < 1000) { ");
                result.append(guardVar).append("++; ");
                result.append(statement).append(" }");
                pos = stmtEnd + 1;
            }
        }
        
        result.append(shader.substring(pos));
        return result.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RANGE CHECKING
    // ═══════════════════════════════════════════════════════════════════════
    
    private String addRangeChecking(String shader) {
        // Clamp texture coordinates — handle nested parentheses properly
        shader = wrapTextureSampling(shader);
        
        // Add sanitization to return values
        shader = addSanitization(shader);
        
        return shader;
    }
    
    /**
     * Wraps texture sampling calls to clamp UV coordinates.
     * Handles texture(), texture2D(), textureLod(), etc.
     */
    private String wrapTextureSampling(String shader) {
        // Match texture sampling functions
        Pattern pattern = Pattern.compile("\\b(texture2D|texture|textureLod|texelFetch)\\s*\\(");
        Matcher matcher = pattern.matcher(shader);
        
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        
        while (matcher.find()) {
            int matchStart = matcher.start();
            
            if (isInComment(shader, matchStart)) continue;
            
            String funcName = matcher.group(1);
            int openParen = matcher.end() - 1;
            int closeParen = findMatchingParen(shader, openParen);
            if (closeParen < 0) continue;
            
            // texelFetch uses integer coordinates, don't clamp
            if (funcName.equals("texelFetch")) continue;
            
            String fullArgs = shader.substring(openParen + 1, closeParen);
            
            // Split arguments carefully (first comma at depth 0 separates sampler from coords)
            int firstComma = findCommaAtDepth0(fullArgs, 0);
            if (firstComma < 0) continue;
            
            String sampler = fullArgs.substring(0, firstComma).trim();
            String rest = fullArgs.substring(firstComma + 1).trim();
            
            // The UV coordinates are the next argument; there may be more (e.g., LOD for textureLod)
            int secondComma = findCommaAtDepth0(rest, 0);
            
            String uvCoord;
            String extraArgs = "";
            if (secondComma >= 0) {
                uvCoord = rest.substring(0, secondComma).trim();
                extraArgs = ", " + rest.substring(secondComma + 1).trim();
            } else {
                uvCoord = rest;
            }
            
            // Don't double-wrap
            if (!uvCoord.startsWith("clamp(")) {
                result.append(shader, lastEnd, matchStart);
                result.append(funcName).append("(").append(sampler).append(", clamp(").append(uvCoord).append(", 0.0, 1.0)").append(extraArgs).append(")");
                lastEnd = closeParen + 1;
            }
        }
        
        result.append(shader.substring(lastEnd));
        return result.toString();
    }
    
    /**
     * Find the position of the next comma at parenthesis depth 0, starting from startPos.
     */
    private int findCommaAtDepth0(String s, int startPos) {
        int depth = 0;
        for (int i = startPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            else if (c == ',' && depth == 0) return i;
        }
        return -1;
    }
    
    private String addSanitization(String shader) {
        // Sanitize return values — but only in user functions, not in helper functions
        // We use a more targeted approach: find return statements and wrap if not already sanitized
        
        StringBuilder result = new StringBuilder();
        int pos = 0;
        Pattern pattern = Pattern.compile("\\breturn\\s+");
        Matcher matcher = pattern.matcher(shader);
        
        while (matcher.find()) {
            int matchStart = matcher.start();
            
            if (isInComment(shader, matchStart)) continue;
            
            // Don't sanitize returns inside our own helper functions
            if (isInsideHelperFunction(shader, matchStart)) continue;
            
            int exprStart = matcher.end();
            int semicolon = shader.indexOf(';', exprStart);
            if (semicolon < 0) continue;
            
            String returnValue = shader.substring(exprStart, semicolon).trim();
            
            // Don't double-wrap
            if (returnValue.startsWith("sanitize(")) continue;
            
            // Don't wrap void returns or simple literals that can't be NaN
            if (returnValue.isEmpty()) continue;
            
            result.append(shader, pos, exprStart);
            result.append("sanitize(").append(returnValue).append(")");
            requiredHelpers.add("sanitize");
            pos = semicolon;
        }
        
        result.append(shader.substring(pos));
        return result.toString();
    }
    
    /**
     * Check if a position is inside one of our injected helper functions
     * to avoid modifying them.
     */
    private boolean isInsideHelperFunction(String shader, int position) {
        // Check if we're between "STABILIZER HELPER FUNCTIONS" markers
        String before = shader.substring(0, position);
        int helperStart = before.lastIndexOf("STABILIZER HELPER FUNCTIONS");
        if (helperStart < 0) return false;
        
        // Find the end of the helper block (next section separator or a reasonable distance)
        String after = shader.substring(helperStart);
        // Count brace depth from helper start — if we're still inside helper definitions
        int braceDepth = 0;
        boolean passedFirstFunction = false;
        for (int i = 0; i < position - helperStart; i++) {
            char c = after.charAt(i);
            if (c == '{') { braceDepth++; passedFirstFunction = true; }
            else if (c == '}') braceDepth--;
        }
        
        // After the helper block, all braces should be balanced (depth 0) and 
        // we should have encountered some functions
        // If we're at depth 0 and have passed functions, we're past the helpers
        return passedFirstFunction && braceDepth > 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PRECISION GUARDS
    // ═══════════════════════════════════════════════════════════════════════
    
    private String addPrecisionGuards(String shader) {
        // Detect and mitigate catastrophic cancellation patterns
        // Pattern: (a + b) - a  or  a - (a + epsilon)
        // These are hard to detect statically, but we can add precision hints
        
        // Add highp precision qualifier to critical calculations if not present
        if (!shader.contains("precision highp float")) {
            // Check if there's any precision statement
            if (shader.contains("precision mediump float")) {
                // Warn but don't override — mediump may be intentional for performance
                // Add a comment
                shader = shader.replace("precision mediump float",
                    "precision mediump float // WARNING: mediump may cause precision issues in complex calculations");
            } else if (!shader.contains("precision ") || !shader.contains("float")) {
                // No float precision specified — add highp for fragment shaders
                int insertPos = findInsertionPoint(shader);
                // Only add if this looks like a fragment shader (contains gl_FragColor or out vec4)
                if (shader.contains("gl_FragColor") || shader.contains("gl_FragData") || 
                    shader.matches("(?s).*\\bout\\s+vec4\\b.*")) {
                    shader = shader.substring(0, insertPos) + 
                             "precision highp float;\n" + 
                             shader.substring(insertPos);
                }
            }
        }
        
        return shader;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Check if a position in the shader source is inside a comment (line or block).
     */
    private boolean isInComment(String shader, int position) {
        // Scan from the beginning to track comment state accurately
        boolean inBlockComment = false;
        int i = 0;
        
        while (i < position) {
            if (inBlockComment) {
                if (i + 1 < shader.length() && shader.charAt(i) == '*' && shader.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i += 2;
                    continue;
                }
            } else {
                if (i + 1 < shader.length() && shader.charAt(i) == '/' && shader.charAt(i + 1) == '/') {
                    // Line comment — everything until newline is comment
                    int newline = shader.indexOf('\n', i);
                    if (newline < 0 || position < newline) {
                        return true; // position is in this line comment
                    }
                    i = newline + 1;
                    continue;
                }
                if (i + 1 < shader.length() && shader.charAt(i) == '/' && shader.charAt(i + 1) == '*') {
                    inBlockComment = true;
                    i += 2;
                    continue;
                }
            }
            i++;
        }
        
        return inBlockComment;
    }
    
    private boolean isInCommentOrString(String shader, int position) {
        return isInComment(shader, position);
    }
    
    private boolean isFloatType(String varName, String shader) {
        Pattern pattern = Pattern.compile("\\b(float|vec[234])\\s+" + Pattern.quote(varName) + "\\s*[;=]");
        return pattern.matcher(shader).find();
    }
    
    /**
     * Replace a regex pattern only in non-comment portions of the shader.
     */
    private String replaceOutsideComments(String shader, String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(shader);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        
        while (matcher.find()) {
            if (!isInComment(shader, matcher.start())) {
                result.append(shader, lastEnd, matcher.start());
                result.append(replacement);
                lastEnd = matcher.end();
            }
        }
        result.append(shader.substring(lastEnd));
        return result.toString();
    }
    
    /**
     * Format a float value for GLSL source code (always with decimal point, no trailing 'E' notation).
     */
    private String formatFloat(float value) {
        if (value == (int) value) {
            return String.format("%.1f", value);
        }
        // Use scientific notation for very small values
        if (Math.abs(value) < 0.001f) {
            // GLSL doesn't support Java's 'E' notation directly, 
            // but most GLSL compilers accept it. Use explicit float notation.
            String s = String.format("%e", value);
            // Convert 1.000000e-07 to a cleaner form
            return s.replaceAll("0+e", "e");
        }
        return Float.toString(value);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public ValidationResult validate(String shader) {
        ValidationResult result = new ValidationResult();
        
        result.potentialNaN = findPotentialNaN(shader);
        result.unsafeOperations = findUnsafeOperations(shader);
        result.unboundedLoops = findUnboundedLoops(shader);
        result.precisionIssues = findPrecisionIssues(shader);
        
        return result;
    }
    
    private List<String> findPotentialNaN(String shader) {
        List<String> issues = new ArrayList<>();
        
        // Check for unprotected divisions (not using safeDivide)
        Pattern divPattern = Pattern.compile("\\b(\\w+)\\s*/\\s*(\\w+)");
        Matcher divMatcher = divPattern.matcher(shader);
        while (divMatcher.find()) {
            if (!isInComment(shader, divMatcher.start())) {
                issues.add("Unprotected division: " + divMatcher.group() + " at position " + divMatcher.start());
            }
        }
        
        // Check for sqrt without safeSqrt
        if (containsOutsideComments(shader, "\\bsqrt\\(") && !shader.contains("safeSqrt")) {
            issues.add("Unprotected sqrt() — may produce NaN for negative inputs");
        }
        
        // Check for normalize without safeNormalize
        if (containsOutsideComments(shader, "\\bnormalize\\(") && !shader.contains("safeNormalize")) {
            issues.add("Unprotected normalize() — may produce NaN/Inf for zero-length vectors");
        }
        
        // Check for unprotected asin/acos
        if (containsOutsideComments(shader, "\\basin\\(") || containsOutsideComments(shader, "\\bacos\\(")) {
            // Check if argument is clamped
            Pattern acosPattern = Pattern.compile("\\bacos\\((?!clamp)");
            if (acosPattern.matcher(shader).find()) {
                issues.add("Unprotected acos() — argument should be clamped to [-1, 1]");
            }
            Pattern asinPattern = Pattern.compile("\\basin\\((?!clamp)");
            if (asinPattern.matcher(shader).find()) {
                issues.add("Unprotected asin() — argument should be clamped to [-1, 1]");
            }
        }
        
        // Check for log of potentially negative values
        if (containsOutsideComments(shader, "\\blog2?\\(") && !shader.contains("max(")) {
            issues.add("Potentially unprotected log/log2 — may produce NaN for non-positive inputs");
        }
        
        // Check for pow with potentially negative base
        if (containsOutsideComments(shader, "\\bpow\\(") && !shader.contains("safePow")) {
            issues.add("Unprotected pow() — may produce NaN for negative base with fractional exponent");
        }
        
        return issues;
    }
    
    private List<String> findUnsafeOperations(String shader) {
        List<String> issues = new ArrayList<>();
        
        // Division by zero constants
        Pattern divByZero = Pattern.compile("/\\s*0\\.0\\b|/\\s*0\\b");
        Matcher matcher = divByZero.matcher(shader);
        while (matcher.find()) {
            if (!isInComment(shader, matcher.start())) {
                issues.add("Division by zero constant at position " + matcher.start());
            }
        }
        
        // Mixing integer and float division
        Pattern intDiv = Pattern.compile("\\bint\\s+\\w+\\s*=\\s*[^;]*/[^;]*;");
        Matcher intDivMatcher = intDiv.matcher(shader);
        while (intDivMatcher.find()) {
            if (!isInComment(shader, intDivMatcher.start())) {
                issues.add("Potential integer division truncation at position " + intDivMatcher.start());
            }
        }
        
        return issues;
    }
    
    private List<String> findUnboundedLoops(String shader) {
        List<String> issues = new ArrayList<>();
        
        Pattern whilePattern = Pattern.compile("\\bwhile\\s*\\(([^)]+)\\)");
        Matcher matcher = whilePattern.matcher(shader);
        
        while (matcher.find()) {
            if (isInComment(shader, matcher.start())) continue;
            
            String condition = matcher.group(1);
            // Check if the condition includes a guard variable or explicit bound
            if (!condition.contains("_guard_") && !condition.matches(".*<\\s*\\d+.*") && !condition.matches(".*>\\s*\\d+.*")) {
                issues.add("Potentially unbounded while loop at position " + matcher.start() + 
                          ": condition '" + condition.trim() + "'");
            }
        }
        
        // Also check for loops without clear bounds
        Pattern forPattern = Pattern.compile("\\bfor\\s*\\([^)]*;\\s*;[^)]*\\)");
        Matcher forMatcher = forPattern.matcher(shader);
        while (forMatcher.find()) {
            if (!isInComment(shader, forMatcher.start())) {
                issues.add("Infinite for loop (no condition) at position " + forMatcher.start());
            }
        }
        
        return issues;
    }
    
    private List<String> findPrecisionIssues(String shader) {
        List<String> issues = new ArrayList<>();
        
        // Check for mediump in fragment shaders doing complex math
        if (shader.contains("precision mediump float")) {
            if (shader.contains("pow(") || shader.contains("exp(") || shader.contains("log(")) {
                issues.add("mediump precision with transcendental functions may cause visible artifacts");
            }
        }
        
        // Check for large constant multiplications that might overflow
        Pattern largeMul = Pattern.compile("\\*\\s*(\\d+\\.\\d*|\\d+)");
        Matcher mulMatcher = largeMul.matcher(shader);
        while (mulMatcher.find()) {
            try {
                double val = Double.parseDouble(mulMatcher.group(1));
                if (val > 1e6) {
                    issues.add("Multiplication by large constant " + mulMatcher.group(1) + 
                              " at position " + mulMatcher.start() + " — may cause overflow");
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return issues;
    }
    
    /**
     * Check if a regex pattern exists outside of comments in the shader.
     */
    private boolean containsOutsideComments(String shader, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(shader);
        while (matcher.find()) {
            if (!isInComment(shader, matcher.start())) {
                return true;
            }
        }
        return false;
    }
    
    public static class ValidationResult {
        public List<String> potentialNaN = new ArrayList<>();
        public List<String> unsafeOperations = new ArrayList<>();
        public List<String> unboundedLoops = new ArrayList<>();
        public List<String> precisionIssues = new ArrayList<>();
        
        public boolean hasIssues() {
            return !potentialNaN.isEmpty() || 
                   !unsafeOperations.isEmpty() || 
                   !unboundedLoops.isEmpty() ||
                   !precisionIssues.isEmpty();
        }
        
        public int totalIssueCount() {
            return potentialNaN.size() + unsafeOperations.size() + 
                   unboundedLoops.size() + precisionIssues.size();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation Result (").append(totalIssueCount()).append(" issue(s)):\n");
            
            if (!potentialNaN.isEmpty()) {
                sb.append("  ⚠ NaN Risks (").append(potentialNaN.size()).append("):\n");
                potentialNaN.forEach(issue -> sb.append("    - ").append(issue).append("\n"));
            }
            
            if (!unsafeOperations.isEmpty()) {
                sb.append("  ⚠ Unsafe Operations (").append(unsafeOperations.size()).append("):\n");
                unsafeOperations.forEach(issue -> sb.append("    - ").append(issue).append("\n"));
            }
            
            if (!unboundedLoops.isEmpty()) {
                sb.append("  ⚠ Unbounded Loops (").append(unboundedLoops.size()).append("):\n");
                unboundedLoops.forEach(issue -> sb.append("    - ").append(issue).append("\n"));
            }
            
            if (!precisionIssues.isEmpty()) {
                sb.append("  ⚠ Precision Issues (").append(precisionIssues.size()).append("):\n");
                precisionIssues.forEach(issue -> sb.append("    - ").append(issue).append("\n"));
            }
            
            if (!hasIssues()) {
                sb.append("  ✓ No issues found!\n");
            }
            
            return sb.toString();
        }
    }
}
