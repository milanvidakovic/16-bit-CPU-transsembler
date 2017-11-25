package transsembler;

public class LocalVar extends CExpression {
	String lName;

	public LocalVar(String currentLine) {
		super();
		// _x$ = -8
		this.lName = extractName(currentLine);
		this.value = extractValue(currentLine);
	}

	private String extractValue(String currentLine) {
		// _x$ = -8
		int idx = currentLine.indexOf('=');
		if (idx != -1) {
			return currentLine.substring(idx + 1).trim();
		}
		throw new RuntimeException("BEGINNING_OF_FUNCTION::LOCAL_VAR: Invalid format of local variable: " + currentLine);
	}

	private String extractName(String currentLine) {
		// _x$ = -8
		int idx = currentLine.indexOf('=');
		if (idx != -1) {
			return currentLine.substring(0, idx).trim();
		}
		throw new RuntimeException("BEGINNING_OF_FUNCTION::LOCAL_VAR: Invalid format of local variable: " + currentLine);
	}

	@Override
	public String toString() {
		return "LocalVar [lName=" + lName + ", value=" + value + "]";
	}

	public String getValue(int max) {
		if (!value.startsWith("-")) {
			// Function argument (parameter)
			return "-" + ((Integer.parseInt(value) / 4) + 1);
		} else {
			// Local variable
			return "+" + (max - (-Integer.parseInt(value) ) + 1);
		}
	}

	public String getValue(String _num, int max) {
		int num = Integer.parseInt(_num);
		int val;
		if (!value.startsWith("-")) {
			// Function argument (parameter)
			val = - ((Integer.parseInt(value) / 4) + 1);
		} else {
			// Local variable
			val = max - (-Integer.parseInt(value) ) + 1;
		}
		int sum = num + val;
		if (sum > 0)
			return "+" + sum;
		else
			return "" + sum;
	}

}
