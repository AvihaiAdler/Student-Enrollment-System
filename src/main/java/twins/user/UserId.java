package twins.user;

public class UserId {
	private String space;
	private String email;
	
	public UserId() {
		this.space = "2021b.twins";
		this.email = "user@demo.com";
	}
	public UserId(String space, String email) {
		this.space = space;
		this.email = email;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getSpace() {
		return space;
	}
	public void setSpace(String space) {
		this.space = space;
	}
	
	
}
