package hr.fipu.footmash.ui.ailab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentAiLabBinding;
import hr.fipu.footmash.databinding.ItemSavedPlayerBinding;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.ui.ailab.career.CareerHubFragment;

/**
 * AI Lab tab landing screen. Hosts:
 *
 * <ul>
 *     <li>The "KREIRAJ IGRAČA" entry into the 4-step wizard</li>
 *     <li>A "Moji igrači" list of every {@code CustomPlayer} the user has
 *         created. Tap to resume that player's career; long-press to
 *         delete them (and their career rows) via {@code SavedGamesRepository}.</li>
 * </ul>
 */
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

        binding.btnSimulatePlayer.setOnClickListener(v ->
            Navigation.findNavController(v)
                .navigate(R.id.action_aiLab_to_playerCreation));

        observeSavedPlayers();
    }

    private void observeSavedPlayers() {
        FootMashApp.container(requireContext())
            .database()
            .customPlayerDao()
            .getAllPlayers()
            .observe(getViewLifecycleOwner(), this::renderSavedPlayers);
    }

    private void renderSavedPlayers(@Nullable List<CustomPlayer> players) {
        binding.containerSavedPlayers.removeAllViews();
        boolean empty = players == null || players.isEmpty();
        binding.textNoSavedPlayers.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.textSavedPlayersHint.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CustomPlayer p : players) {
            View row = inflater.inflate(
                R.layout.item_saved_player, binding.containerSavedPlayers, false);
            bindRow(row, p);
            binding.containerSavedPlayers.addView(row);
        }
    }

    private void bindRow(View row, CustomPlayer p) {
        ItemSavedPlayerBinding b = ItemSavedPlayerBinding.bind(row);
        b.textSavedPlayerName.setText(p.getFullName());
        b.textSavedPlayerMeta.setText(p.getPosition()
            + " • " + p.getNationality()
            + " • " + p.getTargetTeamName());
        b.textSavedPlayerSeason.setText(String.format(Locale.getDefault(),
            "Sezona %d/%02d",
            p.getCurrentSeasonYear(),
            (p.getCurrentSeasonYear() + 1) % 100));
        b.textSavedPlayerOvr.setText(String.valueOf(p.getOverall()));

        row.setOnClickListener(v -> openCareerHub(p.getId()));
        b.btnDeletePlayer.setOnClickListener(v -> confirmDelete(p));
        row.setOnLongClickListener(v -> {
            confirmDelete(p);
            return true;
        });
    }

    private void openCareerHub(int playerId) {
        Bundle args = new Bundle();
        args.putInt(CareerHubFragment.ARG_PLAYER_ID, playerId);
        Navigation.findNavController(requireView())
            .navigate(R.id.action_aiLab_to_careerHub, args);
    }

    private void confirmDelete(CustomPlayer p) {
        new MaterialAlertDialogBuilder(requireContext(),
                R.style.FootMashDialog)
            .setTitle("Obriši igrača?")
            .setMessage(p.getFullName() + " — " + p.getTargetTeamName()
                + "\nKarijera će biti obrisana.")
            .setNegativeButton("Odustani", null)
            .setPositiveButton("Obriši", (dlg, which) -> doDelete(p.getId()))
            .show();
    }

    private void doDelete(int playerId) {
        new Thread(() ->
            FootMashApp.container(requireContext())
                .savedGamesRepository()
                .deletePlayer(playerId)
        ).start();
        // LiveData re-emits automatically; the row vanishes on next observe tick.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
