/*
 * This file defines WishlistActivity, an Android activity used by the Scene app.
 * It contains saved or wishlisted events for campus users.
 * Its functions include onCreate, onResume, loadWishlist, onSuccess to load data, handle user actions, validate input, and save results.
 * It connects this feature to the Scene app's UI, data, navigation, and verification flow.
 */

package com.example.seprojectpart3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;








public class WishlistActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout tvEmpty;
    private TextView tvCount;
    private WishlistAdapter adapter;
    private WishlistRepository wishlistRepo;

    private List<Map<String, Object>> wishlistEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        recyclerView = findViewById(R.id.rvWishlist);
        tvEmpty = findViewById(R.id.tvEmptyWishlist);
        tvCount = findViewById(R.id.tvWishlistCount);

        wishlistRepo = new WishlistRepository();

        adapter = new WishlistAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadWishlist();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        wishlistRepo.getWishlistEvents(
                new WishlistRepository.OnWishlistLoadedListener() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> events) {
                        wishlistEvents.clear();
                        wishlistEvents.addAll(events);
                        adapter.notifyDataSetChanged();

                        if (events.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }

                        tvCount.setText(events.size() + " saved events");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(WishlistActivity.this,
                                errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeFromWishlist(int position) {
        if (position < 0 || position >= wishlistEvents.size()) return;

        String eventId = (String) wishlistEvents.get(position).get("eventId");
        if (eventId == null) return;

        wishlistRepo.removeFromWishlist(eventId,
                new WishlistRepository.OnWishlistActionListener() {
                    @Override
                    public void onSuccess() {
                        wishlistEvents.remove(position);
                        adapter.notifyItemRemoved(position);
                        tvCount.setText(wishlistEvents.size() + " saved events");

                        if (wishlistEvents.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }

                        Toast.makeText(WishlistActivity.this,
                                "Removed from wishlist",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String msg) {
                        Toast.makeText(WishlistActivity.this,
                                msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    
    private class WishlistAdapter
            extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wishlist_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> event = wishlistEvents.get(position);

            String title = (String) event.get("title");
            String locationName = (String) event.get("locationName");
            String category = (String) event.get("category");

            holder.tvTitle.setText(title != null ? title : "Untitled Event");
            holder.tvLocation.setText(
                    locationName != null ? locationName : "Location TBA");
            holder.tvCategory.setText(
                    category != null ? category : "");

            
            Object dateObj = event.get("eventDate");
            if (dateObj instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) dateObj).toDate();
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "EEE, MMM d · h:mm a", Locale.getDefault());
                holder.tvDate.setText(sdf.format(date));
            } else {
                holder.tvDate.setText("Date TBA");
            }

            
            Object priceObj = event.get("price");
            if (priceObj instanceof Number) {
                double price = ((Number) priceObj).doubleValue();
                holder.tvPrice.setText(
                        price == 0 ? "Free" : "PKR " + String.format("%.0f", price));
            } else {
                holder.tvPrice.setText("");
            }

            
            holder.btnRemove.setOnClickListener(v ->
                    removeFromWishlist(holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return wishlistEvents.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvLocation, tvCategory, tvPrice;
            ImageButton btnRemove;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvEventTitle);
                tvDate = itemView.findViewById(R.id.tvEventDate);
                tvLocation = itemView.findViewById(R.id.tvEventLocation);
                tvCategory = itemView.findViewById(R.id.tvEventCategory);
                tvPrice = itemView.findViewById(R.id.tvEventPrice);
                btnRemove = itemView.findViewById(R.id.btnRemoveWishlist);
            }
        }
    }
}
