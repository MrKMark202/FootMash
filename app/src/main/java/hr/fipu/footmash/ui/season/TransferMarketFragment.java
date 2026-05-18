package hr.fipu.footmash.ui.season;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentTransferMarketBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserClub;

public class TransferMarketFragment extends Fragment {

    private FragmentTransferMarketBinding binding;
    private TransferMarketViewModel viewModel;
    private TransferMarketAdapter adapter;

    private String activeFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransferMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) {
            Toast.makeText(requireContext(), "Greška: klub nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel = new ViewModelProvider(this).get(TransferMarketViewModel.class);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club == null) return;

            requireActivity().runOnUiThread(() -> {
                binding.textLeagueName.setText(club.getLeagueName() + " · 2024/25");
                viewModel.init(clubId, club.getLeagueId());
                setupRecyclerView();
                setupFilterChips();
                observeViewModel();
                binding.btnDone.setOnClickListener(v -> onDoneClicked(view, clubId));
            });
        }).start();
    }

    private void setupRecyclerView() {
        adapter = new TransferMarketAdapter(new TransferMarketAdapter.SignCallback() {
            @Override
            public void onSign(RealPlayer player) {
                viewModel.signPlayer(player);
            }

            @Override
            public void onUnsign(int playerId) {
                viewModel.unsignPlayer(playerId);
            }
        });
        binding.recyclerPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPlayers.setAdapter(adapter);
    }

    private void setupFilterChips() {
        View[] chips = {binding.chipAll, binding.chipGK, binding.chipDF, binding.chipMF, binding.chipFW};
        String[] filters = {"ALL", "GK", "DF", "MF", "FW"};

        for (int i = 0; i < chips.length; i++) {
            final String filter = filters[i];
            chips[i].setOnClickListener(v -> setFilter(filter));
        }
    }

    private void setFilter(String filter) {
        activeFilter = filter;
        viewModel.setPositionFilter(filter);

        TextView[] chips = {binding.chipAll, binding.chipGK, binding.chipDF, binding.chipMF, binding.chipFW};
        String[] filters = {"ALL", "GK", "DF", "MF", "FW"};

        for (int i = 0; i < chips.length; i++) {
            boolean selected = filters[i].equals(filter);
            chips[i].setBackground(ContextCompat.getDrawable(requireContext(),
                selected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected));
            chips[i].setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.text_on_primary : R.color.text_secondary));
        }
    }

    private void observeViewModel() {
        viewModel.getFilteredPlayers().observe(getViewLifecycleOwner(), players ->
            adapter.setPlayers(players));

        viewModel.getSignedPlayerIds().observe(getViewLifecycleOwner(), ids ->
            adapter.setSignedIds(ids));

        viewModel.getRemainingBudget().observe(getViewLifecycleOwner(), budget -> {
            double millions = budget / 1_000_000.0;
            binding.textBudget.setText(String.format("€%.1fM", millions));
        });

        viewModel.getSquadCount().observe(getViewLifecycleOwner(), count ->
            binding.textSquadCount.setText(count + " igrača u klubu"));
    }

    private void onDoneClicked(View navView, int clubId) {
        int count = viewModel.getSquadCount().getValue() != null
                ? viewModel.getSquadCount().getValue() : 0;
        if (count < 11) {
            Toast.makeText(requireContext(),
                "Potrebno je minimalno 11 igrača (trenutno: " + count + ")",
                Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putInt("clubId", clubId);
        Navigation.findNavController(navView)
            .navigate(R.id.action_transferMarket_to_squadBuilder, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
