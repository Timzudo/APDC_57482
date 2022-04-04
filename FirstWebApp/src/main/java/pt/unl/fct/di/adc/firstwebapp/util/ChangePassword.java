package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangePassword {

    public String tokenId;
    public String oldPassword;
    public String newPassword;
    public String confirmation;

    public ChangePassword(){

    }

    public ChangePassword(String tokenId, String oldPassword, String newPassword, String confirmation){
        this.tokenId = tokenId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }

    public boolean validPassword(){
        return newPassword.equals(confirmation);
    }
}
