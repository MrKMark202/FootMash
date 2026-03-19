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

    public interface OnTeamClickListener {
        void onTeamClick(StandingResponse team);
    }

    private List<StandingResponse> standings = new ArrayList<>();
    private OnTeamClickListener listener;

    public void setOnTeamClickListener(OnTeamClickListener listener) {
        this.listener = listener;
    }

    public void setStandings(List<StandingResponse> standings) {
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
            
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTeamClick(standings.get(position));
                }
            });
        }

        public void bind(StandingResponse info) {
            binding.textPos.setText(String.valueOf(info.getStandingPlace()));
            binding.textTeamName.setText(info.getStandingTeam());
            
            // Note: AllSportsAPI standings might not provide logo in the flat response always
            // We might need to handle this or load placeholder
            
            binding.textPlayed.setText(String.valueOf(info.getStandingW() + info.getStandingD() + info.getStandingL()));
            binding.textWon.setText(String.valueOf(info.getStandingW()));
            binding.textDrawn.setText(String.valueOf(info.getStandingD()));
            binding.textLost.setText(String.valueOf(info.getStandingL()));

            binding.textGD.setText(String.valueOf(info.getStandingGD()));
            binding.textPts.setText(String.valueOf(info.getStandingPts()));
        }
    }
}
