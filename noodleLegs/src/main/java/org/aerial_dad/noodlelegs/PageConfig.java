package org.aerial_dad.noodlelegs;

import org.bukkit.Material;

public class PageConfig {




    private String pageName;

    private int pageSize;



    public PageConfig(String pageName, int pageSize){
        this.pageName = pageName;
        this.pageSize = pageSize;

    }

    public PageConfig() {

    }

    public String getPageName() {
        return pageName;
    }

    public int getPageSize() {
        return pageSize;
    }


}
