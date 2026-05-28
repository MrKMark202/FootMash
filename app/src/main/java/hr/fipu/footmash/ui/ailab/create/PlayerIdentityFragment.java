package hr.fipu.footmash.ui.ailab.create;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import hr.fipu.footmash.databinding.FragmentPlayerIdentityBinding;

/**
 * Step 1 of the player creation wizard: first name, last name, nationality.
 *
 * <p>Owns the wizard reset — this is the entry point, so we wipe any leftover
 * state from a previous run before binding fields. The shared ViewModel is
 * scoped to the Activity so all wizard fragments observe the same instance.
 */
public class PlayerIdentityFragment extends Fragment {

    private FragmentPlayerIdentityBinding binding;
    private PlayerCreationViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerIdentityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PlayerCreationViewModel.class);

        // Entry point: wipe any leftover state from a previous wizard run.
        if (savedInstanceState == null) viewModel.reset();

        bindNationalityDropdown();
        bindTextWatchers();
        prefillFromViewModel();
        refreshNextButton();

        // Navigation to step 2 is wired in the next commit; the button itself
        // already gates on isStep1Valid so once enabled it means the form is good.
        binding.btnNext.setOnClickListener(v -> { /* wired in commit 2 */ });
    }

    private void bindNationalityDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            Nationalities.ALL);
        binding.autoCompleteNationality.setAdapter(adapter);
        binding.autoCompleteNationality.setOnItemClickListener((parent, v, pos, id) -> {
            viewModel.setNationality((String) parent.getItemAtPosition(pos));
            refreshNextButton();
        });
    }

    private void bindTextWatchers() {
        binding.editFirstName.addTextChangedListener(new SimpleWatcher() {
            @Override public void onChanged(String s) {
                viewModel.setFirstName(s);
                refreshNextButton();
            }
        });
        binding.editLastName.addTextChangedListener(new SimpleWatcher() {
            @Override public void onChanged(String s) {
                viewModel.setLastName(s);
                refreshNextButton();
            }
        });
    }

    /** Repopulate when returning to this screen (e.g. back from step 2). */
    private void prefillFromViewModel() {
        String first = viewModel.getFirstName().getValue();
        String last  = viewModel.getLastName().getValue();
        String nat   = viewModel.getNationality().getValue();
        if (first != null && !first.isEmpty()) binding.editFirstName.setText(first);
        if (last  != null && !last.isEmpty())  binding.editLastName.setText(last);
        if (nat   != null && !nat.isEmpty())   binding.autoCompleteNationality.setText(nat, false);
    }

    private void refreshNextButton() {
        binding.btnNext.setEnabled(viewModel.isStep1Valid());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Adapter that fires only on text changes, sparing 3 boilerplate methods per binding. */
    private abstract static class SimpleWatcher implements TextWatcher {
        public abstract void onChanged(String s);
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
            onChanged(s == null ? "" : s.toString());
        }
    }
}
