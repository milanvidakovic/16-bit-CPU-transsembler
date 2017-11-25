package transsembler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Transsembler {

	private static final int BEGINNING = 1;
	private static final int GLOBAL_OR_FUNCTION = 2;
	private static final int END = 3;
	private static final int FUNCTION = 4;
	private static final int GLOBAL = 5;
	private static final int DATA_SEGMENT_CONSTS = 6;
	private static final int BEGINNING_OF_FUNCTION = 7;
	private static final int FUNCTION_BODY = 8;
	private static final int LOCAL_DATA_SEGMENT = 9;
	private static final int STACK_FRAME_CONSTRUCT = 10;
	private static final int FUNCTION_CODE = 11;
	private static final int STACK_FRAME_END = 12;

	private Map<String, GlobalVar> globalVars = new HashMap<String, GlobalVar>();
	private Map<String, GlobalVar> globalLabels = new HashMap<String, GlobalVar>();
	private Map<String, Function> functions = new HashMap<String, Function>();

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java Transsembler file.asm");
			return;
		}
		new Transsembler(args[0]);
	}

	public Transsembler(String fileName) {
		File f = new File(fileName);
		List<String> lines = new ArrayList<String>(100000);
		if (!f.exists() || f.isDirectory()) {
			System.out.println("File " + f.getName() + " does not exist, or is directory (folder).");
			return;
		}
		BufferedReader in;
		String s;
		try {
			in = new BufferedReader(new FileReader(f));
			while ((s = in.readLine()) != null) {
				lines.add(s);
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		process(lines);
		
		try {
			PrintWriter out = new PrintWriter(new FileWriter(f.getName() + ".asm"));
			if (functions.get("_main") != null) {
				out.println("ld sp, 0xe000");
				out.println("call _main");
				out.println("halt");
			}
			for (Function fun : functions.values()) {
				
				for (String l : fun.localLabels.keySet()) {
					out.println(l + ": ");
					out.println("#str16 " + fun.localLabels.get(l));
				}
				
				if (fun.fName.equals("_main"))
					continue;
				out.println(fun.fName + ":");
				out.println(fun.stackFrameSrc);
				out.println(fun.code);
				out.println(fun.stackFrameCleanup);
			}
			if (functions.get("_main") != null) {
				out.println(functions.get("_main").fName + ":");
				out.println(functions.get("_main").stackFrameSrc);
				out.println(functions.get("_main").code);
				out.println(functions.get("_main").stackFrameCleanup);
			}
			
			for (GlobalVar g : globalVars.values()) {
				out.println(g.gName + ":");
				if (g.type == GlobalVar.S_TYPE) {
					out.println("#str16 " + g.value);
				} else {
					out.println("#d16 " + g.value);
				}
			}
			if (functions.get("_main") != null) {
				out.println("#include \"stdio.asm.asm\"");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		System.out.println(globalVars);
//		System.out.println(globalLabels);
//		System.out.println(functions);
	}

	private void process(List<String> lines) {
		int state = BEGINNING;
		int i = 0;
		String currentLine = "", nextLine = "";
		String currentFunction = "";
		while (state != END) {
			if (i >= lines.size())
				break;
			currentLine = lines.get(i);
			switch (state) {
			case BEGINNING:
				if (currentLine.startsWith("PUBLIC	_")) {
					// PUBLIC _tekst1
					if (i < lines.size() - 2) {
						nextLine = lines.get(i + 1);
						state = GLOBAL_OR_FUNCTION;
						i--;
					} else {
						throw new RuntimeException("Wrong position of PUBLIC _ string within the asm file!");
					}
				}
				break;
			case GLOBAL_OR_FUNCTION:
				if (nextLine.equals("_TEXT	SEGMENT") 
				||  nextLine.contains("EXTRN")) {
					state = FUNCTION;
					i--;
				} else {
					state = GLOBAL;
					// PUBLIC _tekst1
					String gName = extractName(currentLine);
					if (gName == null) {
						throw new RuntimeException("Could not find global var name: " + currentLine);
					}
					globalVars.put(gName, new GlobalVar(gName));
				}
				break;
			case GLOBAL:
				if (currentLine.startsWith("PUBLIC	_")) {
					// PUBLIC _tekst1
					String gName = extractName(currentLine);
					if (gName == null) {
						throw new RuntimeException("GLOBAL: Could not find global var name: " + currentLine);
					}
					globalVars.put(gName, new GlobalVar(gName));
				} else if (currentLine.startsWith("_DATA	SEGMENT")) {
					state = DATA_SEGMENT_CONSTS;
				}
				break;
			case DATA_SEGMENT_CONSTS:
				if (!currentLine.startsWith("_DATA	ENDS")) {
					// _tekst1 DD FLAT:$SG23
					// _globalna_promenljiva DD 02H
					// $SG23 DB 'ovo je globalni tekst', 00H
					String vName = extractGlobalName(currentLine);
					if (vName == null) {
						throw new RuntimeException(
								"DATA_SEGMENT_CONSTS: Could not find global var name: " + currentLine);
					}
					GlobalVar var = globalVars.get(vName);
					if (var == null) {
						var = globalLabels.get(vName);
						if (var == null) {
							throw new RuntimeException("DATA_SEGMENT_CONSTS: Could not find global var: " + vName);
						}
						// found a label in the DATA_SEGMENT
						// $SG23 DB 'ovo je globalni tekst', 00H
						String lValue = extractString(currentLine);
						if (lValue == null) {
							throw new RuntimeException(
									"DATA_SEGMENT_CONSTS: Could not extract global string value: " + currentLine);
						}
						var.value = "\"" + lValue + "\\0\"";
						var.type = GlobalVar.S_TYPE;
						break;
					}
					// found a literal in the DATA_SEGMENT
					// _tekst1 DD FLAT:$SG23
					// _globalna_promenljiva DD 02H
					String vValue = extractValue(currentLine);
					if (vValue == null) {
						throw new RuntimeException(
								"DATA_SEGMENT_CONSTS: Could not extract global var literal value: " + currentLine);
					}
					if (vValue.startsWith("FLAT:")) {
						// _tekst1 DD FLAT:$SG23
						String v = extractLabel(vValue);
						if (v == null) {
							throw new RuntimeException(
									"DATA_SEGMENT_CONSTS: Could not extract global label: " + currentLine);
						}
						var.value = v;
						var.type = GlobalVar.S_TYPE;
						globalLabels.put(var.value, var);
					} else if (!vValue.contains("'")) {
						// _globalna_promenljiva DD 02H
						var.value = extractHex(vValue);
						var.type = GlobalVar.N_TYPE;
					} else {
						throw new RuntimeException(
								"DATA_SEGMENT_CONSTS: Unknown global var value type: " + currentLine);
					}
				} else if (currentLine.startsWith("PUBLIC	_")) {
					// PUBLIC _f2
					if (i < lines.size() - 2) {
						nextLine = lines.get(i + 1);
						state = FUNCTION;
						i--;
					} else {
						throw new RuntimeException("Wrong position of PUBLIC _ string within the asm file!");
					}
				} else if (currentLine.startsWith("_DATA	ENDS")) {
					if (i < lines.size() - 2) {
						nextLine = lines.get(i + 1);
						state = FUNCTION;
					} else {
						throw new RuntimeException("Wrong position of PUBLIC _ string within the asm file!");
					}
				} else {
					throw new RuntimeException("DATA_SEGMENT_CONSTS: Unknown line: " + currentLine);
				}
				break;
			case FUNCTION:
				if (currentLine.equals("END")) {
					state = END;
					break;
				}
				// PUBLIC _f2
				String fName = extractName(currentLine);
				if (fName == null) {
					throw new RuntimeException("FUNCTION: Could not extract function name: " + currentLine);
				}
				functions.put(fName, new Function(fName));
				currentFunction = fName;
				state = BEGINNING_OF_FUNCTION;
				break;
			case BEGINNING_OF_FUNCTION:
				if (currentLine.startsWith(currentFunction + "	PROC NEAR")
						|| currentLine.startsWith(currentFunction + " PROC NEAR")) {
					// _f2 PROC NEAR
					state = FUNCTION_BODY;
				} else if (currentLine.startsWith("_DATA	SEGMENT")) {
					state = LOCAL_DATA_SEGMENT;
					break;
				}
				// LOCAL VARS
				// _x$ = -8
				if (currentLine.startsWith("_") && currentLine.contains("=")) {
					LocalVar lv = new LocalVar(currentLine);
					functions.get(currentFunction).localVars.put(lv.lName, lv);
				}
				break;
			case LOCAL_DATA_SEGMENT:
				if (currentLine.contains("ORG")) {
					break;
				}
				if (currentLine.startsWith("_DATA	ENDS")) {
					state = BEGINNING_OF_FUNCTION;
					break;
				}
				// $SG61 DB 'ovo je lokalni tekst', 00H
				String lName = extractGlobalName(currentLine);
				if (lName == null) {
					throw new RuntimeException("LOCAL_DATA_SEGMENT: Could not extract label name: " + currentLine);
				}
				String lVal = extractString(currentLine);
				if (lVal == null) {
					throw new RuntimeException("LOCAL_DATA_SEGMENT: Could not extract string: " + currentLine);
				}
				functions.get(currentFunction).localLabels.put(lName.substring(1), "\"" + lVal + "\\0\"");
				break;
			case FUNCTION_BODY:
				if (currentLine.contains("push	ebp")) {
					state = STACK_FRAME_CONSTRUCT;
					functions.get(currentFunction).stackFrameSrc = "push b\n";
					break;
				}
				break;
			case STACK_FRAME_CONSTRUCT:
				if (currentLine.contains("mov	ebp, esp")) {
					functions.get(currentFunction).stackFrameSrc += "ld b, sp\n";
				} else if (currentLine.contains("sub	esp,")) {
					System.out.println("LOCAL VARS COUNT: " + functions.get(currentFunction).localVarsCount());
					System.out.println("MAX LOCAL VARS OFFSET: " + functions.get(currentFunction).getMaxLocalVarOffset());
					functions.get(currentFunction).stackFrameSrc += "add sp, "
							+ (functions.get(currentFunction).getMaxLocalVarOffset() + 1) + "\n";
				} else if (currentLine.contains("push	edi")
						|| currentLine.contains("push	ebx")) {
					state = FUNCTION_CODE;
				}
				break;
			case FUNCTION_CODE:
				Function f = functions.get(currentFunction);
				if (currentLine.startsWith("; Line")) {
					f.code += currentLine + "\n";
				} else if (currentLine.contains("push	esi") 
						|| currentLine.contains("push	edi")) {
				} else if (currentLine.matches("\\s*(\\$L\\d+\\:)")) {
					// Label
					Pattern p = Pattern.compile("\\s*(\\$L\\d+\\:)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, " + m.group(2) + "\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+(\\d+)\\],\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+(\\d+)\\],\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, " + m.group(3) + "\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(m.group(2), f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+eax\\+(\\d+)\\],\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+eax\\+(\\d+)\\],\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "add a, b\n";
					f.code += "inc a\n";
					f.code += "ld c, a\n";
					f.code += "ld a, " + m.group(3) + "\n";
					f.code += "st a, [c" + f.localVars.get(m.group(1)).getValue(m.group(2), f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*mov\\sal,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*mov\\sal,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "ld a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\],\\sal")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\],\\sal");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, " + m.group(2) + "\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*jmp\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*jmp\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += "jmp " + f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*inc\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*inc\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, 1\n";
					f.code += "add a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*inc\\seax")) {
					f.code += "inc a\n";
				} else if (currentLine.matches("\\s*cmp\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*cmp\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "cmp a, " + m.group(2) + "\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*jge\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*jge\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += "jp " + f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*mov\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*mov\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "ld a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*mov\\sal,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+eax\\]")) {
					Pattern p = Pattern.compile("\\s*mov\\sal,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+eax\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push c\n";
					f.code += "add a, b\n";
					f.code += "ld c, a\n";
					f.code += "ld a, [c" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "pop c\n";
				} else if (currentLine.matches("\\s*mov\\secx,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*mov\\secx,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push a\n";
					f.code += "ld a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "ld c, a\n";
					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+ecx\\],\\sal")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+ecx\\],\\sal");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "st a, [b+c" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*lea\\seax,\\s\\[eax\\+eax\\*(\\d+)\\]")) {
					Pattern p = Pattern.compile("\\s*lea\\seax,\\s\\[eax\\+eax\\*(\\d+)\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push c\n";
					f.code += "ld c, " + f.localVars.get(m.group(1)).getValue("1", f.getMaxLocalVarOffset()) + "\n";
					f.code += "mul a, c\n";
					f.code += "pop c\n";
				} else if (currentLine.matches("\\s*lea\\seax,\\sDWORD\\sPTR\\s\\[eax\\+eax\\*(\\d+)\\]")) {
					Pattern p = Pattern.compile("\\s*lea\\seax,\\sDWORD\\sPTR\\s\\[eax\\+eax\\*(\\d+)\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push c\n";
					String _num = m.group(1);
					int num = Integer.parseInt(_num);
					f.code += "ld c, " + (num + 1) + "\n";
					f.code += "mul a, c\n";
					f.code += "pop c\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+ecx\\+(\\d+)\\],\\sal")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+ecx\\+(\\d+)\\],\\sal");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "st a, [b+c" + f.localVars.get(m.group(1)).getValue(m.group(2), f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.contains("pop	edi")) {
					state = STACK_FRAME_END;
					break;
				} else if (currentLine.contains("cdq")) {
//					f.code += "ld c, 0\n";
				} else if (currentLine.matches("\\s*xor\\seax,\\sedx")) {
					f.code += "xor a, c\n";
				} else if (currentLine.matches("\\s*sub\\seax,\\sedx")) {
					f.code += "sub a, c\n";
				} else if (currentLine.matches("\\s*and\\seax,\\s(\\d+)")) {
					Pattern p = Pattern.compile("\\s*and\\seax,\\s(\\d+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "and a, " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\seax")) {
					Pattern p = Pattern.compile("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\seax");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*xor\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*xor\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "xor a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*mov\\secx,\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*mov\\secx,\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "ld c, " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\sedx")) {
					Pattern p = Pattern.compile("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\sedx");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, h\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n"; 
				} else if (currentLine.matches("\\s*idiv\\secx")) {
					f.code += "div a, c\n";
				} else if (currentLine.matches("\\s*jne\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*jne\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += "jnz " + f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*add\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*add\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "add a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*test\\sedx,\\sedx")) {
				} else if (currentLine.matches("\\s*imul\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*imul\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "mul a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*cmp\\sedx,\\s(\\d+)")) {
					Pattern p = Pattern.compile("\\s*cmp\\sedx,\\s(\\d+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "cmp a, " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*add\\seax,\\seax")) {
					f.code += "shl a, 1\n";
				} else if (currentLine.matches("\\s*add\\seax,\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*add\\seax,\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "add a," + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*add\\seax,\\secx")) {
					f.code += "add a, c\n";
				} else if (currentLine.matches("\\s*xor\\seax,\\seax")) {
					f.code += "ld a, 0\n";
				} else if (currentLine.matches("\\s*mov\\seax,\\s(\\d+)")) {
					Pattern p = Pattern.compile("\\s*mov\\seax,\\s(\\d+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "ld a, " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*add\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)")) {
					Pattern p = Pattern.compile("\\s*add\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "add a, " + (Integer.parseInt(m.group(2)) / 4) + "\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*sar\\seax,\\s(\\d+)")) {
					Pattern p = Pattern.compile("\\s*sar\\seax,\\s(\\d+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "shr a, " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*cmp\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*cmp\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "cmp a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*cmp\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\seax")) {
					Pattern p = Pattern.compile("\\s*cmp\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\seax");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "cmp [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "], a\n";
				} else if (currentLine.matches("\\s*jle\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*jle\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += "jnp " + f.fName+m.group(1).substring(1) + "\n";
					f.code += "jz " + f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*idiv\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*idiv\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "div a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\sOFFSET\\sFLAT:(.+)")) {
					Pattern p = Pattern.compile("\\s*mov\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\],\\sOFFSET\\sFLAT:(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, " + m.group(2).substring(1) + "\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\],\\s(\\d+)(\\s*;.*|$)");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, " + m.group(2) + "\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*inc\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*inc\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "ld a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "inc a\n";
					f.code += "st a, [b" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*dec\\seax")) {
					f.code += "dec a\n";
				} else if (currentLine.matches("\\s*push\\seax")) {
					f.code += "push a\n";
				} else if (currentLine.matches("\\s*call\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*call\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "call " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*add\\sesp,\\s(\\d+)")) {
					Pattern p = Pattern.compile("\\s*add\\sesp,\\s(\\d+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					int num = Integer.parseInt(m.group(1));
					f.code += "sub sp, " + (num / 4) + "\n";
				} else if (currentLine.matches("\\s*mov\\sDWORD\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\],\\seax")) {
					Pattern p = Pattern.compile("\\s*mov\\sDWORD\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\],\\seax");
					Matcher m = p.matcher(currentLine);
					m.find();
//					int num = Integer.parseInt(m.group(1));
					f.code += "push a\n";
//					if (num > 0) 
//						f.code += "st a, [b+" + (num / 4) + "]\n";
//					else
//						f.code += "st a, [b" + (num / 4) + "]\n";
				} else if (currentLine.matches("\\s*mov\\sBYTE\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\],\\sal")) {
					Pattern p = Pattern.compile("\\s*mov\\sBYTE\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\],\\sal");
					Matcher m = p.matcher(currentLine);
					m.find();
//					int num = Integer.parseInt(m.group(1));
					f.code += "push a\n";
//					if (num > 0) 
//						f.code += "st a, [b+" + (num / 4) + "]\n";
//					else
//						f.code += "st a, [b" + (num / 4) + "]\n";
				} else if (currentLine.matches("\\s*mov\\seax,\\sDWORD\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*mov\\seax,\\sDWORD\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
//					int num = Integer.parseInt(m.group(1));
					f.code += "pop a\n";
//					if (num > 0) 
//						f.code += "ld a, [b+" + (num / 4) + "]\n";
//					else
//						f.code += "ld a, [b" + (num / 4) + "]\n";
				} else if (currentLine.matches("\\s*movsx\\seax,\\sBYTE\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*movsx\\seax,\\sBYTE\\sPTR\\s(-\\d|\\d)\\+\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
//					int num = Integer.parseInt(m.group(1));
					f.code += "pop a\n";
//					if (num > 0) 
//						f.code += "ld a, [b+" + (num / 4) + "]\n";
//					else
//						f.code += "ld a, [b" + (num / 4) + "]\n";
				} else if (currentLine.matches("\\s*movsx\\seax,\\sBYTE\\sPTR\\s\\[eax\\]")) {
					f.code += "push c\n";
					f.code += "ld c, a\n";
					f.code += "ld a, [c]\n";
					f.code += "pop c\n";
				} else if (currentLine.matches("\\s*test\\seax,\\seax")) {
					f.code += "cmp a, 0\n";
				} else if (currentLine.matches("\\s*je\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*je\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += "jz " + f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*push\\s(\\d+)\\s*.*")) {
					Pattern p = Pattern.compile("\\s*push\\s(\\d+)\\s*.*");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*mov\\seax,\\sDWORD\\sPTR\\s(_.+)")) {
					Pattern p = Pattern.compile("\\s*mov\\seax,\\sDWORD\\sPTR\\s(_.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "ld a, " + m.group(1) + "\n";
				} else if (currentLine.matches("\\s*jl\\s(.+)")) {
					Pattern p = Pattern.compile("\\s*jl\\s(.+)");
					Matcher m = p.matcher(currentLine);
					m.find();
					// TODO
					f.code += "jnp " + f.fName+m.group(1).substring(1) + "\n";
				} else if (currentLine.matches("\\s*mov\\sDWORD\\sPTR\\s\\[ecx\\],\\seax")) {
					f.code += "st a, [c]\n";
				} else if (currentLine.matches("\\s*sub\\secx,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*sub\\secx,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push a\n";
					f.code += "ld a, c\n";
					f.code += "sub a, [b " + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "ld c, a\n";
					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*sub\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*sub\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
//					f.code += "push a\n";
					f.code += "sub a, [b " + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
//					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*movsx\\seax,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+eax\\]")) {
					Pattern p = Pattern.compile("\\s*movsx\\seax,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+eax\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "add a, b\n";
					f.code += "ld c, a\n";
					f.code += "ld a, [c" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
				} else if (currentLine.matches("\\s*movsx\\secx,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+ecx\\]")) {
					Pattern p = Pattern.compile("\\s*movsx\\secx,\\sBYTE\\sPTR\\s(_.+\\$)\\[ebp\\+ecx\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "push a\n";
					f.code += "ld a, b\n";
					f.code += "add a, c\n";
					f.code += "ld c, a\n";
					f.code += "ld a, [c" + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()) + "]\n";
					f.code += "ld c, a\n";
					f.code += "pop a\n";
				} else if (currentLine.matches("\\s*lea\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]")) {
					Pattern p = Pattern.compile("\\s*lea\\seax,\\sDWORD\\sPTR\\s(_.+\\$)\\[ebp\\]");
					Matcher m = p.matcher(currentLine);
					m.find();
					f.code += "ld a, " + f.localVars.get(m.group(1)).getValue(f.getMaxLocalVarOffset()).substring(1) + "\n" ;
					f.code += "add a, b\n";
				} else if (currentLine.matches("\\s*mov\\sal,\\sBYTE\\sPTR\\s\\[eax\\]")) {
					f.code += "push c\n";
					f.code += "ld c, a\n";
					f.code += "ld a, [c]\n";
					f.code += "pop c\n";
				} else {
					throw new RuntimeException("Unknown instruction: " + currentLine);
				}

				break;
			case STACK_FRAME_END:
				if (currentLine.startsWith("_TEXT	ENDS")) {
					state = FUNCTION;
					currentFunction = "";
					break;
				}
				f = functions.get(currentFunction);
				f.stackFrameCleanup = "ld sp, b\n";
				f.stackFrameCleanup += "pop b\n";
				f.stackFrameCleanup += "ret\n";
				break;
			}
			i++;
		}

	}

	private String extractString(String value) {
		// $SG23 DB 'ovo je globalni tekst', 00H
		int idx1 = value.indexOf("'");
		int idx2 = value.lastIndexOf("'");
		if (idx1 != -1 && idx2 != -1) {
			String ret = value.substring(idx1 + 1, idx2);
			if (ret.length() %2 == 0) {
				return ret;
			} else {
				return ret + "\\0";
			}
		}
		return null;
	}

	private String extractHex(String vValue) {
		// 02H
		return "0x" + vValue.substring(0, vValue.length() - 1).trim();
	}

	private String extractLabel(String vValue) {
		// FLAT:$SG23
		int idx = vValue.indexOf(':');
		if (idx != -1) {
			return vValue.substring(idx + 1).trim();
		}
		return null;
	}

	private String extractValue(String currentLine) {
		// _tekst1 DD FLAT:$SG23
		// _globalna_promenljiva DD 02H
		int idx = currentLine.lastIndexOf(9);
		if (idx != -1) {
			return currentLine.substring(idx + 1).trim();
		}
		idx = currentLine.lastIndexOf(' ');
		if (idx != -1) {
			return currentLine.substring(idx + 1).trim();
		}
		return null;
	}

	private String extractGlobalName(String currentLine) {
		// _tekst1 DD FLAT:$SG23
		// _globalna_promenljiva DD 02H
		int idx = currentLine.indexOf(9);
		if (idx != -1) {
			return currentLine.substring(0, idx).trim();
		}
		idx = currentLine.indexOf(' ');
		if (idx != -1) {
			return currentLine.substring(0, idx).trim();
		}
		return null;
	}

	private String extractName(String currentLine) {
		// PUBLIC _tekst1
		int idx = currentLine.indexOf(9);
		if (idx != -1) {
			return currentLine.substring(idx + 1).trim();
		}
		idx = currentLine.indexOf(' ');
		if (idx != -1) {
			return currentLine.substring(idx + 1).trim();
		}
		return null;
	}

}
