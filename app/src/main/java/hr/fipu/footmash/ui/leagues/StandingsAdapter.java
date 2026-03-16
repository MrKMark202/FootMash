package hr.fipu.footmash.ui.leagues;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.databinding.ItemStandingBinding;
import hr.fipu.footmash.model.StandingResponse;

public class StandingsAdapter extends RecyclerView.Adapter<StandingsAdapter.StandingViewHolder> {

    private List<StandingResponse.StandingEntry> standings = new ArrayList<>();

    public void setStandings(List<StandingResponse.StandingEntry> standings) {
        this.standings = standings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StandingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStandingBinding binding = ItemStandingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StandingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StandingViewHolder holder, int position) {
        holder.bind(standings.get(position));
    }

    @Override
    public int getItemCount() {
        return standings != null ? standings.size() : 0;
    }

    class StandingViewHolder extends RecyclerView.ViewHolder {
        private final ItemStandingBinding binding;

        public StandingViewHolder(ItemStandingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(StandingResponse.StandingEntry info) {
            binding.textPos.setText(String.valueOf(info.getRank()));
            binding.textTeamName.setText(info.getTeam() != null ? info.getTeam().getName() : "");
            
            if (info.getTeam() != null && info.getTeam().getLogo() != null) {
                Glide.with(itemView.getContext())
                        .load(info.getTeam().getLogo())
                        .into(binding.imageTeamLogo);
            }

            if (info.getAll() != null) {
                binding.textPlayed.setText(String.valueOf(info.getAll().getPlayed()));
                binding.textWon.setText(String.valueOf(info.getAll().getWin()));
                binding.textDrawn.setText(String.valueOf(info.getAll().getDraw()));
                binding.textLost.setText(String.valueOf(info.getAll().getLose()));
            }

            binding.textGD.setText(String.valueOf(info.getGoalsDiff()));
            binding.textPts.setText(String.valueOf(info.getPoints()));
        }
    }
}
