package uk.ac.ebi.pride.gui.form.table.model;

import java.util.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ProjectTagTableModel extends PxTableModel {
    public static final String HUMAN_PROTEOME_PROJECT = "Human Proteome Project";

    public enum TableHeader {
        SELECTION("", "Select a related project"),
        PROJECT_TAG("Parent Project", "Parent project name");

        private final String header;
        private final String toolTip;

        private TableHeader(String header, String tooltip) {
            this.header = header;
            this.toolTip = tooltip;
        }

        public String getHeader() {
            return header;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    private final Map<String, Boolean> projectTags = new LinkedHashMap<String, Boolean>();

    public ProjectTagTableModel(Collection<String> projectTags) {
        super();

        for (String projectTag : projectTags) {
            this.projectTags.put(projectTag, false);
        }

    }

    @Override
    protected void initializeTableModel() {
        TableHeader[] headers = TableHeader.values();
        for (TableHeader header : headers) {
            columnNames.put(header.getHeader(), header.getToolTip());
        }
    }

    @Override
    public int getRowCount() {
        return projectTags.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (!projectTags.isEmpty() && rowIndex >= 0 && columnIndex >= 0) {
            List<Map.Entry<String, Boolean>> entries = new ArrayList<Map.Entry<String, Boolean>>(projectTags.entrySet());

            Map.Entry<String, Boolean> entry = entries.get(rowIndex);
            String projectTag = entry.getKey();
            Boolean selected = entry.getValue();

            if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
                return selected;
            } else if (TableHeader.PROJECT_TAG.getHeader().equals(getColumnName(columnIndex))) {
                return projectTag;
            }
        }

        return null;
    }

    public Set<String> getSelectedProjectTags() {
        Set<String> selectedProjectTags = new HashSet<String>();

        for (String projectTag : projectTags.keySet()) {
            if (projectTags.get(projectTag)) {
                selectedProjectTags.add(projectTag);
            }
        }

        return selectedProjectTags;
    }

    public void setSelectedProjectTags(Collection<String> selectedProjectTags) {
        for (String projectTag : projectTags.keySet()) {
            projectTags.put(projectTag, selectedProjectTags.contains(projectTag));
        }
        fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex)) || super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
            return Boolean.class;
        } else {
            return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        List<Map.Entry<String, Boolean>> entries = new ArrayList<Map.Entry<String, Boolean>>(projectTags.entrySet());

        Map.Entry<String, Boolean> entry = entries.get(rowIndex);
        String projecTag = entry.getKey();

        if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
            Boolean selection = (Boolean) aValue;

            projectTags.put(projecTag, selection);

            if (projecTag.endsWith(HUMAN_PROTEOME_PROJECT) && selection) {
                projectTags.put(HUMAN_PROTEOME_PROJECT, true);
            }

            fireTableDataChanged();
        } else {
            super.setValueAt(aValue, rowIndex, columnIndex);
        }
    }
}
