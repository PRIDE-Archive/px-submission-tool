package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.scene.control.TextField;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Searchable multi-select project tags picker (see {@link SearchableMultiSelectField}).
 */
public class ProjectTagsSearchField extends SearchableMultiSelectField {

    public ProjectTagsSearchField(String[] tags) {
        super("Search project tags...", "No matching tags", List.of(tags));
    }

    public void setOnSelectionChanged(Consumer<Set<String>> handler) {
        super.setOnSelectionChanged(handler);
    }

    @Override
    public TextField getSearchField() {
        return super.getSearchField();
    }

    public List<String> getSelectedTags() {
        return getSelectedItems();
    }

    public void setSelectedTags(Collection<String> tags) {
        setSelectedItems(tags);
    }
}
