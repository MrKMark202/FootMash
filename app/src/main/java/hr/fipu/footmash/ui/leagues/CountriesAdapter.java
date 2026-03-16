package hr.fipu.footmash.ui.leagues;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.ItemCountryBinding;
import hr.fipu.footmash.model.CountryResponse;

public class CountriesAdapter extends RecyclerView.Adapter<CountriesAdapter.CountryViewHolder> {

    private List<CountryResponse> countries = new ArrayList<>();
    private List<CountryResponse> countriesFull = new ArrayList<>(); // za pretraživanje
    private final OnCountryClickListener listener;

    public interface OnCountryClickListener {
        void onCountryClick(CountryResponse country);
    }

    public CountriesAdapter(OnCountryClickListener listener) {
        this.listener = listener;
    }

    public void setCountries(List<CountryResponse> countries) {
        this.countries = new ArrayList<>(countries);
        this.countriesFull = new ArrayList<>(countries);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        countries.clear();
        if (query.isEmpty()) {
            countries.addAll(countriesFull);
        } else {
            String q = query.toLowerCase();
            for (CountryResponse c : countriesFull) {
                if (c.getName().toLowerCase().contains(q)) {
                    countries.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CountryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCountryBinding binding = ItemCountryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CountryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CountryViewHolder holder, int position) {
        holder.bind(countries.get(position));
    }

    @Override
    public int getItemCount() {
        return countries != null ? countries.size() : 0;
    }

    class CountryViewHolder extends RecyclerView.ViewHolder {
        private final ItemCountryBinding binding;

        public CountryViewHolder(ItemCountryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CountryResponse country) {
            binding.textCountryName.setText(country.getName());
            binding.textCountryCode.setText(country.getCode() != null ? country.getCode() : "");

            if (country.getFlag() != null && !country.getFlag().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(country.getFlag())
                        .into(binding.imageFlag);
            } else {
                binding.imageFlag.setImageResource(android.R.drawable.ic_menu_myplaces); // Placeholder
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onCountryClick(country);
            });
        }
    }
}
