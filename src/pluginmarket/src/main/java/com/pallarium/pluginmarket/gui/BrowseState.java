package com.pallarium.pluginmarket.gui;

import com.pallarium.pluginmarket.model.Category;
import com.pallarium.pluginmarket.model.PluginInfo;
import com.pallarium.pluginmarket.model.SortMode;

import java.util.ArrayList;
import java.util.List;

/** Per-player remembered browse context so paging/sorting/search survive menu reopen. */
public class BrowseState {
    public Category category = Category.SPIGOT;
    public SortMode sort = SortMode.MOST_DOWNLOADED;
    public String query = "";
    public int page = 0;
    public List<PluginInfo> results = new ArrayList<>();
    public boolean loading = false;
    public String error = null;
}
