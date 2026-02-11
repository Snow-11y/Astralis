package org.spongepowered.asm.mixin.injection.invoke.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.service.MixinService;

public class InsnFinder {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");

    public AbstractInsnNode findPopInsn(Target target, AbstractInsnNode node) {
        try {
            new PopAnalyzer(node).analyze(target.classNode.name, target.method);
        }
        catch (AnalyzerException ex) {
            if (ex.getCause() instanceof AnalysisResultException) {
                return ((AnalysisResultException)ex.getCause()).getResult();
            }
            logger.catching(ex);
        }
        return null;
    }

    static class PopAnalyzer
    extends Analyzer<BasicValue> {
        protected final AbstractInsnNode node;

        public PopAnalyzer(AbstractInsnNode node) {
            super((Interpreter)new BasicInterpreter());
            this.node = node;
        }

        protected Frame<BasicValue> newFrame(int locals, int stack) {
            return new PopFrame(locals, stack);
        }

        class PopFrame
        extends Frame<BasicValue> {
            private AbstractInsnNode current;
            private AnalyzerState state;
            private int depth;

            public PopFrame(int locals, int stack) {
                super(locals, stack);
                this.state = AnalyzerState.SEARCH;
                this.depth = 0;
            }

            public void execute(AbstractInsnNode insn, Interpreter<BasicValue> interpreter) throws AnalyzerException {
                this.current = insn;
                super.execute(insn, interpreter);
            }

            public void push(BasicValue value) throws IndexOutOfBoundsException {
                if (this.current == PopAnalyzer.this.node && this.state == AnalyzerState.SEARCH) {
                    this.state = AnalyzerState.ANALYSE;
                    ++this.depth;
                } else if (this.state == AnalyzerState.ANALYSE) {
                    ++this.depth;
                }
                super.push((Value)value);
            }

            public BasicValue pop() throws IndexOutOfBoundsException {
                if (this.state == AnalyzerState.ANALYSE && --this.depth == 0) {
                    this.state = AnalyzerState.COMPLETE;
                    throw new AnalysisResultException(this.current);
                }
                return (BasicValue)super.pop();
            }
        }
    }

    static enum AnalyzerState {
        SEARCH,
        ANALYSE,
        COMPLETE;

    }

    static class AnalysisResultException
    extends RuntimeException {
        private AbstractInsnNode result;

        public AnalysisResultException(AbstractInsnNode popNode) {
            this.result = popNode;
        }

        public AbstractInsnNode getResult() {
            return this.result;
        }
    }
}

