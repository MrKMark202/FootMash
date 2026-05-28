package hr.fipu.footmash.ui.ailab.create;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Shared state for the multi-step player creation wizard. Scoped to the
 * MainActivity so all wizard fragments observe the same instance. Callers
 * must invoke {@link #reset()} when entering step 1 to clear any state
 * left over from a previous run.
 *
 * <p>Wizard layout:
 * <ol>
 *     <li>Identity — first name, last name, nationality</li>
 *     <li>Stats + position — 100 points across 6 stats on a 50 baseline</li>
 *     <li>Preview + traits — pick up to 3 traits filtered by position group</li>
 *     <li>Club offers — league pick → 3 OVR-weighted clubs</li>
 *     <li>Signed — confirmation and persistence</li>
 * </ol>
 */
public class PlayerCreationViewModel extends ViewModel {

    // ─── Step 1: identity ────────────────────────────────────────────────────
    private final MutableLiveData<String> firstName   = new MutableLiveData<>("");
    private final MutableLiveData<String> lastName    = new MutableLiveData<>("");
    private final MutableLiveData<String> nationality = new MutableLiveData<>("");

    /** Clears every wizard field. Call from step 1 onViewCreated. */
    public void reset() {
        firstName.setValue("");
        lastName.setValue("");
        nationality.setValue("");
    }

    public LiveData<String> getFirstName()   { return firstName; }
    public LiveData<String> getLastName()    { return lastName; }
    public LiveData<String> getNationality() { return nationality; }

    public void setFirstName(String v)   { firstName.setValue(v == null ? "" : v); }
    public void setLastName(String v)    { lastName.setValue(v == null ? "" : v); }
    public void setNationality(String v) { nationality.setValue(v == null ? "" : v); }

    public boolean isStep1Valid() {
        return notBlank(firstName.getValue())
            && notBlank(lastName.getValue())
            && notBlank(nationality.getValue());
    }

    public String fullName() {
        String first = firstName.getValue() == null ? "" : firstName.getValue().trim();
        String last  = lastName.getValue()  == null ? "" : lastName.getValue().trim();
        if (first.isEmpty()) return last;
        if (last.isEmpty())  return first;
        return first + " " + last;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
