package pt.unl.fct.di.adc.firstwebapp.util;

public class ModifyData {


	public String username;
	public String tokenId;
	public String email;
	public String name;
	public String profile;
	public String phone;
	public String cellPhone;
	public String address;
	public String addressC;
	public String cp;
	public String nif;
	public String state;
	public String role;

	public ModifyData() {

	}

	public ModifyData(String username ,String tokenId ,String email, String name, String profile, String phone, String cellPhone, String address, String addressC, String cp, String nif, String state, String role) {
		this.username = username;
		this.tokenId = tokenId;
		this.email = email;
		this.name = name;
		this.profile = profile;
		this.phone = phone;
		this.cellPhone = cellPhone;
		this.address = address;
		this.addressC = addressC;
		this.cp = cp;
		this.nif = nif;
		this.state = state;
		this.role = role;
	}

	
}
