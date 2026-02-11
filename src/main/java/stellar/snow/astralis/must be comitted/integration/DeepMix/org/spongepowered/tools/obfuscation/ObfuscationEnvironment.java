package org.spongepowered.tools.obfuscation;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.ObfuscationUtil;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public abstract class ObfuscationEnvironment
implements IObfuscationEnvironment {
    protected final ObfuscationType type;
    protected final IMappingProvider mappingProvider;
    protected final IMappingWriter mappingWriter;
    protected final RemapperProxy remapper = new RemapperProxy();
    protected final IMixinAnnotationProcessor ap;
    protected final String outFileName;
    protected final List<String> inFileNames;
    private boolean initDone;

    protected ObfuscationEnvironment(ObfuscationType type) {
        this.type = type;
        this.ap = type.getAnnotationProcessor();
        this.inFileNames = type.getInputFileNames();
        this.outFileName = type.getOutputFileName();
        this.mappingProvider = this.getMappingProvider(this.ap, this.ap.getProcessingEnvironment().getFiler());
        this.mappingWriter = this.getMappingWriter(this.ap, this.ap.getProcessingEnvironment().getFiler());
    }

    public String toString() {
        return this.type.toString();
    }

    protected abstract IMappingProvider getMappingProvider(Messager var1, Filer var2);

    protected abstract IMappingWriter getMappingWriter(Messager var1, Filer var2);

    private boolean initMappings() {
        if (!this.initDone) {
            this.initDone = true;
            if (this.inFileNames == null) {
                this.ap.printMessage(Diagnostic.Kind.ERROR, (CharSequence)("The " + this.type.getConfig().getInputFileOption() + " argument was not supplied, obfuscation processing will not occur"));
                return false;
            }
            int successCount = 0;
            for (String inputFileName : this.inFileNames) {
                File inputFile = new File(inputFileName);
                try {
                    if (!inputFile.isFile()) continue;
                    this.ap.printMessage(IMessagerEx.MessageType.INFO, (CharSequence)("Loading " + this.type + " mappings from " + inputFile.getAbsolutePath()));
                    this.mappingProvider.read(inputFile);
                    ++successCount;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (successCount < 1) {
                this.ap.printMessage(Diagnostic.Kind.ERROR, (CharSequence)("No valid input files for " + this.type + " could be read, processing may not be sucessful."));
                this.mappingProvider.clear();
            }
        }
        return !this.mappingProvider.isEmpty();
    }

    public ObfuscationType getType() {
        return this.type;
    }

    @Override
    public MappingMethod getObfMethod(ITargetSelectorRemappable method) {
        MappingMethod obfd = this.getObfMethod(method.asMethodMapping());
        if (obfd != null || !method.isFullyQualified()) {
            return obfd;
        }
        TypeHandle type = this.ap.getTypeProvider().getTypeHandle(method.getOwner());
        if (type == null || type.isImaginary()) {
            return null;
        }
        TypeHandle superClass = type.getSuperclass();
        if (superClass == null) {
            return null;
        }
        String superClassName = superClass.getSimpleName();
        return this.getObfMethod(method.move(superClassName.replace('.', '/')));
    }

    @Override
    public MappingMethod getObfMethod(MappingMethod method) {
        return this.getObfMethod(method, true);
    }

    @Override
    public MappingMethod getObfMethod(MappingMethod method, boolean lazyRemap) {
        if (this.initMappings()) {
            boolean remapped = true;
            boolean superRef = false;
            MappingMethod mapping = null;
            for (MappingMethod md = method; md != null && mapping == null; md = md.getSuper()) {
                mapping = this.mappingProvider.getMethodMapping(md);
                superRef = true;
            }
            if (mapping == null) {
                if (lazyRemap) {
                    return null;
                }
                mapping = method.copy();
                remapped = false;
            } else if (superRef) {
                String obfOwner = this.getObfClass(method.getOwner());
                mapping = mapping.move(obfOwner != null ? obfOwner : method.getOwner());
            }
            String remappedOwner = this.getObfClass(mapping.getOwner());
            if (remappedOwner == null || remappedOwner.equals(method.getOwner()) || remappedOwner.equals(mapping.getOwner())) {
                return remapped ? mapping : null;
            }
            if (remapped) {
                return mapping.move(remappedOwner);
            }
            String desc = ObfuscationUtil.mapDescriptor(mapping.getDesc(), this.remapper);
            return new MappingMethod(remappedOwner, mapping.getSimpleName(), desc != null ? desc : mapping.getDesc());
        }
        return null;
    }

    @Override
    public ITargetSelectorRemappable remapDescriptor(ITargetSelectorRemappable method) {
        String newDesc;
        String desc;
        String newOwner;
        boolean transformed = false;
        String owner = method.getOwner();
        if (owner != null && (newOwner = this.remapper.map(owner)) != null) {
            owner = newOwner;
            transformed = true;
        }
        if ((desc = method.getDesc()) != null && (newDesc = ObfuscationUtil.mapDescriptor(method.getDesc(), this.remapper)) != null) {
            desc = newDesc;
            transformed = true;
        }
        return transformed ? method.move(owner).transform(desc) : null;
    }

    @Override
    public String remapDescriptor(String desc) {
        String newDesc = ObfuscationUtil.mapDescriptor(desc, this.remapper);
        return newDesc != null ? newDesc : desc;
    }

    @Override
    public MappingField getObfField(ITargetSelectorRemappable field) {
        return this.getObfField(field.asFieldMapping(), true);
    }

    @Override
    public MappingField getObfField(MappingField field) {
        return this.getObfField(field, true);
    }

    @Override
    public MappingField getObfField(MappingField field, boolean lazyRemap) {
        String remappedOwner;
        if (!this.initMappings()) {
            return null;
        }
        MappingField mapping = this.mappingProvider.getFieldMapping(field);
        if (mapping == null) {
            if (lazyRemap) {
                return null;
            }
            mapping = field;
        }
        if ((remappedOwner = this.getObfClass(mapping.getOwner())) == null || remappedOwner.equals(field.getOwner()) || remappedOwner.equals(mapping.getOwner())) {
            return mapping != field ? mapping : null;
        }
        return mapping.move(remappedOwner);
    }

    @Override
    public String getObfClass(String className) {
        if (!this.initMappings()) {
            return null;
        }
        return this.mappingProvider.getClassMapping(className);
    }

    @Override
    public void writeMappings(Collection<IMappingConsumer> consumers) {
        IMappingConsumer.MappingSet<MappingField> fields = new IMappingConsumer.MappingSet<MappingField>();
        IMappingConsumer.MappingSet<MappingMethod> methods = new IMappingConsumer.MappingSet<MappingMethod>();
        for (IMappingConsumer mappings : consumers) {
            fields.addAll(mappings.getFieldMappings(this.type));
            methods.addAll(mappings.getMethodMappings(this.type));
        }
        this.mappingWriter.write(this.outFileName, this.type, fields, methods);
    }

    final class RemapperProxy
    implements ObfuscationUtil.IClassRemapper {
        RemapperProxy() {
        }

        @Override
        public String map(String typeName) {
            if (ObfuscationEnvironment.this.mappingProvider == null) {
                return null;
            }
            return ObfuscationEnvironment.this.mappingProvider.getClassMapping(typeName);
        }

        @Override
        public String unmap(String typeName) {
            if (ObfuscationEnvironment.this.mappingProvider == null) {
                return null;
            }
            return ObfuscationEnvironment.this.mappingProvider.getClassMapping(typeName);
        }
    }
}

