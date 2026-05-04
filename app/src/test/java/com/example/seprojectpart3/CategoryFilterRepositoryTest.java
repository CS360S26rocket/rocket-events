package com.example.seprojectpart3;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class CategoryFilterRepositoryTest {

    private CategoryFilterRepository categoryRepo;
    private FeaturedEventRepository featuredRepo;

    @Before
    public void setUp() {
        categoryRepo = new CategoryFilterRepository(null);
        featuredRepo = new FeaturedEventRepository(null);
    }

    @Test
    public void filterByCategory_nullCategory_callsOnFailure() {
        categoryRepo.filterByCategory(null,
                new CategoryFilterRepository.OnEventsLoadedListener() {
                    @Override
                    public void onSuccess(List events) {
                        fail("Should not succeed with null category");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("must not be empty"));
                    }
                });
    }

    @Test
    public void filterByCategory_emptyCategory_callsOnFailure() {
        categoryRepo.filterByCategory("",
                new CategoryFilterRepository.OnEventsLoadedListener() {
                    @Override
                    public void onSuccess(List events) {
                        fail("Should not succeed with empty category");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("must not be empty"));
                    }
                });
    }

    @Test
    public void filterByTags_nullTags_callsOnFailure() {
        categoryRepo.filterByTags(null,
                new CategoryFilterRepository.OnEventsLoadedListener() {
                    @Override
                    public void onSuccess(List events) {
                        fail("Should not succeed with null tags");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("tag is required"));
                    }
                });
    }

    @Test
    public void filterByTags_emptyList_callsOnFailure() {
        categoryRepo.filterByTags(Collections.emptyList(),
                new CategoryFilterRepository.OnEventsLoadedListener() {
                    @Override
                    public void onSuccess(List events) {
                        fail("Should not succeed with empty tags");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("tag is required"));
                    }
                });
    }

    @Test
    public void filterByCategoryAndTag_nullInputs_callsOnFailure() {
        categoryRepo.filterByCategoryAndTag(null, "music",
                new CategoryFilterRepository.OnEventsLoadedListener() {
                    @Override
                    public void onSuccess(List events) {
                        fail("Should not succeed");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("required"));
                    }
                });
    }

    @Test
    public void supportedCategories_containsExpectedValues() {
        List<String> cats = CategoryFilterRepository.CATEGORIES;

        assertTrue(cats.contains("Academic"));
        assertTrue(cats.contains("Sports"));
        assertTrue(cats.contains("Cultural"));
        assertTrue(cats.contains("Workshop"));
        assertEquals(8, cats.size());
    }

    @Test
    public void setFeatured_nullEventId_callsOnFailure() {
        featuredRepo.setFeatured(null, true,
                new FeaturedEventRepository.OnToggleCompleteListener() {
                    @Override
                    public void onSuccess(boolean newState) {
                        fail("Should not succeed with null ID");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Event ID is required"));
                    }
                });
    }

    @Test
    public void setFeatured_emptyEventId_callsOnFailure() {
        featuredRepo.setFeatured("", true,
                new FeaturedEventRepository.OnToggleCompleteListener() {
                    @Override
                    public void onSuccess(boolean newState) {
                        fail("Should not succeed with empty ID");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Event ID is required"));
                    }
                });
    }
}
