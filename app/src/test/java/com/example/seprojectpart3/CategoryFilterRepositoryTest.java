package com.example.seprojectpart3;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for M3 Sprint 3 stories:
 *  - #3  CategoryFilterRepository
 *  - #29 FeaturedEventRepository
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryFilterRepositoryTest {

    @Mock FirebaseFirestore mockDb;
    @Mock CollectionReference mockCollection;
    @Mock Query mockQuery;
    @Mock Task<QuerySnapshot> mockTask;

    private CategoryFilterRepository categoryRepo;
    private FeaturedEventRepository featuredRepo;

    @Before
    public void setUp() {
        when(mockDb.collection("events")).thenReturn(mockCollection);
        categoryRepo = new CategoryFilterRepository(mockDb);
        featuredRepo = new FeaturedEventRepository(mockDb);
    }

    // ═══ #3 — Category Filter Tests ═══════════════════════════

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

    // ═══ #29 — Featured Events Tests ══════════════════════════

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
