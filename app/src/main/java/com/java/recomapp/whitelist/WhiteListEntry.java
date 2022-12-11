package com.java.recomapp.whitelist;

public class WhiteListEntry {
    private String package_name;
    private String app_name;
    private Boolean is_in_whitelist;
    public WhiteListEntry(String _package_name, Boolean _is_in_whitelist) {
        this.package_name = _package_name;
        this.is_in_whitelist = _is_in_whitelist;
    }

    public String getPackage_name() {
        return package_name;
    }

    public Boolean getIs_in_whitelist() {
        return is_in_whitelist;
    }
}
