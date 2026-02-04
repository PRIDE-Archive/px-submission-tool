# PX Submission Tool - Wizard UX Improvements

## Overview

This document describes the comprehensive UX redesign of the PX Submission Tool wizard, implemented to make the submission process more user-friendly and aligned with PRIDE submission guidelines.

**Date:** February 2026
**Goal:** Redesign the wizard flow so users understand exactly what files they need and cannot easily make mistakes.

---

## Summary of Changes

### New Wizard Flow (9 Steps)

1. **Welcome** - Guidelines overview with file requirements
2. **Login** - PRIDE authentication
3. **Submission Type** - Choose PRIDE or Affinity Proteomics
4. **File Selection** - Add files with drag-drop and auto-classification
5. **File Review** - Multi-tab review of files by category
6. **Sample Metadata** - Species, tissue, instrument (with SDRF parsing)
7. **Project Metadata** - Title, description, keywords (chip-style input)
8. **Summary** - Validation review with recommended file confirmation
9. **Submission** - Upload with checksum calculation

---

## Phase 1: Critical Bug Fixes

### Checksum Calculation Fix
**Files:** `SubmissionModel.java`, `SubmissionStep.java`

**Problem:** Checksums were being calculated but not stored or written to `checksum.txt`.

**Solution:**
- Added `ObservableMap<DataFile, String> checksums` to `SubmissionModel`
- Added methods: `setChecksum()`, `getChecksum()`, `getChecksums()`, `clearChecksums()`
- Added `BooleanProperty checksumsCalculated` to track completion
- Updated `SubmissionStep` to store checksums in model and write `checksum.txt`

### ChipInput Component
**File:** `view/component/ChipInput.java` (NEW)

A tag-style input component for keywords:
- Type a word and press Enter to create a chip/tag
- Click X on a chip to remove it
- Binds to a comma-separated `StringProperty`
- Supports autocomplete suggestions

### Keywords Field Update
**File:** `controller/ProjectMetadataStep.java`

- Replaced `TextField` with `ChipInput` for keywords entry
- Users now add keywords as individual tags instead of comma-separated text

---

## Phase 2: Core Components

### FileTypeDetector Utility
**File:** `util/FileTypeDetector.java` (NEW)

Comprehensive file type detection:

```java
// Extension mapping for mass spec files
.raw, .wiff, .wiff2, .d, .baf, .tdf → RAW
.mzML, .mzXML → RAW
.mzid, .mzIdentML, .mztab → RESULT (standard formats)
.txt, .tsv, .csv, .parquet → SEARCH (analysis outputs)
.mgf, .dta, .pkl → PEAK
.fasta, .fa → detected via isFastaFile()
```

**Analysis Tool Detection:**
- `AnalysisTool` enum: MaxQuant, DIA_NN, FragPipe, Spectronaut, Mascot, ProteomeDiscoverer, Skyline, UNKNOWN
- Each tool has required and optional file patterns
- `ToolDetectionResult` record with confidence scoring (0.0-1.0)

### FileClassificationPanel Component
**File:** `view/component/FileClassificationPanel.java` (NEW)

Visual summary of files by type:
- Color-coded badges for each file type (RAW=blue, SEARCH=green, etc.)
- Shows count and total size per category
- Warning icons for missing mandatory files
- Click to filter table by type
- Tool detection display

---

## Phase 3: Wizard Restructure

### WelcomeStep
**File:** `controller/WelcomeStep.java` (NEW)

Guidelines overview showing:
- File category cards with icons and descriptions:
  - RAW Files (Mandatory)
  - Analysis Files (Mandatory)
  - Standard Formats (Recommended for ProteomeXchange)
  - FASTA Database (Recommended)
  - SDRF (Recommended)
- Checklist of what's needed
- Training mode toggle
- Links to submission guidelines

### FileReviewStep
**File:** `controller/FileReviewStep.java` (NEW)

Multi-tab file review:
- **RAW Files Tab** - Instrument output files
- **Analysis Tab** - Tool outputs with detection panel
- **Standard Tab** - mzIdentML, mzTab files
- **Database Tab** - FASTA and spectral libraries
- **Other Tab** - Unclassified files

Features:
- Editable file type column
- Tool detection info panel
- File validation status

---

## Phase 4: Metadata Improvements

### OlsService
**File:** `service/OlsService.java` (NEW)

REST API client for EBI Ontology Lookup Service:
- Asynchronous search with `CompletableFuture`
- Supported ontologies:
  - `NCBI_TAXON` - Species/organisms
  - `BTO` - Tissues
  - `CL` - Cell types
  - `DOID` - Diseases
  - `MS` - Mass spectrometry terms/instruments
  - `MOD` - Modifications
- Built-in common terms for quick selection
- Result caching

### OlsAutocomplete Component
**File:** `view/component/OlsAutocomplete.java` (NEW)

Inline autocomplete for ontology terms:
- Debounced search (300ms) as user types
- Dropdown with term name and accession
- Selected terms appear as removable chips
- Multi-select support
- Common terms for short queries

### SdrfParserService
**File:** `service/SdrfParserService.java` (NEW)

SDRF file parser extracting:
- Organisms (`characteristics[organism]`)
- Tissues (`characteristics[organism part]`)
- Cell types (`characteristics[cell type]`)
- Diseases (`characteristics[disease]`)
- Instruments (`comment[instrument]`)
- Modifications (`comment[modification parameters]`)

### SampleMetadataStep
**File:** `controller/SampleMetadataStep.java` (NEW)

Sample-level metadata entry:
- SDRF auto-detection and parsing
- OLS autocomplete fields for:
  - Species (required)
  - Tissue
  - Cell Type
  - Disease
  - Instrument (required)
  - Modifications
