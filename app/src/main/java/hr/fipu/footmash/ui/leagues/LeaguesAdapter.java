package hr.fipu.footmash.ui.leagues;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.ItemLeagueBinding;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.ui.util.InitialsBadgeDrawable;
import com.bumptech.glide.Glide;

public class LeaguesAdapter extends RecyclerView.Adapter<LeaguesAdapter.LeagueViewHolder> {

    private List<LeagueResponse> leagues = new ArrayList<>();
    private final OnLeagueClickListener listener;
    private int expandedPosition = RecyclerView.NO_POSITION;

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

        public void bind(LeagueResponse league) {
            binding.textLeagueName.setText(league.getLeagueName());

            String tagline = league.getTagline();
            binding.textLeagueType.setText(
                    tagline != null && !tagline.isEmpty() ? tagline : league.getCountryName());

            if (league.getLeagueLogo() != null && !league.getLeagueLogo().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                     .load(league.getLeagueLogo())
                     .placeholder(new InitialsBadgeDrawable(league.getLeagueName()))
                     .into(binding.imageLeagueLogo);
            } else {
                binding.imageLeagueLogo.setImageDrawable(new InitialsBadgeDrawable(league.getLeagueName()));
            }

            boolean expanded = getBindingAdapterPosition() == expandedPosition;
            binding.detailSection.setVisibility(expanded ? View.VISIBLE : View.GONE);
            binding.iconExpand.setRotation(expanded ? 180f : 0f);

            if (expanded) {
                binding.textSummary.setText(league.getSummary());
                binding.textLegends.setText(league.getLegends());
                bindClubs(league.getClubs());
            }

            binding.headerRow.setOnClickListener(v -> toggle());
            binding.buttonOpenLeague.setOnClickListener(v -> {
                if (listener != null) listener.onLeagueClick(league);
            });
        }

        private void toggle() {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            int previous = expandedPosition;
            expandedPosition = (pos == expandedPosition) ? RecyclerView.NO_POSITION : pos;
            if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
            if (expandedPosition != RecyclerView.NO_POSITION) notifyItemChanged(expandedPosition);
        }

        private void bindClubs(List<RealTeam> clubs) {
            LinearLayout row = binding.clubsRow;
            row.removeAllViews();
            boolean hasClubs = clubs != null && !clubs.isEmpty();
            row.setVisibility(hasClubs ? View.VISIBLE : View.GONE);
            binding.textClubsLabel.setVisibility(hasClubs ? View.VISIBLE : View.GONE);
            if (!hasClubs) return;

            LayoutInflater inflater = LayoutInflater.from(row.getContext());
            for (RealTeam club : clubs) {
                View item = inflater.inflate(R.layout.item_league_club, row, false);
                ImageView badge = item.findViewById(R.id.imageClubBadge);
                TextView name = item.findViewById(R.id.textClubName);
                name.setText(club.getName());

                String url = club.getBadgeUrl();
                if (url != null && !url.isEmpty()) {
                    Glide.with(badge.getContext())
                         .load(url)
                         .placeholder(new InitialsBadgeDrawable(club.getName()))
                         .into(badge);
                } else {
                    badge.setImageDrawable(new InitialsBadgeDrawable(club.getName()));
                }
                row.addView(item);
            }
        }
    }
}
