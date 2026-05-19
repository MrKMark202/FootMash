package hr.fipu.footmash.ui.teams;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.databinding.ItemTeamBinding;
import hr.fipu.footmash.model.TeamResponse;
import hr.fipu.footmash.ui.util.TeamBadges;

public class TeamsAdapter extends RecyclerView.Adapter<TeamsAdapter.TeamViewHolder> {

    private List<TeamResponse> teams = new ArrayList<>();
    private final OnTeamClickListener listener;

    public interface OnTeamClickListener {
        void onTeamClick(TeamResponse team);
    }

    public TeamsAdapter(OnTeamClickListener listener) {
        this.listener = listener;
    }

    public void setTeams(List<TeamResponse> teams) {
        this.teams = teams;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTeamBinding binding = ItemTeamBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TeamViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        holder.bind(teams.get(position));
    }

    @Override
    public int getItemCount() {
        return teams != null ? teams.size() : 0;
    }

    class TeamViewHolder extends RecyclerView.ViewHolder {
        private final ItemTeamBinding binding;

        public TeamViewHolder(ItemTeamBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TeamResponse team) {
            binding.textTeamName.setText(team.getTeamName());
            binding.textTeamCountry.setText(""); // Not directly in simple response
            binding.textFounded.setText("-");

            binding.imageTeamLogo.setImageDrawable(
                    TeamBadges.badgeFor(itemView.getContext(), team.getTeamKey(), team.getTeamName()));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onTeamClick(team);
            });
        }
    }
}
