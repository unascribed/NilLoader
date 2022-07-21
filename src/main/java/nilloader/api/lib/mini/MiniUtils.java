package nilloader.api.lib.mini;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.cadixdev.bombe.type.FieldType;
import org.cadixdev.bombe.type.MethodDescriptor;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MiniUtils {
	
	public static String describe(Iterable<? extends AbstractInsnNode> list) {
		StringBuilder sb = new StringBuilder();
		describe(sb, list);
		return sb.toString();
	}
	
	public static String describe(AbstractInsnNode[] arr) {
		return describe(Arrays.asList(arr));
	}

	public static String describe(AbstractInsnNode a) {
		StringBuilder sb = new StringBuilder();
		describeInner(sb, a, null);
		return sb.toString();
	}
	
	public static void describe(StringBuilder sb, Iterable<? extends AbstractInsnNode> list) {
		describe(sb, list, "");
	}
	
	public static void describe(StringBuilder sb, AbstractInsnNode[] arr) {
		describe(sb, Arrays.asList(arr), "");
	}

	public static void describe(StringBuilder sb, AbstractInsnNode a) {
		describeInner(sb, a, null);
	}
	
	public static void describe(StringBuilder sb, Iterable<? extends AbstractInsnNode> list, String indent) {
		Map<LabelNode, Integer> labelIds = new IdentityHashMap<>();
		boolean first = true;
		for (AbstractInsnNode n : list) {
			if (first) {
				first = false;
			} else {
				sb.append(",\n");
				sb.append(indent);
			}
			describeInner(sb, n, labelIds);
		}
	}
	
	public static void describe(StringBuilder sb, AbstractInsnNode[] arr, String indent) {
		describe(sb, Arrays.asList(arr), indent);
	}
	
	public static void describeInner(StringBuilder sb, AbstractInsnNode a, Map<LabelNode, Integer> labelIds) {
		if (a instanceof LabelNode) {
			if (labelIds == null) {
				sb.append("L"+Integer.toHexString(System.identityHashCode(a)));
				return;
			}
			sb.append("L"+labelIds.computeIfAbsent((LabelNode)a, (ln) -> labelIds.size()));
			return;
		} else if (a instanceof LineNumberNode) {
			sb.append("// line "+((LineNumberNode)a).line);
			return;
		} else if (a instanceof FrameNode) {
			sb.append("// frame: ");
			switch (((FrameNode)a).type) {
				case F_NEW:
					sb.append("new");
					break;
				case F_FULL:
					sb.append("full");
					break;
				case F_APPEND:
					sb.append("append");
					break;
				case F_CHOP:
					sb.append("chop");
					break;
				case F_SAME:
					sb.append("same");
					break;
			}
			return;
		}
		sb.append(getMnemonic(a.getOpcode()));
		sb.append("(");
		switch (a.getType()) {
			case INSN: {
				break;
			}
			case INT_INSN: {
				IntInsnNode n = (IntInsnNode)a;
				sb.append(n.operand);
				break;
			}
			case VAR_INSN: {
				VarInsnNode n = (VarInsnNode)a;
				sb.append(n.var);
				break;
			}
			case TYPE_INSN: {
				TypeInsnNode n = (TypeInsnNode)a;
				appendString(sb, n.desc);
				break;
			}
			case FIELD_INSN: {
				FieldInsnNode n = (FieldInsnNode)a;
				appendString(sb, n.owner).append(", ");
				appendString(sb, n.name).append(", ");
				appendString(sb, n.desc);
				break;
			}
			case METHOD_INSN: {
				MethodInsnNode n = (MethodInsnNode)a;
				appendString(sb, n.owner).append(", ");
				appendString(sb, n.name).append(", ");
				appendString(sb, n.desc);
				break;
			}
			case JUMP_INSN: {
				JumpInsnNode n = (JumpInsnNode)a;
				if (n.label == null) {
					sb.append("*");
				} else {
					describeInner(sb, n.label, labelIds);
				}
				break;
			}
			case LDC_INSN: {
				LdcInsnNode n = (LdcInsnNode)a;
				if (n.cst instanceof String) {
					appendString(sb, (String)n.cst);
				} else if (n.cst instanceof Integer) {
					sb.append(((Integer)n.cst).intValue());
				} else if (n.cst instanceof Long) {
					sb.append(((Long)n.cst).longValue());
					sb.append("L");
				} else if (n.cst instanceof Float) {
					sb.append(((Float)n.cst).floatValue());
					sb.append("f");
				} else if (n.cst instanceof Double) {
					sb.append(((Double)n.cst).doubleValue());
					sb.append("D");
				} else if (n.cst instanceof Type) {
					sb.append("/* type ");
					sb.append(n.cst);
					sb.append(" */");
				}
				break;
			}
			case IINC_INSN: {
				IincInsnNode n = (IincInsnNode)a;
				sb.append(n.var).append(", ");
				sb.append(n.incr);
				break;
			}
			case MULTIANEWARRAY_INSN: {
				MultiANewArrayInsnNode n = (MultiANewArrayInsnNode)a;
				appendString(sb, n.desc).append(", ");
				sb.append(n.dims);
				break;
			}
			case INVOKE_DYNAMIC_INSN:
			case TABLESWITCH_INSN:
			case LOOKUPSWITCH_INSN: {
				sb.append("...");
				break;
			}
			default: {
				sb.append("???????");
				break;
			}
		}
		sb.append(")");
	}

	private static StringBuilder appendString(StringBuilder sb, String str) {
		sb.append("\"");
		sb.append(str.replace("\\", "\\\\").replace("\"", "\\\""));
		sb.append("\"");
		return sb;
	}

	/**
	 * <b>WARNING</b>: MappingSet is not considered to be API. Use with caution.
	 */
	public static String remapType(Optional<MappingSet> mappings, String type) {
		return mappings.map(m -> m.computeClassMapping(type))
				.orElse(Optional.empty())
				.map(ClassMapping::getFullDeobfuscatedName)
				.orElse(type);
	}
	
	/**
	 * <b>WARNING</b>: MappingSet is not considered to be API. Use with caution.
	 */
	public static String remapField(Optional<MappingSet> mappings, String owner, String name, String desc) {
		return mappings.map(m -> m.computeClassMapping(owner))
				.orElse(Optional.empty())
				.map(cm -> cm.computeFieldMapping(FieldSignature.of(name, desc)))
				.orElse(Optional.empty())
				.map(FieldMapping::getDeobfuscatedName)
				.orElse(name);
	}

	/**
	 * <b>WARNING</b>: MappingSet is not considered to be API. Use with caution.
	 */
	public static String remapMethod(Optional<MappingSet> mappings, String owner, String name, String desc) {
		return mappings.map(m -> m.computeClassMapping(owner))
			.orElse(Optional.empty())
			.map(cm -> cm.getMethodMapping(name, desc))
			.orElse(Optional.empty())
			.map(MethodMapping::getDeobfuscatedSignature)
			.map(MethodSignature::getName)
			.orElse(name);
	}

	/**
	 * <b>WARNING</b>: MappingSet is not considered to be API. Use with caution.
	 */
	public static String remapMethodDesc(Optional<MappingSet> mappings, String desc) {
		return mappings.map(m -> m.deobfuscate(MethodDescriptor.of(desc)))
				.map(MethodDescriptor::toString)
				.orElse(desc);
	}

	/**
	 * <b>WARNING</b>: MappingSet is not considered to be API. Use with caution.
	 */
	public static String remapFieldDesc(Optional<MappingSet> mappings, String desc) {
		return mappings.map(m -> m.deobfuscate(FieldType.of(desc)))
				.map(FieldType::toString)
				.orElse(desc);
	}
	
	public static String getMnemonic(int opcode) {
		switch (opcode) {
			case NOP: return "NOP";
			case ACONST_NULL: return "ACONST_NULL";
			case ICONST_M1: return "ICONST_M1";
			case ICONST_0: return "ICONST_0";
			case ICONST_1: return "ICONST_1";
			case ICONST_2: return "ICONST_2";
			case ICONST_3: return "ICONST_3";
			case ICONST_4: return "ICONST_4";
			case ICONST_5: return "ICONST_5";
			case LCONST_0: return "LCONST_0";
			case LCONST_1: return "LCONST_1";
			case FCONST_0: return "FCONST_0";
			case FCONST_1: return "FCONST_1";
			case FCONST_2: return "FCONST_2";
			case DCONST_0: return "DCONST_0";
			case DCONST_1: return "DCONST_1";
			case BIPUSH: return "BIPUSH";
			case SIPUSH: return "SIPUSH";
			case LDC: return "LDC";
			case ILOAD: return "ILOAD";
			case LLOAD: return "LLOAD";
			case FLOAD: return "FLOAD";
			case DLOAD: return "DLOAD";
			case ALOAD: return "ALOAD";
			case IALOAD: return "IALOAD";
			case LALOAD: return "LALOAD";
			case FALOAD: return "FALOAD";
			case DALOAD: return "DALOAD";
			case AALOAD: return "AALOAD";
			case BALOAD: return "BALOAD";
			case CALOAD: return "CALOAD";
			case SALOAD: return "SALOAD";
			case ISTORE: return "ISTORE";
			case LSTORE: return "LSTORE";
			case FSTORE: return "FSTORE";
			case DSTORE: return "DSTORE";
			case ASTORE: return "ASTORE";
			case IASTORE: return "IASTORE";
			case LASTORE: return "LASTORE";
			case FASTORE: return "FASTORE";
			case DASTORE: return "DASTORE";
			case AASTORE: return "AASTORE";
			case BASTORE: return "BASTORE";
			case CASTORE: return "CASTORE";
			case SASTORE: return "SASTORE";
			case POP: return "POP";
			case POP2: return "POP2";
			case DUP: return "DUP";
			case DUP_X1: return "DUP_X1";
			case DUP_X2: return "DUP_X2";
			case DUP2: return "DUP2";
			case DUP2_X1: return "DUP2_X1";
			case DUP2_X2: return "DUP2_X2";
			case SWAP: return "SWAP";
			case IADD: return "IADD";
			case LADD: return "LADD";
			case FADD: return "FADD";
			case DADD: return "DADD";
			case ISUB: return "ISUB";
			case LSUB: return "LSUB";
			case FSUB: return "FSUB";
			case DSUB: return "DSUB";
			case IMUL: return "IMUL";
			case LMUL: return "LMUL";
			case FMUL: return "FMUL";
			case DMUL: return "DMUL";
			case IDIV: return "IDIV";
			case LDIV: return "LDIV";
			case FDIV: return "FDIV";
			case DDIV: return "DDIV";
			case IREM: return "IREM";
			case LREM: return "LREM";
			case FREM: return "FREM";
			case DREM: return "DREM";
			case INEG: return "INEG";
			case LNEG: return "LNEG";
			case FNEG: return "FNEG";
			case DNEG: return "DNEG";
			case ISHL: return "ISHL";
			case LSHL: return "LSHL";
			case ISHR: return "ISHR";
			case LSHR: return "LSHR";
			case IUSHR: return "IUSHR";
			case LUSHR: return "LUSHR";
			case IAND: return "IAND";
			case LAND: return "LAND";
			case IOR: return "IOR";
			case LOR: return "LOR";
			case IXOR: return "IXOR";
			case LXOR: return "LXOR";
			case IINC: return "IINC";
			case I2L: return "I2L";
			case I2F: return "I2F";
			case I2D: return "I2D";
			case L2I: return "L2I";
			case L2F: return "L2F";
			case L2D: return "L2D";
			case F2I: return "F2I";
			case F2L: return "F2L";
			case F2D: return "F2D";
			case D2I: return "D2I";
			case D2L: return "D2L";
			case D2F: return "D2F";
			case I2B: return "I2B";
			case I2C: return "I2C";
			case I2S: return "I2S";
			case LCMP: return "LCMP";
			case FCMPL: return "FCMPL";
			case FCMPG: return "FCMPG";
			case DCMPL: return "DCMPL";
			case DCMPG: return "DCMPG";
			case IFEQ: return "IFEQ";
			case IFNE: return "IFNE";
			case IFLT: return "IFLT";
			case IFGE: return "IFGE";
			case IFGT: return "IFGT";
			case IFLE: return "IFLE";
			case IF_ICMPEQ: return "IF_ICMPEQ";
			case IF_ICMPNE: return "IF_ICMPNE";
			case IF_ICMPLT: return "IF_ICMPLT";
			case IF_ICMPGE: return "IF_ICMPGE";
			case IF_ICMPGT: return "IF_ICMPGT";
			case IF_ICMPLE: return "IF_ICMPLE";
			case IF_ACMPEQ: return "IF_ACMPEQ";
			case IF_ACMPNE: return "IF_ACMPNE";
			case GOTO: return "GOTO";
			case JSR: return "JSR";
			case RET: return "RET";
			case TABLESWITCH: return "TABLESWITCH";
			case LOOKUPSWITCH: return "LOOKUPSWITCH";
			case IRETURN: return "IRETURN";
			case LRETURN: return "LRETURN";
			case FRETURN: return "FRETURN";
			case DRETURN: return "DRETURN";
			case ARETURN: return "ARETURN";
			case RETURN: return "RETURN";
			case GETSTATIC: return "GETSTATIC";
			case PUTSTATIC: return "PUTSTATIC";
			case GETFIELD: return "GETFIELD";
			case PUTFIELD: return "PUTFIELD";
			case INVOKEVIRTUAL: return "INVOKEVIRTUAL";
			case INVOKESPECIAL: return "INVOKESPECIAL";
			case INVOKESTATIC: return "INVOKESTATIC";
			case INVOKEINTERFACE: return "INVOKEINTERFACE";
			case INVOKEDYNAMIC: return "INVOKEDYNAMIC";
			case NEW: return "NEW";
			case NEWARRAY: return "NEWARRAY";
			case ANEWARRAY: return "ANEWARRAY";
			case ARRAYLENGTH: return "ARRAYLENGTH";
			case ATHROW: return "ATHROW";
			case CHECKCAST: return "CHECKCAST";
			case INSTANCEOF: return "INSTANCEOF";
			case MONITORENTER: return "MONITORENTER";
			case MONITOREXIT: return "MONITOREXIT";
			case MULTIANEWARRAY: return "MULTIANEWARRAY";
			case IFNULL: return "IFNULL";
			case IFNONNULL: return "IFNONNULL";
			default: return "UNK_"+Integer.toHexString(opcode);
		}
	}
	
}
