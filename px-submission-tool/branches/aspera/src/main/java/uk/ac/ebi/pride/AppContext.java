package uk.ac.ebi.pride;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.*;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Application context
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AppContext extends DesktopContext {

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);

    /**
     * Properties to notify property change listener
     */
    public static final String NEW_SUBMISSION_FILE = "newSubmissionFile";
    public static final String ADD_NEW_DATA_FILE = "addNewDataFile";
    public static final String REMOVE_DATA_FILE = "removeDataFile";
    public static final String ADD_NEW_DATA_FILE_MAPPING = "addNewDataFileMapping";
    public static final String REMOVE_DATA_FILE_MAPPING = "removeDataFileMapping";
    public static final String CHANGE_DATA_FILE_TYPE = "changeDataFileType";
    public static final String ADD_NEW_SPECIES = "addNewSpecies";
    public static final String REMOVE_SPECIES = "removeSpecies";
    public static final String ADD_NEW_INSTRUMENT = "addNewInstrument";
    public static final String REMOVE_INSTRUMENT = "removeInstrument";
    public static final String ADD_NEW_MODIFICATION = "addNewMod";
    public static final String REMOVE_MODIFICATION = "removeMod";
    public static final String ADD_NEW_QUANTIFICATION = "addNewQuant";
    public static final String REMOVE_QUANTIFICATION = "removeQuant";
    public static final String SUBMISSION_TYPE_CHANGED = "changeSubmissionType";
    public static final String ADD_NEW_MASS_SPEC_EXPERIMENT_METHOD = "addMassSpecExperimentMethod";
    public static final String REMOVE_MASS_SPEC_EXPERIMENT_METHOD = "removeMassSpecExperimentMethod";
    public static final String ADD_NEW_SAMPLE_METADATA_ENTRY = "addNewSampleMetaDataEntry";
    public static final String REMOVE_SAMPLE_METADATA_ENTRY = "removeSampleMetaDataEntry";

    /**
     * The data model of the whole GUI
     */
    private SubmissionRecord submissionRecord;

    /**
     * Keep a count of the data file entries, this can be used as the file entry id
     */
    private int dataFileEntryCount;

    /**
     * The main help set for PRIDE Inspector
     */
    private HelpSet mainHelpSet;

    /**
     * The main help broker
     */
    private HelpBroker mainHelpBroker;

    /**
     * The path to open file
     */
    private String openFilePath;

    public AppContext() {
        this.submissionRecord = new SubmissionRecord();
        this.submissionRecord.setSubmission(new Submission());
        this.dataFileEntryCount = 0;
    }

    public synchronized SubmissionRecord getSubmissionRecord() {
        return submissionRecord;
    }

    public synchronized void setSubmissionRecord(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;

        Submission submission = this.submissionRecord.getSubmission();
        this.dataFileEntryCount = maxDataFileID(submission.getDataFiles());

        firePropertyChange(NEW_SUBMISSION_FILE, false, true);
    }

    /**
     * Find maximum ID from a list of data files;
     */
    private int maxDataFileID(List<DataFile> dataFiles) {
        int max = 0;

        for (DataFile dataFile : dataFiles) {
            if (dataFile.getFileId() > max) {
                max = dataFile.getFileId();
            }
        }

        return max;
    }

    public synchronized void resetDataFileEntryCount() {
        this.dataFileEntryCount = 0;
    }

    public synchronized String getOpenFilePath() {
        return openFilePath;
    }

    public synchronized void setOpenFilePath(String openFilePath) {
        this.openFilePath = openFilePath;
    }

    /**
     * Get the supporting state of the submission
     */
    public synchronized boolean isSupported() {
        return submissionRecord.getSubmission().getProjectMetaData().isCompleteSubmission();
    }

    /**
     * Get type of the submission
     */
    public synchronized SubmissionType getSubmissionType() {
        return submissionRecord.getSubmission().getProjectMetaData().getSubmissionType();
    }

    /**
     * Set the supporting state of the submission
     *
     * @param type submission type, it could be supported, unsupported and raw only
     */
    public synchronized void setSubmissionsType(SubmissionType type) {
        ProjectMetaData metadata = submissionRecord.getSubmission().getProjectMetaData();
        if (metadata.getSubmissionType() == null || !metadata.getSubmissionType().equals(type)) {
            SubmissionType oldType = metadata.getSubmissionType();
            metadata.setSubmissionType(type);
            firePropertyChange(SUBMISSION_TYPE_CHANGED, oldType, type);
        }
    }

    /**
     * Find a list of data file of a given file type
     *
     * @param fileType mass spec file type
     * @return a list of data files
     */
    public synchronized List<DataFile> getSubmissionFilesByType(ProjectFileType fileType) {
        Submission submission = submissionRecord.getSubmission();
        return submission.getDataFileByType(fileType);
    }

    /**
     * Add a new data file for submission
     *
     * @param dataFile new data file
     */
    public synchronized void addDataFile(DataFile dataFile) {
        Submission submission = submissionRecord.getSubmission();
        if (!submission.containsDataFile(dataFile)) {
            if (dataFile.getFileId() < 0) {
                // assign a new id
                while (hasSameDataFileId(dataFileEntryCount)) {
                    dataFileEntryCount++;
                }
                dataFile.setFileId(dataFileEntryCount);
            }
            submission.addDataFile(dataFile);
            dataFileEntryCount++;

            firePropertyChange(ADD_NEW_DATA_FILE, null, dataFile);
        }
    }


    /**
     * Remove a data file from submission
     *
     * @param dataFile data file to remove
     */
    public synchronized void removeDatafile(DataFile dataFile) {
        Submission submission = submissionRecord.getSubmission();

        if (submission.containsDataFile(dataFile)) {
            // remove data file
            submission.removeDataFile(dataFile);
            // remove related file mappings
            for (DataFile file : submission.getDataFiles()) {
                removeFileMapping(file, dataFile);
            }
            firePropertyChange(REMOVE_DATA_FILE, dataFile, null);
        }
    }

    /**
     * Add a new file mapping to a given data file
     *
     * @param dataFile given data file
     * @param mapping  new file mapping
     */
    public synchronized void addFileMapping(DataFile dataFile, DataFile mapping) {
        if (dataFile != null) {
            // add mapping
            if (!dataFile.containsFileMapping(mapping)) {
                dataFile.addFileMapping(mapping);
                firePropertyChange(ADD_NEW_DATA_FILE_MAPPING, null, dataFile);
            }
        }
    }

    /**
     * Remove a file mapping from a given data file
     *
     * @param dataFile given data file
     * @param mapping  file mapping to remove
     */
    public synchronized void removeFileMapping(DataFile dataFile, DataFile mapping) {
        if (dataFile != null && mapping != null) {
            // add mapping
            if (dataFile.containsFileMapping(mapping)) {
                dataFile.removeFileMapping(mapping);
                firePropertyChange(REMOVE_DATA_FILE_MAPPING, null, dataFile);
            }
        }
    }

    /**
     * Remove all file mappings from a given data file
     *
     * @param dataFile data file
     */
    public synchronized void removeAllFileMappings(DataFile dataFile) {
        if (dataFile != null) {
            dataFile.removeAllFileMappings();
            firePropertyChange(REMOVE_DATA_FILE_MAPPING, null, dataFile);
        }
    }

    /**
     * Add a set of metadata entries
     */
    public synchronized void setSampleMetaDataEntries(DataFile dataFile, SampleMetaData.Type type, Set<CvParam> params) {
        if (dataFile != null) {
            SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
            if (sampleMetaData == null) {
                sampleMetaData = new SampleMetaData();
                dataFile.setSampleMetaData(sampleMetaData);
            }
            sampleMetaData.setMetaData(type, params);
            firePropertyChange(ADD_NEW_SAMPLE_METADATA_ENTRY, null, dataFile);
        }
    }


    /**
     * Add an new sample metadata entry
     */
    public synchronized void addSampleMetaDataEntry(DataFile dataFile, SampleMetaData.Type type, CvParam param) {
        if (dataFile != null) {
            SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
            if (sampleMetaData == null) {
                sampleMetaData = new SampleMetaData();
                dataFile.setSampleMetaData(sampleMetaData);
            }
            sampleMetaData.addMetaData(type, param);

            firePropertyChange(ADD_NEW_SAMPLE_METADATA_ENTRY, null, dataFile);
        }
    }

    /**
     * Remove a sample metadata entry
     */
    public synchronized void removeSampleMetadataEntry(DataFile dataFile, SampleMetaData.Type type, CvParam param) {
        if (dataFile != null) {
            SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
            if (sampleMetaData != null) {
                sampleMetaData.removeMetaData(type, param);
                firePropertyChange(REMOVE_SAMPLE_METADATA_ENTRY, null, dataFile);
            }
        }
    }

    /**
     * Set mass spec file type for a given data file
     *
     * @param dataFile data file
     * @param type     mass spec file type
     */
    public synchronized void setFileType(DataFile dataFile, ProjectFileType type) {
        if (dataFile != null && type != null && !dataFile.getFileType().equals(type)) {
            dataFile.setFileType(type);
            firePropertyChange(CHANGE_DATA_FILE_TYPE, null, dataFile);
        }
    }

    public synchronized int getNumberOfMassSpecExperimentMethod() {
        return submissionRecord.getSubmission().getProjectMetaData().getNumberOfMassSpecExperimentMethods();
    }

    public synchronized boolean hasMassSpecExperimentMethod(CvParam expMethod) {
        return submissionRecord.getSubmission().getProjectMetaData().hasMassSpecExperimentMethod(expMethod);
    }

    public synchronized void addMassSpecExperimentMethod(CvParam expMethod) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();

        if (!metaData.hasMassSpecExperimentMethod(expMethod)) {
            metaData.addMassSpecExperimentMethods(expMethod);
            firePropertyChange(ADD_NEW_MASS_SPEC_EXPERIMENT_METHOD, null, expMethod);
        }
    }

    public synchronized void removeMassSpecExperimentMethod(CvParam expMethod) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (metaData.hasMassSpecExperimentMethod(expMethod)) {
            metaData.removeMassSpecExperimentMethods(expMethod);
            firePropertyChange(REMOVE_MASS_SPEC_EXPERIMENT_METHOD, null, expMethod);
        }
    }

    /**
     * get the number of species
     */
    public synchronized int getNumberOfSpecies() {
        return submissionRecord.getSubmission().getProjectMetaData().getNumberOfSpecies();
    }

    /**
     * Check whether a species already exists in meta data
     */
    public synchronized boolean hasSpecies(CvParam species) {
        return submissionRecord.getSubmission().getProjectMetaData().hasSpecies(species);
    }

    /**
     * Add a new species
     *
     * @param species new species
     */
    public synchronized void addSpecies(CvParam species) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();

        if (!metaData.hasSpecies(species)) {
            metaData.addSpecies(species);
            firePropertyChange(ADD_NEW_SPECIES, null, species);
        }
    }

    /**
     * Remove a species
     *
     * @param species species to be removed
     */
    public synchronized void removeSpecies(CvParam species) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (metaData.hasSpecies(species)) {
            metaData.removeSpecies(species);
            firePropertyChange(REMOVE_SPECIES, null, species);
        }
    }

    /**
     * Get the number of instruments
     */
    public synchronized int getNumberOfInstruments() {
        return submissionRecord.getSubmission().getProjectMetaData().getNumberOfInstruments();
    }

    /**
     * check whether a instrument already exists in meta data
     */
    public synchronized boolean hasInstrument(CvParam instrument) {
        return submissionRecord.getSubmission().getProjectMetaData().hasInstrument(instrument);
    }

    /**
     * Add a new instrument
     *
     * @param instrument new instrument
     */
    public synchronized void addInstrument(CvParam instrument) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (!metaData.hasInstrument(instrument)) {
            metaData.addInstruments(instrument);
            firePropertyChange(ADD_NEW_INSTRUMENT, null, instrument);
        }
    }

    /**
     * Remove a instrument
     *
     * @param instrument instrument to be removed
     */
    public synchronized void removeInstrument(CvParam instrument) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (metaData.hasInstrument(instrument)) {
            metaData.removeInstruments(instrument);
            firePropertyChange(REMOVE_INSTRUMENT, null, instrument);
        }
    }

    /**
     * Get the number of modifications
     */
    public synchronized int getNumberOfModifications() {
        return submissionRecord.getSubmission().getProjectMetaData().getNumberOfModifications();
    }

    /**
     * Check whether a modification already exists in modification
     */
    public synchronized boolean hasModification(CvParam modification) {
        return submissionRecord.getSubmission().getProjectMetaData().hasModification(modification);
    }

    /**
     * Add a new modification
     *
     * @param modification new modification
     */
    public synchronized void addModification(CvParam modification) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (!metaData.hasModification(modification)) {
            metaData.addModifications(modification);
            firePropertyChange(ADD_NEW_MODIFICATION, null, modification);
        }
    }

    /**
     * Remove a modification
     *
     * @param modification modification to be removed
     */
    public synchronized void removeModification(CvParam modification) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (metaData.hasModification(modification)) {
            metaData.removeModifications(modification);
            firePropertyChange(REMOVE_MODIFICATION, null, modification);
        }
    }

    /**
     * Add a new quantification method
     */
    public synchronized void addQuantification(CvParam quant) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (!metaData.hasQuantification(quant)) {
            metaData.addQuantifications(quant);
            firePropertyChange(ADD_NEW_QUANTIFICATION, null, quant);
        }
    }

    /**
     * Check whether a quantification method already exists
     */
    public synchronized boolean hasQuantification(CvParam quant) {
        return submissionRecord.getSubmission().getProjectMetaData().hasQuantification(quant);
    }

    /**
     * Get the number of quantifications
     */
    public synchronized int getNumberOfQuantifications() {
        return submissionRecord.getSubmission().getProjectMetaData().getNumberOfQuantifications();
    }

    /**
     * Remove a quantification
     */
    public synchronized void removeQuantification(CvParam quant) {
        ProjectMetaData metaData = submissionRecord.getSubmission().getProjectMetaData();
        if (metaData.hasQuantification(quant)) {
            metaData.removeQuantifications(quant);
            firePropertyChange(REMOVE_QUANTIFICATION, null, quant);
        }
    }

    /**
     * Check whether the submission has a data file with the given id
     *
     * @param id given data file id
     * @return boolean true means id already in use
     */
    public synchronized boolean hasSameDataFileId(int id) {
        if (id >= 0) {
            List<DataFile> dataFiles = submissionRecord.getSubmission().getDataFiles();
            for (DataFile file : dataFiles) {
                if (file.getFileId() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    public HelpSet getMainHelpSet() {
        if (mainHelpSet == null) {
            createHelp();
        }
        return mainHelpSet;
    }

    public HelpBroker getMainHelpBroker() {
        if (mainHelpBroker == null) {
            createHelp();
        }
        return mainHelpBroker;
    }

    private void createHelp() {
        try {
            ClassLoader cl = AppContext.class.getClassLoader();
            URL url = HelpSet.findHelpSet(cl, this.getProperty("help.main.set"));
            mainHelpSet = new HelpSet(cl, url);
            mainHelpBroker = mainHelpSet.createHelpBroker();
        } catch (HelpSetException e) {
            logger.error("Failed to initialize help documents", e);
        }
    }
}
