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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.hadoop.conf.Configuration;

public class ParquetValidationTask {

    private static final Logger logger = LoggerFactory.getLogger(ParquetValidationTask.class);

    private static final Set<String> mandatoryColumns = new HashSet<>(Arrays.asList(
            "SampleID", "OlinkID", "UniProt", "Normalization", "Assay",
            "Panel", "PlateID", "NPX"
    ));

    public static boolean validate(File parquetFilePath) {
        Configuration conf = new Configuration();
        conf.setBoolean("fs.file.impl.disable.cache",true);
        conf.set("fs.defaultFS", "file:///");
        conf.set("fs.viewfs.impl", "org.apache.hadoop.fs.RawLocalFileSystem"); // Force local FS

        Path path = new Path(parquetFilePath.getPath());

        try {
            HadoopInputFile hadoopInputFile = HadoopInputFile.fromPath(path, conf);

            // Read Parquet file metadata
            try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(hadoopInputFile).build()) {
                GenericRecord record = reader.read();
                if (record == null) {
                    logger.error("Parquet file is empty: {}", parquetFilePath);
                    return false;
                }

                // Get column names from the schema
                Schema schema = record.getSchema();
                Set<String> columnNames = new HashSet<>();
                for (Schema.Field field : schema.getFields()) {
                    columnNames.add(field.name());
                }

                // Validate mandatory columns
                Set<String> missingColumns = new HashSet<>(mandatoryColumns);
                missingColumns.removeAll(columnNames);

                if (missingColumns.isEmpty()) {
                    logger.info("All mandatory columns are present in {}", parquetFilePath);
                    return true;
                } else {
                    logger.error("Missing columns in {}: {}", parquetFilePath, missingColumns);
                    return false;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read Parquet file: {}", parquetFilePath, e);
            return false;
        }
    }
}