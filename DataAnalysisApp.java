import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class DataAnalysisApp extends JFrame {
    private JComboBox<String> filterCity, filterGender, filterAge, filterDisease;
    private JTable resultTable;
    private JButton generateReport, showChart;
    private List<Map<String, String>> data; // Store loaded data
    private JPanel chartPanel;

    public DataAnalysisApp() {
        setTitle("Análisis de Datos - Colombia");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel: Filters
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridLayout(2, 4));
        
        filterCity = new JComboBox<>();
        filterGender = new JComboBox<>();
        filterAge = new JComboBox<>();
        filterDisease = new JComboBox<>();
        
        filterPanel.add(new JLabel("Ciudad:"));
        filterPanel.add(filterCity);
        filterPanel.add(new JLabel("Género:"));
        filterPanel.add(filterGender);
        filterPanel.add(new JLabel("Edad:"));
        filterPanel.add(filterAge);
        filterPanel.add(new JLabel("Enfermedad:"));
        filterPanel.add(filterDisease);

        add(filterPanel, BorderLayout.NORTH);

        // Center Panel: Results Table
        resultTable = new JTable(new DefaultTableModel(new String[]{"Top", "Ciudad", "Cantidad"}, 0));
        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        // Bottom Panel: Buttons
        JPanel buttonPanel = new JPanel();
        generateReport = new JButton("Generar Reporte CSV");
        showChart = new JButton("Mostrar Gráfica");
        buttonPanel.add(generateReport);
        buttonPanel.add(showChart);
        add(buttonPanel, BorderLayout.SOUTH);

        // Right Panel: Chart
        chartPanel = new JPanel();
        chartPanel.setPreferredSize(new Dimension(400, 300));
        add(chartPanel, BorderLayout.EAST);

        // Load data and initialize filters
        loadData();
        initializeFilters();

        // Event Listeners
        generateReport.addActionListener(e -> generateCSV());
        showChart.addActionListener(e -> showCharts());

        filterCity.addActionListener(e -> filterData());
        filterGender.addActionListener(e -> filterData());
        filterAge.addActionListener(e -> filterData());
        filterDisease.addActionListener(e -> filterData());
    }

    private void loadData() {
        data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("datos.csv"))) {
            String line;
            String[] headers = br.readLine().split(",");
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], values[i]);
                }
                data.add(row);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar el archivo de datos.");
        }
    }

    private void initializeFilters() {
        setFilterOptions(filterCity, "Ciudad");
        setFilterOptions(filterGender, "Género");
        setFilterOptions(filterAge, "Edad");
        setFilterOptions(filterDisease, "Enfermedad");
    }

    private void setFilterOptions(JComboBox<String> comboBox, String key) {
        Set<String> options = data.stream().map(row -> row.get(key)).collect(Collectors.toSet());
        comboBox.removeAllItems();
        comboBox.addItem("Todos");
        options.forEach(comboBox::addItem);
    }

    private void filterData() {
        String city = (String) filterCity.getSelectedItem();
        String gender = (String) filterGender.getSelectedItem();
        String age = (String) filterAge.getSelectedItem();
        String disease = (String) filterDisease.getSelectedItem();

        List<Map<String, String>> filtered = data.stream()
                .filter(row -> city.equals("Todos") || row.get("Ciudad").equals(city))
                .filter(row -> gender.equals("Todos") || row.get("Género").equals(gender))
                .filter(row -> age.equals("Todos") || row.get("Edad").equals(age))
                .filter(row -> disease.equals("Todos") || row.get("Enfermedad").equals(disease))
                .collect(Collectors.toList());

        updateTable(filtered);
    }

    private void updateTable(List<Map<String, String>> filtered) {
        Map<String, Long> counts = filtered.stream()
                .collect(Collectors.groupingBy(row -> row.get("Ciudad"), Collectors.counting()));

        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        model.setRowCount(0);

        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted) {
            model.addRow(new Object[]{rank++, entry.getKey(), entry.getValue()});
        }
    }

    private void generateCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("reporte.csv"))) {
            pw.println("Top,Ciudad,Cantidad");
            for (int i = 0; i < resultTable.getRowCount(); i++) {
                pw.println(resultTable.getValueAt(i, 0) + "," +
                        resultTable.getValueAt(i, 1) + "," +
                        resultTable.getValueAt(i, 2));
            }
            JOptionPane.showMessageDialog(this, "Reporte generado exitosamente.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al generar el reporte.");
        }
    }

    private void showCharts() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            String city = (String) resultTable.getValueAt(i, 1);
            Long count = (Long) resultTable.getValueAt(i, 2);
            dataset.addValue(count, "Ciudades", city);
        }

        JFreeChart barChart = ChartFactory.createBarChart("Top Ciudades", "Ciudad", "Cantidad", dataset);
        JFreeChart pieChart = ChartFactory.createPieChart("Distribución de Ciudades", createPieDataset());

        chartPanel.removeAll();
        ChartPanel chart = new ChartPanel(barChart); // Change to pieChart for Pie Chart
        chartPanel.add(chart);
        chartPanel.revalidate();
    }

    private DefaultPieDataset createPieDataset() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            String city = (String) resultTable.getValueAt(i, 1);
            Long count = (Long) resultTable.getValueAt(i, 2);
            dataset.setValue(city, count);
        }
        return dataset;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DataAnalysisApp().setVisible(true));
    }
}

