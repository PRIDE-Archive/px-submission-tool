package uk.ac.ebi.pride.gui.task;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ParquetValidationTask {

    private static final Logger logger = LoggerFactory.getLogger(ParquetValidationTask.class);

    private static Set<String> mandatoryColumns = new HashSet<>();

    static {
        // Add the names of mandatory columns here
        mandatoryColumns.add("SampleID");
        mandatoryColumns.add("SampleType");
        mandatoryColumns.add("WellID");
        mandatoryColumns.add("PlateID");
        mandatoryColumns.add("DataAnalysisRefID");
        mandatoryColumns.add("OlinkID");
        mandatoryColumns.add("UniProt");
        mandatoryColumns.add("Panel");
        mandatoryColumns.add("Block");
        mandatoryColumns.add("Normalization");
        mandatoryColumns.add("SampleQC");
        mandatoryColumns.add("ExploreVersion");
        mandatoryColumns.add("Count");
        mandatoryColumns.add("ExtNPX");
        mandatoryColumns.add("NPX");
    }

    public static boolean validate(File file) {
        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(HadoopInputFile.fromPath(new Path(file.getPath()), new org.apache.hadoop.conf.Configuration())).build()) {
            GenericRecord record;
            while ((record = reader.read()) != null) {
                if (validateRecord(record)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean validateRecord(GenericRecord record) {
        for (String column : mandatoryColumns) {
            boolean found = false;
            for (String columnName : record.getSchema().getFields().stream()
                    .map(org.apache.avro.Schema.Field::name)
                    .collect(Collectors.toList())) {
                if (column.equals(columnName)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                logger.error("Mandatory Column " + column + " not found");
                return false;
            }
        }


        // Check for the presence of integer columns (Count)
        if (!hasSpecificTypeColumn(record, "Count", Schema.Type.INT)) {
            logger.error("Mandatory Integer Column \"count\" not found");
            return false;
        }

        // Check for the presence of double columns (ExtNPX, NPX)
        if (!hasSpecificTypeColumn(record, "ExtNPX", Schema.Type.DOUBLE) || !hasSpecificTypeColumn(record, "NPX", Schema.Type.DOUBLE)) {
            logger.error("Mandatory Double Column \"ExtNPX\" or \"npx\" not found");
            return false;
        }

        // Check for SampleQC values (NA, PASS, WARN, FAIL)
        String sampleQC = record.get("SampleQC").toString().toUpperCase();
        if (!sampleQC.equals("NA") && !sampleQC.equals("PASS") && !sampleQC.equals("WARN") && !sampleQC.equals("FAIL")) {
            logger.error("Mandatory Double Column \"ExtNPX\" or \"NPX\" not found");
            return false;
        }

        // Check the NPX value based on SampleQC
        if ((sampleQC.equals("NA") || sampleQC.equals("FAIL"))
                && !Double.isNaN((Double) record.get("NPX"))) {
            logger.error("Check the NPX value based on SampleQC");
            return false;
        }
        if ((sampleQC.equals("PASS") || sampleQC.equals("WARN")) && Double.isNaN((Double) record.get("NPX"))) {
            logger.error("Check the NPX value based on SampleQC");
            return false;
        }

        return true;
    }

    private static boolean hasSpecificTypeColumn(GenericRecord record, String columnName, Schema.Type type) {
        Schema.Field field = record.getSchema().getField(columnName);
        if (field!=null && field.schema().getType().equals(type)) {
            return true;
        }
        return false;
    }
}