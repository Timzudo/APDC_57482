package pt.unl.fct.di.adc.firstwebapp.util;

public class Roles {

    public static int getValue(String role){
        switch (role){
            case "USER":
                return 1;
            case "GBO":
                return 2;
            case "GS":
                return 3;
            case "SU":
                return 4;
        }
        return -1;
    }

}

