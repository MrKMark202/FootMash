package hr.fipu.footmash.ui.season;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hr.fipu.footmash.R;
import hr.fipu.footmash.model.RealPlayer;

public class TransferMarketAdapter extends RecyclerView.Adapter<TransferMarketAdapter.ViewHolder> {

    public interface SignCallback {
        void onSign(RealPlayer player);
        void onUnsign(int playerId);
    }

    private List<RealPlayer> players = new ArrayList<>();
    private Set<Integer> signedIds = new HashSet<>();
    private final SignCallback callback;

    public TransferMarketAdapter(SignCallback callback) {
        this.callback = callback;
    }

    public void setPlayers(List<RealPlayer> players) {
        this.players = players != null ? players : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSignedIds(List<Integer> ids) {
        this.signedIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transfer_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RealPlayer player = players.get(position);
        boolean isSigned = signedIds.contains(player.getId());

        holder.textOverall.setText(String.valueOf(player.getOverall()));
        holder.textPlayerName.setText(player.getName());
        holder.textPosition.setText(player.getPosition());
        holder.textTeamName.setText(player.getTeamName());
        holder.textMarketValue.setText(formatValue(player.getMarketValue()));

        int overallColor = getOverallColor(player.getOverall());
        holder.textOverall.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(overallColor));

        if (isSigned) {
            holder.btnSign.setText("Otpusti");
            holder.btnSign.setStrokeColor(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#FF3B30")));
            holder.btnSign.setTextColor(Color.parseColor("#FF3B30"));
            holder.btnSign.setOnClickListener(v -> callback.onUnsign(player.getId()));
        } else {
            holder.btnSign.setText("Potpiši");
            holder.btnSign.setStrokeColor(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#2962FF")));
            holder.btnSign.setTextColor(Color.parseColor("#2962FF"));
            holder.btnSign.setOnClickListener(v -> callback.onSign(player));
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    private String formatValue(long value) {
        if (value >= 1_000_000) {
            return String.format("€%.0fM", value / 1_000_000.0);
        }
        return String.format("€%.0fK", value / 1_000.0);
    }

    private int getOverallColor(int overall) {
        if (overall >= 88) return Color.parseColor("#FFD700");
        if (overall >= 83) return Color.parseColor("#C0C0C0");
        if (overall >= 78) return Color.parseColor("#CD7F32");
        return Color.parseColor("#2962FF");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textOverall, textPlayerName, textPosition, textTeamName, textMarketValue;
        MaterialButton btnSign;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textOverall = itemView.findViewById(R.id.textOverall);
            textPlayerName = itemView.findViewById(R.id.textPlayerName);
            textPosition = itemView.findViewById(R.id.textPosition);
            textTeamName = itemView.findViewById(R.id.textTeamName);
            textMarketValue = itemView.findViewById(R.id.textMarketValue);
            btnSign = itemView.findViewById(R.id.btnSign);
        }
    }
}
