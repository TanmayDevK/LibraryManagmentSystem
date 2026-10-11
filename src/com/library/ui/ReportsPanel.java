package com.library.ui;

import com.library.db.BookDAO;
import com.library.db.TransactionDAO;
import com.library.model.Book;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Inventory report: two charts plus a text summary built with List, Map and Set. */
public class ReportsPanel extends JPanel {

    private final BarChartPanel genreChart = new BarChartPanel("Copies by genre");
    private final BarChartPanel monthChart = new BarChartPanel("Books borrowed per month");
    private final JTextArea textArea = new JTextArea();

    public ReportsPanel() {
        setLayout(new BorderLayout(8, 8));

        JPanel charts = new JPanel(new GridLayout(1, 2, 8, 8));
        charts.add(genreChart);
        charts.add(monthChart);
        add(charts, BorderLayout.NORTH);

        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Report");
        JPanel actions = new JPanel();
        actions.add(refreshBtn);
        add(actions, BorderLayout.SOUTH);
        refreshBtn.addActionListener(e -> refresh());
    }

    public void refresh() {
        try {
            List<Book> books = new BookDAO().getAll();
            books.sort(Comparator.comparing(Book::getTitle));

            Map<String, Integer> copiesByGenre = new TreeMap<>(); // genre -> total copies
            Set<String> authors = new TreeSet<>();                // unique authors, sorted
            int totalCopies = 0;
            int availableCopies = 0;

            for (Book b : books) {
                String genre = (b.getGenre() == null || b.getGenre().isBlank()) ? "Other" : b.getGenre();
                copiesByGenre.merge(genre, b.getTotalCopies(), Integer::sum);
                authors.add(b.getAuthor());
                totalCopies += b.getTotalCopies();
                availableCopies += b.getAvailableCopies();
            }

            TransactionDAO transactions = new TransactionDAO();
            Map<String, Integer> perMonth = transactions.getLoansPerMonth(6);
            Map<String, Integer> topBooks = transactions.getTopBorrowedBooks(5);

            genreChart.setData(copiesByGenre);
            monthChart.setData(perMonth);

            StringBuilder sb = new StringBuilder();
            sb.append("INVENTORY SUMMARY\n=================\n");
            sb.append("Book titles      : ").append(books.size()).append('\n');
            sb.append("Total copies     : ").append(totalCopies).append('\n');
            sb.append("Copies available : ").append(availableCopies).append('\n');
            sb.append("Copies on loan   : ").append(totalCopies - availableCopies).append("\n\n");

            sb.append("MOST BORROWED BOOKS\n-------------------\n");
            if (topBooks.isEmpty()) {
                sb.append("No books have been borrowed yet.\n");
            }
            for (Map.Entry<String, Integer> entry : topBooks.entrySet()) {
                sb.append(String.format("%-35s %d%n", entry.getKey(), entry.getValue()));
            }

            sb.append("\nAUTHORS (").append(authors.size()).append(")\n-------------------\n");
            for (String author : authors) {
                sb.append(author).append('\n');
            }
            textArea.setText(sb.toString());
            textArea.setCaretPosition(0);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
