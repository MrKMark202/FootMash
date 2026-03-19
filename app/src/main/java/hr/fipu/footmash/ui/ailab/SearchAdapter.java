package hr.fipu.footmash.ui.ailab;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.R;

public class SearchAdapter<T> extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    private List<T> items = new ArrayList<>();
    private final OnItemClickListener<T> listener;
    private final ItemFormatter<T> formatter;

    public interface ItemFormatter<T> {
        String format(T item);
    }

    public SearchAdapter(OnItemClickListener<T> listener, ItemFormatter<T> formatter) {
        this.listener = listener;
        this.formatter = formatter;
    }

    public void setItems(List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        T item = items.get(position);
        holder.text1.setText(formatter.format(item));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView text1;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            // Postavljamo boju teksta jer je pozadina tamna
            text1.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
        }
    }
}
