/*
 * This file defines WishlistRepositoryTest, a test class used to verify Scene app behavior.
 * It contains automated test coverage for WishlistRepository behavior and expected results.
 * Its functions include setUp, addToWishlist_noAuth_returnsFailure, onSuccess, onFailure to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;





@RunWith(MockitoJUnitRunner.class)
public class WishlistRepositoryTest {

    @Mock FirebaseFirestore mockDb;
    @Mock FirebaseAuth mockAuth;
    @Mock FirebaseUser mockUser;

    private WishlistRepository wishlistRepo;

    @Before
    public void setUp() {
        wishlistRepo = new WishlistRepository(mockDb, mockAuth);
    }

    

    @Test
    public void addToWishlist_noAuth_returnsFailure() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        wishlistRepo.addToWishlist("event1",
                new WishlistRepository.OnWishlistActionListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed without auth");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("not authenticated"));
                    }
                });
    }

    @Test
    public void removeFromWishlist_noAuth_returnsFailure() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        wishlistRepo.removeFromWishlist("event1",
                new WishlistRepository.OnWishlistActionListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed without auth");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("not authenticated"));
                    }
                });
    }

    @Test
    public void getWishlistEvents_noAuth_returnsFailure() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        wishlistRepo.getWishlistEvents(
                new WishlistRepository.OnWishlistLoadedListener() {
                    @Override
                    public void onSuccess(java.util.List events) {
                        fail("Should not succeed without auth");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("not authenticated"));
                    }
                });
    }

    

    @Test
    public void addToWishlist_nullEventId_returnsFailure() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("user123");

        wishlistRepo.addToWishlist(null,
                new WishlistRepository.OnWishlistActionListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed with null eventId");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Event ID is required"));
                    }
                });
    }

    @Test
    public void addToWishlist_emptyEventId_returnsFailure() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("user123");

        wishlistRepo.addToWishlist("",
                new WishlistRepository.OnWishlistActionListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed with empty eventId");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Event ID is required"));
                    }
                });
    }

    

    @Test
    public void isWishlisted_noAuth_returnsFalse() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        wishlistRepo.isWishlisted("event1", result -> assertFalse(result));
    }

    @Test
    public void isWishlisted_nullEventId_returnsFalse() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("user123");

        wishlistRepo.isWishlisted(null, result -> assertFalse(result));
    }
}
