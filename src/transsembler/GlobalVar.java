package transsembler;

public class GlobalVar extends CExpression {
	String gName;

	public GlobalVar(String gName) {
		super();
		this.gName = gName;
	}

	@Override
	public String toString() {
		return "GlobalVar [gName=" + gName + ", value=" + value + "]";
	}
}
