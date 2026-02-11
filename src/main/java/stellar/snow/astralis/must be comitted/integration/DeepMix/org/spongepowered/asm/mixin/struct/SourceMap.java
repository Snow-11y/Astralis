package org.spongepowered.asm.mixin.struct;

import java.util.LinkedHashMap;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode;

public class SourceMap {
    private final String sourceFile;
    private final Map<String, Stratum> strata = new LinkedHashMap<String, Stratum>();
    private int nextLineOffset = 1;
    private String defaultStratum = "Mixin";

    public SourceMap(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return this.sourceFile;
    }

    public String getPseudoGeneratedSourceFile() {
        return this.sourceFile.replace(".java", "$mixin.java");
    }

    public File addFile(ClassNode classNode) {
        return this.addFile(this.defaultStratum, classNode);
    }

    public File addFile(String stratumName, ClassNode classNode) {
        return this.addFile(stratumName, classNode.sourceFile, classNode.name + ".java", Bytecode.getMaxLineNumber(classNode, 500, 50));
    }

    public File addFile(String sourceFileName, String sourceFilePath, int size) {
        return this.addFile(this.defaultStratum, sourceFileName, sourceFilePath, size);
    }

    public File addFile(String stratumName, String sourceFileName, String sourceFilePath, int size) {
        Stratum stratum = this.strata.get(stratumName);
        if (stratum == null) {
            stratum = new Stratum(stratumName);
            this.strata.put(stratumName, stratum);
        }
        File file = stratum.addFile(this.nextLineOffset, size, sourceFileName, sourceFilePath);
        this.nextLineOffset += size;
        return file;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.appendTo(sb);
        return sb.toString();
    }

    private void appendTo(StringBuilder sb) {
        sb.append("SMAP").append("\n");
        sb.append(this.getSourceFile()).append("\n");
        sb.append(this.defaultStratum).append("\n");
        for (Stratum stratum : this.strata.values()) {
            stratum.appendTo(sb);
        }
        sb.append("*E").append("\n");
    }

    static class Stratum {
        public final String name;
        private final Map<String, File> files = new LinkedHashMap<String, File>();

        public Stratum(String name) {
            this.name = name;
        }

        public File addFile(int lineOffset, int size, String sourceFileName, String sourceFilePath) {
            File file = this.files.get(sourceFilePath);
            if (file == null) {
                file = new File(this.files.size() + 1, lineOffset, size, sourceFileName, sourceFilePath);
                this.files.put(sourceFilePath, file);
            }
            return file;
        }

        void appendTo(StringBuilder sb) {
            sb.append("*S").append(" ").append(this.name).append("\n");
            sb.append("*F").append("\n");
            for (File file : this.files.values()) {
                file.appendFile(sb);
            }
            sb.append("*L").append("\n");
            for (File file : this.files.values()) {
                file.appendLines(sb);
            }
        }
    }

    public static class File {
        public final int id;
        public final int lineOffset;
        public final int size;
        public final String sourceFileName;
        public final String sourceFilePath;

        public File(int id, int lineOffset, int size, String sourceFileName) {
            this(id, lineOffset, size, sourceFileName, null);
        }

        public File(int id, int lineOffset, int size, String sourceFileName, String sourceFilePath) {
            this.id = id;
            this.lineOffset = lineOffset;
            this.size = size;
            this.sourceFileName = sourceFileName;
            this.sourceFilePath = sourceFilePath;
        }

        public void applyOffset(ClassNode classNode) {
            for (MethodNode method : classNode.methods) {
                this.applyOffset(method);
            }
        }

        public void applyOffset(MethodNode method) {
            for (AbstractInsnNode node : method.instructions) {
                if (!(node instanceof LineNumberNode)) continue;
                ((LineNumberNode)node).line += this.lineOffset - 1;
            }
        }

        void appendFile(StringBuilder sb) {
            if (this.sourceFilePath != null) {
                sb.append("+ ").append(this.id).append(" ").append(this.sourceFileName).append("\n");
                sb.append(this.sourceFilePath).append("\n");
            } else {
                sb.append(this.id).append(" ").append(this.sourceFileName).append("\n");
            }
        }

        public void appendLines(StringBuilder sb) {
            sb.append("1#").append(this.id).append(",").append(this.size).append(":").append(this.lineOffset).append("\n");
        }
    }
}

