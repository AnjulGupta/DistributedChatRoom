public class TotalMessage implements java.io.Serializable {
	private String message;
	private int source;
	private MetaData data;
	private int id;
	
	public TotalMessage(String msg, int source, MetaData data, int id) {
		message = msg;
		this.source = source;
		this.data = data;
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int num) {
		this.id = num;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public int getSource() {
		return this.source;
	}
	
	public MetaData getMetaData() {
		return this.data;
	}
	
	public void setMetaData(MetaData d) {
		this.data = d;
	}
}