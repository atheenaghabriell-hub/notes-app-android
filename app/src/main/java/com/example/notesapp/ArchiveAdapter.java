package com.example.notesapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder> {

    private final List<Note> archiveList;
    private final OnArchiveActionListener actionListener;

    public interface OnArchiveActionListener {
        void onRestore(Note note);
        void onDeletePermanent(Note note);
    }

    public ArchiveAdapter(List<Note> archiveList, OnArchiveActionListener actionListener) {
        this.archiveList = archiveList;
        this.actionListener = actionListener;
    }

    public void setNotes(List<Note> notes) {
        archiveList.clear();
        archiveList.addAll(notes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArchiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archive, parent, false);
        return new ArchiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchiveViewHolder holder, int position) {
        Note note = archiveList.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        holder.btnRestore.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRestore(note);
            }
        });

        holder.btnDeletePermanent.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeletePermanent(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return archiveList.size();
    }

    public static class ArchiveViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvContent;
        final Button btnRestore, btnDeletePermanent;

        public ArchiveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvArchiveTitle);
            tvContent = itemView.findViewById(R.id.tvArchiveContent);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDeletePermanent = itemView.findViewById(R.id.btnDeletePermanent);
        }
    }
}