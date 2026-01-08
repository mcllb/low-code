package tcdx.uap.service.entities;

public class DSHasSettings {
    public boolean has_rows = false;
    public boolean has_total = false;
    public boolean has_count = false;
    public DSHasSettings(boolean r, boolean t, boolean c){
        this.has_rows = r;
        this.has_count = c;
        this.has_total = t;
    }
}