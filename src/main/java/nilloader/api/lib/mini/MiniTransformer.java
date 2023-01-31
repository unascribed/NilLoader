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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import nilloader.NilAgent;
import nilloader.NilLoaderLog;
import nilloader.api.ASMTransformer;
import nilloader.api.lib.mini.annotation.Patch;

import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public abstract class MiniTransformer implements ASMTransformer {
	
	private interface PatchMethod {
		boolean patch(PatchContext ctx) throws Throwable;
	}
	
	private final String classTargetName;
	private final Map<String, List<PatchMethod>> methods = new HashMap<String, List<PatchMethod>>();
	private final Set<String> requiredMethods = new HashSet<String>();
	private final Optional<MappingSet> mappings;
	
	public MiniTransformer() {
		this.mappings = Optional.ofNullable(NilAgent.getActiveMappings(NilAgent.getActiveMod()));
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
	
	/**
	 * Override point to perform modifications to a class that Mini's method patching is not sufficient
	 * for. This can be used to add entirely new methods, add interfaces, add fields, etc.
	 * <p>
	 * <b>Mini and NilGradle will not be aware of what you are doing in this method, and therefore
	 * <i>no remapping will occur</i>!</b>
	 * @return {@code true} if frames need to be computed
	 */
	protected boolean modifyClassStructure(ClassNode clazz) {
		return false;
	}
	
	public Set<String> getTargets() {
		return Collections.singleton(classTargetName);
	}
	
	@Override
	public final boolean transform(ClassLoader loader, ClassNode clazz) {
		String className = clazz.name.replace('.', '/');
		if (!classTargetName.equals(className)) return false;
		
		boolean frames = modifyClassStructure(clazz);
		
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
			throw new Error(msgS);
		}
		
		return frames;
	}
	
	@Override
	public final boolean canTransform(ClassLoader loader, String className) {
		return classTargetName.equals(className.replace('.', '/'));
	}
	
	@Override @Deprecated
	public final byte[] transform(ClassLoader loader, String className, byte[] originalData) {
		return ASMTransformer.super.transform(loader, className, originalData);
	}
	
	@Override @Deprecated
	public final byte[] transform(String className, byte[] originalData) {
		return ASMTransformer.super.transform(className, originalData);
	}
	
	protected void $$internal$logDebug(String fmt, Object... params) {
		NilLoaderLog.log.debug(fmt, params);
	}

	protected void $$internal$logError(String fmt, Object... params) {
		NilLoaderLog.log.error(fmt, params);
	}

	protected String remapType(String type) {
		return MiniUtils.remapType(mappings, type);
	}
	protected String remapField(String owner, String name, String desc) {
		return MiniUtils.remapField(mappings, owner, name, desc);
	}
	protected String remapMethod(String owner, String name, String desc) {
		return MiniUtils.remapMethod(mappings, owner, name, desc);
	}
	protected String remapMethodDesc(String desc) {
		return MiniUtils.remapMethodDesc(mappings, desc);
	}
	protected String remapFieldDesc(String desc) {
		return MiniUtils.remapFieldDesc(mappings, desc);
	}

	// Below javadocs based on https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5.frem
	// This took a lot of work. I hope someone other than me benefits from it...
	
	protected enum ArrayType {
		BOOLEAN(Opcodes.T_BOOLEAN),
		CHAR(Opcodes.T_CHAR),
		FLOAT(Opcodes.T_FLOAT),
		DOUBLE(Opcodes.T_DOUBLE),
		BYTE(Opcodes.T_BYTE),
		SHORT(Opcodes.T_SHORT),
		INT(Opcodes.T_INT),
		LONG(Opcodes.T_LONG),
		;
		private final int value;
		ArrayType(int value) {
			this.value = value;
		}
	}
	
	protected static final ArrayType T_BOOLEAN = ArrayType.BOOLEAN;
	protected static final ArrayType T_CHAR = ArrayType.CHAR;
	protected static final ArrayType T_FLOAT = ArrayType.FLOAT;
	protected static final ArrayType T_DOUBLE = ArrayType.DOUBLE;
	protected static final ArrayType T_BYTE = ArrayType.BYTE;
	protected static final ArrayType T_SHORT = ArrayType.SHORT;
	protected static final ArrayType T_INT = ArrayType.INT;
	protected static final ArrayType T_LONG = ArrayType.LONG;
	
	/**
	 * <b>Do nothing</b>
	 * <p>
	 * Do nothing.
	 */
	protected final InsnNode NOP() {
		return new InsnNode(NOP);
	}

	/**
	 * <b>Push null</b>
	 * <p>
	 * Push the null object reference onto the operand stack.
	 */
	protected final InsnNode ACONST_NULL() {
		return new InsnNode(ACONST_NULL);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant -1 onto the operand stack.
	 */
	protected final InsnNode ICONST_M1() {
		return new InsnNode(ICONST_M1);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant 0 onto the operand stack.
	 */
	protected final InsnNode ICONST_0() {
		return new InsnNode(ICONST_0);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant 1 onto the operand stack.
	 */
	protected final InsnNode ICONST_1() {
		return new InsnNode(ICONST_1);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant 2 onto the operand stack.
	 */
	protected final InsnNode ICONST_2() {
		return new InsnNode(ICONST_2);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant 3 onto the operand stack.
	 */
	protected final InsnNode ICONST_3() {
		return new InsnNode(ICONST_3);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant 4 onto the operand stack.
	 */
	protected final InsnNode ICONST_4() {
		return new InsnNode(ICONST_4);
	}

	/**
	 * <b>Push int constant</b>
	 * <p>
	 * Push the int constant 5 onto the operand stack.
	 */
	protected final InsnNode ICONST_5() {
		return new InsnNode(ICONST_5);
	}

	/**
	 * <b>Push long constant</b>
	 * <p>
	 * Push the long constant 0 onto the operand stack.
	 */
	protected final InsnNode LCONST_0() {
		return new InsnNode(LCONST_0);
	}

	/**
	 * <b>Push long constant</b>
	 * <p>
	 * Push the long constant 1 onto the operand stack.
	 */
	protected final InsnNode LCONST_1() {
		return new InsnNode(LCONST_1);
	}

	/**
	 * <b>Push float constant</b>
	 * <p>
	 * Push the float constant 0 onto the operand stack.
	 */
	protected final InsnNode FCONST_0() {
		return new InsnNode(FCONST_0);
	}

	/**
	 * <b>Push float constant</b>
	 * <p>
	 * Push the float constant 1 onto the operand stack.
	 */
	protected final InsnNode FCONST_1() {
		return new InsnNode(FCONST_1);
	}

	/**
	 * <b>Push float constant</b>
	 * <p>
	 * Push the float constant 2 onto the operand stack.
	 */
	protected final InsnNode FCONST_2() {
		return new InsnNode(FCONST_2);
	}

	/**
	 * <b>Push double constant</b>
	 * <p>
	 * Push the double constant 0 onto the operand stack.
	 */
	protected final InsnNode DCONST_0() {
		return new InsnNode(DCONST_0);
	}

	/**
	 * <b>Push double constant</b>
	 * <p>
	 * Push the double constant 1 onto the operand stack.
	 */
	protected final InsnNode DCONST_1() {
		return new InsnNode(DCONST_1);
	}

	/**
	 * <b>Push byte</b>
	 * <p>
	 * The immediate byte is sign-extended to an int value. That value is pushed onto the operand stack.
	 */
	protected final IntInsnNode BIPUSH(int i) {
		if (i < Byte.MIN_VALUE || i > Byte.MAX_VALUE) throw new IllegalArgumentException("Value out of range: "+i);
		return new IntInsnNode(BIPUSH, i);
	}

	/**
	 * <b>Push short</b>
	 * <p>
	 * The immediate short is sign-extended to an int value. That value is pushed onto the operand stack.
	 */
	protected final IntInsnNode SIPUSH(int i) {
		if (i < Short.MIN_VALUE || i > Short.MAX_VALUE) throw new IllegalArgumentException("Value out of range: "+i);
		return new IntInsnNode(SIPUSH, i);
	}

	/**
	 * <b>Push item from run-time constant pool</b>
	 * <p>
	 * The int is stored in the constant pool, and upon execution of the instruction, is pushed onto
	 * the operand stack.
	 */
	protected final LdcInsnNode LDC(int v) {
		return new LdcInsnNode(v);
	}

	/**
	 * <b>Push item from run-time constant pool</b>
	 * <p>
	 * The float is stored in the constant pool, and upon execution of the instruction, is pushed
	 * onto the operand stack.
	 * <p>
	 * The ldc instruction can only be used to push a value of type float taken from the float value
	 * set because a constant of type float in the constant pool must be taken from the float value
	 * set.
	 */
	protected final LdcInsnNode LDC(float v) {
		return new LdcInsnNode(v);
	}

	/**
	 * <b>Push item from run-time constant pool</b>
	 * <p>
	 * The long is stored in the constant pool, and upon execution of the instruction, is pushed
	 * onto the operand stack.
	 */
	protected final LdcInsnNode LDC(long v) {
		return new LdcInsnNode(v);
	}

	/**
	 * <b>Push item from run-time constant pool</b>
	 * <p>
	 * The double is stored in the constant pool, and upon execution of the instruction, is pushed
	 * onto the operand stack.
	 */
	protected final LdcInsnNode LDC(double v) {
		return new LdcInsnNode(v);
	}

	/**
	 * <b>Push item from run-time constant pool</b>
	 * <p>
	 * The String is stored in the constant pool, and upon execution of the instruction, is pushed
	 * onto the operand stack.
	 */
	protected final LdcInsnNode LDC(String v) {
		return new LdcInsnNode(v);
	}

	/**
	 * <b>Push item from run-time constant pool</b>
	 * <p>
	 * A symbolic reference to the given type is stored in the constant pool, and during linking,
	 * the reference is resolved. If successful, the reference is pushed onto the operand stack when
	 * this instruction is executed. Otherwise, an exception is thrown during linking.
	 */
	protected final LdcInsnNode LDC(Type v) {
		return new LdcInsnNode(v);
	}

	/**
	 * <b>Load int from local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The local
	 * variable at index must contain an int. The value of the local variable at index is pushed
	 * onto the operand stack.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode ILOAD(int var) {
		return new VarInsnNode(ILOAD, var);
	}

	/**
	 * <b>Load long from local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The local
	 * variable at index must contain a long. The value of the local variable at index is pushed
	 * onto the operand stack.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode LLOAD(int var) {
		return new VarInsnNode(LLOAD, var);
	}

	/**
	 * <b>Load float from local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The local
	 * variable at index must contain a float. The value of the local variable at index is pushed
	 * onto the operand stack.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode FLOAD(int var) {
		return new VarInsnNode(FLOAD, var);
	}

	/**
	 * <b>Load double from local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The local
	 * variable at index must contain a double. The value of the local variable at index is pushed
	 * onto the operand stack.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode DLOAD(int var) {
		return new VarInsnNode(DLOAD, var);
	}
	
	/**
	 * <b>Load reference from local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The local
	 * variable at index must contain a reference. The objectref in the local variable at index is
	 * pushed onto the operand stack.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode ALOAD(int var) {
		return new VarInsnNode(ALOAD, var);
	}

	/**
	 * <b>Load int from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type int. The index must be of type int. Both arrayref and index are popped from the operand
	 * stack. The int value in the component of the array at index is retrieved and pushed onto the
	 * operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode IALOAD() {
		return new InsnNode(IALOAD);
	}

	/**
	 * <b>Load long from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type long. The index must be of type int. Both arrayref and index are popped from the operand
	 * stack. The long value in the component of the array at index is retrieved and pushed onto the
	 * operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode LALOAD() {
		return new InsnNode(LALOAD);
	}

	/**
	 * <b>Load float from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type float. The index must be of type int. Both arrayref and index are popped from the
	 * operand stack. The float value in the component of the array at index is retrieved and pushed
	 * onto the operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode FALOAD() {
		return new InsnNode(FALOAD);
	}

	/**
	 * <b>Load double from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type double. The index must be of type int. Both arrayref and index are popped from the
	 * operand stack. The double value in the component of the array at index is retrieved and
	 * pushed onto the operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode DALOAD() {
		return new InsnNode(DALOAD);
	}

	/**
	 * <b>Load reference from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type reference. The index must be of type int. Both arrayref and index are popped from the
	 * operand stack. The reference value in the component of the array at index is retrieved and
	 * pushed onto the operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode AALOAD() {
		return new InsnNode(AALOAD);
	}

	/**
	 * <b>Load byte or boolean from array </b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type byte or of type boolean. The index must be of type int. Both arrayref and index are
	 * popped from the operand stack. The byte value in the component of the array at index is
	 * retrieved, sign-extended to an int value, and pushed onto the top of the operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode BALOAD() {
		return new InsnNode(BALOAD);
	}
	
	/**
	 * <b>Load char from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type char. The index must be of type int. Both arrayref and index are popped from the operand
	 * stack. The component of the array at index is retrieved and zero-extended to an int value.
	 * That value is pushed onto the operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode CALOAD() {
		return new InsnNode(CALOAD);
	}

	/**
	 * <b>Load short from array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type short. The index must be of type int. Both arrayref and index are popped from the
	 * operand stack. The component of the array at index is retrieved and sign-extended to an int
	 * value. That value is pushed onto the operand stack.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode SALOAD() {
		return new InsnNode(SALOAD);
	}

	/**
	 * <b>Store int into local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The value on
	 * the top of the operand stack must be of type int. It is popped from the operand stack, and
	 * the value of the local variable at index is set to value.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode ISTORE(int var) {
		return new VarInsnNode(ISTORE, var);
	}

	/**
	 * <b>Store long into local variable</b>
	 * <p>
	 * Both index and index+1 must be an index into the local variable array of the current frame.
	 * The value on the top of the operand stack must be of type long. It is popped from the operand
	 * stack, and the value of the local variable at index and index+1 are set to value.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode LSTORE(int var) {
		return new VarInsnNode(LSTORE, var);
	}

	/**
	 * <b>Store float into local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The value on
	 * the top of the operand stack must be of type float. It is popped from the operand stack, and
	 * the value of the local variable at index is set to value.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode FSTORE(int var) {
		return new VarInsnNode(FSTORE, var);
	}

	/**
	 * <b>Store double into local variable</b>
	 * <p>
	 * Both index and index+1 must be an index into the local variable array of the current frame.
	 * The value on the top of the operand stack must be of type double. It is popped from the
	 * operand stack, and the value of the local variable at index and index+1 are set to value.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode DSTORE(int var) {
		return new VarInsnNode(DSTORE, var);
	}

	/**
	 * <b>Store reference into local variable</b>
	 * <p>
	 * The index must be an index into the local variable array of the current frame. The objectref
	 * on the top of the operand stack must be of type reference. It is popped from the operand
	 * stack, and the value of the local variable at index is set to value.
	 * <p>
	 * Previously, the operand stack could also contain an objectref of type returnAddress, used for
	 * JSR/RET insns. However, this capability was deprecated and removed. A modern classfile will
	 * fail to verify if it contains JSR/RET insns.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn
	 * to allow further local variables.
	 */
	protected final VarInsnNode ASTORE(int var) {
		return new VarInsnNode(ASTORE, var);
	}

	/**
	 * <b>Store into int array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type int. Both index and value must be of type int. The arrayref, index, and value are
	 * popped from the operand stack. The int value is stored as the component of the array indexed
	 * by index.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode IASTORE() {
		return new InsnNode(IASTORE);
	}

	/**
	 * <b>Store into long array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type long. The index must be of type int, and value must be of type long. The arrayref,
	 * index, and value are popped from the operand stack. The long value is stored as the
	 * component of the array indexed by index.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode LASTORE() {
		return new InsnNode(LASTORE);
	}

	/**
	 * <b>Store into float array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type float. The index must be of type int, and value must be of type float. The arrayref,
	 * index, and value are popped from the operand stack. The float value is stored as the
	 * component of the array indexed by index.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode FASTORE() {
		return new InsnNode(FASTORE);
	}

	/**
	 * <b>Store into double array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type double. The index must be of type int, and value must be of type double. The arrayref,
	 * index, and value are popped from the operand stack. The double value is stored as the
	 * component of the array indexed by index.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode DASTORE() {
		return new InsnNode(DASTORE);
	}

	/**
	 * <b>Store into reference array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type reference. The index must be of type int, and value must be of type reference. The
	 * arrayref, index, and value are popped from the operand stack.
	 * <p>
	 * If value is null, then value is stored as the component of the array at index.
	 * <p>
	 * Otherwise, value is non-null. If the type of value is assignment compatible with the type of
	 * the components of the array referenced by arrayref, then value is stored as the component of
	 * the array at index.
	 * <p>
	 * The following rules are used to determine whether a value that is not null is assignment
	 * compatible with the array component type. If S is the type of the object referred to by
	 * value, and T is the reference type of the array components, then aastore determines whether
	 * assignment is compatible as follows:
	 * <ul>
	 * <li>If S is a class type, then:
	 * <ul>
	 * <li>If T is a class type, then S must be the same class as T, or S must be a subclass of T;</li>
	 * <li>If T is an interface type, then S must implement interface T.</li>
	 * </ul>
	 * </li>
	 * <li>If S is an array type SC[], that is, an array of components of type SC, then:
	 * <ul>
	 * <li>If T is a class type, then T must be Object.</li>
	 * <li>If T is an interface type, then T must be one of the interfaces implemented by arrays</li>
	 * <li>If T is an array type TC[], that is, an array of components of type TC, then one of the following must be true:
	 * <ul>
	 * <li>TC and SC are the same primitive type.
	 * <li>TC and SC are reference types, and type SC is assignable to TC by these run-time rules.
	 * </ul>
	 * </li>
	 * </ul>
	 * </ul>
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 * @throws ArrayStoreException if arrayref is not null and the actual type of the non-null value
	 * 		is not assignment compatible with the actual type of the components of the array
	 */
	protected final InsnNode AASTORE() {
		return new InsnNode(AASTORE);
	}

	/**
	 * <b>Store into byte or boolean array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type byte or of type boolean. The index and the value must both be of type int. The arrayref,
	 * index, and value are popped from the operand stack.
	 * <p>
	 * If the arrayref refers to an array whose components are of type byte, then the int value is
	 * truncated to a byte and stored as the component of the array indexed by index.
	 * <p>
	 * If the arrayref refers to an array whose components are of type boolean, then the int value
	 * is narrowed by taking the bitwise AND of value and 1; the result is stored as the component
	 * of the array indexed by index.
	 * <p>
	 * The bastore instruction is used to store values into both byte and boolean arrays. In
	 * Oracle's Java Virtual Machine implementation, boolean arrays - that is, arrays of type
	 * T_BOOLEAN - are implemented as arrays of 8-bit values. Other implementations may implement
	 * packed boolean arrays; in such implementations the bastore instruction must be able to store
	 * boolean values into packed boolean arrays as well as byte values into byte arrays.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode BASTORE() {
		return new InsnNode(BASTORE);
	}

	/**
	 * <b>Store into char array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type char. The index and the value must both be of type int. The arrayref, index, and value
	 * are popped from the operand stack. The int value is truncated to a char and stored as the
	 * component of the array indexed by index.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode CASTORE() {
		return new InsnNode(CASTORE);
	}

	/**
	 * <b>Store into short array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array whose components are of
	 * type short. The index and the value must both be of type int. The arrayref, index, and value
	 * are popped from the operand stack. The int value is truncated to a short and stored as the
	 * component of the array indexed by index.
	 * @throws NullPointerException if arrayref is null
	 * @throws ArrayIndexOutOfBoundsException if index is not within the bounds of the array
	 * 		referenced by arrayref
	 */
	protected final InsnNode SASTORE() {
		return new InsnNode(SASTORE);
	}

	/**
	 * <b>Pop the top operand stack value</b>
	 * <p>
	 * Pop the top value from the operand stack.
	 * <p>
	 * The pop instruction must not be used unless value is a value of a category 1 computational
	 * type. (i.e. it is not a long or double)
	 */
	protected final InsnNode POP() {
		return new InsnNode(POP);
	}

	/**
	 * <b>Pop the top one or two operand stack value</b>
	 * <p>
	 * Pop the top one or two values from the operand stack, depending on if it is a category 1 or
	 * 2 computational type.
	 */
	protected final InsnNode POP2() {
		return new InsnNode(POP2);
	}

	/**
	 * <b>Duplicate the top operand stack value</b>
	 * <p>
	 * Duplicate the top value on the operand stack and push the duplicated value onto the operand
	 * stack.
	 * <p>
	 * The dup instruction must not be used unless value is a value of a category 1 computational
	 * type. (i.e. it is not a long or double)
	 */
	protected final InsnNode DUP() {
		return new InsnNode(DUP);
	}

	/**
	 * <b>Duplicate the top operand stack value and insert two values down</b>
	 * <p>
	 * Duplicate the top value on the operand stack and insert the duplicated value two values down
	 * in the operand stack.
	 * <p>
	 * The dup_x1 instruction must not be used unless value and the previous value on the stack are
	 * of a category 1 computational type. (i.e. they are not a component of a long or double)
	 */
	protected final InsnNode DUP_X1() {
		return new InsnNode(DUP_X1);
	}

	/**
	 * <b>Duplicate the top operand stack value and insert two or three values down</b>
	 * <p>
	 * Duplicate the top value on the operand stack and insert the duplicated value two or three
	 * values down in the operand stack, depending on if the current and previous value are of a
	 * category 1 or 2 computational type.
	 */
	protected final InsnNode DUP_X2() {
		return new InsnNode(DUP_X2);
	}

	/**
	 * <b>Duplicate the top one or two operand stack values</b>
	 * <p>
	 * Duplicate the top one or two values on the operand stack and push the duplicated value or
	 * values, depending on if it is a category 1 or 2 computational type, back onto the operand
	 * stack in the original order.
	 */
	protected final InsnNode DUP2() {
		return new InsnNode(DUP2);
	}

	/**
	 * <b>Duplicate the top one or two operand stack values and insert two or three values down</b>
	 * <p>
	 * Duplicate the top one or two values on the operand stack and insert the duplicated values, in
	 * the original order, one value beneath the original value or values in the operand stack.
	 */
	protected final InsnNode DUP2_X1() {
		return new InsnNode(DUP2_X1);
	}

	/**
	 * <b>Duplicate the top one or two operand stack values and insert two, three, or four values down</b>
	 * <p>
	 * Duplicate the top one or two values on the operand stack and insert the duplicated values, in
	 * the original order, into the operand stack.
	 */
	protected final InsnNode DUP2_X2() {
		return new InsnNode(DUP2_X2);
	}

	/**
	 * <b>Swap the top two operand stack values</b>
	 * <p>
	 * Swap the top two values on the operand stack.
	 * <p>
	 * The swap instruction must not be used unless value1 and value2 are both values of a category
	 * 1 computational type. (i.e. they are not a component of a long or double)
	 */
	protected final InsnNode SWAP() {
		return new InsnNode(SWAP);
	}

	/**
	 * <b>Add int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. The
	 * int result is value1 + value2. The result is pushed onto the operand stack.
	 * <p>
	 * The result is the 32 low-order bits of the true mathematical result in a sufficiently wide
	 * two's-complement format, represented as a value of type int. If overflow occurs, then the
	 * sign of the result may not be the same as the sign of the mathematical sum of the two values.
	 * <p>
	 * Despite the fact that overflow may occur, execution of an iadd instruction never throws a
	 * run-time exception.
	 */
	protected final InsnNode IADD() {
		return new InsnNode(IADD);
	}

	/**
	 * <b>Add long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. The values are popped from the operand stack.
	 * The long result is value1 + value2. The result is pushed onto the operand stack.
	 * <p>
	 * The result is the 64 low-order bits of the true mathematical result in a sufficiently wide
	 * two's-complement format, represented as a value of type long. If overflow occurs, the sign of
	 * the result may not be the same as the sign of the mathematical sum of the two values.
	 * <p>
	 * Despite the fact that overflow may occur, execution of an ladd instruction never throws a
	 * run-time exception.
	 */
	protected final InsnNode LADD() {
		return new InsnNode(LADD);
	}

	/**
	 * <b>Add float</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The float result is
	 * value1' + value2'. The result is pushed onto the operand stack.
	 * <p>
	 * The result of an fadd instruction is governed by the rules of IEEE arithmetic:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>The sum of two infinities of opposite sign is NaN.
	 * <li>The sum of two infinities of the same sign is the infinity of that sign.
	 * <li>The sum of an infinity and any finite value is equal to the infinity.
	 * <li>The sum of two zeroes of opposite sign is positive zero.
	 * <li>The sum of two zeroes of the same sign is the zero of that sign.
	 * <li>The sum of a zero and a nonzero finite value is equal to the nonzero value.
	 * <li>The sum of two nonzero finite values of the same magnitude and opposite sign is positive
	 * 		zero.
	 * <li>In the remaining cases, where neither operand is an infinity, a zero, or NaN and the
	 * 		values have the same sign or have different magnitudes, the sum is computed and rounded
	 * 		to the nearest representable value using IEEE 754 round to nearest mode. If the
	 * 		magnitude is too large to represent as a float, we say the operation overflows; the
	 * 		result is then an infinity of appropriate sign. If the magnitude is too small to
	 * 		represent as a float, we say the operation underflows; the result is then a zero of
	 * 		appropriate sign.
	 * </ul>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, or loss of precision may occur, execution of an
	 * fadd instruction never throws a run-time exception.
	 */
	protected final InsnNode FADD() {
		return new InsnNode(FADD);
	}

	/**
	 * <b>Add double</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The double result is
	 * value1' + value2'. The result is pushed onto the operand stack.
	 * <p>
	 * The result of a dadd instruction is governed by the rules of IEEE arithmetic:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>The sum of two infinities of opposite sign is NaN.
	 * <li>The sum of two infinities of the same sign is the infinity of that sign.
	 * <li>The sum of an infinity and any finite value is equal to the infinity.
	 * <li>The sum of two zeroes of opposite sign is positive zero.
	 * <li>The sum of two zeroes of the same sign is the zero of that sign.
	 * <li>The sum of a zero and a nonzero finite value is equal to the nonzero value.
	 * <li>The sum of two nonzero finite values of the same magnitude and opposite sign is positive
	 * 		zero.
	 * <li>In the remaining cases, where neither operand is an infinity, a zero, or NaN and the values
	 * 		have the same sign or have different magnitudes, the sum is computed and rounded to the
	 * 		nearest representable value using IEEE 754 round to nearest mode. If the magnitude is too
	 * 		large to represent as a double, we say the operation overflows; the result is then an
	 * 		infinity of appropriate sign. If the magnitude is too small to represent as a double, we
	 * 		say the operation underflows; the result is then a zero of appropriate sign.
	 * </ul>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, or loss of precision may occur, execution of a
	 * dadd instruction never throws a run-time exception.
	 */
	protected final InsnNode DADD() {
		return new InsnNode(DADD);
	}

	/**
	 * <b>Subtract int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. The
	 * int result is value1 - value2. The result is pushed onto the operand stack.
	 * <p>
	 * For int subtraction, a-b produces the same result as a+(-b). For int values, subtraction from
	 * zero is the same as negation.
	 * <p>
	 * The result is the 32 low-order bits of the true mathematical result in a sufficiently wide
	 * two's-complement format, represented as a value of type int. If overflow occurs, then the
	 * sign of the result may not be the same as the sign of the mathematical difference of the two
	 * values.
	 * <p>
	 * Despite the fact that overflow may occur, execution of an isub instruction never throws a
	 * run-time exception.
	 */
	protected final InsnNode ISUB() {
		return new InsnNode(ISUB);
	}

	/**
	 * <b>Subtract long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. The values are popped from the operand stack.
	 * The long result is value1 - value2. The result is pushed onto the operand stack.
	 * <p>
	 * For long subtraction, a-b produces the same result as a+(-b). For long values, subtraction
	 * from zero is the same as negation.
	 * <p>
	 * The result is the 64 low-order bits of the true mathematical result in a sufficiently wide
	 * two's-complement format, represented as a value of type long. If overflow occurs, then the
	 * sign of the result may not be the same as the sign of the mathematical difference of the two
	 * values.
	 * <p>
	 * Despite the fact that overflow may occur, execution of an lsub instruction never throws a
	 * run-time exception.
	 */
	protected final InsnNode LSUB() {
		return new InsnNode(LSUB);
	}

	/**
	 * <b>Subtract float</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The float result is
	 * value1' - value2'. The result is pushed onto the operand stack.
	 * <p>
	 * For float subtraction, it is always the case that a-b produces the same result as a+(-b).
	 * However, for the fsub instruction, subtraction from zero is not the same as negation, because
	 * if x is +0.0, then 0.0-x equals +0.0, but -x equals -0.0.
	 * <p>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, or loss of precision may occur, execution of an
	 * fsub instruction never throws a run-time exception.
	 */
	protected final InsnNode FSUB() {
		return new InsnNode(FSUB);
	}

	/**
	 * <b>Subtract double</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The double result is
	 * value1' - value2'. The result is pushed onto the operand stack.
	 * <p>
	 * For double subtraction, it is always the case that a-b produces the same result as a+(-b).
	 * However, for the dsub instruction, subtraction from zero is not the same as negation, because
	 * if x is +0.0, then 0.0-x equals +0.0, but -x equals -0.0.
	 * <p>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, or loss of precision may occur, execution of a
	 * dsub instruction never throws a run-time exception.
	 */
	protected final InsnNode DSUB() {
		return new InsnNode(DSUB);
	}

	/**
	 * <b>Multiply int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. The
	 * int result is value1 * value2. The result is pushed onto the operand stack.
	 * <p>
	 * The result is the 32 low-order bits of the true mathematical result in a sufficiently wide
	 * two's-complement format, represented as a value of type int. If overflow occurs, then the
	 * sign of the result may not be the same as the sign of the mathematical multiplication of the
	 * two values.
	 * <p>
	 * Despite the fact that overflow may occur, execution of an imul instruction never throws a
	 * run-time exception.
	 */
	protected final InsnNode IMUL() {
		return new InsnNode(IMUL);
	}

	/**
	 * <b>Multiply long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. The values are popped from the operand stack.
	 * The long result is value1 * value2. The result is pushed onto the operand stack.
	 * <p>
	 * The result is the 64 low-order bits of the true mathematical result in a sufficiently wide
	 * two's-complement format, represented as a value of type long. If overflow occurs, the sign of
	 * the result may not be the same as the sign of the mathematical multiplication of the two
	 * values.
	 * <p>
	 * Despite the fact that overflow may occur, execution of an lmul instruction never throws a
	 * run-time exception.
	 */
	protected final InsnNode LMUL() {
		return new InsnNode(LMUL);
	}

	/**
	 * <b>Multiply float</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The float result is
	 * value1' * value2'. The result is pushed onto the operand stack.
	 * <p>
	 * The result of an fmul instruction is governed by the rules of IEEE arithmetic:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>If neither value1' nor value2' is NaN, the sign of the result is positive if both values
	 * 		have the same sign, and negative if the values have different signs.
	 * <li>Multiplication of an infinity by a zero results in NaN.
	 * <li>Multiplication of an infinity by a finite value results in a signed infinity, with the
	 * 		sign-producing rule just given.
	 * <li>In the remaining cases, where neither an infinity nor NaN is involved, the product is
	 * 		computed and rounded to the nearest representable value using IEEE 754 round to nearest
	 * 		mode. If the magnitude is too large to represent as a float, we say the operation
	 * 		overflows; the result is then an infinity of appropriate sign. If the magnitude is too
	 * 		small to represent as a float, we say the operation underflows; the result is then a
	 * 		zero of appropriate sign.
	 * </ul>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, or loss of precision may occur, execution of an
	 * fmul instruction never throws a run-time exception.
	 */
	protected final InsnNode FMUL() {
		return new InsnNode(FMUL);
	}

	/**
	 * <b>Multiply double</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The double result is
	 * value1' * value2'. The result is pushed onto the operand stack.
	 * <p>
	 * The result of a dmul instruction is governed by the rules of IEEE arithmetic:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>If neither value1' nor value2' is NaN, the sign of the result is positive if both values
	 * 		have the same sign and negative if the values have different signs.
	 * <li>Multiplication of an infinity by a zero results in NaN.
	 * <li>Multiplication of an infinity by a finite value results in a signed infinity, with the
	 * 		sign-producing rule just given.
	 * <li>In the remaining cases, where neither an infinity nor NaN is involved, the product is
	 * 		computed and rounded to the nearest representable value using IEEE 754 round to nearest
	 * 		mode. If the magnitude is too large to represent as a double, we say the operation
	 * 		overflows; the result is then an infinity of appropriate sign. If the magnitude is too
	 * 		small to represent as a double, we say the operation underflows; the result is then a
	 * 		zero of appropriate sign.
	 * </ul>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, or loss of precision may occur, execution of a
	 * dmul instruction never throws a run-time exception.
	 */
	protected final InsnNode DMUL() {
		return new InsnNode(DMUL);
	}

	/**
	 * <b>Divide int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. The
	 * int result is the value of the Java programming language expression value1 / value2. The
	 * result is pushed onto the operand stack.
	 * <p>
	 * An int division rounds towards 0; that is, the quotient produced for int values in n/d is an
	 * int value q whose magnitude is as large as possible while satisfying |d ⋅ q| ≤ |n|. Moreover,
	 * q is positive when |n| ≥ |d| and n and d have the same sign, but q is negative when |n| ≥ |d|
	 * and n and d have opposite signs.
	 * <p>
	 * There is one special case that does not satisfy this rule: if the dividend is the negative
	 * integer of largest possible magnitude for the int type, and the divisor is -1, then overflow
	 * occurs, and the result is equal to the dividend. Despite the overflow, no exception is thrown
	 * in this case.
	 * @throws ArithmeticException if the value of the divisor is 0
	 */
	protected final InsnNode IDIV() {
		return new InsnNode(IDIV);
	}

	/**
	 * <b>Divide long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. The values are popped from the operand stack.
	 * The long result is the value of the Java programming language expression value1 / value2. The
	 * result is pushed onto the operand stack.
	 * <p>
	 * A long division rounds towards 0; that is, the quotient produced for long values in n / d is
	 * a long value q whose magnitude is as large as possible while satisfying |d ⋅ q| ≤ |n|.
	 * Moreover, q is positive when |n| ≥ |d| and n and d have the same sign, but q is negative when
	 * |n| ≥ |d| and n and d have opposite signs.
	 * <p>
	 * There is one special case that does not satisfy this rule: if the dividend is the negative
	 * integer of largest possible magnitude for the long type and the divisor is -1, then overflow
	 * occurs and the result is equal to the dividend; despite the overflow, no exception is thrown
	 * in this case.
	 * @throws ArithmeticException if the value of the divisor is 0
	 */
	protected final InsnNode LDIV() {
		return new InsnNode(LDIV);
	}

	/**
	 * <b>Divide float</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The float result is
	 * value1' / value2'. The result is pushed onto the operand stack.
	 * <p>
	 * The result of an fdiv instruction is governed by the rules of IEEE arithmetic:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>If neither value1' nor value2' is NaN, the sign of the result is positive if both values have
	 * 		the same sign, negative if the values have different signs.
	 * <li>Division of an infinity by an infinity results in NaN.
	 * <li>Division of an infinity by a finite value results in a signed infinity, with the
	 * 		sign-producing rule just given.
	 * <li>Division of a finite value by an infinity results in a signed zero, with the sign-producing
	 * 		rule just given.
	 * <li>Division of a zero by a zero results in NaN; division of zero by any other finite value
	 * 		results in a signed zero, with the sign-producing rule just given.
	 * <li>Division of a nonzero finite value by a zero results in a signed infinity, with the
	 * 		sign-producing rule just given.
	 * <li>In the remaining cases, where neither operand is an infinity, a zero, or NaN, the quotient is
	 * 		computed and rounded to the nearest float using IEEE 754 round to nearest mode. If the
	 * 		magnitude is too large to represent as a float, we say the operation overflows; the result is
	 * 		then an infinity of appropriate sign. If the magnitude is too small to represent as a float,
	 * 		we say the operation underflows; the result is then a zero of appropriate sign.
	 * </ul>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, division by zero, or loss of precision may occur,
	 * execution of an fdiv instruction never throws a run-time exception.
	 */
	protected final InsnNode FDIV() {
		return new InsnNode(FDIV);
	}

	/**
	 * <b>Divide double</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The double result is
	 * value1' / value2'. The result is pushed onto the operand stack.
	 * <p>
	 * The result of a ddiv instruction is governed by the rules of IEEE arithmetic:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>If neither value1' nor value2' is NaN, the sign of the result is positive if both values have
	 * 		the same sign, negative if the values have different signs.
	 * <li>Division of an infinity by an infinity results in NaN.
	 * <li>Division of an infinity by a finite value results in a signed infinity, with the
	 * 		sign-producing rule just given.
	 * <li>Division of a finite value by an infinity results in a signed zero, with the sign-producing
	 * 		rule just given.
	 * <li>Division of a zero by a zero results in NaN; division of zero by any other finite value
	 * 		results in a signed zero, with the sign-producing rule just given.
	 * <li>Division of a nonzero finite value by a zero results in a signed infinity, with the
	 * 		sign-producing rule just given.
	 * <li>In the remaining cases, where neither operand is an infinity, a zero, or NaN, the quotient is
	 * 		computed and rounded to the nearest double using IEEE 754 round to nearest mode. If the
	 * 		magnitude is too large to represent as a double, we say the operation overflows; the result
	 * 		is then an infinity of appropriate sign. If the magnitude is too small to represent as a
	 * 		double, we say the operation underflows; the result is then a zero of appropriate sign.
	 * </ul>
	 * The Java Virtual Machine requires support of gradual underflow as defined by IEEE 754.
	 * Despite the fact that overflow, underflow, division by zero, or loss of precision may occur,
	 * execution of a ddiv instruction never throws a run-time exception.
	 */
	protected final InsnNode DDIV() {
		return new InsnNode(DDIV);
	}

	/**
	 * <b>Remainder int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. The
	 * int result is value1 - (value1 / value2) * value2. The result is pushed onto the operand
	 * stack.
	 * <p>
	 * The result of the irem instruction is such that (a/b)*b + (a%b) is equal to a. This identity
	 * holds even in the special case in which the dividend is the negative int of largest possible
	 * magnitude for its type and the divisor is -1 (the remainder is 0). It follows from this rule
	 * that the result of the remainder operation can be negative only if the dividend is negative
	 * and can be positive only if the dividend is positive. Moreover, the magnitude of the result
	 * is always less than the magnitude of the divisor.
	 * @throws ArithmeticException if the value of the divisor is 0
	 */
	protected final InsnNode IREM() {
		return new InsnNode(IREM);
	}

	/**
	 * <b>Remainder long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. The values are popped from the operand stack.
	 * The long result is value1 - (value1 / value2) * value2. The result is pushed onto the operand
	 * stack.
	 * <p>
	 * The result of the lrem instruction is such that (a/b)*b + (a%b) is equal to a. This identity
	 * holds even in the special case in which the dividend is the negative long of largest possible
	 * magnitude for its type and the divisor is -1 (the remainder is 0). It follows from this rule
	 * that the result of the remainder operation can be negative only if the dividend is negative
	 * and can be positive only if the dividend is positive; moreover, the magnitude of the result
	 * is always less than the magnitude of the divisor.
	 * @throws ArithmeticException if the value of the divisor is 0
	 */
	protected final InsnNode LREM() {
		return new InsnNode(LREM);
	}

	/**
	 * <b>Remainder float</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The result is calculated
	 * and pushed onto the operand stack as a float.
	 * <p>
	 * The result of an frem instruction is not the same as that of the so-called remainder
	 * operation defined by IEEE 754. The IEEE 754 "remainder" operation computes the remainder from
	 * a rounding division, not a truncating division, and so its behavior is not analogous to that
	 * of the usual integer remainder operator. Instead, the Java Virtual Machine defines frem to
	 * behave in a manner analogous to that of the Java Virtual Machine integer remainder
	 * instructions (irem and lrem); this may be compared with the C library function fmod.
	 * <p>
	 * The result of an frem instruction is governed by these rules:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>If neither value1' nor value2' is NaN, the sign of the result equals the sign of the
	 * 		dividend.
	 * <li>If the dividend is an infinity or the divisor is a zero or both, the result is NaN.
	 * <li>If the dividend is finite and the divisor is an infinity, the result equals the dividend.
	 * <li>If the dividend is a zero and the divisor is finite, the result equals the dividend.
	 * <li>In the remaining cases, where neither operand is an infinity, a zero, or NaN, the
	 * 		floating-point remainder result from a dividend value1' and a divisor value2' is defined
	 * 		by the mathematical relation result = value1' - (value2' * q), where q is an integer
	 * 		that is negative only if value1' / value2' is negative and positive only if value1' /
	 * 		value2' is positive, and whose magnitude is as large as possible without exceeding the
	 * 		magnitude of the true mathematical quotient of value1' and value2'.
	 * <p>
	 * Despite the fact that division by zero may occur, evaluation of an frem instruction never
	 * throws a run-time exception. Overflow, underflow, or loss of precision cannot occur.
	 * <p>
	 * The IEEE 754 remainder operation may be computed by the library routine Math.IEEEremainder.
	 */
	protected final InsnNode FREM() {
		return new InsnNode(FREM);
	}

	/**
	 * <b>Remainder double</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. The result is calculated
	 * and pushed onto the operand stack as a double.
	 * <p>
	 * The result of a drem instruction is not the same as that of the so-called remainder operation
	 * defined by IEEE 754. The IEEE 754 "remainder" operation computes the remainder from a
	 * rounding division, not a truncating division, and so its behavior is not analogous to that of
	 * the usual integer remainder operator. Instead, the Java Virtual Machine defines drem to
	 * behave in a manner analogous to that of the Java Virtual Machine integer remainder
	 * instructions (irem and lrem); this may be compared with the C library function fmod.
	 * <p>
	 * The result of a drem instruction is governed by these rules:
	 * <ul>
	 * <li>If either value1' or value2' is NaN, the result is NaN.
	 * <li>If neither value1' nor value2' is NaN, the sign of the result equals the sign of the
	 * 		dividend.
	 * <li>If the dividend is an infinity or the divisor is a zero or both, the result is NaN.
	 * <li>If the dividend is finite and the divisor is an infinity, the result equals the dividend.
	 * <li>If the dividend is a zero and the divisor is finite, the result equals the dividend.
	 * <li>In the remaining cases, where neither operand is an infinity, a zero, or NaN, the
	 * 		floating-point remainder result from a dividend value1' and a divisor value2' is defined
	 * 		by the mathematical relation result = value1' - (value2' * q), where q is an integer
	 * 		that is negative only if value1' / value2' is negative, and positive only if value1' /
	 * 		value2' is positive, and whose magnitude is as large as possible without exceeding the
	 * 		magnitude of the true mathematical quotient of value1' and value2'.
	 * <p>
	 * Despite the fact that division by zero may occur, evaluation of a drem instruction never
	 * throws a run-time exception. Overflow, underflow, or loss of precision cannot occur.
	 * <p>
	 * The IEEE 754 remainder operation may be computed by the library routine Math.IEEEremainder.
	 */
	protected final InsnNode DREM() {
		return new InsnNode(DREM);
	}

	/**
	 * <b>Negate int</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack. The int result is the
	 * arithmetic negation of value, -value. The result is pushed onto the operand stack.
	 * <p>
	 * For int values, negation is the same as subtraction from zero. Because the Java Virtual
	 * Machine uses two's-complement representation for integers and the range of two's-complement
	 * values is not symmetric, the negation of the maximum negative int results in that same
	 * maximum negative number. Despite the fact that overflow has occurred, no exception is thrown.
	 * <p>
	 * For all int values x, -x equals (~x)+1.
	 */
	protected final InsnNode INEG() {
		return new InsnNode(INEG);
	}

	/**
	 * <b>Negate long</b>
	 * <p>
	 * The value must be of type long. It is popped from the operand stack. The long result is the
	 * arithmetic negation of value, -value. The result is pushed onto the operand stack.
	 * <p>
	 * For long values, negation is the same as subtraction from zero. Because the Java Virtual
	 * Machine uses two's-complement representation for integers and the range of two's-complement
	 * values is not symmetric, the negation of the maximum negative long results in that same
	 * maximum negative number. Despite the fact that overflow has occurred, no exception is thrown.
	 * <p>
	 * For all long values x, -x equals (~x)+1.
	 */
	protected final InsnNode LNEG() {
		return new InsnNode(LNEG);
	}

	/**
	 * <b>Negate float</b>
	 * <p>
	 * The value must be of type float. It is popped from the operand stack and undergoes value set
	 * conversion, resulting in value'. The float result is the arithmetic negation of value'. This
	 * result is pushed onto the operand stack.
	 * <p>
	 * For float values, negation is not the same as subtraction from zero. If x is +0.0, then 0.0-x
	 * equals +0.0, but -x equals -0.0. Unary minus merely inverts the sign of a float.
	 * <p>
	 * Special cases of interest:
	 * <ul>
	 * <li>If the operand is NaN, the result is NaN (recall that NaN has no sign).
	 * <li>If the operand is an infinity, the result is the infinity of opposite sign.
	 * <li>If the operand is a zero, the result is the zero of opposite sign.
	 * </ul>
	 */
	protected final InsnNode FNEG() {
		return new InsnNode(FNEG);
	}

	/**
	 * <b>Negate double</b>
	 * <p>
	 * The value must be of type double. It is popped from the operand stack and undergoes value set
	 * conversion (§2.8.3), resulting in value'. The double result is the arithmetic negation of
	 * value'. The result is pushed onto the operand stack.
	 * <p>
	 * For double values, negation is not the same as subtraction from zero. If x is +0.0, then
	 * 0.0-x equals +0.0, but -x equals -0.0. Unary minus merely inverts the sign of a double.
	 * <p>
	 * Special cases of interest:
	 * <ul>
	 * <li>If the operand is NaN, the result is NaN (recall that NaN has no sign).
	 * <li>If the operand is an infinity, the result is the infinity of opposite sign.
	 * <li>If the operand is a zero, the result is the zero of opposite sign.
	 * </ul>
	 */
	protected final InsnNode DNEG() {
		return new InsnNode(DNEG);
	}

	/**
	 * <b>Shift left int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. An
	 * int result is calculated by shifting value1 left by s bit positions, where s is the value of
	 * the low 5 bits of value2. The result is pushed onto the operand stack.
	 * <p>
	 * This is equivalent (even if overflow occurs) to multiplication by 2 to the power s. The shift
	 * distance actually used is always in the range 0 to 31, inclusive, as if value2 were subjected
	 * to a bitwise logical AND with the mask value 0x1f.
	 */
	protected final InsnNode ISHL() {
		return new InsnNode(ISHL);
	}

	/**
	 * <b>Shift left long</b>
	 * <p>
	 * The value1 must be of type long, and value2 must be of type int. The values are popped from
	 * the operand stack. A long result is calculated by shifting value1 left by s bit positions,
	 * where s is the low 6 bits of value2. The result is pushed onto the operand stack.
	 * <p>
	 * This is equivalent (even if overflow occurs) to multiplication by 2 to the power s. The shift
	 * distance actually used is therefore always in the range 0 to 63, inclusive, as if value2 were
	 * subjected to a bitwise logical AND with the mask value 0x3f.
	 */
	protected final InsnNode LSHL() {
		return new InsnNode(LSHL);
	}

	/**
	 * <b>Arithmetic shift right int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. An
	 * int result is calculated by shifting value1 right by s bit positions, with sign extension,
	 * where s is the value of the low 5 bits of value2. The result is pushed onto the operand
	 * stack.
	 * <p>
	 * The resulting value is floor(value1 / 2s), where s is value2 & 0x1f. For non-negative value1,
	 * this is equivalent to truncating int division by 2 to the power s. The shift distance
	 * actually used is always in the range 0 to 31, inclusive, as if value2 were subjected to a
	 * bitwise logical AND with the mask value 0x1f.
	 */
	protected final InsnNode ISHR() {
		return new InsnNode(ISHR);
	}

	/**
	 * <b>Arithmetic shift right long</b>
	 * <p>
	 * The value1 must be of type long, and value2 must be of type int. The values are popped from
	 * the operand stack. A long result is calculated by shifting value1 right by s bit positions,
	 * with sign extension, where s is the value of the low 6 bits of value2. The result is pushed
	 * onto the operand stack.
	 * <p>
	 * The resulting value is floor(value1 / 2s), where s is value2 & 0x3f. For non-negative value1,
	 * this is equivalent to truncating long division by 2 to the power s. The shift distance
	 * actually used is therefore always in the range 0 to 63, inclusive, as if value2 were
	 * subjected to a bitwise logical AND with the mask value 0x3f.
	 */
	protected final InsnNode LSHR() {
		return new InsnNode(LSHR);
	}

	/**
	 * <b>Logical shift right int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. The values are popped from the operand stack. An
	 * int result is calculated by shifting value1 right by s bit positions, with zero extension,
	 * where s is the value of the low 5 bits of value2. The result is pushed onto the operand
	 * stack.
	 * <p>
	 * If value1 is positive and s is value2 & 0x1f, the result is the same as that of value1 >> s;
	 * if value1 is negative, the result is equal to the value of the expression (value1 >> s) + (2
	 * << ~s). The addition of the (2 << ~s) term cancels out the propagated sign bit. The shift
	 * distance actually used is always in the range 0 to 31, inclusive.
	 */
	protected final InsnNode IUSHR() {
		return new InsnNode(IUSHR);
	}

	/**
	 * <b>Logical shift right long</b>
	 * <p>
	 * The value1 must be of type long, and value2 must be of type int. The values are popped from
	 * the operand stack. A long result is calculated by shifting value1 right logically by s bit
	 * positions, with zero extension, where s is the value of the low 6 bits of value2. The result
	 * is pushed onto the operand stack.
	 * <p>
	 * If value1 is positive and s is value2 & 0x3f, the result is the same as that of value1 >> s;
	 * if value1 is negative, the result is equal to the value of the expression (value1 >> s) + (2L
	 * << ~s). The addition of the (2L << ~s) term cancels out the propagated sign bit. The shift
	 * distance actually used is always in the range 0 to 63, inclusive.
	 */
	protected final InsnNode LUSHR() {
		return new InsnNode(LUSHR);
	}

	/**
	 * <b>Boolean AND int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are popped from the operand stack. An int
	 * result is calculated by taking the bitwise AND (conjunction) of value1 and value2. The result
	 * is pushed onto the operand stack.
	 */
	protected final InsnNode IAND() {
		return new InsnNode(IAND);
	}

	/**
	 * <b>Boolean AND long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. They are popped from the operand stack. A long
	 * result is calculated by taking the bitwise AND (conjunction) of value1 and value2. The result
	 * is pushed onto the operand stack.
	 */
	protected final InsnNode LAND() {
		return new InsnNode(LAND);
	}

	/**
	 * <b>Boolean AND int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are popped from the operand stack. An int
	 * result is calculated by taking the bitwise OR of value1 and value2. The result is pushed onto
	 * the operand stack.
	 */
	protected final InsnNode IOR() {
		return new InsnNode(IOR);
	}
	
	/**
	 * <b>Boolean AND long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. They are popped from the operand stack. A long
	 * result is calculated by taking the bitwise OR of value1 and value2. The result is pushed onto
	 * the operand stack.
	 */
	protected final InsnNode LOR() {
		return new InsnNode(LOR);
	}

	/**
	 * <b>Boolean XOR int</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are popped from the operand stack. An int
	 * result is calculated by taking the bitwise XOR of value1 and value2. The result is pushed
	 * onto the operand stack.
	 */
	protected final InsnNode IXOR() {
		return new InsnNode(IXOR);
	}
	/**
	 * <b>Boolean AND long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. They are popped from the operand stack. A long
	 * result is calculated by taking the bitwise XOR of value1 and value2. The result is pushed
	 * onto the operand stack.
	 */
	protected final InsnNode LXOR() {
		return new InsnNode(LXOR);
	}

	/**
	 * <b>Increment local variable by constant</b>
	 * <p>
	 * The index is an unsigned byte that must be an index into the local variable array of the
	 * current frame. The const is an immediate signed byte. The local variable at index must
	 * contain an int. The value const is first sign-extended to an int, and then the local variable
	 * at index is incremented by that amount.
	 * <p>
	 * If the index is greater than 255, ASM will automatically wrap this insn in a WIDE insn to
	 * allow further local variables.
	 */
	protected final IincInsnNode IINC(int var, int incr) {
		return new IincInsnNode(var, incr);
	}

	/**
	 * <b>Convert int to long</b>
	 * <p>
	 * The value on the top of the operand stack must be of type int. It is popped from the operand
	 * stack and sign-extended to a long result. That result is pushed onto the operand stack.
	 * <p>
	 * The i2l instruction performs a widening primitive conversion (JLS §5.1.2). Because all values
	 * of type int are exactly representable by type long, the conversion is exact.
	 */
	protected final InsnNode I2L() {
		return new InsnNode(I2L);
	}

	/**
	 * <b>Convert int to float</b>
	 * <p>
	 * The value on the top of the operand stack must be of type int. It is popped from the operand
	 * stack and converted to the float result using IEEE 754 round to nearest mode. The result is
	 * pushed onto the operand stack.
	 * <p>
	 * The i2f instruction performs a widening primitive conversion (JLS §5.1.2), but may result in
	 * a loss of precision because values of type float have only 24 significand bits.
	 */
	protected final InsnNode I2F() {
		return new InsnNode(I2F);
	}

	/**
	 * <b>Convert int to double</b>
	 * <p>
	 * The value on the top of the operand stack must be of type int. It is popped from the operand
	 * stack and converted to a double result. The result is pushed onto the operand stack.
	 * <p>
	 * The i2d instruction performs a widening primitive conversion (JLS §5.1.2). Because all values
	 * of type int are exactly representable by type double, the conversion is exact.
	 */
	protected final InsnNode I2D() {
		return new InsnNode(I2D);
	}

	/**
	 * <b>Convert long to int</b>
	 * <p>
	 * The value on the top of the operand stack must be of type long. It is popped from the operand
	 * stack and converted to an int result by taking the low-order 32 bits of the long value and
	 * discarding the high-order 32 bits. The result is pushed onto the operand stack.
	 * <p>
	 * The l2i instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value. The result may also not have the same sign
	 * as value.
	 */
	protected final InsnNode L2I() {
		return new InsnNode(L2I);
	}

	/**
	 * <b>Convert long to float</b>
	 * <p>
	 * The value on the top of the operand stack must be of type long. It is popped from the operand
	 * stack and converted to a float result using IEEE 754 round to nearest mode. The result is
	 * pushed onto the operand stack.
	 * <p>
	 * The l2f instruction performs a widening primitive conversion (JLS §5.1.2) that may lose
	 * precision because values of type float have only 24 significand bits.
	 */
	protected final InsnNode L2F() {
		return new InsnNode(L2F);
	}

	/**
	 * <b>Convert long to double</b>
	 * <p>
	 * The value on the top of the operand stack must be of type long. It is popped from the operand
	 * stack and converted to a double result using IEEE 754 round to nearest mode. The result is
	 * pushed onto the operand stack.
	 * <p>
	 * The l2d instruction performs a widening primitive conversion (JLS §5.1.2) that may lose
	 * precision because values of type double have only 53 significand bits.
	 */
	protected final InsnNode L2D() {
		return new InsnNode(L2D);
	}

	/**
	 * <b>Convert float to int</b>
	 * <p>
	 * The value on the top of the operand stack must be of type float. It is popped from the
	 * operand stack and undergoes value set conversion, resulting in value'. Then value' is
	 * converted to an int result. This result is pushed onto the operand stack:
	 * <ul>
	 * <li>If the value' is NaN, the result of the conversion is an int 0.
	 * <li>Otherwise, if the value' is not an infinity, it is rounded to an integer value V,
	 * 		rounding towards zero using IEEE 754 round towards zero mode. If this integer value V
	 * 		can be represented as an int, then the result is the int value V.
	 * <li>Otherwise, either the value' must be too small (a negative value of large magnitude or
	 * 		negative infinity), and the result is the smallest representable value of type int, or
	 * 		the value' must be too large (a positive value of large magnitude or positive infinity),
	 * 		and the result is the largest representable value of type int.
	 * </ul>
	 * The f2i instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value' and may also lose precision.
	 */
	protected final InsnNode F2I() {
		return new InsnNode(F2I);
	}

	/**
	 * <b>Convert float to long</b>
	 * <p>
	 * The value on the top of the operand stack must be of type float. It is popped from the
	 * operand stack and undergoes value set conversion, resulting in value'. Then value' is
	 * converted to a long result. This result is pushed onto the operand stack:
	 * <ul>
	 * <li>If the value' is NaN, the result of the conversion is a long 0.
	 * <li>Otherwise, if the value' is not an infinity, it is rounded to an integer value V,
	 * 		rounding towards zero using IEEE 754 round towards zero mode. If this integer value V
	 * 		can be represented as a long, then the result is the long value V.
	 * <li>Otherwise, either the value' must be too small (a negative value of large magnitude or
	 * 		negative infinity), and the result is the smallest representable value of type long, or
	 * 		the value' must be too large (a positive value of large magnitude or positive infinity),
	 * 		and the result is the largest representable value of type long.
	 * </ul>
	 * The f2l instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value' and may also lose precision.
	 */
	protected final InsnNode F2L() {
		return new InsnNode(F2L);
	}

	/**
	 * <b>Convert float to double</b>
	 * <p>
	 * The value on the top of the operand stack must be of type float. It is popped from the
	 * operand stack and undergoes value set conversion, resulting in value'. Then value' is
	 * converted to a double result. This result is pushed onto the operand stack.
	 * <p>
	 * Where an f2d instruction is FP-strict it performs a widening primitive conversion (JLS
	 * §5.1.2). Because all values of the float value set are exactly representable by values of the
	 * double value set, such a conversion is exact.
	 * <p>
	 * Where an f2d instruction is not FP-strict, the result of the conversion may be taken from the
	 * double-extended-exponent value set; it is not necessarily rounded to the nearest
	 * representable value in the double value set. However, if the operand value is taken from the
	 * float-extended-exponent value set and the target result is constrained to the double value
	 * set, rounding of value may be required.
	 */
	protected final InsnNode F2D() {
		return new InsnNode(F2D);
	}

	/**
	 * <b>Convert double to int</b>
	 * <p>
	 * The value on the top of the operand stack must be of type double. It is popped from the
	 * operand stack and undergoes value set conversion resulting in value'. Then value' is
	 * converted to an int. The result is pushed onto the operand stack:
	 * <ul>
	 * <li>If the value' is NaN, the result of the conversion is an int 0.
	 * <li>Otherwise, if the value' is not an infinity, it is rounded to an integer value V,
	 * 		rounding towards zero using IEEE 754 round towards zero mode. If this integer value V
	 * 		can be represented as an int, then the result is the int value V.
	 * <li>Otherwise, either the value' must be too small (a negative value of large magnitude or
	 * 		negative infinity), and the result is the smallest representable value of type int, or
	 * 		the value' must be too large (a positive value of large magnitude or positive infinity),
	 * 		and the result is the largest representable value of type int.
	 * </ul>
	 * The d2i instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value' and may also lose precision.
	 */
	protected final InsnNode D2I() {
		return new InsnNode(D2I);
	}

	/**
	 * <b>Convert double to long</b>
	 * <p>
	 * The value on the top of the operand stack must be of type double. It is popped from the
	 * operand stack and undergoes value set conversion resulting in value'. Then value' is
	 * converted to a long. The result is pushed onto the operand stack:
	 * <ul>
	 * <li>If the value' is NaN, the result of the conversion is a long 0.
	 * <li>Otherwise, if the value' is not an infinity, it is rounded to an integer value V,
	 * 		rounding towards zero using IEEE 754 round towards zero mode. If this integer value V
	 * 		can be represented as a long, then the result is the long value V.
	 * <li>Otherwise, either the value' must be too small (a negative value of large magnitude or
	 * 		negative infinity), and the result is the smallest representable value of type long, or
	 * 		the value' must be too large (a positive value of large magnitude or positive infinity),
	 * 		and the result is the largest representable value of type long.
	 * </ul>
	 * The d2l instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value' and may also lose precision.
	 */
	protected final InsnNode D2L() {
		return new InsnNode(D2L);
	}

	/**
	 * <b>Convert double to float</b>
	 * <p>
	 * The value on the top of the operand stack must be of type double. It is popped from the
	 * operand stack and undergoes value set conversion resulting in value'. Then value' is
	 * converted to a float result using IEEE 754 round to nearest mode. The result is pushed onto
	 * the operand stack.
	 * <p>
	 * Where an d2f instruction is FP-strict, the result of the conversion is always rounded to the
	 * nearest representable value in the float value set.
	 * <p>
	 * Where an d2f instruction is not FP-strict, the result of the conversion may be taken from the
	 * float-extended-exponent value set; it is not necessarily rounded to the nearest representable
	 * value in the float value set.
	 * <p>
	 * A finite value' too small to be represented as a float is converted to a zero of the same
	 * sign; a finite value' too large to be represented as a float is converted to an infinity of
	 * the same sign. A double NaN is converted to a float NaN. Notes
	 * <p>
	 * The d2f instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value' and may also lose precision.
	 */
	protected final InsnNode D2F() {
		return new InsnNode(D2F);
	}

	/**
	 * <b>Convert int to byte</b>
	 * <p>
	 * The value on the top of the operand stack must be of type int. It is popped from the operand
	 * stack, truncated to a byte, then sign-extended to an int result. That result is pushed onto
	 * the operand stack.
	 * <p>
	 * The i2b instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value. The result may also not have the same sign
	 * as value.
	 */
	protected final InsnNode I2B() {
		return new InsnNode(I2B);
	}

	/**
	 * <b>Convert int to char</b>
	 * <p>
	 * The value on the top of the operand stack must be of type int. It is popped from the operand
	 * stack, truncated to char, then zero-extended to an int result. That result is pushed onto the
	 * operand stack.
	 * <p>
	 * The i2c instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value. The result (which is always positive) may
	 * also not have the same sign as value.
	 */
	protected final InsnNode I2C() {
		return new InsnNode(I2C);
	}

	/**
	 * <b>Convert int to short</b>
	 * <p>
	 * The value on the top of the operand stack must be of type int. It is popped from the operand
	 * stack, truncated to a short, then sign-extended to an int result. That result is pushed onto
	 * the operand stack.
	 * <p>
	 * The i2s instruction performs a narrowing primitive conversion (JLS §5.1.3). It may lose
	 * information about the overall magnitude of value. The result may also not have the same sign
	 * as value.
	 */
	protected final InsnNode I2S() {
		return new InsnNode(I2S);
	}

	/**
	 * <b>Compare long</b>
	 * <p>
	 * Both value1 and value2 must be of type long. They are both popped from the operand stack, and
	 * a signed integer comparison is performed. If value1 is greater than value2, the int value 1
	 * is pushed onto the operand stack. If value1 is equal to value2, the int value 0 is pushed
	 * onto the operand stack. If value1 is less than value2, the int value -1 is pushed onto the
	 * operand stack.
	 */
	protected final InsnNode LCMP() {
		return new InsnNode(LCMP);
	}

	/**
	 * <b>Compare float, NaN is lesser</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. A floating-point
	 * comparison is performed:
	 * <ul>
	 * <li>If value1' is greater than value2', the int value 1 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is equal to value2', the int value 0 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is less than value2', the int value -1 is pushed onto the operand
	 * 		stack.
	 * <li>Otherwise, at least one of value1' or value2' is NaN. The int value -1 is pushed onto the
	 * 		operand stack.
	 * </ul>
	 * Floating-point comparison is performed in accordance with IEEE 754. All values other than NaN
	 * are ordered, with negative infinity less than all finite values and positive infinity greater
	 * than all finite values. Positive zero and negative zero are considered equal.
	 * <p>
	 * The fcmpg and fcmpl instructions differ only in their treatment of a comparison involving
	 * NaN. NaN is unordered, so any float comparison fails if either or both of its operands are
	 * NaN. With both fcmpg and fcmpl available, any float comparison may be compiled to push the
	 * same result onto the operand stack whether the comparison fails on non-NaN values or fails
	 * because it encountered a NaN.
	 */
	protected final InsnNode FCMPL() {
		return new InsnNode(FCMPL);
	}

	/**
	 * <b>Compare float, NaN is greater</b>
	 * <p>
	 * Both value1 and value2 must be of type float. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. A floating-point
	 * comparison is performed:
	 * <ul>
	 * <li>If value1' is greater than value2', the int value 1 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is equal to value2', the int value 0 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is less than value2', the int value -1 is pushed onto the operand
	 * 		stack.
	 * <li>Otherwise, at least one of value1' or value2' is NaN. The int value 1 is pushed onto the
	 * 		operand stack.
	 * </ul>
	 * Floating-point comparison is performed in accordance with IEEE 754. All values other than NaN
	 * are ordered, with negative infinity less than all finite values and positive infinity greater
	 * than all finite values. Positive zero and negative zero are considered equal.
	 * <p>
	 * The fcmpg and fcmpl instructions differ only in their treatment of a comparison involving
	 * NaN. NaN is unordered, so any float comparison fails if either or both of its operands are
	 * NaN. With both fcmpg and fcmpl available, any float comparison may be compiled to push the
	 * same result onto the operand stack whether the comparison fails on non-NaN values or fails
	 * because it encountered a NaN.
	 */
	protected final InsnNode FCMPG() {
		return new InsnNode(FCMPG);
	}

	/**
	 * <b>Compare double, NaN is lesser</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. A floating-point
	 * comparison is performed:
	 * <ul>
	 * <li>If value1' is greater than value2', the int value 1 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is equal to value2', the int value 0 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is less than value2', the int value -1 is pushed onto the operand
	 * 		stack.
	 * <li>Otherwise, at least one of value1' or value2' is NaN. The int value -1 is pushed onto the
	 * 		operand stack.
	 * </ul>
	 * Floating-point comparison is performed in accordance with IEEE 754. All values other than NaN
	 * are ordered, with negative infinity less than all finite values and positive infinity greater
	 * than all finite values. Positive zero and negative zero are considered equal.
	 * <p>
	 * The dcmpg and dcmpl instructions differ only in their treatment of a comparison involving
	 * NaN. NaN is unordered, so any double comparison fails if either or both of its operands are
	 * NaN. With both dcmpg and dcmpl available, any double comparison may be compiled to push the
	 * same result onto the operand stack whether the comparison fails on non-NaN values or fails
	 * because it encountered a NaN.
	 */
	protected final InsnNode DCMPL() {
		return new InsnNode(DCMPL);
	}
	/**
	 * <b>Compare double, NaN is greater</b>
	 * <p>
	 * Both value1 and value2 must be of type double. The values are popped from the operand stack
	 * and undergo value set conversion, resulting in value1' and value2'. A floating-point
	 * comparison is performed:
	 * <ul>
	 * <li>If value1' is greater than value2', the int value 1 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is equal to value2', the int value 0 is pushed onto the operand stack.
	 * <li>Otherwise, if value1' is less than value2', the int value -1 is pushed onto the operand
	 * 		stack.
	 * <li>Otherwise, at least one of value1' or value2' is NaN. The int value 1 is pushed onto the
	 * 		operand stack.
	 * </ul>
	 * Floating-point comparison is performed in accordance with IEEE 754. All values other than NaN
	 * are ordered, with negative infinity less than all finite values and positive infinity greater
	 * than all finite values. Positive zero and negative zero are considered equal.
	 * <p>
	 * The dcmpg and dcmpl instructions differ only in their treatment of a comparison involving
	 * NaN. NaN is unordered, so any double comparison fails if either or both of its operands are
	 * NaN. With both dcmpg and dcmpl available, any double comparison may be compiled to push the
	 * same result onto the operand stack whether the comparison fails on non-NaN values or fails
	 * because it encountered a NaN.
	 */
	protected final InsnNode DCMPG() {
		return new InsnNode(DCMPG);
	}

	/**
	 * <b>Branch if int comparison with zero succeeds</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack and compared against zero.
	 * All comparisons are signed. ifeq succeeds if and only if value = 0.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFEQ(LabelNode label) {
		return new JumpInsnNode(IFEQ, label);
	}

	/**
	 * <b>Branch if int comparison with zero succeeds</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack and compared against zero.
	 * All comparisons are signed. ifne succeeds if and only if value ≠ 0.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFNE(LabelNode label) {
		return new JumpInsnNode(IFNE, label);
	}

	/**
	 * <b>Branch if int comparison with zero succeeds</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack and compared against zero.
	 * All comparisons are signed. iflt succeeds if and only if value &lt; 0.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFLT(LabelNode label) {
		return new JumpInsnNode(IFLT, label);
	}

	/**
	 * <b>Branch if int comparison with zero succeeds</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack and compared against zero.
	 * All comparisons are signed. ifge succeeds if and only if value ≥ 0.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFGE(LabelNode label) {
		return new JumpInsnNode(IFGE, label);
	}

	/**
	 * <b>Branch if int comparison with zero succeeds</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack and compared against zero.
	 * All comparisons are signed. ifgt succeeds if and only if value &gt; 0.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFGT(LabelNode label) {
		return new JumpInsnNode(IFGT, label);
	}

	/**
	 * <b>Branch if int comparison with zero succeeds</b>
	 * <p>
	 * The value must be of type int. It is popped from the operand stack and compared against zero.
	 * All comparisons are signed. ifle succeeds if and only if value ≤ 0.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFLE(LabelNode label) {
		return new JumpInsnNode(IFLE, label);
	}

	/**
	 * <b>Branch if int comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_icmpeq succeeds if and only if value1 = value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ICMPEQ(LabelNode label) {
		return new JumpInsnNode(IF_ICMPEQ, label);
	}

	/**
	 * <b>Branch if int comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_icmpne succeeds if and only if value1 ≠ value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ICMPNE(LabelNode label) {
		return new JumpInsnNode(IF_ICMPNE, label);
	}

	/**
	 * <b>Branch if int comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_icmplt succeeds if and only if value1 &lt; value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ICMPLT(LabelNode label) {
		return new JumpInsnNode(IF_ICMPLT, label);
	}

	/**
	 * <b>Branch if int comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_icmpge succeeds if and only if value1 ≥ value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ICMPGE(LabelNode label) {
		return new JumpInsnNode(IF_ICMPGE, label);
	}

	/**
	 * <b>Branch if int comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_icmpgt succeeds if and only if value1 &gt; value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ICMPGT(LabelNode label) {
		return new JumpInsnNode(IF_ICMPGT, label);
	}

	/**
	 * <b>Branch if int comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type int. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_icmple succeeds if and only if value1 ≤ value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ICMPLE(LabelNode label) {
		return new JumpInsnNode(IF_ICMPLE, label);
	}

	/**
	 * <b>Branch if reference comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type reference. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_acmpeq succeeds if and only if value1 = value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ACMPEQ(LabelNode label) {
		return new JumpInsnNode(IF_ACMPEQ, label);
	}

	/**
	 * <b>Branch if reference comparison succeeds</b>
	 * <p>
	 * Both value1 and value2 must be of type reference. They are both popped from the operand stack and
	 * compared. All comparisons are signed. if_acmpne succeeds if and only if value1 ≠ value2.
	 * <p>
	 * If comparison succeeds, execution proceeds at the location of the given label. Otherwise,
	 * execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IF_ACMPNE(LabelNode label) {
		return new JumpInsnNode(IF_ACMPNE, label);
	}

	/**
	 * <b>Branch always</b>
	 * <p>
	 * Execution proceeds at the location of the given label.
	 */
	protected final JumpInsnNode GOTO(LabelNode label) {
		return new JumpInsnNode(GOTO, label);
	}

	/**
	 * <b>Jump subroutine</b>
	 * <p>
	 * The address of the opcode of the instruction immediately following this jsr instruction is
	 * pushed onto the operand stack as a value of type returnAddress. Execution proceeds at the
	 * location of the given label.
	 * <p>
	 * Note that jsr pushes the address onto the operand stack and {@link #RET} gets it out of a
	 * local variable. This asymmetry is intentional.
	 * <p>
	 * In Oracle's implementation of a compiler for the Java programming language prior to Java SE
	 * 6, the jsr instruction was used with the ret instruction in the implementation of the finally
	 * clause.
	 * @deprecated If encountered in a modern classfile, a VerifyError will be thrown.
	 */
	@Deprecated
	protected final JumpInsnNode JSR(LabelNode label) {
		return new JumpInsnNode(JSR, label);
	}

	/**
	 * <b>Return from subroutine</b>
	 * <p>
	 * The local variable at index in the current frame must contain a value of type returnAddress.
	 * The contents of the local variable are written into the Java Virtual Machine's pc register,
	 * and execution continues there.
	 * <p>
	 * Note that {@link #JSR} pushes the address onto the operand stack and ret gets it out of a
	 * local variable. This asymmetry is intentional.
	 * <p>
	 * In Oracle's implementation of a compiler for the Java programming language prior to Java SE
	 * 6, the ret instruction was used with the jsr instruction in the implementation of the finally
	 * clause.
	 * <p>
	 * The ret instruction should not be confused with the {@link #RETURN} instruction. A return
	 * instruction returns control from a method to its invoker, without passing any value back to
	 * the invoker.
	 * <p>
	 * @deprecated While RET itself is not deprecated and may be used, it is useless as its
	 * 		companion JSR instruction has been deprecated and may not be used, throwing a VerifyError
	 * 		if it is encountered. There is no other way to get a returnAddress value onto the operand
	 * 		stack.
	 */
	@Deprecated
	protected final VarInsnNode RET(int i) {
		return new VarInsnNode(RET, i);
	}

	/**
	 * <b>Access jump table by index and jump</b>
	 * <p>
	 * The index must be of type int and is popped from the operand stack. If index is less than low
	 * or index is greater than high, execution proceeds at the dflt label. Otherwise, the label at
	 * position index - low of the given jump table is extracted. Execution then proceeds at the
	 * given label.
	 */
	protected final TableSwitchInsnNode TABLESWITCH(int min, int max, LabelNode dflt, LabelNode... labels) {
		return new TableSwitchInsnNode(min, max, dflt, labels);
	}

	/**
	 * <b>Access jump table by key match and jump</b>
	 * <p>
	 * The table match-offset pairs of the lookupswitch instruction must be sorted in increasing
	 * numerical order by match.
	 * <p>
	 * The key must be of type int and is popped from the operand stack. The key is compared against
	 * the match values. If it is equal to one of them, then execution continues at the label in
	 * the same index in the jump table. If the key does not match any of the match values,
	 * execution continues at the dflt label.
	 * <p>
	 * The match-offset pairs are sorted to support lookup routines that are quicker than linear
	 * search.
	 */
	protected final LookupSwitchInsnNode LOOKUPSWITCH(LabelNode dflt, int[] keys, LabelNode[] labels) {
		return new LookupSwitchInsnNode(dflt, keys, labels);
	}

	/**
	 * <b>Return int from method</b>
	 * <p>
	 * The current method must have return type boolean, byte, char, short, or int. The value must
	 * be of type int. If the current method is a synchronized method, the monitor entered or
	 * reentered on invocation of the method is updated and possibly exited as if by execution of a
	 * {@link #MONITOREXIT} instruction in the current thread. If no exception is thrown, value is
	 * popped from the operand stack of the current frame and pushed onto the operand stack of the
	 * frame of the invoker. Any other values on the operand stack of the current method are
	 * discarded.
	 * <p>
	 * Prior to pushing value onto the operand stack of the frame of the invoker, it may have to be
	 * converted. If the return type of the invoked method was byte, char, or short, then value is
	 * converted from int to the return type as if by execution of i2b, i2c, or i2s, respectively.
	 * If the return type of the invoked method was boolean, then value is narrowed from int to
	 * boolean by taking the bitwise AND of value and 1.
	 * <p>
	 * The interpreter then returns control to the invoker of the method, reinstating the frame of
	 * the invoker.
	 * @throws IllegalMonitorStateException if the Java Virtual Machine implementation does not
	 * 		enforce the rules on structured locking and the current method is a synchronized method,
	 * 		and the current thread is not the owner of the monitor entered or reentered on
	 * 		invocation of the method. This can happen, for example, if a synchronized method
	 * 		contains a monitorexit instruction, but no monitorenter instruction, on the object on
	 * 		which the method is synchronized. Otherwise, it may also be thrown if the Java Virtual
	 * 		Machine implementation enforces the rules on structured locking and the first of those
	 * 		rules is violated during invocation of the current method.
	 */
	protected final InsnNode IRETURN() {
		return new InsnNode(IRETURN);
	}

	/**
	 * <b>Return long from method</b>
	 * <p>
	 * The current method must have return type long. The value must be of type long. If the current
	 * method is a synchronized method, the monitor entered or reentered on invocation of the method
	 * is updated and possibly exited as if by execution of a {@link #MONITOREXIT} instruction in
	 * the current thread. If no exception is thrown, value is popped from the operand stack of the
	 * current frame and pushed onto the operand stack of the frame of the invoker. Any other values
	 * on the operand stack of the current method are discarded.
	 * <p>
	 * The interpreter then returns control to the invoker of the method, reinstating the frame of
	 * the invoker.
	 * @throws IllegalMonitorStateException if the Java Virtual Machine implementation does not
	 * 		enforce the rules on structured locking and the current method is a synchronized method,
	 * 		and the current thread is not the owner of the monitor entered or reentered on
	 * 		invocation of the method. This can happen, for example, if a synchronized method
	 * 		contains a monitorexit instruction, but no monitorenter instruction, on the object on
	 * 		which the method is synchronized. Otherwise, it may also be thrown if the Java Virtual
	 * 		Machine implementation enforces the rules on structured locking and the first of those
	 * 		rules is violated during invocation of the current method.
	 */
	protected final InsnNode LRETURN() {
		return new InsnNode(LRETURN);
	}

	/**
	 * <b>Return float from method</b>
	 * <p>
	 * The current method must have return type float. The value must be of type float. If the
	 * current method is a synchronized method, the monitor entered or reentered on invocation of
	 * the method is updated and possibly exited as if by execution of a {@link #MONITOREXIT}
	 * instruction in the current thread. If no exception is thrown, value is popped from the
	 * operand stack of the current frame and pushed onto the operand stack of the frame of the
	 * invoker. Any other values on the operand stack of the current method are discarded.
	 * <p>
	 * The interpreter then returns control to the invoker of the method, reinstating the frame of
	 * the invoker.
	 * @throws IllegalMonitorStateException if the Java Virtual Machine implementation does not
	 * 		enforce the rules on structured locking and the current method is a synchronized method,
	 * 		and the current thread is not the owner of the monitor entered or reentered on
	 * 		invocation of the method. This can happen, for example, if a synchronized method
	 * 		contains a monitorexit instruction, but no monitorenter instruction, on the object on
	 * 		which the method is synchronized. Otherwise, it may also be thrown if the Java Virtual
	 * 		Machine implementation enforces the rules on structured locking and the first of those
	 * 		rules is violated during invocation of the current method.
	 */
	protected final InsnNode FRETURN() {
		return new InsnNode(FRETURN);
	}

	/**
	 * <b>Return double from method</b>
	 * <p>
	 * The current method must have return type double. The value must be of type double. If the
	 * current method is a synchronized method, the monitor entered or reentered on invocation of
	 * the method is updated and possibly exited as if by execution of a {@link #MONITOREXIT}
	 * instruction in the current thread. If no exception is thrown, value is popped from the
	 * operand stack of the current frame and pushed onto the operand stack of the frame of the
	 * invoker. Any other values on the operand stack of the current method are discarded.
	 * <p>
	 * The interpreter then returns control to the invoker of the method, reinstating the frame of
	 * the invoker.
	 * @throws IllegalMonitorStateException if the Java Virtual Machine implementation does not
	 * 		enforce the rules on structured locking and the current method is a synchronized method,
	 * 		and the current thread is not the owner of the monitor entered or reentered on
	 * 		invocation of the method. This can happen, for example, if a synchronized method
	 * 		contains a monitorexit instruction, but no monitorenter instruction, on the object on
	 * 		which the method is synchronized. Otherwise, it may also be thrown if the Java Virtual
	 * 		Machine implementation enforces the rules on structured locking and the first of those
	 * 		rules is violated during invocation of the current method.
	 */
	protected final InsnNode DRETURN() {
		return new InsnNode(DRETURN);
	}

	/**
	 * <b>Return reference from method</b>
	 * <p>
	 * The objectref must be of type reference and must refer to an object of a type that is
	 * assignment compatible (JLS §5.2) with the type represented by the return descriptor of the
	 * current method. If the current method is a synchronized method, the monitor entered or
	 * reentered on invocation of the method is updated and possibly exited as if by execution of a
	 * {@link #MONITOREXIT} instruction in the current thread. If no exception is thrown, objectref
	 * is popped from the operand stack of the current frame and pushed onto the operand stack of
	 * the frame of the invoker. Any other values on the operand stack of the current method are
	 * discarded.
	 * <p>
	 * The interpreter then returns control to the invoker of the method, reinstating the frame of
	 * the invoker.
	 * @throws IllegalMonitorStateException if the Java Virtual Machine implementation does not
	 * 		enforce the rules on structured locking and the current method is a synchronized method,
	 * 		and the current thread is not the owner of the monitor entered or reentered on
	 * 		invocation of the method. This can happen, for example, if a synchronized method
	 * 		contains a monitorexit instruction, but no monitorenter instruction, on the object on
	 * 		which the method is synchronized. Otherwise, it may also be thrown if the Java Virtual
	 * 		Machine implementation enforces the rules on structured locking and the first of those
	 * 		rules is violated during invocation of the current method.
	 */
	protected final InsnNode ARETURN() {
		return new InsnNode(ARETURN);
	}

	/**
	 * <b>Return void from method</b>
	 * <p>
	 * The current method must have return type void. If the current method is a synchronized
	 * method, the monitor entered or reentered on invocation of the method is updated and possibly
	 * exited as if by execution of a {@link #MONITOREXIT} instruction in the current thread. If no
	 * exception is thrown, any values on the operand stack of the current frame are discarded.
	 * <p>
	 * The interpreter then returns control to the invoker of the method, reinstating the frame of
	 * the invoker.
	 * @throws IllegalMonitorStateException if the Java Virtual Machine implementation does not
	 * 		enforce the rules on structured locking and the current method is a synchronized method,
	 * 		and the current thread is not the owner of the monitor entered or reentered on
	 * 		invocation of the method. This can happen, for example, if a synchronized method
	 * 		contains a monitorexit instruction, but no monitorenter instruction, on the object on
	 * 		which the method is synchronized. Otherwise, it may also be thrown if the Java Virtual
	 * 		Machine implementation enforces the rules on structured locking and the first of those
	 * 		rules is violated during invocation of the current method.
	 */
	protected final InsnNode RETURN() {
		return new InsnNode(RETURN);
	}

	/**
	 * <b>Get static field from class</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool. They are retrieved and
	 * resolved upon linking. On successful resolution of the field, the class or interface that
	 * declared the resolved field is initialized if that class or interface has not already been
	 * initialized.
	 * <p>
	 * The value of the class or interface field is fetched and pushed onto the operand stack.
	 * <p>
	 * During resolution of the symbolic reference to the class or interface field, any of the
	 * exceptions pertaining to field resolution can be thrown.
	 * @throws IncompatibleClassChangeError if the resolved field is not a static (class) field or
	 * 		an interface field
	 */
	protected final FieldInsnNode GETSTATIC(String owner, String name, String desc) {
		return new FieldInsnNode(GETSTATIC, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc));
	}

	/**
	 * <b>Set static field in class</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool. They are retrieved and
	 * resolved upon linking. On successful resolution of the field, the class or interface that
	 * declared the resolved field is initialized if that class or interface has not already been
	 * initialized.
	 * <p>
	 * The type of a value stored by a putstatic instruction must be compatible with the descriptor
	 * of the referenced field. If the field descriptor type is boolean, byte, char, short, or int,
	 * then the value must be an int. If the field descriptor type is float, long, or double, then
	 * the value must be a float, long, or double, respectively. If the field descriptor type is a
	 * reference type, then the value must be of a type that is assignment compatible (JLS §5.2)
	 * with the field descriptor type. If the field is final, it must be declared in the current
	 * class or interface, and the instruction must occur in the class or interface initialization
	 * method of the current class or interface.
	 * <p>
	 * The value is popped from the operand stack.
	 * <p>
	 * If the value is of type int and the field descriptor type is boolean, then the int value is
	 * narrowed by taking the bitwise AND of value and 1, resulting in value'. Otherwise, the value
	 * undergoes value set conversion, resulting in value'.
	 * <p>
	 * The referenced field in the class or interface is set to value'.
	 * <p>
	 * During resolution of the symbolic reference to the class or interface field, any of the
	 * exceptions pertaining to field resolution can be thrown.
	 * @throws IncompatibleClassChangeError if the resolved field is not a static (class) field or
	 * 		an interface field
	 * @throws IllegalAccessError if the resolved field is final and it is not declared in the
	 * 		current class or interface and the instruction does not occur in the class or interface
	 *		initialization method of the current class or interface
	 */
	protected final FieldInsnNode PUTSTATIC(String owner, String name, String desc) {
		return new FieldInsnNode(PUTSTATIC, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc));
	}

	/**
	 * <b>Fetch field from object</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool as a symbolic reference.
	 * They are retrieved and resolved upon linking.
	 * <p>
	 * The objectref, which must be of type reference but not an array type, is popped from the
	 * operand stack. The value of the referenced field in objectref is fetched and pushed onto the
	 * operand stack.
	 * <p>
	 * During resolution of the symbolic reference to the class or interface field, any of the
	 * exceptions pertaining to field resolution can be thrown.
	 * <p>
	 * The getfield instruction cannot be used to access the length field of an array. The
	 * {@link #ARRAYLENGTH} instruction is used instead.
	 * @throws NullPointerException if objectref is null
	 */
	protected final FieldInsnNode GETFIELD(String owner, String name, String desc) {
		return new FieldInsnNode(GETFIELD, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc));
	}

	/**
	 * <b>Set field in object</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool as a symbolic reference.
	 * They are retrieved and resolved upon linking.
	 * <p>
	 * The type of a value stored by a putfield instruction must be compatible with the descriptor
	 * of the referenced field. If the field descriptor type is boolean, byte, char, short, or int,
	 * then the value must be an int. If the field descriptor type is float, long, or double, then
	 * the value must be a float, long, or double, respectively. If the field descriptor type is a
	 * reference type, then the value must be of a type that is assignment compatible (JLS §5.2)
	 * with the field descriptor type. If the field is final, it must be declared in the current
	 * class, and the instruction must occur in an instance initialization method of the current
	 * class.
	 * <p>
	 * The value and objectref are popped from the operand stack.
	 * <p>
	 * The objectref must be of type reference but not an array type.
	 * <p>
	 * If the value is of type int and the field descriptor type is boolean, then the int value is
	 * narrowed by taking the bitwise AND of value and 1, resulting in value'. Otherwise, the value
	 * undergoes value set conversion, resulting in value'.
	 * <p>
	 * The referenced field in objectref is set to value'.
	 * <p>
	 * During resolution of the symbolic reference to the class or interface field, any of the
	 * exceptions pertaining to field resolution can be thrown.
	 * @throws NullPointerException if objectref is null
	 * @throws IncompatibleClassChangeError if the resolved field is a static field
	 * @throws IllegalAccessError if the resolved field is final and it is not declared in the
	 * 		current class and the instruction does not occur inan instance initialization method of
	 * 		the current class
	 */
	protected final FieldInsnNode PUTFIELD(String owner, String name, String desc) {
		return new FieldInsnNode(PUTFIELD, remapType(owner), remapField(owner, name, desc), remapFieldDesc(desc));
	}

	/**
	 * <b>Invoke instance method; dispatch based on class</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool as a symbolic reference.
	 * They are retrieved and resolved upon linking.
	 * <p>
	 * If the resolved method is not signature polymorphic, then the invokevirtual instruction
	 * proceeds as follows.
	 * <p>
	 * Let C be the class of objectref. A method is selected with respect to C and the resolved
	 * method. This is the method to be invoked.
	 * <p>
	 * The objectref must be followed on the operand stack by nargs argument values, where the
	 * number, type, and order of the values must be consistent with the descriptor of the selected
	 * instance method.
	 * <p>
	 * If the method to be invoked is synchronized, the monitor associated with objectref is entered
	 * or reentered as if by execution of a monitorenter instruction in the current thread.
	 * <p>
	 * If the method to be invoked is not native, the nargs argument values and objectref are popped
	 * from the operand stack. A new frame is created on the Java Virtual Machine stack for the
	 * method being invoked. The objectref and the argument values are consecutively made the values
	 * of local variables of the new frame, with objectref in local variable 0, arg1 in local
	 * variable 1 (or, if arg1 is of type long or double, in local variables 1 and 2), and so on.
	 * Any argument value that is of a floating-point type undergoes value set conversion prior to
	 * being stored in a local variable. The new frame is then made current, and the Java Virtual
	 * Machine pc is set to the opcode of the first instruction of the method to be invoked.
	 * Execution continues with the first instruction of the method.
	 * <p>
	 * If the method to be invoked is native and the platform-dependent code that implements it has
	 * not yet been bound into the Java Virtual Machine, that is done. The nargs argument values and
	 * objectref are popped from the operand stack and are passed as parameters to the code that
	 * implements the method. Any argument value that is of a floating-point type undergoes value
	 * set conversion prior to being passed as a parameter. The parameters are passed and the code
	 * is invoked in an implementation-dependent manner. When the platform-dependent code returns,
	 * the following take place:
	 * <ul>
	 * <li>If the native method is synchronized, the monitor associated with objectref is updated
	 * 		and possibly exited as if by execution of a monitorexit instruction in the current
	 * 		thread.
	 * <li>If the native method returns a value, the return value of the platform-dependent code is
	 * 		converted in an implementation-dependent way to the return type of the native method and
	 * 		pushed onto the operand stack.
	 * </ul>
	 * This instruction has various special behaviors for methods defined in MethodHandle and
	 * VarHandle. Please see the
	 * <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5.invokevirtual">original JVMS instruction documentation</a>
	 * for more information.
	 * <p>
	 * During resolution of the symbolic reference to the method, any of the exceptions pertaining
	 * to method resolution can be thrown.
	 * <p>
	 * The nargs argument values and objectref are not one-to-one with the first nargs+1 local
	 * variables. Argument values of types long and double must be stored in two consecutive local
	 * variables, thus more than nargs local variables may be required to pass nargs argument values
	 * to the invoked method.
	 * <p>
	 * It is possible that the symbolic reference of an invokevirtual instruction resolves to an
	 * interface method. In this case, it is possible that there is no overriding method in the
	 * class hierarchy, but that a non-abstract interface method matches the resolved method's
	 * descriptor. The selection logic matches such a method, using the same rules as for
	 * invokeinterface.
	 * @throws NullPointerException if objectref is null
	 * @throws IncompatibleClassChangeError if the resolved method is a class (static) method, or
	 * 		if no method is selected and there are multiple maximally-specific superinterface
	 * 		methods of C that match the resolved method's name and descriptor and are not abstract
	 * @throws AbstractMethodError if the selected method is abstract or there are no
	 * 		maximally-specific superinterface methods of C that match the resolved method's name and
	 * 		descriptor and are not abstract
	 * @throws UnsatisfiedLinkError if the selected method is native and the code that implements
	 * 		the method cannot be bound
	 */
	protected final MethodInsnNode INVOKEVIRTUAL(String owner, String name, String desc) {
		return new MethodInsnNode(INVOKEVIRTUAL, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc));
	}

	/**
	 * <b>Invoke instance method; direct invocation of instance initialization methods and methods
	 * of the current class and its supertypes</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool as a symbolic reference.
	 * They are retrieved and resolved upon linking.
	 * <p>
	 * If all of the following are true, let C be the direct superclass of the current class:
	 * <ul>
	 * <li>The resolved method is not an instance initialization method.
	 * <li>If the symbolic reference names a class (not an interface), then that class is a superclass
	 * 		of the current class.
	 * <li>The ACC_SUPER flag is set for the class file. (ACC_SUPER is always set for Java 8+ class
	 * 		files.)
	 * </ul>
	 * Otherwise, let C be the class or interface named by the symbolic reference.
	 * <p>
	 * The actual method to be invoked is selected by the following lookup procedure:
	 * <ol>
	 * <li>If C contains a declaration for an instance method with the same name and descriptor as the
	 * 		resolved method, then it is the method to be invoked.
	 * <li>Otherwise, if C is a class and has a superclass, a search for a declaration of an instance
	 * 		method with the same name and descriptor as the resolved method is performed, starting with
	 * 		the direct superclass of C and continuing with the direct superclass of that class, and so
	 * 		forth, until a match is found or no further superclasses exist. If a match is found, then it
	 * 		is the method to be invoked.
	 * <li>Otherwise, if C is an interface and the class Object contains a declaration of a public
	 * 		instance method with the same name and descriptor as the resolved method, then it is the
	 * 		method to be invoked.
	 * <li>Otherwise, if there is exactly one maximally-specific method (§5.4.3.3) in the
	 * 		superinterfaces of C that matches the resolved method's name and descriptor and is not
	 * 		abstract, then it is the method to be invoked.
	 * </ol>
	 * The objectref must be of type reference and must be followed on the operand stack by nargs
	 * argument values, where the number, type, and order of the values must be consistent with the
	 * descriptor of the selected instance method.
	 * <p>
	 * If the method is synchronized, the monitor associated with objectref is entered or reentered
	 * as if by execution of a monitorenter instruction in the current thread.
	 * <p>
	 * If the method is not native, the nargs argument values and objectref are popped from the
	 * operand stack. A new frame is created on the Java Virtual Machine stack for the method being
	 * invoked. The objectref and the argument values are consecutively made the values of local
	 * variables of the new frame, with objectref in local variable 0, arg1 in local variable 1 (or,
	 * if arg1 is of type long or double, in local variables 1 and 2), and so on. Any argument value
	 * that is of a floating-point type undergoes value set conversion (§2.8.3) prior to being
	 * stored in a local variable. The new frame is then made current, and the Java Virtual Machine
	 * pc is set to the opcode of the first instruction of the method to be invoked. Execution
	 * continues with the first instruction of the method.
	 * <p>
	 * If the method is native and the platform-dependent code that implements it has not yet been
	 * bound (§5.6) into the Java Virtual Machine, that is done. The nargs argument values and
	 * objectref are popped from the operand stack and are passed as parameters to the code that
	 * implements the method. Any argument value that is of a floating-point type undergoes value
	 * set conversion (§2.8.3) prior to being passed as a parameter. The parameters are passed and
	 * the code is invoked in an implementation-dependent manner. When the platform-dependent code
	 * returns, the following take place:
	 * <ul>
	 * <li>If the native method is synchronized, the monitor associated with objectref is updated and
	 * 		possibly exited as if by execution of a monitorexit instruction (§monitorexit) in the current
	 * 		thread.
	 * <li>If the native method returns a value, the return value of the platform-dependent code is
	 * 		converted in an implementation-dependent way to the return type of the native method and
	 * 		pushed onto the operand stack.
	 * </ul>
	 * The difference between the invokespecial instruction and the {@link #INVOKEVIRTUAL}
	 * instruction is that invokevirtual invokes a method based on the class of the object. The
	 * invokespecial instruction is used to directly invoke instance initialization methods as well
	 * as methods of the current class and its supertypes.
	 * <p>
	 * The invokespecial instruction was named invokenonvirtual prior to JDK release 1.0.2.
	 * <p>
	 * The nargs argument values and objectref are not one-to-one with the first nargs+1 local
	 * variables. Argument values of types long and double must be stored in two consecutive local
	 * variables, thus more than nargs local variables may be required to pass nargs argument values
	 * to the invoked method.
	 * <p>
	 * The invokespecial instruction handles invocation of a non-abstract interface method,
	 * referenced either via a direct superinterface or via a superclass. In these cases, the rules
	 * for selection are essentially the same as those for invokeinterface (except that the search
	 * starts from a different class).
	 * @throws NullPointerException if objectref is null
	 * @throws NoSuchMethodError if the resolved method is an instance initialization method, and
	 * 		the class in which it is declared is not the class symbolically referenced by the
	 * 		instruction
	 * @throws AbstractMethodError if step 1, 2, or 3 of the lookup procedure selects an abstract
	 * 		method or if step 4 of the lookup procedure determines there are no maximally-specific
	 * 		superinterface methods of C that match the resolved method's name and descriptor and are
	 * 		not abstract
	 * @throws UnsatisfiedLinkError if step 1, step 2, or step 3 of the lookup procedure selects a
	 * 		native method and the code that implements the method cannot be bound
	 * @throws IncompatibleClassChangeError if the resolved method is a class (static) method, or
	 * 		if step 4 of the lookup procedure determines there are multiple maximally-specific
	 * 		superinterface methods of C that match the resolved method's name and descriptor and are
	 * 		not abstract
	 */
	protected final MethodInsnNode INVOKESPECIAL(String owner, String name, String desc) {
		return new MethodInsnNode(INVOKESPECIAL, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc));
	}

	/**
	 * <b>Invoke a class (static) method</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool as a symbolic reference. They
	 * are retrieved and resolved upon linking.
	 * <p>
	 * The resolved method must not be an instance initialization method, or the class or interface
	 * initialization method.
	 * <p>
	 * The resolved method must be static, and therefore cannot be abstract.
	 * <p>
	 * On successful resolution of the method, the class or interface that declared the resolved
	 * method is initialized if that class or interface has not already been initialized (§5.5).
	 * <p>
	 * The operand stack must contain nargs argument values, where the number, type, and order of
	 * the values must be consistent with the descriptor of the resolved method.
	 * <p>
	 * If the method is synchronized, the monitor associated with the resolved Class object is
	 * entered or reentered as if by execution of a monitorenter instruction (§monitorenter) in the
	 * current thread.
	 * <p>
	 * If the method is not native, the nargs argument values are popped from the operand stack. A
	 * new frame is created on the Java Virtual Machine stack for the method being invoked. The
	 * nargs argument values are consecutively made the values of local variables of the new frame,
	 * with arg1 in local variable 0 (or, if arg1 is of type long or double, in local variables 0
	 * and 1) and so on. Any argument value that is of a floating-point type undergoes value set
	 * conversion (§2.8.3) prior to being stored in a local variable. The new frame is then made
	 * current, and the Java Virtual Machine pc is set to the opcode of the first instruction of the
	 * method to be invoked. Execution continues with the first instruction of the method.
	 * <p>
	 * If the method is native and the platform-dependent code that implements it has not yet been
	 * bound (§5.6) into the Java Virtual Machine, that is done. The nargs argument values are
	 * popped from the operand stack and are passed as parameters to the code that implements the
	 * method. Any argument value that is of a floating-point type undergoes value set conversion
	 * (§2.8.3) prior to being passed as a parameter. The parameters are passed and the code is
	 * invoked in an implementation-dependent manner. When the platform-dependent code returns, the
	 * following take place:
	 * <ul>
	 * <li>If the native method is synchronized, the monitor associated with the resolved Class object
	 * 		is updated and possibly exited as if by execution of a monitorexit instruction (§monitorexit)
	 * 		in the current thread.
	 * <li>If the native method returns a value, the return value of the platform-dependent code is
	 * 		converted in an implementation-dependent way to the return type of the native method and
	 * 		pushed onto the operand stack.
	 * </ul>
	 * During resolution of the symbolic reference to the method, any of the exceptions pertaining
	 * to method resolution (§5.4.3.3) can be thrown.
	 * <p>
	 * The nargs argument values are not one-to-one with the first nargs local variables. Argument
	 * values of types long and double must be stored in two consecutive local variables, thus more
	 * than nargs local variables may be required to pass nargs argument values to the invoked
	 * method.
	 * @throws IncompatibleClassChangeError if the resolved method is an instance method
	 * @throws UnsatisfiedLinkError if the resolved method is native and the code that implements
	 * 		the method cannot be bound
	 */
	protected final MethodInsnNode INVOKESTATIC(String owner, String name, String desc) {
		return new MethodInsnNode(INVOKESTATIC, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc));
	}

	/**
	 * <b>Invoke interface method</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the references to your
	 * currently selected mappings.</i>
	 * <p>
	 * The owner, name, and descriptor are stored in the constant pool as a symbolic reference. They
	 * are retrieved and resolved upon linking.
	 * <p>
	 * The resolved interface method must not be an instance initialization method, or the class or
	 * interface initialization method.
	 * <p>
	 * Let C be the class of objectref. A method is selected with respect to C and the resolved
	 * method (§5.4.6). This is the method to be invoked.
	 * <p>
	 * If the method to be invoked is synchronized, the monitor associated with objectref is entered
	 * or reentered as if by execution of a monitorenter instruction (§monitorenter) in the current
	 * thread.
	 * <p>
	 * If the method to be invoked is not native, the nargs argument values and objectref are popped
	 * from the operand stack. A new frame is created on the Java Virtual Machine stack for the
	 * method being invoked. The objectref and the argument values are consecutively made the values
	 * of local variables of the new frame, with objectref in local variable 0, arg1 in local
	 * variable 1 (or, if arg1 is of type long or double, in local variables 1 and 2), and so on.
	 * Any argument value that is of a floating-point type undergoes value set conversion (§2.8.3)
	 * prior to being stored in a local variable. The new frame is then made current, and the Java
	 * Virtual Machine pc is set to the opcode of the first instruction of the method to be invoked.
	 * Execution continues with the first instruction of the method.
	 * <p>
	 * If the method to be invoked is native and the platform-dependent code that implements it has
	 * not yet been bound (§5.6) into the Java Virtual Machine, then that is done. The nargs
	 * argument values and objectref are popped from the operand stack and are passed as parameters
	 * to the code that implements the method. Any argument value that is of a floating-point type
	 * undergoes value set conversion (§2.8.3) prior to being passed as a parameter. The parameters
	 * are passed and the code is invoked in an implementation-dependent manner. When the
	 * platform-dependent code returns:
	 * <ul>
	 * <li>If the native method is synchronized, the monitor associated with objectref is updated and
	 * 		possibly exited as if by execution of a monitorexit instruction in the current thread.
	 * <li>If the native method returns a value, the return value of the platform-dependent code is
	 * 		converted in an implementation-dependent way to the return type of the native method and
	 * 		pushed onto the operand stack.
	 * </ul>
	 * During resolution of the symbolic reference to the interface method, any of the exceptions
	 * pertaining to interface method resolution an be thrown.
	 * <p>
	 * Note that invokeinterface may refer to private methods declared in interfaces, including
	 * nestmate interfaces.
	 * <p>
	 * The nargs argument values and objectref are not one-to-one with the first nargs+1 local
	 * variables. Argument values of types long and double must be stored in two consecutive local
	 * variables, thus more than nargs local variables may be required to pass nargs argument
	 * values to the invoked method.
	 * <p>
	 * The selection logic allows a non-abstract method declared in a superinterface to be selected.
	 * Methods in interfaces are only considered if there is no matching method in the class
	 * hierarchy. In the event that there are two non-abstract methods in the superinterface
	 * hierarchy, with neither more specific than the other, an error occurs; there is no attempt
	 * to disambiguate (for example, one may be the referenced method and one may be unrelated, but
	 * we do not prefer the referenced method). On the other hand, if there are many abstract
	 * methods but only one non-abstract method, the non-abstract method is selected (unless an
	 * abstract method is more specific).
	 * @throws IncompatibleClassChangeError if the resolved method is static, or the class of
	 * 		objectref does not implement the resolved interface, or if no method is selected, and
	 * 		there are multiple maximally-specific superinterface methods of C that match the
	 * 		resolved method's name and descriptor and are not abstract
	 * @throws IllegalAccessError if the selected method is neither public nor private
	 * @throws AbstractMethodError if the selected method is abstract, or if no method is selected,
	 * 		and there are no maximally-specific superinterface methods of C that match the resolved
	 * 		method's name and descriptor and are not abstract
	 * @throws UnsatisfiedLinkError if the selected method is native and the code that implements the method cannot be bound
	 */
	protected final MethodInsnNode INVOKEINTERFACE(String owner, String name, String desc) {
		return new MethodInsnNode(INVOKEINTERFACE, remapType(owner), remapMethod(owner, name, desc), remapMethodDesc(desc));
	}

	/**
	 * <b>Invoke a dynamically-computed call site</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the descriptor to your
	 * currently selected mappings.</i>
	 * <p>
	 * The arguments are stored in the constant pool as a symbolic call site reference.
	 * <p>
	 * The symbolic reference is resolved for this specific invokedynamic instruction to obtain a
	 * reference to an instance of java.lang.invoke.CallSite. The instance of
	 * java.lang.invoke.CallSite is considered "bound" to this specific invokedynamic instruction.
	 * <p>
	 * The instance of java.lang.invoke.CallSite indicates a target method handle. The nargs
	 * argument values are popped from the operand stack, and the target method handle is invoked.
	 * The invocation occurs as if by execution of an invokevirtual instruction that indicates a
	 * run-time constant pool index to a symbolic reference R where:
	 * <ul>
	 * <li>R is a symbolic reference to a method of a class;
	 * <li>for the symbolic reference to the class in which the method is to be found, R specifies
	 * 		java.lang.invoke.MethodHandle;
	 * <li>for the name of the method, R specifies invokeExact;
	 * <li>for the descriptor of the method, R specifies the method descriptor in the
	 * 		dynamically-computed call site.
	 * </ul>
	 * and where it is as if the following items were pushed, in order, onto the operand stack:
	 * <ul>
	 * <li>a reference to the target method handle;
	 * <li>the nargs argument values, where the number, type, and order of the values must be consistent
	 * 		with the method descriptor in the dynamically-computed call site.
	 * </ul>
	 * During resolution of the symbolic reference to a dynamically-computed call site, any of the
	 * exceptions pertaining to dynamically-computed call site resolution can be thrown.
	 * <p>
	 * If the symbolic reference to the dynamically-computed call site can be resolved, it implies
	 * that a non-null reference to an instance of java.lang.invoke.CallSite is bound to the
	 * invokedynamic instruction. Therefore, the target method handle, indicated by the instance of
	 * java.lang.invoke.CallSite, is non-null.
	 * <p>
	 * Similarly, successful resolution implies that the method descriptor in the symbolic reference
	 * is semantically equal to the type descriptor of the target method handle.
	 * <p>
	 * Together, these invariants mean that an invokedynamic instruction which is bound to an
	 * instance of java.lang.invoke.CallSite never throws a NullPointerException or a
	 * java.lang.invoke.WrongMethodTypeException.
	 */
	protected final InvokeDynamicInsnNode INVOKEDYNAMIC(String name, String desc, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		return new InvokeDynamicInsnNode(name, remapMethodDesc(desc), bootstrapMethodHandle, bootstrapMethodArguments);
	}

	/**
	 * <b>Create new object</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the type to your
	 * currently selected mappings.</i>
	 * <p>
	 * The given type is stored in the constant pool as a symbolic reference. It is resolved
	 * during linking and should result in a class type.
	 * <p>
	 * Memory for a new instance of that class is allocated from the garbage-collected heap, and the
	 * instance variables of the new object are initialized to their default initial values. The
	 * objectref, a reference to the instance, is pushed onto the operand stack.
	 * <p>
	 * On successful resolution of the class, it is initialized if it has not already been
	 * initialized.
	 * <p>
	 * During resolution of the symbolic reference to the class or interface type, any of the
	 * exceptions documented in can be thrown.
	 * <p>
	 * The new instruction does not completely create a new instance; instance creation is not
	 * completed until an instance initialization method has been invoked on the uninitialized
	 * instance.
	 * @throws InstantiationError if the symbolic reference to the class or interface type resolved
	 * 		to an interface or an abstract class
	 */
	protected final TypeInsnNode NEW(String type) {
		return new TypeInsnNode(NEW, remapType(type));
	}
	
	/**
	 * <b>Create new array</b>
	 * <p>
	 * <i>For your convenience, fields are declared on MiniTransformer with names matching those used
	 * by the JVMS (and emitted by various bytecode tools). You may choose to write insns of this
	 * type as either the conventional {@code NEWARRAY(T_INT)} or the more proper
	 * {@code NEWARRAY(ArrayType.INT)}.</i>
	 * <p>
	 * The count must be of type int. It is popped off the operand stack. The count represents the
	 * number of elements in the array to be created.
	 * <p>
	 * A new array whose components are of type atype and of length count is allocated from the
	 * garbage-collected heap. A reference arrayref to this new array object is pushed into the
	 * operand stack. Each of the elements of the new array is initialized to the default initial
	 * value (§2.3, §2.4) for the element type of the array type.
	 * <p>
	 * In Oracle's Java Virtual Machine implementation, arrays of type boolean (atype is T_BOOLEAN)
	 * are stored as arrays of 8-bit values and are manipulated using the baload and bastore
	 * instructions (§baload, §bastore) which also access arrays of type byte. Other implementations
	 * may implement packed boolean arrays; the baload and bastore instructions must still be used
	 * to access those arrays.
	 * @throws NegativeArraySizeException if count is less than zero
	 */
	protected final IntInsnNode NEWARRAY(ArrayType atype) {
		return new IntInsnNode(NEWARRAY, atype.value);
	}
	
	/**
	 * @deprecated Prefer {@link #NEWARRAY(ArrayType)} as it is type safe
	 * @see #NEWARRAY(ArrayType)
	 */
	@Deprecated
	protected final IntInsnNode NEWARRAY(int atype) {
		return new IntInsnNode(NEWARRAY, atype);
	}

	/**
	 * <b>Create new array of reference</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the type to your
	 * currently selected mappings.</i>
	 * <p>
	 * The given type is stored in the constant pool as a symbolic reference. It is resolved during
	 * linking and must result in a class, array, or interface type.
	 * <p>
	 * The count must be of type int. It is popped off the operand stack. The count represents the
	 * number of components of the array to be created.
	 * <p>
	 * During resolution of the symbolic reference to the class, array, or interface type, any of
	 * the exceptions documented in JVMS §5.4.3.1 can be thrown.
	 * <p>
	 * The anewarray instruction is used to create a single dimension of an array of object
	 * references or part of a multidimensional array.
	 * 
	 * @throws NegativeArraySizeException if count is less than zero
	 */
	protected final TypeInsnNode ANEWARRAY(String type) {
		return new TypeInsnNode(ANEWARRAY, remapType(type));
	}

	/**
	 * <b>Get length of array</b>
	 * <p>
	 * The arrayref must be of type reference and must refer to an array. It is popped from the
	 * operand stack. The length of the array it references is determined. That length is pushed
	 * onto the operand stack as an int.
	 * 
	 * @throws NullPointerException if arrayref is null
	 */
	protected final InsnNode ARRAYLENGTH() {
		return new InsnNode(ARRAYLENGTH);
	}

	/**
	 * <b>Throw an exception or error</b>
	 * <p>
	 * The objectref must be of type reference and must refer to an object that is an instance of
	 * class Throwable or of a subclass of Throwable. It is popped from the operand stack. The
	 * objectref is then thrown by searching the current method for the first exception handler that
	 * matches the class of objectref, as given by the algorithm in JVMS §2.10.
	 * <p>
	 * If an exception handler that matches objectref is found, it contains the location of the code
	 * intended to handle this exception. The pc register is reset to that location, the operand
	 * stack of the current frame is cleared, objectref is pushed back onto the operand stack, and
	 * execution continues.
	 * <p>
	 * If no matching exception handler is found in the current frame, that frame is popped. If the
	 * current frame represents an invocation of a synchronized method, the monitor entered or
	 * reentered on invocation of the method is exited as if by execution of a monitorexit
	 * instruction. Finally, the frame of its invoker is reinstated, if such a frame exists, and the
	 * objectref is rethrown. If no such frame exists, the current thread exits.
	 * <p>
	 * If objectref is null, athrow throws a NullPointerException instead of objectref.
	 * <p>
	 * Otherwise, if the Java Virtual Machine implementation does not enforce the rules on
	 * structured locking described in §2.11.10, then if the method of the current frame is a
	 * synchronized method and the current thread is not the owner of the monitor entered or
	 * reentered on invocation of the method, athrow throws an IllegalMonitorStateException instead
	 * of the object previously being thrown. This can happen, for example, if an abruptly
	 * completing synchronized method contains a monitorexit instruction, but no monitorenter
	 * instruction, on the object on which the method is synchronized.
	 * <p>
	 * Otherwise, if the Java Virtual Machine implementation enforces the rules on structured
	 * locking described in §2.11.10 and if the first of those rules is violated during invocation
	 * of the current method, then athrow throws an IllegalMonitorStateException instead of the
	 * object previously being thrown.
	 * <p>
	 * If a handler for this exception is matched in the current method, the athrow instruction
	 * discards all the values on the operand stack, then pushes the thrown object onto the operand
	 * stack. However, if no handler is matched in the current method and the exception is thrown
	 * farther up the method invocation chain, then the operand stack of the method (if any) that
	 * handles the exception is cleared and objectref is pushed onto that empty operand stack. All
	 * intervening frames from the method that threw the exception up to, but not including, the
	 * method that handles the exception are discarded.
	 */
	protected final InsnNode ATHROW() {
		return new InsnNode(ATHROW);
	}

	/**
	 * <b>Check whether object is of given type</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the descriptor to your
	 * currently selected mappings.</i>
	 * <p>
	 * The given descriptor is stored in the constant pool as a symbolic reference. It is resolved
	 * during linking and must result in a class, array, or interface type.
	 * <p>
	 * The objectref, which must be of type reference, is peeked from the operand stack. If
	 * objectref is null, then the operand stack is unchanged.
	 * <p>
	 * Otherwise, the named class, array, or interface type is resolved. If objectref can be cast to
	 * the resolved class, array, or interface type, the operand stack is unchanged; otherwise, the
	 * checkcast instruction throws a ClassCastException.
	 * <p>
	 * The following rules are used to determine whether an objectref that is not null can be cast
	 * to the resolved type. If S is the type of the object referred to by objectref, and T is the
	 * resolved class, array, or interface type, then checkcast determines whether objectref can be
	 * cast to type T as follows:
	 * <ul>
	 * <li>If S is a class type, then:
	 * <ul>
	 * <li>If T is a class type, then S must be the same class as T, or S must be a subclass of T;
	 * <li>If T is an interface type, then S must implement interface T.
	 * </ul>
	 * </li>
	 * <li>If S is an array type SC[], that is, an array of components of type SC, then:
	 * <ul>
	 * <li>If T is a class type, then T must be Object.
	 * <li>If T is an interface type, then T must be one of the interfaces implemented by arrays (JLS
	 * 		§4.10.3).
	 * <li>If T is an array type TC[], that is, an array of components of type TC, then one of the
	 * 		following must be true:
	 * <ul>
	 * <li>TC and SC are the same primitive type.
	 * <li>TC and SC are reference types, and type SC can be cast to TC by recursive application of
	 * 		these rules.
	 * </ul>
	 * </li>
	 * </ul>
	 * During resolution of the symbolic reference to the class, array, or interface type, any of
	 * the exceptions documented in JVMS §5.4.3.1 can be thrown.
	 * <p>
	 * The checkcast instruction is very similar to the {@link #INSTANCEOF} instruction. It differs
	 * in its treatment of null, its behavior when its test fails (checkcast throws an exception,
	 * instanceof pushes a result code), and its effect on the operand stack.
	 * @throws ClassCastException  if objectref cannot be cast to the resolved class, array, or
	 * 		interface type
	 */
	protected final TypeInsnNode CHECKCAST(String desc) {
		return new TypeInsnNode(CHECKCAST, remapType(desc));
	}

	/**
	 * <b>Determine if object is of given type</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the descriptor to your
	 * currently selected mappings.</i>
	 * <p>
	 * The given descriptor is stored in the constant pool as a symbolic reference. It is resolved
	 * during linking and must result in a class, array, or interface type.
	 * <p>
	 * The objectref, which must be of type reference, is popped from the operand stack. If
	 * objectref is null, the instanceof instruction pushes an int result of 0 as an int onto the
	 * operand stack.
	 * <p>
	 * Otherwise, the named class, array, or interface type is resolved. If objectref can be cast to
	 * the resolved class, array, or interface type, the instanceof instruction pushes an int
	 * result of 1 as an int onto the operand stack; otherwise, it pushes an int result of 0.
	 * <p>
	 * The following rules are used to determine whether an objectref that is not null can be cast
	 * to the resolved type. If S is the type of the object referred to by objectref, and T is the
	 * resolved class, array, or interface type, then checkcast determines whether objectref can be
	 * cast to type T as follows:
	 * <ul>
	 * <li>If S is a class type, then:
	 * <ul>
	 * <li>If T is a class type, then S must be the same class as T, or S must be a subclass of T;
	 * <li>If T is an interface type, then S must implement interface T.
	 * </ul>
	 * </li>
	 * <li>If S is an array type SC[], that is, an array of components of type SC, then:
	 * <ul>
	 * <li>If T is a class type, then T must be Object.
	 * <li>If T is an interface type, then T must be one of the interfaces implemented by arrays (JLS
	 * 		§4.10.3).
	 * <li>If T is an array type TC[], that is, an array of components of type TC, then one of the
	 * 		following must be true:
	 * <ul>
	 * <li>TC and SC are the same primitive type.
	 * <li>TC and SC are reference types, and type SC can be cast to TC by recursive application of
	 * 		these rules.
	 * </ul>
	 * </li>
	 * </ul>
	 * During resolution of the symbolic reference to the class, array, or interface type, any of
	 * the exceptions documented in JVMS §5.4.3.1 can be thrown.
	 * <p>
	 * The instanceof instruction is very similar to the {@link #CHECKCAST} instruction. It differs
	 * in its treatment of null, its behavior when its test fails (checkcast throws an exception,
	 * instanceof pushes a result code), and its effect on the operand stack.
	 */
	protected final TypeInsnNode INSTANCEOF(String desc) {
		return new TypeInsnNode(INSTANCEOF, remapType(desc));
	}

	/**
	 * <b>Enter monitor for object</b>
	 * <p>
	 * The objectref must be of type reference. It is peeked from the operand stack.
	 * <p>
	 * Each object is associated with a monitor. A monitor is locked if and only if it has an owner.
	 * The thread that executes monitorenter attempts to gain ownership of the monitor associated
	 * with objectref, as follows:
	 * <ul>
	 * <li>If the entry count of the monitor associated with objectref is zero, the thread enters the
	 * 		monitor and sets its entry count to one. The thread is then the owner of the monitor.
	 * <li>If the thread already owns the monitor associated with objectref, it reenters the monitor,
	 * 		incrementing its entry count.
	 * <li>If another thread already owns the monitor associated with objectref, the thread blocks until
	 * 		the monitor's entry count is zero, then tries again to gain ownership.
	 * </ul>
	 * A monitorenter instruction may be used with one or more {@link #MONITOREXIT} instructions to
	 * implement a synchronized statement in the Java programming language. The monitorenter and
	 * monitorexit instructions are not used in the implementation of synchronized methods, although
	 * they can be used to provide equivalent locking semantics. Monitor entry on invocation of a
	 * synchronized method, and monitor exit on its return, are handled implicitly by the Java
	 * Virtual Machine's method invocation and return instructions, as if monitorenter and
	 * monitorexit were used.
	 * <p>
	 * The association of a monitor with an object may be managed in various ways that are beyond
	 * the scope of this specification. For instance, the monitor may be allocated and deallocated
	 * at the same time as the object. Alternatively, it may be dynamically allocated at the time
	 * when a thread attempts to gain exclusive access to the object and freed at some later time
	 * when no thread remains in the monitor for the object.
	 * <p>
	 * The synchronization constructs of the Java programming language require support for
	 * operations on monitors besides entry and exit. These include waiting on a monitor
	 * (Object.wait) and notifying other threads waiting on a monitor (Object.notifyAll and
	 * Object.notify). These operations are supported in the standard package java.lang supplied
	 * with the Java Virtual Machine. No explicit support for these operations appears in the
	 * instruction set of the Java Virtual Machine.
	 * @throws NullPointerException if objectref is null
	 */
	protected final InsnNode MONITORENTER() {
		return new InsnNode(MONITORENTER);
	}

	/**
	 * <b>Exit monitor for object</b>
	 * <p>
	 * The objectref must be of type reference. It is peeked from the operand stack.
	 * <p>
	 * The thread that executes monitorexit must be the owner of the monitor associated with the
	 * instance referenced by objectref.
	 * <p>
	 * The thread decrements the entry count of the monitor associated with objectref. If as a
	 * result the value of the entry count is zero, the thread exits the monitor and is no longer
	 * its owner. Other threads that are blocking to enter the monitor are allowed to attempt to do
	 * so.
	 * <p>
	 * One or more monitorexit instructions may be used with a {@link #MONITORENTER()} instruction
	 * to implement a synchronized statement in the Java programming language. The monitorenter and
	 * monitorexit instructions are not used in the implementation of synchronized methods,
	 * although they can be used to provide equivalent locking semantics.
	 * <p>
	 * The Java Virtual Machine supports exceptions thrown within synchronized methods and synchronized
	 * statements differently:
	 * <ul>
	 * <li>Monitor exit on normal synchronized method completion is handled by the Java Virtual Machine's
	 * 		return instructions. Monitor exit on abrupt synchronized method completion is handled implicitly
	 * 		by the Java Virtual Machine's athrow instruction.
	 * <li>When an exception is thrown from within a synchronized statement, exit from the monitor entered
	 * 		prior to the execution of the synchronized statement is achieved using the Java Virtual Machine's
	 * 		exception handling mechanism.
	 * </ul>
	 * @throws NullPointerException if objectref is null
	 * @throws IllegalMonitorStateException if the thread that executes monitorexit is not the owner
	 * 		of the monitor associated with the instance referenced by objectref, or if the Java
	 * 		Virtual Machine implementation enforces the rules on structured locking and if the
	 * 		second of those rules is violated by the execution of this monitorexit instruction
	 */
	protected final InsnNode MONITOREXIT() {
		return new InsnNode(MONITOREXIT);
	}

	/**
	 * <b>Create new multidimensional array</b>
	 * <p>
	 * <i>If you have mappings available, NilLoader will automatically remap the type to your
	 * currently selected mappings.</i>
	 * <p>
	 * The given type is stored in the constant pool as a symbolic reference. It is resolved during
	 * linking and must be an array class type of dimensionality greater than or equal to
	 * dimensions.
	 * <p>
	 * The dimensions operand is an unsigned byte that must be greater than or equal to 1. It
	 * represents the number of dimensions of the array to be created. The operand stack must
	 * contain dimensions values. Each such value represents the number of components in a dimension
	 * of the array to be created, must be of type int, and must be non-negative. The count1 is the
	 * desired length in the first dimension, count2 in the second, etc.
	 * <p>
	 * All of the count values are popped off the operand stack.
	 * <p>
	 * A new multidimensional array of the array type is allocated from the garbage-collected heap.
	 * If any count value is zero, no subsequent dimensions are allocated. The components of the
	 * array in the first dimension are initialized to subarrays of the type of the second
	 * dimension, and so on. The components of the last allocated dimension of the array are
	 * initialized to the default initial value for the element type of the array type. A reference
	 * arrayref to the new array is pushed onto the operand stack.
	 * <p>
	 * During resolution of the symbolic reference to the class, array, or interface type, any of
	 * the exceptions documented in §5.4.3.1 can be thrown.
	 * <p>
	 * It may be more efficient to use {@link #NEWARRAY} or {@link #ANEWARRAY} when creating an
	 * array of a single dimension.
	 * <p>
	 * The array class referenced via the run-time constant pool may have more dimensions than the
	 * dimensions operand of the multianewarray instruction. In that case, only the first dimensions
	 * of the dimensions of the array are created.
	 * @throws IllegalAccessError if the current class does not have permission to access the
	 * 		element type of the resolved array class
	 * @throws NegativeArraySizeException if any of the dimensions values on the operand stack are
	 * 		less than zero
	 */
	protected final MultiANewArrayInsnNode MULTIANEWARRAY(String type, int dim) {
		return new MultiANewArrayInsnNode(remapType(type), dim);
	}

	/**
	 * <b>Branch if reference is null</b>
	 * <p>
	 * The value must of type reference. It is popped from the operand stack. If value is null,
	 * execution proceeds at the given label. Otherwise, execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFNULL(LabelNode label) {
		return new JumpInsnNode(IFNULL, label);
	}

	/**
	 * <b>Branch if reference is not null</b>
	 * <p>
	 * The value must of type reference. It is popped from the operand stack. If value is not null,
	 * execution proceeds at the given label. Otherwise, execution proceeds following this instruction.
	 */
	protected final JumpInsnNode IFNONNULL(LabelNode label) {
		return new JumpInsnNode(IFNONNULL, label);
	}

}
