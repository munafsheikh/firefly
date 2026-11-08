package demo.review;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Demo for the AI review gate: a plausible data-access class with deliberate defects.
 * Not part of any Maven module; reverted after the review is captured.
 */
public class InvoiceRepository {

    private final Connection connection;
    private List<String> cache = new ArrayList<>();

    public InvoiceRepository(Connection connection) {
        this.connection = connection;
    }

    public List<String> findByCustomer(String customerName) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT id FROM invoices WHERE customer = '" + customerName + "'");
        List<String> ids = new ArrayList<>();
        while (rs.next()) {
            ids.add(rs.getString("id"));
        }
        return ids;
    }

    public Properties loadSettings(String path) {
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(path);
            props.load(in);
        } catch (IOException e) {}
        return props;
    }

    public boolean isOverdue(String status) {
        return status == "OVERDUE";
    }

    public void remember(String id) {
        // Called from request threads.
        cache.add(id);
    }
}
