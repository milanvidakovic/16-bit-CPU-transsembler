package transsembler;

public class GlobalVar extends CExpression {
	
	public static final int S_TYPE = 1;
	public static final int N_TYPE = 2;
	
	String gName;
	int type;

	public GlobalVar(String gName) {
		super();
		this.gName = gName;
	}

	@Override
	public String toString() {
		return "GlobalVar [gName=" + gName + ", value=" + value + "]";
	}
}
