package pl.sk.ocr.configurator.app;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.dto.ProfileDto;

public final class ProfileWorkspace {
    private Path profilePath;
    private ProfileDto profile;
    private Path categoriesDirectory;
    private final List<CategoryEntry> categories = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean dirty;

    public Path profilePath() {
        return profilePath;
    }

    public void profilePath(Path profilePath) {
        this.profilePath = profilePath;
    }

    public ProfileDto profile() {
        return profile;
    }

    public void profile(ProfileDto profile) {
        this.profile = profile;
    }

    public Path categoriesDirectory() {
        return categoriesDirectory;
    }

    public void categoriesDirectory(Path categoriesDirectory) {
        this.categoriesDirectory = categoriesDirectory;
    }

    public List<CategoryEntry> categories() {
        return categories;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void selectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public CategoryEntry selectedCategory() {
        return selectedIndex >= 0 && selectedIndex < categories.size() ? categories.get(selectedIndex) : null;
    }

    public boolean dirty() {
        return dirty;
    }

    public void dirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void markSaved() {
        this.dirty = false;
    }

    public record CategoryEntry(String id, String displayName, Path path, CategoryDto draft) {
        @Override
        public String toString() {
            var label = displayName == null || displayName.isBlank() ? id : displayName;
            return label == null || label.isBlank() ? "(unnamed category)" : label;
        }
    }
}
