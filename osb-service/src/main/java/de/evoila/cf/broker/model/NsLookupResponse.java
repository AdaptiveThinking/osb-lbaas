package de.evoila.cf.broker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rene Schollmeyer.
 */
public class NsLookupResponse {

    private List<String> falseResults = new ArrayList<>();

    public List<String> getFalseResults() {
        return falseResults;
    }

    public void setFalseResults(List<String> falseResults) {
        this.falseResults = falseResults;
    }

    public void addFalseResult(String result) {
        this.falseResults.add(result);
    }
}
