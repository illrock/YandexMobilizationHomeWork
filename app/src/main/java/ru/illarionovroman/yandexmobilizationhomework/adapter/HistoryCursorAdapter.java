package ru.illarionovroman.yandexmobilizationhomework.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.illarionovroman.yandexmobilizationhomework.R;
import ru.illarionovroman.yandexmobilizationhomework.db.DBManager;
import ru.illarionovroman.yandexmobilizationhomework.model.HistoryItem;


public class HistoryCursorAdapter extends RecyclerView.Adapter<HistoryCursorAdapter.HistoryViewHolder> {

    private Context mContext;
    private Cursor mCursor;

    public HistoryCursorAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.history_list_item, parent,false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        final HistoryItem item = new HistoryItem(mCursor);
        bindHolderItem(holder, item);
    }

    private void bindHolderItem(HistoryViewHolder holder, HistoryItem item) {
        holder.itemView.setTag(item.getId());
        holder.tvOriginalWord.setText(item.getWord());
        holder.tvTranslation.setText(item.getTranslation());
        holder.tvTranslationDirection.setText(item.getLanguageFrom() + " - " + item.getLanguageTo());
        String translationDirection = String.format(
                mContext.getString(R.string.language_from_to),
                item.getLanguageFrom(),
                item.getLanguageTo());
        holder.tvTranslationDirection.setText(translationDirection);

        holder.tbFavorite.setChecked(item.getIsFavorite());
        holder.tbFavorite.setOnClickListener(view -> {
            if (((ToggleButton) view).isChecked()) {
                item.setIsFavorite(true);
            } else {
                item.setIsFavorite(false);
            }
            int updatedCount = DBManager.updateHistoryItem(mContext, item);
            Toast.makeText(mContext, "updatedCount =" + updatedCount, Toast.LENGTH_SHORT).show();
        });
        // TODO: Implement view onClick behaviour (open Translation fragment with passed in _id)
        holder.itemView.setOnClickListener(holderItemView -> {
            Toast.makeText(mContext, "tag = " + holder.itemView.getTag() +
                    ", date = " + item.getDate(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    /**
     * When data changes and a re-query occurs, this function swaps the old Cursor
     * with a newly updated Cursor (Cursor c) that is passed in.
     */
    public Cursor swapCursor(Cursor c) {
        // check if this cursor is the same as the previous cursor (mCursor)
        if (mCursor == c) {
            return null;
        }

        Cursor temp = mCursor;
        this.mCursor = c;

        //check if this is a valid cursor, then update the cursor
        //if (c != null) {
            this.notifyDataSetChanged();
        //}
        return temp;
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tbFavorite)
        ToggleButton tbFavorite;
        @BindView(R.id.tvOriginalWord)
        TextView tvOriginalWord;
        @BindView(R.id.tvTranslation)
        TextView tvTranslation;
        @BindView(R.id.tvTranslationDirection)
        TextView tvTranslationDirection;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}