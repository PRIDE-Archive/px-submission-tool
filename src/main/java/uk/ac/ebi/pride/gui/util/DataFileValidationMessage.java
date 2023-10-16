package uk.ac.ebi.pride.gui.util;

import uk.ac.ebi.pride.data.model.DataFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class DataFileValidationMessage extends ValidationMessage {

    private final Map<DataFile, List<String>> dataFileValidationResults;

    public DataFileValidationMessage(ValidationState state) {
        this(state, null);
    }

    public DataFileValidationMessage(ValidationState state, String message) {
        super(state, message);

        this.dataFileValidationResults = new HashMap<>();
    }

    public Map<DataFile, List<String>> getDataFileValidationResults() {
        return dataFileValidationResults;
    }

    public void addDataFileValidationResults(Map<DataFile, List<String>> results) {
        this.dataFileValidationResults.putAll(results);
    }

    public void addDataFileValidationResult(DataFile dataFile, String message) {
        List<String> validationResults = dataFileValidationResults.computeIfAbsent(dataFile, k -> new ArrayList<>());

        validationResults.add(message);
    }

}
