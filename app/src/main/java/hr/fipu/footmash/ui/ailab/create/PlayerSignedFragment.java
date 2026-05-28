package hr.fipu.footmash.ui.ailab.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentPlayerSignedBinding;
import hr.fipu.footmash.db.CustomPlayerDao;
import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.ui.ailab.career.CareerHubFragment;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;

/**
 * Final wizard screen: persists the built CustomPlayer to the database and
 * shows a confirmation. Two CTAs:
 *
 * <ul>
 *   <li>"Kreiraj još jednog" — pops the back stack to AI Lab and re-enters
 *       step 1, with the ViewModel wiped via {@link PlayerCreationViewModel#reset}</li>
 *   <li>"Završi" — just pops back to AI Lab</li>
 * </ul>
 *
 * <p>Persistence happens once in onViewCreated (savedInstanceState == null)
 * so a rotation doesn't insert the player twice.
 */
public class PlayerSignedFragment extends Fragment {

    private FragmentPlayerSignedBinding binding;
    private PlayerCreationViewModel viewModel;

    /** Set once the insert returns; gates the "Pokreni karijeru" CTA. */
    private volatile int insertedPlayerId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerSignedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PlayerCreationViewModel.class);

        renderSummary();
        bindButtons();

        if (savedInstanceState == null) {
            persistPlayer();
        }
    }

    private void renderSummary() {
        binding.textSignedName.setText(viewModel.fullName());
        binding.textSignedMeta.setText(
            viewModel.getPosition().getValue() + " • "
            + viewModel.getNationality().getValue() + " • OVR "
            + viewModel.currentOverall());

        RealTeam club = viewModel.getSelectedClub().getValue();
        if (club != null) {
            binding.textSignedClub.setText(club.getName());
            Glide.with(this)
                .load(club.getBadgeUrl())
                .placeholder(new InitialsBadgeDrawable(club.getName()))
                .into(binding.imageSignedBadge);
        }
    }

    private void bindButtons() {
        // Disable until insert returns — otherwise the career hub gets playerId=0.
        binding.btnStartCareer.setEnabled(false);

        binding.btnStartCareer.setOnClickListener(v -> {
            if (insertedPlayerId <= 0) return;
            Bundle args = new Bundle();
            args.putInt(CareerHubFragment.ARG_PLAYER_ID, insertedPlayerId);
            Navigation.findNavController(v)
                .navigate(R.id.action_playerSigned_to_careerHub, args);
        });

        binding.btnCreateAnother.setOnClickListener(v -> {
            // Pop the wizard back stack to AI Lab, then start a fresh wizard.
            // reset() will fire from PlayerIdentityFragment when it re-enters.
            Navigation.findNavController(v).popBackStack(R.id.nav_ai_lab, false);
            Navigation.findNavController(v).navigate(R.id.nav_player_identity);
        });

        binding.btnDone.setOnClickListener(v ->
            Navigation.findNavController(v).popBackStack(R.id.nav_ai_lab, false));
    }

    private void persistPlayer() {
        final CustomPlayer player;
        try {
            player = viewModel.buildCustomPlayer();
        } catch (IllegalStateException e) {
            Toast.makeText(requireContext(),
                "Nije moguće spremiti igrača — neki podaci nedostaju.",
                Toast.LENGTH_LONG).show();
            return;
        }

        CustomPlayerDao dao = FootMashApp.container(requireContext()).database().customPlayerDao();
        new Thread(() -> {
            long newId = dao.insert(player);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                insertedPlayerId = (int) newId;
                if (binding != null) binding.btnStartCareer.setEnabled(true);
                Toast.makeText(requireContext(), "Igrač spremljen", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
