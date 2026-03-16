package hr.fipu.footmash.ui.leagues;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.ItemLeagueBinding;
import hr.fipu.footmash.model.LeagueResponse;

public class LeaguesAdapter extends RecyclerView.Adapter<LeaguesAdapter.LeagueViewHolder> {

    private List<LeagueResponse> leagues = new ArrayList<>();
    private final OnLeagueClickListener listener;

    public interface OnLeagueClickListener {
        void onLeagueClick(LeagueResponse league);
    }

    public LeaguesAdapter(OnLeagueClickListener listener) {
        this.listener = listener;
    }

    public void setLeagues(List<LeagueResponse> leagues) {
        this.leagues = leagues;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeagueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLeagueBinding binding = ItemLeagueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LeagueViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LeagueViewHolder holder, int position) {
        holder.bind(leagues.get(position));
    }

    @Override
    public int getItemCount() {
        return leagues != null ? leagues.size() : 0;
    }

    class LeagueViewHolder extends RecyclerView.ViewHolder {
        private final ItemLeagueBinding binding;

        public LeagueViewHolder(ItemLeagueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LeagueResponse leagueResponse) {
            LeagueResponse.LeagueInfo league = leagueResponse.getLeague();
            binding.textLeagueName.setText(league.getName());
            
            String type = league.getType() != null ? league.getType() : "Unknown";
            binding.textLeagueType.setText(type.equals("League") ? R.string.league_type : R.string.cup_type);

            Glide.with(itemView.getContext())
                    .load(league.getLogo())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(binding.imageLeagueLogo);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onLeagueClick(leagueResponse);
            });
        }
    }
}
