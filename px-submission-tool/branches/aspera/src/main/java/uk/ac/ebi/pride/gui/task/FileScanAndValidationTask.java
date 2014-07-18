package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.FileUtil;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.gui.util.*;
import uk.ac.ebi.pride.jaxb.model.CvParam;
import uk.ac.ebi.pride.jaxb.model.SampleDescription;
import uk.ac.ebi.pride.jaxb.xml.unmarshaller.PrideXmlUnmarshaller;
import uk.ac.ebi.pride.jaxb.xml.unmarshaller.PrideXmlUnmarshallerFactory;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validate all the files selected in the file selection step
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileScanAndValidationTask extends TaskAdapter<DataFileValidationMessage, Void> {
    private static final Logger logger = LoggerFactory.getLogger(FileScanAndValidationTask.class);

    private static final String MZIDENTML_ACCEPTED_VERSION = "1.1.0";
    public static final Pattern MZIDENTML_PEAK_LIST_FILE_PATTERN = Pattern.compile("^[^<]*<SpectraData[^>]*location=\"([^\"]+)\"[^>]*>.*$");
    public static final String MZIDENTML_PEAK_LIST_FILE_END = "</SpectraData>";
    public static final String PRIDEXML_SAMPLE_DESCRIPTION_BEGIN = "<sampleDescription";
    public static final String PRIDEXML_SAMPLE_DESCRIPTION_END = "/sampleDescription>";
    public static final String PRIDEXML_GEL_FREE_IDENTIFICATION = "GelFreeIdentification";
    public static final String PRIDEXML_TWO_DIMENSIONAL_IDENTIFICATION = "TwoDimensionalIdentification";
    public static final Pattern PRIDEXML_SAMPLE_DESCRIPTION_PATTERN = Pattern.compile(".*<sampleDescription[^>]*/>.*");

    private Submission submission;

    public FileScanAndValidationTask(Submission submission) {
        this.submission = submission;
    }

    @Override
    protected DataFileValidationMessage doInBackground() throws Exception {
        // generate validation results
        QuickValidationResult quickValidationResult = runQuickValidation(submission.getDataFiles());
        setProgress(10);

        // cannot have invalidate files
        if (quickValidationResult.getNumOfInvalidFiles() > 0) {
            return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getInvalidFileWarning(quickValidationResult.numOfInvalidFiles));
        }
        setProgress(20);

        SubmissionType submissionType = submission.getProjectMetaData().getSubmissionType();

        List<DataFile> prideXmlDataFiles = submission.getDataFilesByFormat(MassSpecFileFormat.PRIDE);
        boolean noPrideXml = prideXmlDataFiles.isEmpty();

        List<DataFile> mzIdentMLDataFiles = submission.getDataFilesByFormat(MassSpecFileFormat.MZIDENTML);
        boolean noMzIdentML = mzIdentMLDataFiles.isEmpty();

        boolean noRawFile = submission.getDataFileByType(ProjectFileType.RAW).isEmpty();
        boolean noSearchFile = submission.getDataFileByType(ProjectFileType.SEARCH).isEmpty();

        List<DataFile> resultDataFiles = submission.getDataFileByType(ProjectFileType.RESULT);
        boolean noResultFile = resultDataFiles.isEmpty();

        if (submissionType.equals(SubmissionType.COMPLETE)) {

            // should have both result files and raw files
            if (noResultFile || noRawFile) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getMissedFileWarning(submissionType, !noResultFile, !noSearchFile, !noRawFile));
            }
            setProgress(30);

            // cannot have unsupported result files
            if (quickValidationResult.hasUnsupportedResultFile() && !quickValidationResult.isUrlBasedResultFilePresent()) {
                // construct error message
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getUnsupportedResultFileWarning());
            }
            setProgress(40);

            if (noPrideXml && noMzIdentML && !quickValidationResult.isUrlBasedResultFilePresent()) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getInvalidResultFileWarning());
            }

            // cannot have both PRIDE xml and mzIdentML at the same time
            if (!noPrideXml && !noMzIdentML) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getMultipleResultFileFormatWarning());
            }

            if (!noPrideXml) {
                // cannot have PRIDE XML result files with no identifications
                List<DataFile> invalidPrideXmlFiles = runPrideXmlProteinIdentValidation(prideXmlDataFiles);
                if (invalidPrideXmlFiles.size() > 0) {
                    return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getInvalidPRIDEXMLFileWarning(invalidPrideXmlFiles));
                }

                // scan for sample details
                scanPrideXmlSampleDetails(prideXmlDataFiles);
            }
            setProgress(50);

            if (!noMzIdentML) {
                // cannot have mzIdentML other than version 1.1.0
                List<DataFile> invalidMzIdentMLFiles = runMzIdentMLVersionValidation(mzIdentMLDataFiles);
                if (invalidMzIdentMLFiles.size() > 0) {
                    return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getInvalidMzIdentMLVersionWarning(invalidMzIdentMLFiles));
                }
                setProgress(60);

                // cannot have mzIdentML without peak list files
                Map<DataFile, List<String>> invalidMzIdentMLPeakListFiles = runMzIdentMLPeakListFileScanAndValidation(submission.getDataFiles());
                if (invalidMzIdentMLPeakListFiles.size() > 0) {
                    DataFileValidationMessage validationMessage = new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getMzIdentMLPeakListFilWarning());
                    validationMessage.addDataFileValidationResults(invalidMzIdentMLPeakListFiles);
                    return validationMessage;
                }
                setProgress(80);
            }
        } else if (submissionType.equals(SubmissionType.PARTIAL)) {
            // should have both search engine output and raw files
            if (noSearchFile || noRawFile) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getMissedFileWarning(submissionType, !noResultFile, !noSearchFile, !noRawFile));
            }
            setProgress(40);

            // cannot have supported search engine output, these should be converted to PRIDE XMl
            // using PRIDE Converter
            if (quickValidationResult.hasSupportedSearchFile()) {
                boolean result = WarningMessageGenerator.showSupportedSearchFileWarning();
                scanForFileMappings();
                return new DataFileValidationMessage(result ? ValidationState.SUCCESS : ValidationState.ERROR);
            }
            setProgress(60);

            if (quickValidationResult.hasUnsupportedSearchFile()) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getUnsupportedSearchFileWarning());
            }

            // cannot have result files
            if (!noResultFile) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getResultFileWarning());
            }
            setProgress(80);

            // ms image data
            if (quickValidationResult.hasImagingRawFile() && !quickValidationResult.hasImagingDataFile()) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getMissingImageDataFileWarning());
            }
            setProgress(85);

        } else {
            // must have raw files
            if (noRawFile) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getMissedFileWarning(submissionType, !noResultFile, !noSearchFile, !noRawFile));
            }
            setProgress(50);

            // cannot have search engine output or result files
            if (!noSearchFile || !noResultFile) {
                return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getRawOnlyWarning());
            }
            setProgress(80);
        }


        // cannot have unsupported raw files, such as: pkl
        if (quickValidationResult.hasUnsupportedRawFile()) {
            return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getUnsupportedRawFileWarning());
        }

        // cannot have submission px file
        if (quickValidationResult.hasSubmissionPxFile()) {
            return new DataFileValidationMessage(ValidationState.ERROR, WarningMessageGenerator.getSubmissionPxWarning());
        }

        // pre-scan for file relation
        scanForFileMappings();

        setProgress(100);

        return new DataFileValidationMessage(ValidationState.SUCCESS);
    }


    private void scanForFileMappings() {
        List<DataFile> dataFiles = submission.getDataFiles();
        List<DataFile> resultOrSearchFiles = getResultOrSearchFile(dataFiles, submission.getProjectMetaData().getSubmissionType());
        dataFiles.removeAll(resultOrSearchFiles);

        Set<DataFile> rawFiles = new HashSet<DataFile>();
        Set<DataFile> peakFiles = new HashSet<DataFile>();

        // single result file, map the rest to the result file
        if (resultOrSearchFiles.size() == 1) {
            DataFile resultOrSearchFile = resultOrSearchFiles.get(0);
            for (DataFile dataFile : dataFiles) {
                if (!resultOrSearchFile.containsFileMapping(dataFile)) {
                    resultOrSearchFile.addFileMapping(dataFile);
                }
            }
        }

        // scan for every file
        for (DataFile resultOrSearchFile : resultOrSearchFiles) {
            for (DataFile dataFile : dataFiles) {
                if (isDataFileRelated(resultOrSearchFile, dataFile) && !resultOrSearchFile.containsFileMapping(dataFile)) {
                    resultOrSearchFile.addFileMapping(dataFile);
                }

                if (dataFile.getFileType().equals(ProjectFileType.RAW)) {
                    rawFiles.add(dataFile);
                } else if (dataFile.getFileType().equals(ProjectFileType.PEAK)) {
                    peakFiles.add(dataFile);
                }
            }
        }

        // single raw file
        if (rawFiles.size() == 1) {
            addFileMapping(resultOrSearchFiles, rawFiles.iterator().next());
        }

        // single peak file
        if (peakFiles.size() == 1) {
            addFileMapping(resultOrSearchFiles, peakFiles.iterator().next());
        }

    }

    private void addFileMapping(List<DataFile> resultOrSearchFiles, DataFile dataFile) {
        for (DataFile resultOrSearchFile : resultOrSearchFiles) {
            if (!resultOrSearchFile.containsFileMapping(dataFile)) {
                resultOrSearchFile.addFileMapping(dataFile);
            }
        }
    }

    private List<DataFile> getResultOrSearchFile(List<DataFile> dataFiles, SubmissionType type) {
        List<DataFile> holder = new ArrayList<DataFile>();

        // decide project file type to look for
        ProjectFileType typeToLookFor;
        if (type.equals(SubmissionType.PARTIAL)) {
            typeToLookFor = ProjectFileType.SEARCH;
        } else {
            typeToLookFor = ProjectFileType.RESULT;
        }

        // find data file
        for (DataFile dataFile : dataFiles) {
            if (dataFile.getFileType().equals(typeToLookFor)) {
                holder.add(dataFile);
            }
        }

        return holder;
    }

    private boolean isDataFileRelated(DataFile fileOne, DataFile fileTwo) {
        String fileOneName = removeUnwantedCharacters(fileOne.getFileName());
        String fileTwoName = removeUnwantedCharacters(fileTwo.getFileName());

        return fileOneName.length() > 1 && fileTwoName.length() > 1 && (fileOneName.startsWith(fileTwoName) || fileTwoName.startsWith(fileOneName));
    }

    private String removeUnwantedCharacters(String fileName) {
        String replacedString = fileName;
        int mid = fileName.lastIndexOf(".");
        if (mid > 0) {
            replacedString = fileName.substring(0, mid + 1).toLowerCase();
        }

        return replacedString.replace("-", "").replace("_", "").replace(".", "").replace(" ", "");
    }

    /**
     * Validate a list of data files
     *
     * @param dataFiles a list of data files
     * @return ValidationResult    stores all the validation results
     */
    private QuickValidationResult runQuickValidation(List<DataFile> dataFiles) {
        QuickValidationResult result = new QuickValidationResult();

        for (DataFile dataFile : dataFiles) {
            String fileName = dataFile.getFileName();
            if (SubmissionValidator.validateDataFile(dataFile).hasError()) {
                logger.debug("runQuickValidation(): SubmissionValidator.validateDataFile(" + fileName + ").hasError() = " + SubmissionValidator.validateDataFile(dataFile).hasError());
                result.incrementNumOfInvalidFiles();
            }

            if (Constant.PX_SUBMISSION_SUMMARY_FILE.equals(fileName)) {
                result.setSubmissionPxFile(true);
            }

            MassSpecFileFormat fileFormat = dataFile.getFileFormat();
            ProjectFileType fileType = dataFile.getFileType();
            if (ProjectFileType.RESULT.equals(fileType)) {
                if (dataFile.isUrl()) {
                    result.setUrlBasedResultFilePresent(true);
                }

                if (fileFormat == null || !ProjectFileType.RESULT.equals(fileType)) {
                    result.setUnsupportedResultFile(true);
                } else {
                    result.setSupportedResultFile(true);
                    if (MassSpecFileFormat.PRIDE.equals(fileFormat)) {
                        result.setPrideXml(true);
                    } else if (MassSpecFileFormat.MZIDENTML.equals(fileFormat)) {
                        result.setMzIdentML(true);
                    }
                }
            } else if (ProjectFileType.RAW.equals(fileType)) {
                if (fileFormat == null || ProjectFileType.RAW.equals(fileType)) {
                    result.setSupportedRawFile(true);
                    if (MassSpecFileFormat.IBD.equals(fileFormat)) {
                        result.setImagingRawFile(true);
                    }
                } else {
                    result.setUnsupportedRawFile(true);
                }
            } else if (ProjectFileType.SEARCH.equals(fileType)) {
                if (PrideConverterSupport.isSupported(dataFile)) {
                    result.setSupportedSearchFile(true);
                } else {
                    if (MassSpecFileFormat.PRIDE.equals(fileFormat) || MassSpecFileFormat.MZIDENTML.equals(fileFormat)) {
                        result.setUnsupportedSearchFile(true);
                    }
                }
            } else if (ProjectFileType.IMAGE_DATA.equals(fileType)) {
                result.setImagingDataFile(true);
            }
        }

        return result;
    }

    /**
     * Scan pride xml for sample related metadata, this will avoid asking user to input them again
     */
    private void scanPrideXmlSampleDetails(List<DataFile> dataFiles) throws IOException {
        for (DataFile dataFile : dataFiles) {
            InputStream prideInputStream = null;

            try {
                // get file as input stream
                prideInputStream = FileUtil.getFileInputStream(dataFile.getFile());

                // scan the file
                StringBuilder builder = readSampleDescription(prideInputStream);

                // parse sample description
                String sample = builder.toString().trim();
                if (sample.length() > 0) {
                    PrideXmlUnmarshaller unmarshaller = PrideXmlUnmarshallerFactory.getInstance().initializeUnmarshaller();
                    try {
                        SampleDescription sampleDescription = unmarshaller.unmarshal(sample, SampleDescription.class);
                        updateSampleMetaData(dataFile, sampleDescription);
                    } catch (JAXBException e) {
                        logger.error("Failed to parse sample description for PRIDE XML file: " + dataFile.getFile().getName(), e);
                    }
                }
            } finally {
                if (prideInputStream != null) {
                    prideInputStream.close();
                }
            }
        }
    }

    /**
     * Read sample description xml from pride xml file input stream
     */
    private StringBuilder readSampleDescription(InputStream prideInputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(prideInputStream));
        StringBuilder builder = new StringBuilder();
        boolean withinSampleDescription = false;
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = PRIDEXML_SAMPLE_DESCRIPTION_PATTERN.matcher(line);
            if (line.contains(PRIDEXML_SAMPLE_DESCRIPTION_END) || matcher.matches()) {
                builder.append(line);
                break;
            } else if (line.contains(PRIDEXML_SAMPLE_DESCRIPTION_BEGIN) || withinSampleDescription) {
                builder.append(line);
                withinSampleDescription = true;
            }
        }
        return builder;
    }

    /**
     * Update sample metadata of a given data file using a given sample description
     */
    private void updateSampleMetaData(DataFile dataFile, SampleDescription sampleDescription) {
        SampleMetaData sampleMetaData = dataFile.getSampleMetaData();

        if (sampleMetaData == null) {
            sampleMetaData = new SampleMetaData();
            dataFile.setSampleMetaData(sampleMetaData);
        }

        List<CvParam> cvParams = sampleDescription.getCvParam();
        for (CvParam param : cvParams) {
            String cvLabel = param.getCvLabel();

            SampleMetaData.Type type = getSampleMetaDataType(cvLabel);

            if (type != null) {
                uk.ac.ebi.pride.data.model.CvParam value = new uk.ac.ebi.pride.data.model.CvParam(cvLabel, param.getAccession(), param.getName(), null);
                sampleMetaData.addMetaData(type, value);
            }
        }
    }

    /**
     * Get sample metadata type
     */
    private SampleMetaData.Type getSampleMetaDataType(String cvLabel) {
        SampleMetaData.Type type = null;

        if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.NEWT)) {
            type = SampleMetaData.Type.SPECIES;
        } else if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.BTO)) {
            type = SampleMetaData.Type.TISSUE;
        } else if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.CL)) {
            type = SampleMetaData.Type.CELL_TYPE;
        } else if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.DOID)) {
            type = SampleMetaData.Type.DISEASE;
        }

        return type;
    }

    /**
     * validate whether pride xml contains protein identifications
     *
     * @param prideXmlDatafiles a list of data files
     * @return a list of invalid pride xml
     */
    private List<DataFile> runPrideXmlProteinIdentValidation(List<DataFile> prideXmlDatafiles) throws IOException {
        List<DataFile> invalidPrideXmlFiles = new ArrayList<DataFile>();

        for (DataFile dataFile : prideXmlDatafiles) {
            File file = dataFile.getFile();
            if (!isCompressed(file)) {
                InputStream inputStream = null;
                try {
                    inputStream = FileUtil.getFileInputStream(file);
                    String endOfFile = FileUtil.tail(file, 7000);

                    if (!endOfFile.contains(PRIDEXML_GEL_FREE_IDENTIFICATION) && !endOfFile.contains(PRIDEXML_TWO_DIMENSIONAL_IDENTIFICATION)) {
                        invalidPrideXmlFiles.add(dataFile);
                    }

                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }
        }

        return invalidPrideXmlFiles;
    }

    /**
     * validate whether mzIdentML is version 1.1.0
     *
     * @param dataFiles a list of all data files
     * @return a list of invalid mzIdentML files
     */
    private List<DataFile> runMzIdentMLVersionValidation(List<DataFile> dataFiles) throws IOException {
        List<DataFile> invalidMzIdentMLFiles = new ArrayList<DataFile>();

        for (DataFile dataFile : dataFiles) {
            BufferedReader fileReader = null;
            InputStream inputStream = null;
            try {
                inputStream = FileUtil.getFileInputStream(dataFile.getFile());
                // read the first ten lines
                fileReader = new BufferedReader(new InputStreamReader(inputStream));

                boolean correctVersion = false;
                for (int i = 0; i < 5; i++) {
                    String line = fileReader.readLine();
                    if (line.contains("version=\"" + MZIDENTML_ACCEPTED_VERSION + "\"")) {
                        correctVersion = true;
                        break;
                    }
                }

                if (!correctVersion) {
                    invalidMzIdentMLFiles.add(dataFile);
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (fileReader != null) {
                    fileReader.close();
                }
            }

        }

        return invalidMzIdentMLFiles;
    }

    /**
     * validate whether the peak list files referenced by mzIdentML files are present
     *
     * @param dataFiles a list of all data files
     * @return a map of original mzIdentML data file and missing peak list file name
     */
    private Map<DataFile, List<String>> runMzIdentMLPeakListFileScanAndValidation(List<DataFile> dataFiles) throws IOException {
        Map<DataFile, List<String>> invalidMzIdentMLFiles = new HashMap<DataFile, List<String>>();

        for (DataFile dataFile : dataFiles) {
            if (MassSpecFileFormat.MZIDENTML.equals(dataFile.getFileFormat())) {
                Set<String> peakListFileNames = parsePeakListFileNames(dataFile.getFile());
                for (String peakListFileName : peakListFileNames) {
                    boolean present = false;
                    for (DataFile spectraFile : dataFiles) {
                        String spectraFileName = FileUtil.getDecompressedFileName(spectraFile.getFile());
                        if (peakListFileName.equalsIgnoreCase(spectraFileName)) {
                            present = true;
                            // add file mapping
                            if (!dataFile.getFileMappings().contains(spectraFile)) {
                                spectraFile.setFileType(ProjectFileType.PEAK);
                                dataFile.addFileMapping(spectraFile);
                            }
                            break;
                        }
                    }

                    if (!present) {
                        List<String> nonePresentPreakListFiles = invalidMzIdentMLFiles.get(dataFile);
                        if (nonePresentPreakListFiles == null) {
                            nonePresentPreakListFiles = new ArrayList<String>();
                            invalidMzIdentMLFiles.put(dataFile, nonePresentPreakListFiles);
                        }
                        nonePresentPreakListFiles.add(peakListFileName);
                    }
                }
            }
        }

        return invalidMzIdentMLFiles;
    }

    private Set<String> parsePeakListFileNames(File mzIdentMLFile) throws IOException {
        Set<String> peakListFileNames = new HashSet<String>();

        InputStream mzIdentMLInputStream = null;

        try {
            // get file as input stream
            mzIdentMLInputStream = FileUtil.getFileInputStream(mzIdentMLFile);

            // scan the file
            BufferedReader reader = new BufferedReader(new InputStreamReader(mzIdentMLInputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(MZIDENTML_PEAK_LIST_FILE_END)) {
                    break;
                }


                Matcher matcher = MZIDENTML_PEAK_LIST_FILE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String filePath = matcher.group(1);
                    String fileName = FileUtil.getRealFileName(filePath);
                    peakListFileNames.add(fileName);
                }
            }

        } finally {
            if (mzIdentMLInputStream != null) {
                mzIdentMLInputStream.close();
            }
        }


        return peakListFileNames;
    }

    private boolean isCompressed(File file) {
        return FileUtil.isZipped(file) || FileUtil.isGzipped(file);
    }

    private static class QuickValidationResult {

        boolean supportedResultFile;
        boolean urlBasedResultFilePresent;
        boolean prideXml;
        boolean mzIdentML;
        boolean unsupportedResultFile;
        boolean supportedRawFile;
        boolean unsupportedRawFile;
        boolean supportedSearchFile;
        boolean unsupportedSearchFile;
        int numOfInvalidFiles = 0;
        boolean submissionPxFile;
        boolean imagingRawFile;
        boolean imagingDataFile;

        public boolean hasSupportedResultFile() {
            return supportedResultFile;
        }

        public void setSupportedResultFile(boolean supportedResultFile) {
            this.supportedResultFile = supportedResultFile;
        }

        public boolean isUrlBasedResultFilePresent() {
            return urlBasedResultFilePresent;
        }

        public void setUrlBasedResultFilePresent(boolean urlBasedResultFilePresent) {
            this.urlBasedResultFilePresent = urlBasedResultFilePresent;
        }

        public boolean hasPrideXml() {
            return prideXml;
        }

        public void setPrideXml(boolean prideXml) {
            this.prideXml = prideXml;
        }

        public boolean hasMzIdentML() {
            return mzIdentML;
        }

        public void setMzIdentML(boolean mzIdentML) {
            this.mzIdentML = mzIdentML;
        }

        public boolean hasUnsupportedResultFile() {
            return unsupportedResultFile;
        }

        public void setUnsupportedResultFile(boolean unsupportedResultFile) {
            this.unsupportedResultFile = unsupportedResultFile;
        }

        public boolean hasMixedResultFile() {
            return supportedResultFile && unsupportedResultFile;
        }

        public boolean hasResultFile() {
            return supportedResultFile || unsupportedResultFile;
        }

        public boolean hasSupportedRawFile() {
            return supportedRawFile;
        }

        public void setSupportedRawFile(boolean supportedRawFile) {
            this.supportedRawFile = supportedRawFile;
        }

        public boolean hasUnsupportedRawFile() {
            return unsupportedRawFile;
        }

        public void setUnsupportedRawFile(boolean unsupportedRawFile) {
            this.unsupportedRawFile = unsupportedRawFile;
        }

        private boolean hasSubmissionPxFile() {
            return submissionPxFile;
        }

        private void setSubmissionPxFile(boolean submissionPxFile) {
            this.submissionPxFile = submissionPxFile;
        }

        public boolean hasMixedRawFile() {
            return supportedRawFile && unsupportedRawFile;
        }

        public boolean hasRawFile() {
            return supportedRawFile || unsupportedRawFile;
        }

        public boolean hasSupportedSearchFile() {
            return supportedSearchFile;
        }

        public void setSupportedSearchFile(boolean supportedSearchFile) {
            this.supportedSearchFile = supportedSearchFile;
        }

        public boolean hasUnsupportedSearchFile() {
            return unsupportedSearchFile;
        }

        public void setUnsupportedSearchFile(boolean unsupportedSearchFile) {
            this.unsupportedSearchFile = unsupportedSearchFile;
        }

        public boolean hasMixedSearchFile() {
            return supportedSearchFile && unsupportedSearchFile;
        }

        public boolean hasSearchFile() {
            return supportedSearchFile || unsupportedSearchFile;
        }

        public int getNumOfInvalidFiles() {
            return numOfInvalidFiles;
        }

        public void incrementNumOfInvalidFiles() {
            this.numOfInvalidFiles++;
        }

        public boolean hasImagingRawFile() {
            return imagingRawFile;
        }

        public void setImagingRawFile(boolean imagingRawFile) {
            this.imagingRawFile = imagingRawFile;
        }

        public boolean hasImagingDataFile() {
            return imagingDataFile;
        }

        public void setImagingDataFile(boolean imagingDataFile) {
            this.imagingDataFile = imagingDataFile;
        }
    }
}
