package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentLeaguePickerBinding;

public class LeaguePickerFragment extends Fragment {

    private FragmentLeaguePickerBinding binding;

    static class LeagueOption {
        int leagueId;
        String name;
        String country;
        String emoji;
        int teamCount;

        LeagueOption(int leagueId, String name, String country, String emoji, int teamCount) {
            this.leagueId = leagueId;
            this.name = name;
            this.country = country;
            this.emoji = emoji;
            this.teamCount = teamCount;
        }
    }

    private static final List<LeagueOption> LEAGUES = Arrays.asList(
        new LeagueOption(177, "Premier League", "Engleska", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", 20),
        new LeagueOption(302, "La Liga", "Španjolska", "🇪🇸", 20)
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaguePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerLeagues.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerLeagues.setAdapter(new LeaguePickAdapter(LEAGUES, option -> {
            Bundle args = new Bundle();
            args.putInt("leagueId", option.leagueId);
            args.putString("leagueName", option.name);
            Navigation.findNavController(view).navigate(R.id.action_leaguePicker_to_clubSetup, args);
        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- inline adapter ---

    interface OnLeagueSelected {
        void onSelected(LeagueOption option);
    }

    static class LeaguePickAdapter extends RecyclerView.Adapter<LeaguePickAdapter.VH> {
        private final List<LeagueOption> leagues;
        private final OnLeagueSelected listener;

        LeaguePickAdapter(List<LeagueOption> leagues, OnLeagueSelected listener) {
            this.leagues = leagues;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_league_pick, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LeagueOption opt = leagues.get(position);
            holder.emoji.setText(opt.emoji);
            holder.name.setText(opt.name);
            holder.country.setText(opt.country);
            holder.teams.setText(opt.teamCount + " klubova · Sezona 2024/25");
            holder.itemView.setOnClickListener(v -> listener.onSelected(opt));
        }

        @Override
        public int getItemCount() { return leagues.size(); }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView emoji, name, country, teams;
            VH(@NonNull View v) {
                super(v);
                emoji = v.findViewById(R.id.textLeagueEmoji);
                name = v.findViewById(R.id.textLeagueName);
                country = v.findViewById(R.id.textLeagueCountry);
                teams = v.findViewById(R.id.textLeagueTeams);
            }
        }
    }
}
