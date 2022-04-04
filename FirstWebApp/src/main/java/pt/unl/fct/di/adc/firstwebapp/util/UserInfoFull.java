package pt.unl.fct.di.adc.firstwebapp.util;

public class UserInfoFull {

    public String username;
    public String email;
    public String name;
    public String profile;
    public String phone;
    public String cellPhone;
    public String address;
    public String addressC;
    public String cp;
    public String nif;

    public UserInfoFull(){
    }

    public UserInfoFull(String username, String email, String name, String profile, String phone, String cellPhone, String address, String addressC, String cp, String nif) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.profile = profile;
        this.phone = phone;
        this.cellPhone = cellPhone;
        this.address = address;
        this.addressC = addressC;
        this.cp = cp;
        this.nif = nif;
    }
}
