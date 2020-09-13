package net.chetch.captainslog;

import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.chetch.captainslog.data.LogEntries;
import net.chetch.captainslog.data.LogEntry;

public class LogEntriesAdapter extends RecyclerView.Adapter<LogEntriesAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        View contentView;
        LogEntryFragment logEntryFragment;

        public ViewHolder(View v, LogEntryFragment logEntryFragment) {
            super(v);
            contentView = v;
            this.logEntryFragment = logEntryFragment;
        }
    }

    private MainActivity mainActivity;
    public LogEntries entries;
    public boolean flashFirstItemOnBind = false;

    // Provide a suitable constructor (depends on the kind of dataset)
    public LogEntriesAdapter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setDataset(LogEntries entries){
        this.entries = entries;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LogEntriesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LogEntryFragment lef = new LogEntryFragment();
        lef.mainActivity = mainActivity;
        lef.populateOnCreate = false;
        View v = lef.onCreateView(LayoutInflater.from(parent.getContext()), parent, null);

        ViewHolder vh = new ViewHolder(v, lef);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        if(entries != null) {
            LogEntry entry = entries.get(position);
            holder.logEntryFragment.logEntry = entry;
            holder.logEntryFragment.crewMember = mainActivity.model.getCrewMember(entry.getEmployeeID());
            holder.logEntryFragment.populateContent();
            if(position == 0 && flashFirstItemOnBind){
                holder.logEntryFragment.flash();
                flashFirstItemOnBind = false;
            }

            Log.i("LEAdapter","Binding view holder at pos " + position + " for " + holder.logEntryFragment.crewMember.getFullName());
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }
}
