package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.FootMashApp;
import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentExistingClubPickerBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.repository.DraftRepository;
import hr.fipu.footmash.season.FormationCatalog;
import com.bumptech.glide.Glide;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import android.widget.ImageView;

public class ExistingClubPickerFragment extends Fragment {

    private FragmentExistingClubPickerBinding binding;
    private TeamAdapter adapter;
    private int leagueId = 177;
    private String leagueName = "Premier League";
    private boolean creating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExistingClubPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            leagueId = args.getInt("leagueId", 177);
            leagueName = args.getString("leagueName", "Premier League");
        }

        adapter = new TeamAdapter(team -> inheritClub(team, view));
        binding.recyclerTeams.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTeams.setAdapter(adapter);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.realTeamDao().getTeamsByLeague(leagueId).observe(getViewLifecycleOwner(),
            adapter::setTeams);
    }

    private void inheritClub(RealTeam team, View navView) {
        if (creating) return;
        creating = true;

        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            // Pick a sensible default formation (4-3-3) — user can re-draft as they sell/buy
            String formation = FormationCatalog.DEFAULT;

            db.userClubDao().deactivateAll();

            UserClub club = new UserClub();
            club.setClubName(team.getName());
            club.setLeagueId(leagueId);
            club.setLeagueName(leagueName);
            club.setBudget(UserClub.HALF_BUDGET);
            club.setFormation(formation);
            club.setSeasonYear(2025);
            club.setActive(true);
            club.setRealTeamSourceId(team.getId());

            long newId = db.userClubDao().insertClub(club);

            DraftRepository repo = FootMashApp.container(requireContext()).draftRepository();
            repo.seedExistingClub((int) newId, team.getId(), formation);

            requireActivity().runOnUiThread(() -> {
                Bundle args = new Bundle();
                args.putInt("clubId", (int) newId);
                Navigation.findNavController(navView)
                    .navigate(R.id.action_existingClubPicker_to_draftCanvas, args);
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- adapter ---

    interface OnTeamSelected {
        void onSelected(RealTeam team);
    }

    static class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.VH> {
        private List<RealTeam> teams = new ArrayList<>();
        private final OnTeamSelected listener;

        TeamAdapter(OnTeamSelected listener) { this.listener = listener; }

        void setTeams(List<RealTeam> t) {
            this.teams = t != null ? t : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_existing_club, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RealTeam team = teams.get(position);
            holder.name.setText(team.getName());
            holder.meta.setText(team.getCountry() != null ? team.getCountry() : "");
            
            String badgeUrl = team.getBadgeUrl();
            if (badgeUrl == null || badgeUrl.isEmpty()) {
                String kebabName = team.getName().toLowerCase()
                    .replace(" & ", "-")
                    .replace("&", "")
                    .replace(" ", "-")
                    .replace(".", "");
                badgeUrl = "https://apiv2.allsportsapi.com/logo/" + team.getId() + "_" + kebabName + ".jpg";
            }
            Glide.with(holder.itemView.getContext())
                 .load(badgeUrl)
                 .placeholder(new InitialsBadgeDrawable(team.getName()))
                 .into(holder.badge);
            
            holder.itemView.setOnClickListener(v -> listener.onSelected(team));
        }

        @Override
        public int getItemCount() { return teams.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, meta;
            ImageView badge;
            VH(@NonNull View v) {
                super(v);
                name = v.findViewById(R.id.textTeamName);
                meta = v.findViewById(R.id.textTeamMeta);
                badge = v.findViewById(R.id.imageTeamBadge);
            }
        }
    }
}
