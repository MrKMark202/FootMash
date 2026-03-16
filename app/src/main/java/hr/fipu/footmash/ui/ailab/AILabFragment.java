package hr.fipu.footmash.ui.ailab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import hr.fipu.footmash.databinding.FragmentAiLabBinding;

public class AILabFragment extends Fragment {

    private FragmentAiLabBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiLabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.btnSimulatePlayer.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                .navigate(hr.fipu.footmash.R.id.action_aiLab_to_playerSimulation);
        });

        binding.btnSimulateTeam.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                .navigate(hr.fipu.footmash.R.id.action_aiLab_to_teamSimulation);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
