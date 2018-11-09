package de.evoila.cf.broker.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 13.03.18.
 */
public class NsLookupResponse {

    private Map<String, List<String>> falseResults;

    public NsLookupResponse() {
        falseResults = new HashMap<>();
        falseResults.put("message", new ArrayList<>());
    }

    public Map<String, List<String>> getFalseResults() {
        return falseResults;
    }

    public void setFalseResults(Map<String, List<String>> falseResults) {
        this.falseResults = falseResults;
    }
}