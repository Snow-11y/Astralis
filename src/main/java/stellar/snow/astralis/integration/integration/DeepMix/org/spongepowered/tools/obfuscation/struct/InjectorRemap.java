package org.spongepowered.tools.obfuscation.struct;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.struct.Message;

public class InjectorRemap {
    private final boolean remap;
    private Message message;
    private int remappedCount;

    public InjectorRemap(boolean remap) {
        this.remap = remap;
    }

    public boolean shouldRemap() {
        return this.remap;
    }

    public void notifyRemapped() {
        ++this.remappedCount;
        this.clearMessage();
    }

    public void addMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationHandle annotation) {
        this.message = new Message(type, msg, element, annotation);
    }

    public void clearMessage() {
        this.message = null;
    }

    public void dispatchPendingMessages(Messager messager) {
        if (this.remappedCount == 0 && this.message != null) {
            this.message.sendTo(messager);
        }
    }
}

