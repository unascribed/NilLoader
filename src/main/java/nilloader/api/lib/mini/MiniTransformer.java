/*
 * Mini - an ASM-based class transformer reminiscent of MalisisCore and Mixin
 * 
 * The MIT License
 *
 * Copyright (c) 2017-2021 Una Thompson (unascribed) and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nilloader.api.lib.mini;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.cadixdev.bombe.type.FieldType;
import org.cadixdev.bombe.type.MethodDescriptor;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import nilloader.NilLoader;
import nilloader.NilLoaderLog;
import nilloader.api.ClassTransformer;
import nilloader.api.lib.mini.annotation.Patch;

import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public abstract class MiniTransformer implements ClassTransformer {
	
	private interface PatchMethod {
		boolean patch(PatchContext ctx) throws Throwable;
	}
	
	private final String classTargetName;
	private final Map<String, List<PatchMethod>> methods = new HashMap<String, List<PatchMethod>>();
	private final Set<String> requiredMethods = new HashSet<String>();
	private final Optional<MappingSet> mappings;
	
	public MiniTransformer() {
		this.mappings = Optional.ofNullable(NilLoader.getActiveMappings(NilLoader.getActiveMod()));
		Patch.Class classAnn = getClass().getAnnotation(Patch.Class.class);
		String className = classAnn.value().replace('.', '/');
		classTargetName = remapType(className);
		if (!className.equals(classTargetName)) {
			$$internal$logDebug("Retargeted {} from {} to {}", getClass().getSimpleName(), className, classTargetName);
		}
		for (final Method m : getClass().getMethods()) {
			final String name = m.getName();
			for (final Patch.Method a : m.getAnnotationsByType(Patch.Method.class)) {
				MethodSignature sig = MethodSignature.of(a.value());
				String desc = remapMethod(className, sig.getName(), sig.getDescriptor().toString())+remapMethodDesc(sig.getDescriptor().toString());
				if (!a.value().equals(desc)) {
					$$internal$logDebug("Retargeted {}.{} from {} to {}", getClass().getSimpleName(), name, a.value(), desc);
				}
				if (!methods.containsKey(desc)) {
					methods.put(desc, new ArrayList<PatchMethod>());
				}
				final boolean frames = m.getAnnotation(Patch.Method.AffectsControlFlow.class) != null;
				methods.get(desc).add(new PatchMethod() {
					@Override
					public boolean patch(PatchContext ctx) throws Throwable {
						try {
							m.invoke(MiniTransformer.this, ctx);
						} catch (InvocationTargetException e) {
							throw e.getCause();
						}
						return frames;
					}
					@Override
					public String toString() {
						return name;
					}
				});
				if (m.getAnnotation(Patch.Method.Optional.class) == null) {
					requiredMethods.add(desc);
				}
			}
		}
	}
	
	public String getClassTargetName() {
		return classTargetName;
	}
	
	@Override
	public final byte[] transform(String className, byte[] basicClass) {
		className = className.replace('.', '/');
		if (!classTargetName.equals(className)) return basicClass;
		
		ClassReader reader = new ClassReader(basicClass);
		ClassNode clazz = new ClassNode();
		reader.accept(clazz, 0);
		
		boolean frames = false;
		
		List<String> foundMethods = new ArrayList<String>();
		Set<String> requiredsNotSeen = new HashSet<String>(requiredMethods.size());
		requiredsNotSeen.addAll(requiredMethods);
		
		for (MethodNode mn : clazz.methods) {
			String name = mn.name+mn.desc;
			foundMethods.add(name);
			List<PatchMethod> li = methods.get(name);
			if (li != null) {
				for (PatchMethod pm : li) {
					try {
						PatchContext ctx = new PatchContext(mn);
						frames |= pm.patch(ctx);
						ctx.finish();
					} catch (Throwable t) {
						throw new Error("Failed to patch "+className+"."+mn.name+mn.desc+" via "+pm, t);
					}
					$$internal$logDebug("[{}] Successfully transformed {}.{}{} via {}", getClass().getName(), className, mn.name, mn.desc, pm);
				}
			}
			requiredsNotSeen.remove(name);
		}
		
		if (!requiredsNotSeen.isEmpty()) {
			StringBuilder msg = new StringBuilder();
			msg.append(requiredsNotSeen.size());
			msg.append(" required method");
			msg.append(requiredsNotSeen.size() == 1 ? " was" : "s were");
			msg.append(" not found while patching ");
			msg.append(className);
			msg.append("!");
			for (String name : requiredsNotSeen) {
				msg.append(" ");
				msg.append(name);
				msg.append(",");
			}
			msg.deleteCharAt(msg.length()-1);
			msg.append("\nThe following methods were found:");
			for (String name : foundMethods) {
				msg.append(" ");
				msg.append(name);
				msg.append(",");
			}
			msg.deleteCharAt(msg.length()-1);
			String msgS = msg.toString();
			$$internal$logError("[{}] {}", getClass().getName(), msgS);
			throw new Error(msgS);
		}
		
		int flags = ClassWriter.COMPUTE_MAXS;
		if (frames) {
			flags |= ClassWriter.COMPUTE_FRAMES;
		}
		ClassWriter writer = new ClassWriter(flags);
		clazz.accept(writer);
		return writer.toByteArray();
	}
	
	protected void $$internal$logDebug(String fmt, Object... params) {
		NilLoaderLog.log.debug(fmt, params);
	}

	protected void $$internal$logError(String fmt, Object... params) {
		NilLoaderLog.log.error(fmt, params);
	}

	private String remapType(String type) {
		return mappings.map(m -> m.computeClassMapping(type))
				.orElse(Optional.empty())
				.map(ClassMapping::getFullDeobfuscatedName)
				.orElse(type);
	}
	private String remapField(String owner, String name, String desc) {
		return mappings.map(m -> m.computeClassMapping(owner))
				.orElse(Optional.empty())
				.map(cm -> cm.computeFieldMapping(FieldSignature.of(name, desc)))
				.orElse(Optional.empty())
				.map(FieldMapping::getDeobfuscatedName)
				.orElse(name);
	}
	private String remapMethod(String owner, String name, String desc) {
		return mappings.map(m -> m.computeClassMapping(owner))
			.orElse(Optional.empty())
			.map(cm -> cm.getMethodMapping(name, desc))
			.orElse(Optional.empty())
			.map(MethodMapping::getDeobfuscatedSignature)
			.map(MethodSignature::getName)
			.orElse(name);
	}
	private String remapMethodDesc(String desc) {
		return mappings.map(m -> m.deobfuscate(MethodDescriptor.of(desc)))
				.map(MethodDescriptor::toString)
				.orElse(desc);
	}
	private String remapFieldDesc(String desc) {
		return mappings.map(m -> m.deobfuscate(FieldType.of(desc)))
				.map(FieldType::toString)
				.orElse(desc);
	}
	
	protected InsnNode NOP() { return new InsnNode(NOP); }
	protected InsnNode ACONST_NULL() { return new InsnNode(ACONST_NULL); }
	protected InsnNode ICONST_M1() { return new InsnNode(ICONST_M1); }
	protected InsnNode ICONST_0() { return new InsnNode(ICONST_0); }
	protected InsnNode ICONST_1() { return new InsnNode(ICONST_1); }
	protected InsnNode ICONST_2() { return new InsnNode(ICONST_2); }
	protected InsnNode ICONST_3() { return new InsnNode(ICONST_3); }
	protected InsnNode ICONST_4() { return new InsnNode(ICONST_4); }
	protected InsnNode ICONST_5() { return new InsnNode(ICONST_5); }
	protected InsnNode LCONST_0() { return new InsnNode(LCONST_0); }
	protected InsnNode LCONST_1() { return new InsnNode(LCONST_1); }
	protected InsnNode FCONST_0() { return new InsnNode(FCONST_0); }
	protected InsnNode FCONST_1() { return new InsnNode(FCONST_1); }
	protected InsnNode FCONST_2() { return new InsnNode(FCONST_2); }
	protected InsnNode DCONST_0() { return new InsnNode(DCONST_0); }
	protected InsnNode DCONST_1() { return new InsnNode(DCONST_1); }
	protected IntInsnNode BIPUSH(int i) { return new IntInsnNode(BIPUSH, i); }
	protected IntInsnNode SIPUSH(int i) { return new IntInsnNode(SIPUSH, i); }
	protected LdcInsnNode LDC(int v) { return new LdcInsnNode(v); }
	protected LdcInsnNode LDC(float v) { return new LdcInsnNode(v); }
	protected LdcInsnNode LDC(long v) { return new LdcInsnNode(v); }
	protected LdcInsnNode LDC(double v) { return new LdcInsnNode(v); }
	protected LdcInsnNode LDC(String v) { return new LdcInsnNode(v); }
	protected LdcInsnNode LDC(Type v) { return new LdcInsnNode(v); }
	protected VarInsnNode ILOAD(int var) { return new VarInsnNode(ILOAD, var); }
	protected VarInsnNode LLOAD(int var) { return new VarInsnNode(LLOAD, var); }
	protected VarInsnNode FLOAD(int var) { return new VarInsnNode(FLOAD, var); }
	protected VarInsnNode DLOAD(int var) { return new VarInsnNode(DLOAD, var); }
	protected VarInsnNode ALOAD(int var) { return new VarInsnNode(ALOAD, var); }
	protected InsnNode IALOAD() { return new InsnNode(IALOAD); }
	protected InsnNode LALOAD() { return new InsnNode(LALOAD); }
	protected InsnNode FALOAD() { return new InsnNode(FALOAD); }
	protected InsnNode DALOAD() { return new InsnNode(DALOAD); }
	protected InsnNode AALOAD() { return new InsnNode(AALOAD); }
	protected InsnNode BALOAD() { return new InsnNode(BALOAD); }
	protected InsnNode CALOAD() { return new InsnNode(CALOAD); }
	protected InsnNode SALOAD() { return new InsnNode(SALOAD); }
	protected VarInsnNode ISTORE(int var) { return new VarInsnNode(ISTORE, var); }
	protected VarInsnNode LSTORE(int var) { return new VarInsnNode(LSTORE, var); }
	protected VarInsnNode FSTORE(int var) { return new VarInsnNode(FSTORE, var); }
	protected VarInsnNode DSTORE(int var) { return new VarInsnNode(DSTORE, var); }
	protected VarInsnNode ASTORE(int var) { return new VarInsnNode(ASTORE, var); }
	protected InsnNode IASTORE() { return new InsnNode(IASTORE); }
	protected InsnNode LASTORE() { return new InsnNode(LASTORE); }
	protected InsnNode FASTORE() { return new InsnNode(FASTORE); }
	protected InsnNode DASTORE() { return new InsnNode(DASTORE); }
	protected InsnNode AASTORE() { return new InsnNode(AASTORE); }
	protected InsnNode BASTORE() { return new InsnNode(BASTORE); }
	protected InsnNode CASTORE() { return new InsnNode(CASTORE); }
	protected InsnNode SASTORE() { return new InsnNode(SASTORE); }
	protected InsnNode POP() { return new InsnNode(POP); }
	protected InsnNode POP2() { return new InsnNode(POP2); }
	protected InsnNode DUP() { return new InsnNode(DUP); }
	protected InsnNode DUP_X1() { return new InsnNode(DUP_X1); }
	protected InsnNode DUP_X2() { return new InsnNode(DUP_X2); }
	protected InsnNode DUP2() { return new InsnNode(DUP2); }
	protected InsnNode DUP2_X1() { return new InsnNode(DUP2_X1); }
	protected InsnNode DUP2_X2() { return new InsnNode(DUP2_X2); }
	protected InsnNode SWAP() { return new InsnNode(SWAP); }
	protected InsnNode IADD() { return new InsnNode(IADD); }
	protected InsnNode LADD() { return new InsnNode(LADD); }
	protected InsnNode FADD() { return new InsnNode(FADD); }
	protected InsnNode DADD() { return new InsnNode(DADD); }
	protected InsnNode ISUB() { return new InsnNode(ISUB); }
	protected InsnNode LSUB() { return new InsnNode(LSUB); }
	protected InsnNode FSUB() { return new InsnNode(FSUB); }
	protected InsnNode DSUB() { return new InsnNode(DSUB); }
	protected InsnNode IMUL() { return new InsnNode(IMUL); }
	protected InsnNode LMUL() { return new InsnNode(LMUL); }
	protected InsnNode FMUL() { return new InsnNode(FMUL); }
	protected InsnNode DMUL() { return new InsnNode(DMUL); }
	protected InsnNode IDIV() { return new InsnNode(IDIV); }
	protected InsnNode LDIV() { return new InsnNode(LDIV); }
	protected InsnNode FDIV() { return new InsnNode(FDIV); }
	protected InsnNode DDIV() { return new InsnNode(DDIV); }
	protected InsnNode IREM() { return new InsnNode(IREM); }
	protected InsnNode LREM() { return new InsnNode(LREM); }
	protected InsnNode FREM() { return new InsnNode(FREM); }
	protected InsnNode DREM() { return new InsnNode(DREM); }
	protected InsnNode INEG() { return new InsnNode(INEG); }
	protected InsnNode LNEG() { return new InsnNode(LNEG); }
	protected InsnNode FNEG() { return new InsnNode(FNEG); }
	protected InsnNode DNEG() { return new InsnNode(DNEG); }
	protected InsnNode ISHL() { return new InsnNode(ISHL); }
	protected InsnNode LSHL() { return new InsnNode(LSHL); }
	protected InsnNode ISHR() { return new InsnNode(ISHR); }
	protected InsnNode LSHR() { return new InsnNode(LSHR); }
	protected InsnNode IUSHR() { return new InsnNode(IUSHR); }
	protected InsnNode LUSHR() { return new InsnNode(LUSHR); }
	protected InsnNode IAND() { return new InsnNode(IAND); }
	protected InsnNode LAND() { return new InsnNode(LAND); }
	protected InsnNode IOR() { return new InsnNode(IOR); }
	protected InsnNode LOR() { return new InsnNode(LOR); }
	protected InsnNode IXOR() { return new InsnNode(IXOR); }
	protected InsnNode LXOR() { return new InsnNode(LXOR); }
	protected IincInsnNode IINC(int var, int incr) { return new IincInsnNode(var, incr); }
	protected InsnNode I2L() { return new InsnNode(I2L); }
	protected InsnNode I2F() { return new InsnNode(I2F); }
	protected InsnNode I2D() { return new InsnNode(I2D); }
	protected InsnNode L2I() { return new InsnNode(L2I); }
	protected InsnNode L2F() { return new InsnNode(L2F); }
	protected InsnNode L2D() { return new InsnNode(L2D); }
	protected InsnNode F2I() { return new InsnNode(F2I); }
	protected InsnNode F2L() { return new InsnNode(F2L); }
	protected InsnNode F2D() { return new InsnNode(F2D); }
	protected InsnNode D2I() { return new InsnNode(D2I); }
	protected InsnNode D2L() { return new InsnNode(D2L); }
	protected InsnNode D2F() { return new InsnNode(D2F); }
	protected InsnNode I2B() { return new InsnNode(I2B); }
	protected InsnNode I2C() { return new InsnNode(I2C); }
	protected InsnNode I2S() { return new InsnNode(I2S); }
	protected InsnNode LCMP() { return new InsnNode(LCMP); }
	protected InsnNode FCMPL() { return new InsnNode(FCMPL); }
	protected InsnNode FCMPG() { return new InsnNode(FCMPG); }
	protected InsnNode DCMPL() { return new InsnNode(DCMPL); }
	protected InsnNode DCMPG() { return new InsnNode(DCMPG); }
	protected JumpInsnNode IFEQ(LabelNode label) { return new JumpInsnNode(IFEQ, label); }
	protected JumpInsnNode IFNE(LabelNode label) { return new JumpInsnNode(IFNE, label); }
	protected JumpInsnNode IFLT(LabelNode label) { return new JumpInsnNode(IFLT, label); }
	protected JumpInsnNode IFGE(LabelNode label) { return new JumpInsnNode(IFGE, label); }
	protected JumpInsnNode IFGT(LabelNode label) { return new JumpInsnNode(IFGT, label); }
	protected JumpInsnNode IFLE(LabelNode label) { return new JumpInsnNode(IFLE, label); }
	protected JumpInsnNode IF_ICMPEQ(LabelNode label) { return new JumpInsnNode(IF_ICMPEQ, label); }
	protected JumpInsnNode IF_ICMPNE(LabelNode label) { return new JumpInsnNode(IF_ICMPNE, label); }
	protected JumpInsnNode IF_ICMPLT(LabelNode label) { return new JumpInsnNode(IF_ICMPLT, label); }
	protected JumpInsnNode IF_ICMPGE(LabelNode label) { return new JumpInsnNode(IF_ICMPGE, label); }
	protected JumpInsnNode IF_ICMPGT(LabelNode label) { return new JumpInsnNode(IF_ICMPGT, label); }
	protected JumpInsnNode IF_ICMPLE(LabelNode label) { return new JumpInsnNode(IF_ICMPLE, label); }
	protected JumpInsnNode IF_ACMPEQ(LabelNode label) { return new JumpInsnNode(IF_ACMPEQ, label); }
	protected JumpInsnNode IF_ACMPNE(LabelNode label) { return new JumpInsnNode(IF_ACMPNE, label); }
	protected JumpInsnNode GOTO(LabelNode label) { return new JumpInsnNode(GOTO, label); }
	protected JumpInsnNode JSR(LabelNode label) { return new JumpInsnNode(JSR, label); }
	protected VarInsnNode RET(int i) { return new VarInsnNode(RET, i); }
	protected TableSwitchInsnNode TABLESWITCH(int min, int max, LabelNode dflt, LabelNode... labels) { return new TableSwitchInsnNode(min, max, dflt, labels); }
	protected LookupSwitchInsnNode LOOKUPSWITCH(LabelNode dflt, int[] keys, LabelNode[] labels) { return new LookupSwitchInsnNode(dflt, keys, labels); }
	protected InsnNode IRETURN() { return new InsnNode(IRETURN); }
	protected InsnNode LRETURN() { return new InsnNode(LRETURN); }
	protected InsnNode FRETURN() { return new InsnNode(FRETURN); }
	protected InsnNode DRETURN() { return new InsnNode(DRETURN); }
	protected InsnNode ARETURN() { return new InsnNode(ARETURN); }
	protected InsnNode RETURN() { return new InsnNode(RETURN); }
	protected FieldInsnNode GETSTATIC(String owner, String name, String desc) { return new FieldInsnNode(GETSTATIC, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc)); }
	protected FieldInsnNode PUTSTATIC(String owner, String name, String desc) { return new FieldInsnNode(PUTSTATIC, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc)); }
	protected FieldInsnNode GETFIELD(String owner, String name, String desc) { return new FieldInsnNode(GETFIELD, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc)); }
	protected FieldInsnNode PUTFIELD(String owner, String name, String desc) { return new FieldInsnNode(PUTFIELD, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc)); }
	protected MethodInsnNode INVOKEVIRTUAL(String owner, String name, String desc) { return new MethodInsnNode(INVOKEVIRTUAL, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc)); }
	protected MethodInsnNode INVOKESPECIAL(String owner, String name, String desc) { return new MethodInsnNode(INVOKESPECIAL, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc)); }
	protected MethodInsnNode INVOKESTATIC(String owner, String name, String desc) { return new MethodInsnNode(INVOKESTATIC, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc)); }
	protected MethodInsnNode INVOKEINTERFACE(String owner, String name, String desc) { return new MethodInsnNode(INVOKEINTERFACE, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc)); }
	protected InvokeDynamicInsnNode INVOKEDYNAMIC(String name, String desc, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) { return new InvokeDynamicInsnNode(name, remapMethodDesc(desc), bootstrapMethodHandle, bootstrapMethodArguments); }
	protected TypeInsnNode NEW(String desc) { return new TypeInsnNode(NEW, remapType(desc)); }
	protected IntInsnNode NEWARRAY(int i) { return new IntInsnNode(NEWARRAY, i); }
	protected TypeInsnNode ANEWARRAY(String desc) { return new TypeInsnNode(ANEWARRAY, remapType(desc)); }
	protected InsnNode ARRAYLENGTH() { return new InsnNode(ARRAYLENGTH); }
	protected InsnNode ATHROW() { return new InsnNode(ATHROW); }
	protected TypeInsnNode CHECKCAST(String desc) { return new TypeInsnNode(CHECKCAST, remapType(desc)); }
	protected TypeInsnNode INSTANCEOF(String desc) { return new TypeInsnNode(INSTANCEOF, remapType(desc)); }
	protected InsnNode MONITORENTER() { return new InsnNode(MONITORENTER); }
	protected InsnNode MONITOREXIT() { return new InsnNode(MONITOREXIT); }
	protected MultiANewArrayInsnNode MULTIANEWARRAY(String desc, int dim) { return new MultiANewArrayInsnNode(remapType(desc), dim); }
	protected JumpInsnNode IFNULL(LabelNode label) { return new JumpInsnNode(IFNULL, label); }
	protected JumpInsnNode IFNONNULL(LabelNode label) { return new JumpInsnNode(IFNONNULL, label); }

}
