package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;

public interface ITargetSelector {
    public ITargetSelector next();

    public ITargetSelector configure(Configure var1, String ... var2);

    public ITargetSelector validate() throws InvalidSelectorException;

    public ITargetSelector attach(ISelectorContext var1) throws InvalidSelectorException;

    public int getMinMatchCount();

    public int getMaxMatchCount();

    public <TNode> MatchResult match(ElementNode<TNode> var1);

    public static enum Configure {
        SELECT_MEMBER(0),
        SELECT_INSTRUCTION(0),
        MOVE(1),
        ORPHAN(0),
        TRANSFORM(1),
        PERMISSIVE(0),
        CLEAR_LIMITS(0);

        private int requiredArgs;

        private Configure(int requiredArgs) {
            this.requiredArgs = requiredArgs;
        }

        public void checkArgs(String ... args) throws IllegalArgumentException {
            int argc;
            int n = argc = args == null ? 0 : args.length;
            if (argc < this.requiredArgs) {
                throw new IllegalArgumentException("Insufficient arguments for " + this.name() + " mutation. Required " + this.requiredArgs + " but received " + argc);
            }
        }
    }
}