- Pre-populated from SDRF if available
- Manual entry fallback

---

## Phase 5: Polish

### ToolDetectionPanel Component
**File:** `view/component/ToolDetectionPanel.java` (NEW)

Dedicated panel for analysis tool detection:
- Shows detected tool name and confidence
- Progress bar for confidence score
- Checklist of expected files (required/optional)
- Status indicators: found (green), missing required (red), missing optional (gray)
- Collapsible design

### ValidationFeedback Component
**File:** `view/component/ValidationFeedback.java` (NEW)

Unified validation message display:
- Message types: ERROR, WARNING, SUCCESS, INFO
- Color-coded boxes with icons
- Methods: `addError()`, `addWarning()`, `addInfo()`, `setSuccess()`
- Properties: `hasErrors()`, `hasWarnings()`, `isValid()`

### Enhanced Validation Messages

**FileSelectionStep:**
- Real-time file requirement status
- Missing mandatory file errors
- Recommended file info messages
- File count summary

**ProjectMetadataStep:**
- Field completion counter (X/5 completed)
- Per-field validation messages
- Success message when all complete

**SummaryStep:**
- Comprehensive validation summary
- File validation (RAW, analysis, FASTA, SDRF)
- Metadata validation (species, instruments, title, description)
- **Confirmation dialog for missing recommended files**

### Step Progress Indicator
**Files:** `MainWindow.fxml`, `WizardController.java`, `main.css`

Added to wizard header:
- "Step X of Y" label
- Visual progress bar
- Styled for header background

### Help Tooltips
**Files:** `MainWindow.fxml`, `ProjectMetadataStep.java`

- Navigation button tooltips (Help, Cancel, Back, Next)
- Form field tooltips with guidance
- 300ms show delay for non-intrusive help

---

## Submission Type Changes

**File:** `controller/SubmissionTypeStep.java`

### Old System (Removed)
- Complete Submission
- Partial Submission
- Affinity Proteomics

### New System
Two clear options:

1. **PRIDE Submission** (Mass Spectrometry)
   - RAW files from instruments
   - Analysis outputs (MaxQuant, DIA-NN, etc.)
   - Optional: Standard formats (mzIdentML, mzTab) for ProteomeXchange

2. **Affinity Proteomics** (Non-MS)
   - SomaScan (ADAT files)
   - Olink (NPX, Parquet)
   - Other antibody/aptamer-based assays

**Note:** Including standard format files (mzIdentML, mzTab) automatically makes the submission a complete ProteomeXchange dataset.

---

## Recommended Files Handling

### Philosophy
- **Required files** block submission if missing
- **Recommended files** show info message and require confirmation to proceed without them

### Recommended Files
1. **FASTA database** - For protein sequence validation
2. **SDRF file** - For sample metadata and data discovery

### Implementation
- Info messages (not warnings) in FileSelectionStep and SummaryStep
- Confirmation dialog in SummaryStep.validate():
  ```
  "Missing Recommended Files"

  The following recommended files were not found:
  • FASTA database file - helps with protein sequence validation
  • SDRF file - provides sample metadata for better data discovery

  Do you want to proceed without them?

  [Proceed Anyway] [Go Back]
  ```

---

## File Structure

### New Files Created

```
src/main/java/uk/ac/ebi/pride/pxsubmit/
├── controller/
│   ├── WelcomeStep.java
│   ├── FileReviewStep.java
│   └── SampleMetadataStep.java
├── service/
│   ├── OlsService.java
│   └── SdrfParserService.java
├── util/
│   └── FileTypeDetector.java
└── view/component/
    ├── ChipInput.java
    ├── OlsAutocomplete.java
    ├── FileClassificationPanel.java
    ├── ToolDetectionPanel.java
    └── ValidationFeedback.java
```

### Modified Files

```
src/main/java/uk/ac/ebi/pride/pxsubmit/
├── PxSubmitApplication.java (wizard step registration)
├── controller/
│   ├── SubmissionTypeStep.java (simplified types)
│   ├── FileSelectionStep.java (validation feedback)
│   ├── ProjectMetadataStep.java (ChipInput, tooltips)
│   ├── SummaryStep.java (validation, confirmation)
│   └── WizardController.java (progress indicator)
└── model/
    └── SubmissionModel.java (checksum storage)

src/main/resources/
├── fxml/
│   └── MainWindow.fxml (progress indicator, tooltips)
└── css/
    └── main.css (step progress styling)
```

---

## Testing Recommendations

1. **File Classification Test**
   - Add MaxQuant output folder → Verify tool detected
   - Add DIA-NN output → Verify correct classification
   - Add mixed files → Verify categorization

2. **SDRF Test**
   - Add valid SDRF → Verify metadata extracted
   - No SDRF → Verify manual entry works

3. **Checksum Test**
   - Add files → Calculate checksums → Verify written to checksum.txt
   - Verify upload blocked until checksums complete

4. **Recommended Files Test**
   - Submit without FASTA → Verify confirmation dialog appears
   - Click "Proceed Anyway" → Verify submission continues
   - Click "Go Back" → Verify returns to summary

5. **Full Workflow Test**
   - Complete submission with all file types
   - Verify upload succeeds
   - Check submission ticket received

---

## Dependencies

- JavaFX 21+
- Jackson (JSON parsing for OLS API)
- Java HttpClient (OLS REST API)
- PRIDE data-provider-api (SubmissionTypeConstants, ProjectFileType)

---

## Future Considerations

1. **Standard Format Detection** - Auto-detect mzIdentML/mzTab and note ProteomeXchange eligibility
2. **Offline Mode** - Cache OLS common terms for offline use
3. **SDRF Generation** - Help users create SDRF from entered metadata
4. **Validation API** - Server-side validation before upload
