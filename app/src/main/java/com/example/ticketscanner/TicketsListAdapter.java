package com.example.ticketscanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ticketscanner.R.color;

import java.util.List;

public class TicketsListAdapter extends ArrayAdapter<List<String>> {

    private final List<List<String>> tickets;

    public TicketsListAdapter(@NonNull Context context, int resource, @NonNull List<List<String>> tickets) {
        super(context, resource, tickets);
        this.tickets = tickets;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_ticket, parent, false);
        }

        // Get the ticket list for this position
        List<String> ticket = tickets.get(position);

        // Extract ticket details
        String ticketInfo = "Row: " + ticket.get(1)+" | Column: "+ticket.get(2)+ " | TicketNumber: "+ticket.get(3);
        int status = Integer.parseInt(ticket.get(4)); // Assuming the status is the fifth element

        // Display ticket information
        TextView textViewTicketInfo = convertView.findViewById(R.id.textViewTicketInfo);
        textViewTicketInfo.setText(ticketInfo);

        // Display button based on status
        Button buttonTicketStatus = convertView.findViewById(R.id.buttonTicketStatus);
        if (status == 1) {
            buttonTicketStatus.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.red));
            buttonTicketStatus.setText("סרוק");
        } else {
            buttonTicketStatus.setBackgroundColor(ContextCompat.getColor(this.getContext(), color.green));
            buttonTicketStatus.setText("פתוח");
        }
        buttonTicketStatus.setOnClickListener(v -> {
            // Show a toast with ticket.get(5)
            ((TicketsDetailsActivity) getContext()).changeIt(ticket.get(5), buttonTicketStatus);
        });

        return convertView;
    }
}

