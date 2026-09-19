package com.pallarium.questboard.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerData {
    public String day = "";
    public List<String> questIds = new ArrayList<>();
    public Map<String, Integer> progress = new HashMap<>();
    public Set<String> completed = new HashSet<>();
    public boolean dailyClaimed = false;
    public int rerollsUsed = 0;
    public int streak = 0;
    public String lastFullDay = "";
    public int totalCompleted = 0;
}
