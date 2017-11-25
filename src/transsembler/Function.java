package transsembler;

import java.util.HashMap;
import java.util.Map;

public class Function extends CExpression {
	String fName;
	Map<String, LocalVar> localVars = new HashMap<String, LocalVar>();
	Map<String, String> localLabels = new HashMap<String, String>();
	
	String stackFrameSrc;
	String code  = "";
	String stackFrameCleanup;

	public Function(String fName) {
		super();
		this.fName = fName;
	}
	
	public int localVarsCount() {
		int c = 0;
		for (LocalVar lv : localVars.values()) {
			if (lv.value.startsWith("-")) {
				c++;
			}
		}
		return c;
	}

	public int paramsCount() {
		int c = 0;
		for (LocalVar lv : localVars.values()) {
			if (!lv.value.startsWith("-")) {
				c++;
			}
		}
		return c;
	}

	public int getMaxLocalVarOffset() {
		int c = 0;
		for (LocalVar lv : localVars.values()) {
			if (lv.value.startsWith("-")) {
				int num = - Integer.parseInt(lv.value);
				if (num > c) {
					c = num;
				}
			}
		}
		return c;
	}

	@Override
	public String toString() {
		return "Function [fName=" + fName + ", localVars=" + localVars + ", localLabels=" + localLabels
				+ ", stackFrameSrc=" + stackFrameSrc + ", code=" + code + ", stackFrameCleanup=" + stackFrameCleanup
				+ ", value=" + value + "]";
	}

}
