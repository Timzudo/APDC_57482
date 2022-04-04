package pt.unl.fct.di.adc.firstwebapp.util;

public class DeleteData {

    public String usernameDelete;
    public String tokenId;

    public DeleteData(){

    }

    public DeleteData(String usernameDelete, String tokenId){
        this.usernameDelete = usernameDelete;
        this.tokenId = tokenId;
    }

    public boolean isValid(){
        return !(usernameDelete.equals(""));
    }
}
