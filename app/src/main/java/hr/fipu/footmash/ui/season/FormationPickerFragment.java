package hr.fipu.footmash.ui.season;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.R;
import hr.fipu.footmash.databinding.FragmentFormationPickerBinding;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.season.FormationCatalog;

public class FormationPickerFragment extends Fragment {

    private FragmentFormationPickerBinding binding;
    private FormationAdapter adapter;
    private String selected = null;
    private int clubId = -1;

    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
    static {
        DESCRIPTIONS.put("4-4-2", "Klasično, balansirano");
        DESCRIPTIONS.put("4-3-3", "Napadački, krila");
        DESCRIPTIONS.put("3-5-2", "Mid-dominacija");
        DESCRIPTIONS.put("4-2-3-1", "Moderni 10-ka");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFormationPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clubId = getArguments() != null ? getArguments().getInt("clubId", -1) : -1;
        if (clubId == -1) {
            Toast.makeText(requireContext(), "Greška: klub nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new FormationAdapter(FormationCatalog.names(), name -> {
            selected = name;
            adapter.setSelected(name);
            binding.btnContinue.setEnabled(true);
            binding.btnContinue.setAlpha(1.0f);
        });
        binding.recyclerFormations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFormations.setAdapter(adapter);

        binding.btnContinue.setOnClickListener(v -> {
            if (selected == null) return;
            binding.btnContinue.setEnabled(false);
            saveAndContinue(view);
        });
    }

    private void saveAndContinue(View navView) {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            UserClub club = db.userClubDao().getClubByIdSync(clubId);
            if (club != null) {
                club.setFormation(selected);
                db.userClubDao().updateClub(club);
            }
            requireActivity().runOnUiThread(() -> {
                Bundle args = new Bundle();
                args.putInt("clubId", clubId);
                Navigation.findNavController(navView)
                    .navigate(R.id.action_formationPicker_to_draftCanvas, args);
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- adapter ---

    interface OnFormationSelected {
        void onSelected(String name);
    }

    static class FormationAdapter extends RecyclerView.Adapter<FormationAdapter.VH> {
        private final List<String> names;
        private final OnFormationSelected listener;
        private String selectedName;

        FormationAdapter(List<String> names, OnFormationSelected listener) {
            this.names = names;
            this.listener = listener;
        }

        void setSelected(String name) {
            this.selectedName = name;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_formation_pick, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String name = names.get(position);
            holder.name.setText(name);
            String desc = DESCRIPTIONS.get(name);
            holder.desc.setText(desc != null ? desc : "");

            boolean isSelected = name.equals(selectedName);
            holder.card.setStrokeWidth(isSelected ? dp(holder.itemView, 2) : 0);
            holder.card.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.accent_blue));

            holder.itemView.setOnClickListener(v -> listener.onSelected(name));
        }

        @Override
        public int getItemCount() { return names.size(); }

        static int dp(View v, int dp) {
            return (int) (dp * v.getResources().getDisplayMetrics().density);
        }

        static class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView name, desc;
            VH(@NonNull View v) {
                super(v);
                card = (MaterialCardView) v;
                name = v.findViewById(R.id.textFormationName);
                desc = v.findViewById(R.id.textFormationDesc);
            }
        }
    }
}
