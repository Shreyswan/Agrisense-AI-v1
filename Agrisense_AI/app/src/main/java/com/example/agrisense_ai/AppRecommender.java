package com.example.agrisense_ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppRecommender {
    private static final Map<String, String[]> APP_CATEGORIES = new HashMap<>();

    static {
        APP_CATEGORIES.put("irrigation", new String[]{
                "com.example.irrigationguide",
                "org.farming.watermanagement"
        });
        APP_CATEGORIES.put("crops", new String[]{
                "com.agriculture.croprotation",
                "org.farming.cropcalendar"
        });
        APP_CATEGORIES.put("fertilizer", new String[]{
                "com.agri.fertilizercalculator",
                "org.soil.healthmonitor"
        });
        APP_CATEGORIES.put("harvest", new String[]{
                "com.farming.harvestplanner",
                "org.agriculture.yieldcalculator"
        });
    }

    public List<String> recommendApps(List<String> keywords) {
        List<String> recommended = new ArrayList<>();

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            if (APP_CATEGORIES.containsKey(lowerKeyword)) {
                recommended.addAll(Arrays.asList(APP_CATEGORIES.get(lowerKeyword)));
            }
        }

        // Remove duplicates
        List<String> uniqueRecommendations = new ArrayList<>();
        for (String app : recommended) {
            if (!uniqueRecommendations.contains(app)) {
                uniqueRecommendations.add(app);
            }
        }

        return uniqueRecommendations;
    }
}