package hr.fipu.footmash.ui.players;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.ItemPlayerBinding;
import hr.fipu.footmash.model.PlayerResponse;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> {

    private List<PlayerResponse> players = new ArrayList<>();
    private final OnPlayerClickListener listener;

    public interface OnPlayerClickListener {
        void onPlayerClick(PlayerResponse player);
    }

    public PlayersAdapter(OnPlayerClickListener listener) {
        this.listener = listener;
    }

    public void setPlayers(List<PlayerResponse> players) {
        this.players = players;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlayerBinding binding = ItemPlayerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PlayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        holder.bind(players.get(position));
    }

    @Override
    public int getItemCount() {
        return players != null ? players.size() : 0;
    }

    class PlayerViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlayerBinding binding;

        public PlayerViewHolder(ItemPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PlayerResponse playerResponse) {
            if (playerResponse.getPlayer() != null) {
                PlayerResponse.PlayerInfo p = playerResponse.getPlayer();
                binding.textPlayerName.setText(p.getName());
                binding.textPlayerInfo.setText(p.getAge() + " god. | " + p.getNationality());

                Glide.with(itemView.getContext())
                        .load(p.getPhoto())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(binding.imagePlayerPhoto);
            }

            if (playerResponse.getStatistics() != null && !playerResponse.getStatistics().isEmpty()) {
                PlayerResponse.Statistics stat = playerResponse.getStatistics().get(0);
                if (stat.getGames() != null && stat.getGames().getPosition() != null) {
                    binding.textPlayerPosition.setText(stat.getGames().getPosition());
                    
                    // Postavi boju ovisno o poziciji
                    String pos = stat.getGames().getPosition();
                    int colorRes = R.color.primary_light;
                    if (pos.equals("Attacker")) colorRes = R.color.position_fw;
                    else if (pos.equals("Midfielder")) colorRes = R.color.position_mf;
                    else if (pos.equals("Defender")) colorRes = R.color.position_df;
                    else if (pos.equals("Goalkeeper")) colorRes = R.color.position_gk;
                    
                    binding.textPlayerPosition.setBackgroundColor(itemView.getContext().getResources().getColor(colorRes, itemView.getContext().getTheme()));
                } else {
                    binding.textPlayerPosition.setText("UNK");
                }
            } else {
                binding.textPlayerPosition.setText("UNK");
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onPlayerClick(playerResponse);
            });
        }
    }
}
