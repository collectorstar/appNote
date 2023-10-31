package com.example.appnote.listeners;

import com.example.appnote.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
