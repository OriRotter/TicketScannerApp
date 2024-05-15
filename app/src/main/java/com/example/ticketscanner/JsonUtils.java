package com.example.ticketscanner;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    public static List<List<String>> jsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<List<String>> resultList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray innerArray = jsonArray.getJSONArray(i);
            List<String> innerList = new ArrayList<>();
            for (int j = 0; j < innerArray.length(); j++) {
                innerList.add(innerArray.getString(j));
            }
            resultList.add(innerList);
        }

        return resultList;
    }
}
