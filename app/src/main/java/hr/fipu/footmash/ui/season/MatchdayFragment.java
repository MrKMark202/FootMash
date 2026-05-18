package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import hr.fipu.footmash.databinding.FragmentMatchdayBinding;

public class MatchdayFragment extends Fragment {

    private final String GEMINI_API_KEY = hr.fipu.footmash.BuildConfig.GEMINI_API_KEY;

    private FragmentMatchdayBinding binding;
    private MatchdayViewModel viewModel;
    private MatchdayAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchdayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        int matchday = getArguments() != null ? getArguments().getInt("matchday", -1) : -1;
        if (clubId == -1 || matchday == -1) return;

        viewModel = new ViewModelProvider(this).get(MatchdayViewModel.class);
        viewModel.init(clubId, matchday);

        binding.textMatchdayTitle.setText("Kolo " + matchday);

        adapter = new MatchdayAdapter();
        binding.recyclerFixtures.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFixtures.setAdapter(adapter);

        viewModel.getFixtures().observe(getViewLifecycleOwner(), this::bindFixtures);
        viewModel.getSimState().observe(getViewLifecycleOwner(), this::bindSimState);

        binding.btnSimulateMatchday.setOnClickListener(v ->
            viewModel.simulate(GEMINI_API_KEY));
    }

    private void bindFixtures(List<MatchdayViewModel.FixtureDisplay> items) {
        adapter.setItems(items);
    }

    private void bindSimState(MatchdayViewModel.SimState state) {
        switch (state) {
            case SIMULATING:
                binding.progressSimulation.setVisibility(View.VISIBLE);
                binding.btnSimulateMatchday.setEnabled(false);
                binding.btnSimulateMatchday.setAlpha(0.5f);
                break;
            case DONE:
                binding.progressSimulation.setVisibility(View.GONE);
                binding.btnSimulateMatchday.setEnabled(false);
                binding.btnSimulateMatchday.setText("Kolo odigrano");
                binding.btnSimulateMatchday.setAlpha(0.6f);
                break;
            case ERROR:
                binding.progressSimulation.setVisibility(View.GONE);
                binding.btnSimulateMatchday.setEnabled(true);
                binding.btnSimulateMatchday.setAlpha(1f);
                Toast.makeText(requireContext(),
                    "Greška pri simulaciji, pokušaj ponovo", Toast.LENGTH_SHORT).show();
                break;
            default:
                binding.progressSimulation.setVisibility(View.GONE);
                binding.btnSimulateMatchday.setEnabled(true);
                binding.btnSimulateMatchday.setAlpha(1f);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
