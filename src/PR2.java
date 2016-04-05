import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;
import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

/**
 * Homework 8: An applet that displays a CSV file. There are two views, table
 * view (in the applet) and graphics view (HTML5 Canvas).
 *
 * @author Denis Gracanin
 * @author Sheng Zhou (zsheng2)
 * @version 1
 */
public class PR2
    extends JApplet
    implements ActionListener, ListSelectionListener, TableModelListener
{
    private static final long   serialVersionUID    = 1L;
    private final static String TITLE               = "PR2: zsheng2";
    private final static String HELP                = "Project2 version 1.";
    private static int          VERTEX_DATA_PADDING = 28;
    private JMenuBar            menuBar             = null;
    private JMenu               fileMenu            = null;
    private JMenu               helpMenu            = null;
    private JMenuItem           openMenuItem        = null;
    private JMenuItem           closeMenuItem       = null;
    private JMenuItem           saveMenuItem        = null;
    private JMenuItem           quitMenuItem        = null;
    private JMenuItem           aboutMenuItem       = null;

    /**
     * Current title.
     */
    private String              title               = null;

    /**
     * Current file.
     */
    private File                file                = null;

    /**
     * The model.
     */
    private PR2Model            model               = null;

    /**
     * The table view.
     */
    private JTable              table               = null;

    /**
     * The graph view.
     */
    private JSObject            window              = null;


    /**
     * Initializes <code>HW8</code> applet.
     *
     * @param title
     *            The title of the application window.
     */
    public void init()
    {
        title = TITLE;
        menuBar = new JMenuBar();

        // File menu
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        // File->Open menu item
        openMenuItem = new JMenuItem("Open (Alt-F O)");
        openMenuItem.addActionListener(this);
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.setActionCommand("O");
        fileMenu.add(openMenuItem);

        // File->Close menu item
        closeMenuItem = new JMenuItem("Close (Alt-F W)");
        closeMenuItem.addActionListener(this);
        closeMenuItem.setMnemonic(KeyEvent.VK_W);
        closeMenuItem.setActionCommand("W");
        fileMenu.add(closeMenuItem);

        // File->Save menu item
        saveMenuItem = new JMenuItem("Save (Alt-F S)");
        saveMenuItem.addActionListener(this);
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.setActionCommand("S");
        fileMenu.add(saveMenuItem);

        // File->Quit menu item
        quitMenuItem = new JMenuItem("Quit (Alt-F Q)");
        quitMenuItem.addActionListener(this);
        quitMenuItem.setMnemonic(KeyEvent.VK_Q);
        quitMenuItem.setActionCommand("Q");
        fileMenu.add(quitMenuItem);

        menuBar.add(fileMenu);

        // Help menu
        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        // Help->About menu item
        aboutMenuItem = new JMenuItem("About (Alt-F A)");
        aboutMenuItem.addActionListener(this);
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.setActionCommand("A");
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        // Create the model.
        model = new PR2Model();

        // Create the table view (table).
        table = new JTable(model);

        // Configure the color scheme.
        table.setGridColor(Color.BLUE);
        table.getTableHeader().setBackground(Color.YELLOW);
        table.setSelectionBackground(Color.RED);

        // Configure the selection model.
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // HW3 (controller) listens to the list selection events (cell
// selection) generated by the table view.
        // the column has changed (the column of the selected cell).
        table.getColumnModel().getSelectionModel()
            .addListSelectionListener(this);
        // the row has changed (the row of the selected cell).
        table.getSelectionModel().addListSelectionListener(this);

        setLayout(new BorderLayout());

        // Add the table view to the frame (within a JScrollPane object).
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Data updates are indirectly sent to the graph view in the WebGL
// canvas.
        model.addTableModelListener(this);
        model.setPadding(28);
    }


    /**
     * Starts the applet.
     */
    public void start()
    {
        try
        {
            window = JSObject.getWindow(this);
        }
        catch (JSException jse)
        {
            jse.printStackTrace();
        }
        setTitle(TITLE);
    }


    /**
     * Process the menu item selections.
     *
     * @param e
     *            Action event.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source instanceof JMenuItem)
        {
            JMenuItem menuItem = (JMenuItem)source;
            switch (menuItem.getActionCommand())
            {

                case "O":
                    // Open a file (select using a JFileChooser object)
                    JFileChooser fileChooser = new JFileChooser();
                    int returnVal = fileChooser.showOpenDialog(this);
                    if (returnVal == JFileChooser.APPROVE_OPTION)
                    {
                        file = fileChooser.getSelectedFile();
                        try
                        {

                            // Create input stream (a BufferdReader object) from
// the file.
                            BufferedReader inputStream =
                                new BufferedReader(new FileReader(file));

                            // Read the first line to get the column names.
                            String line = null;
                            Vector<String> columnNames = new Vector<String>();
                            if ((line = inputStream.readLine()) != null)
                            {
                                Scanner scanner = new Scanner(line);
                                scanner.useDelimiter(",");
                                while (scanner.hasNext())
                                {
                                    columnNames.add(scanner.next());
                                }
                                scanner.close();
                            }

                            // Read the remaining lines to get the data.
                            Vector<Vector<String>> rowData =
                                new Vector<Vector<String>>();
                            while ((line = inputStream.readLine()) != null)
                            {
                                Vector<String> tempVector =
                                    new Vector<String>();
                                Scanner scanner = new Scanner(line);
                                scanner.useDelimiter(",");
                                while (scanner.hasNext())
                                {
                                    tempVector.add(scanner.next());
                                }
                                scanner.close();
                                if (columnNames.size() != tempVector.size())
                                {
                                    System.out.println("Inconsistent data!");
                                    return;
                                }
                                rowData.add(tempVector);
                            }

                            // Close the input stream.
                            inputStream.close();

                            // Store the data in the model.
                            model.setDataVector(rowData, columnNames);
                            setTitle(title + ": " + file.getName());
                        }
                        catch (IOException ex)
                        {
                            System.err.println(ex);
                        }
                    }
                    break;

                case "W":
                    // CLose the model (erase the model data).
                    file = null;
                    model.close();
                    setTitle(title);
                    break;

                case "S":
                    // Save the file.
                    try
                    {
                        // Create output stream (a BufferdReader object) from
// the file.
                        BufferedWriter outputStream =
                            new BufferedWriter(new FileWriter(file));

                        // Write the first line to store the column names.
                        int columnCount = model.getColumnCount();
                        if (columnCount > 0)
                        {
                            for (int i = 0; i < columnCount - 1; i++)
                            {
                                outputStream
                                    .write(model.getColumnName(i) + ",");
                            }
                            outputStream.write(model
                                .getColumnName(columnCount - 1) + "\n");
                        }
                        // Write data row by row.
                        @SuppressWarnings("unchecked")
                        Vector<Vector<String>> data =
                            (Vector<Vector<String>>)model.getDataVector();
                        for (int i = 0; i < data.size(); i++)
                        {
                            Vector<String> row = (Vector<String>)data.get(i);
                            columnCount = row.size();
                            if (columnCount > 0)
                            {
                                for (int j = 0; j < columnCount - 1; j++)
                                {
                                    outputStream.write(row.get(j) + ",");
                                }
                                outputStream.write(row.get(columnCount - 1)
                                    + "\n");
                            }
                        }
                        outputStream.close();
                    }
                    catch (IOException ex)
                    {
                        System.err.println(ex);
                    }
                    break;

                case "Q":
                    // Exit the application.
                    System.exit(0);
                    break;

                case "A":
                    // Show the about dialog.
                    JOptionPane.showMessageDialog(this, HELP);
                    break;
            }
        }
    }


    /**
     * Processes the list selection event (cell selection from the table view)
     * and updates the graph view selection.
     *
     * @param e
     *            List selection event
     */
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        int c = -1; // selected column
        int r = -1; // selected row
        if (!e.getValueIsAdjusting())
        {

            // If the selected column has changed, update the value of c.
            if (table.getSelectedColumns().length > 0)
            {
                c = table.getSelectedColumns()[0];
            }

            // If the selected row has changed, update the value of r.
            if (table.getSelectedRows().length > 0)
            {
                r = table.getSelectedRows()[0];
            }

            // Check if previous setValueAt attempt failed and select that cell.
            if (model.isError())
            {
                c = model.getOldColumn();
                r = model.getOldRow();
                model.setError(false);
                table.setColumnSelectionInterval(c, c);
                table.getSelectionModel().setSelectionInterval(r, r);
            }

            // Use the new values of c and r to update the graph view.
            if (window != null)
            {
                window.eval("draw(" + c + ", " + r + ")");
            }
        }
    }


    /**
     * Processes the change event (from the plot canvas) and updates the table
     * view selection.
     *
     * @param e
     *            Change event
     */
    public void stateChanged(int v)
    {
        // Sets the selected table row based on the point selection in the graph
// view.
        table.getSelectionModel().setSelectionInterval(v, v);
    }


    /**
     * Processes the table update event and updates the plot canvas data.
     *
     * @param e
     *            Table model event.
     */
    @Override
    public void tableChanged(TableModelEvent e)
    {
        if (window == null)
        {
            return;
        }
        PR2Model model = (PR2Model)e.getSource();
        model.setPadding(VERTEX_DATA_PADDING);
        float[] vertexData = model.getVertexData();
        String sData = "";
        String minData = "";
        String maxData = "";
        for (int i = 0; i < vertexData.length; i++)
        {
            sData += (i == 0 ? "" : ", ") + vertexData[i];
        }
        for (int i = 0; i < model.getPlotCount(); i++)
        {
            minData += (i == 0 ? "" : ", ") + model.getMinValue(i);
            maxData += (i == 0 ? "" : ", ") + model.getMaxValue(i);
        }
        int c = -1;
        if (table.getSelectedColumns().length > 0)
        {
            c = table.getSelectedColumns()[0];
        }
        int r = -1;
        if (table.getSelectedRows().length > 0)
        {
            r = table.getSelectedRows()[0];
        }
        String minH = "";
        String maxH = "";
        window.eval("tableChanged(" + model.getPlotSize() + ", "
            + model.getPlotCount() + ", " + c + ", " + r
            + ", new Float32Array([" + sData + "]), new Float32Array(["
            + minData + "]), new Float32Array([" +
            maxData + "]), new Float32Array([" + minH + "]), new Float32Array(["
            + maxH + "]))");
    }

    /**
     * Sets the window title.
     *
     * @param t
     *            Window title.
     */
    public void setTitle(String t)
    {
        if (window != null)
        {
            window.eval("document.title=\"" + t + "\";");
        }
    }

    // ----------------------------------------------------------
    /**
     * Place a description of your method here.
     * @param plotSelection
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     * @param binNo
     */
    public void setHCurrent(
        int plotSelection,
        float maxY,
        float minY,
        float maxX,
        float minX,
        int binNo)
    {
        model.setHistogramCurrent(plotSelection, minX, maxX, minY, maxY, binNo);
    }

    // ----------------------------------------------------------
    /**
     * Place a description of your method here.
     * @param plotSelection
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    public void setCurrent(
        int plotSelection,
        float minX,
        float maxX,
        float minY,
        float maxY)
    {
        model.setCurrent(plotSelection, minX, maxX, minY, maxY);
    }

    //HISTOGRAM
    public float getHMinX(int i)
    {
        return model.getHLeft(i);
    }


    public float getHMaxX(int i)
    {
        return model.getHRight(i);
    }


    public float getHMinY(int i)
    {
        return model.getHBottom(i);
    }

    public float getBinNo(int i)
    {
        return model.getHCount(i);
    }

    public void getHistData(int plotSelection)
    {
        String strData = "";
        float[] str =
            model.getHistogramData(
                plotSelection,
                model.getHCount(plotSelection),
                model.getHBottom(plotSelection),
                model.getHTop(plotSelection));
        for (int i = 0; i < str.length; i++)
        {
            strData += (i == 0 ? "" : ", ") + str[i];
        }
        window.eval("setHistogram(new Float32Array([" + strData + "]))");
    }

    public int getBin(int p1, int p2, int p3, float min, float max){
        return model.getPointBin(p1, p2, p3, min, max);
    }

    public float getHMaxY(int i)
    {
        return model.getHTop(i);
    }

    //PLOT
    public float getMinX(int i)
    {
        return model.getLeft(i);
    }


    public float getMaxX(int i)
    {
        return model.getRight(i);
    }


    public float getMinY(int i)
    {
        return model.getBottom(i);
    }


    public float getMaxY(int i)
    {
        return model.getTop(i);
    }

}
