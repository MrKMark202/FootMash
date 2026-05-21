package hr.fipu.footmash.ui.season;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.MatchResult;

public class MatchdayAdapter extends RecyclerView.Adapter<MatchdayAdapter.ViewHolder> {

    private List<MatchdayViewModel.FixtureDisplay> items = new ArrayList<>();
    /** Colour used to highlight the user's club; overridden by the inherited club theme. */
    private int accentColor = 0xFF2962FF;

    public void setItems(List<MatchdayViewModel.FixtureDisplay> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_matchday_fixture, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchdayViewModel.FixtureDisplay d = items.get(position);
        Fixture f = d.fixture;
        MatchResult mr = d.result;

        holder.textHome.setText(f.getHomeTeamName());
        holder.textAway.setText(f.getAwayTeamName());
        holder.textHome.setTextColor(0xCCFFFFFF);
        holder.textAway.setTextColor(0xCCFFFFFF);

        if (mr != null) {
            holder.textScore.setText(mr.getHomeGoals() + " — " + mr.getAwayGoals());
            holder.textScore.setTextColor(Color.WHITE);

            if (f.isUserTeam()) {
                if (f.getHomeTeamId() == 0) holder.textHome.setTextColor(accentColor);
                else holder.textAway.setTextColor(accentColor);
            }

            String[] parts = d.scorersText.split("\\|", -1);
            String homeScorers = parts.length > 0 ? parts[0] : "";
            String awayScorers = parts.length > 1 ? parts[1] : "";

            if (!homeScorers.isEmpty()) {
                holder.textHomeScorers.setVisibility(View.VISIBLE);
                holder.textHomeScorers.setText(homeScorers);
            } else {
                holder.textHomeScorers.setVisibility(View.GONE);
            }

            if (!awayScorers.isEmpty()) {
                holder.textAwayScorers.setVisibility(View.VISIBLE);
                holder.textAwayScorers.setText(awayScorers);
            } else {
                holder.textAwayScorers.setVisibility(View.GONE);
            }
        } else {
            holder.textScore.setText("vs");
            holder.textScore.setTextColor(0x88FFFFFF);
            holder.textHomeScorers.setVisibility(View.GONE);
            holder.textAwayScorers.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textHome, textAway, textScore, textHomeScorers, textAwayScorers;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textHome = itemView.findViewById(R.id.text_home_team);
            textAway = itemView.findViewById(R.id.text_away_team);
            textScore = itemView.findViewById(R.id.text_score);
            textHomeScorers = itemView.findViewById(R.id.text_home_scorers);
            textAwayScorers = itemView.findViewById(R.id.text_away_scorers);
        }
    }
}
