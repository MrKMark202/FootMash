package hr.fipu.footmash.ui.leagues;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentCountriesBinding;

public class CountriesFragment extends Fragment {

    private FragmentCountriesBinding binding;
    private LeaguesViewModel viewModel;
    private CountriesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCountriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LeaguesViewModel.class);

        binding.toolbar.setTitle(R.string.countries_title);

        setupRecyclerView();
        setupSearch();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new CountriesAdapter(country -> {
            Bundle args = new Bundle();
            args.putString("countryName", country.getName());
            
            // Provjera putanje (iz main nav grafa)
            try {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_countries_to_leagues, args);
            } catch (Exception e) {
                // Ako nismo s home taba nego iz bottom nava
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_leagues_tab_to_leagues, args);
            }
        });

        binding.recyclerCountries.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCountries.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        viewModel.getCountries().observe(getViewLifecycleOwner(), countries -> {
            binding.progressBar.setVisibility(View.GONE);
            if (countries != null && !countries.isEmpty()) {
                adapter.setCountries(countries);
                binding.recyclerCountries.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            } else {
                binding.recyclerCountries.setVisibility(View.GONE);
                binding.textEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
