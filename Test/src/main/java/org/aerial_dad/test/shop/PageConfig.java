package org.aerial_dad.test.shop;

import org.bukkit.Material;
import sun.jvm.hotspot.debugger.Page;

import java.util.Map;

public class PageConfig {




    private String pageName;

    private int pageSize;

    private int pageIndex;

    private Material pageSwitchItem;


    public PageConfig(String pageName, int pageSize, int pageIndex, Material pageSwitchItem){
        this.pageName = pageName;
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
        this.pageSwitchItem = pageSwitchItem;

    }

    public PageConfig() {

    }

    public String getPageName() {
        return pageName;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public Material getPageSwitchItem() {
        return pageSwitchItem;
    }

}
