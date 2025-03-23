import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Filipe de Avila Belbute Peres
 */
public class Main 
{
    public static final void main(String args[]) throws IOException
    {
        currentDir = System.getProperty("user.dir") + File.separator;
        inputPath = currentDir + "My Clippings - Kindle.txt";
        outputPath = currentDir + "Parsed Annotations" + File.separator;
        mergeOption = true;
        //sets native OS look and feel
        nativeLookAndFeel();
        createMainFrame();
    }
    
    private static String getFilePath(String titleMessage, boolean isSelectingFiles)
    {
        System.setProperty("apple.awt.fileDialogForDirectories", "true");
        //opens file chooser dialog
        JFileChooser fileChooser = new JFileChooser();
        if (isSelectingFiles == true)
        {
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setCurrentDirectory(new File(inputPath));
        }
        else
        {
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            File outputDir = new File(outputPath);
            fileChooser.setCurrentDirectory(outputDir.getParentFile());
        }
            
        
        fileChooser.setDialogTitle(titleMessage); 
        //gets the value returned by the file chooser dialog
        int returnVal = fileChooser.showOpenDialog(null);
        //if user has chosen a file, return the complete path to that file
        if(returnVal == JFileChooser.APPROVE_OPTION) 
            return fileChooser.getSelectedFile().getPath() + File.separator;
        else //else return empty string
            return "";
    }
    
    private static void createMainFrame()
    {
        JTextArea inputText = new JTextArea(inputPath);
        inputText.setAutoscrolls(true);
        inputText.setLineWrap(true);

        JScrollPane inputTextScroll = new JScrollPane(inputText);
        inputTextScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputTextScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        JButton inputButton = new JButton("Choose file...");
        inputButton.addActionListener(new 
            ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    inputPath = getFilePath("Choose the Kindle annotations text file", true);
                    inputText.setText(inputPath);
                }
            });
        
        JTextArea outputText = new JTextArea(outputPath);
        outputText.setAutoscrolls(true);
        outputText.setLineWrap(true);
        
        JScrollPane outputTextScroll = new JScrollPane(outputText);
        outputTextScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputTextScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        JButton outputButton = new JButton("Choose folder...");
        outputButton.addActionListener(new 
            ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    outputPath = getFilePath("Choose the output folder", false);
                    outputText.setText(outputPath);
                }
            });
        
        inputText.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (inputText.hasFocus())
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        runParser();
            }
         
            public void keyReleased(KeyEvent e)
            {
                if (inputText.hasFocus())
                {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    {
                            String currentText = inputText.getText();
                            inputText.setText(currentText.replace("\n", ""));
                    }
                    if (e.getKeyCode() == KeyEvent.VK_TAB)
                    {
                        String currentText = inputText.getText();
                        inputText.setText(currentText.replace("\t", ""));
                        outputText.requestFocusInWindow();
                        outputText.setCaretPosition(outputText.getText().length());
                    }
                    inputPath = inputText.getText();
                }
            }
        });
        
        outputText.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (outputText.hasFocus())
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        runParser();
            }
            
            public void keyReleased(KeyEvent e)
            {
                if (outputText.hasFocus())
                {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    {
                            String currentText = outputText.getText();
                            outputText.setText(currentText.replace("\n", ""));
                    }
                    if (e.getKeyCode() == KeyEvent.VK_TAB)
                    {
                        String currentText = outputText.getText();
                        outputText.setText(currentText.replace("\t", ""));
                        inputText.requestFocusInWindow();
                        inputText.setCaretPosition(inputText.getText().length());
                    }
                    outputPath = outputText.getText() + File.separator;
                }
            }
        });
                
        JButton runButton = new JButton("Run Parser");
        
        runButton.addActionListener(new 
            ActionListener() 
            {
                public void actionPerformed(ActionEvent e)
                {
                    runParser();
                }
            });
        
        JRadioButton mergeButton = new JRadioButton(new 
            AbstractAction()
            {
                public void actionPerformed(ActionEvent e)
                {
                    mergeOption = true;
                }
            });
        mergeButton.setSelected(true);
        mergeButton.setText("Merge");
        
        JRadioButton overwriteButton = new JRadioButton(new 
            AbstractAction()
            {
                public void actionPerformed(ActionEvent e)
                {
                    mergeOption = false;
                }
            });
        overwriteButton.setText("Overwrite");
        
        dateStampBox = new JCheckBox("Save parse date");
        dateStampBox.setSelected(true);
        
        mergeButton.setToolTipText("Specifies the behavior when a duplicate annotation"
                + " file is found. \n\"Merge\" merges the annotations and \"Overwrite\""
                + " replaces the previously existing files. \nIf parsing annotations from"
                + " two Kindles with different annotations for the same books, merging should "
                + " be a good option.");
        overwriteButton.setToolTipText("Specifies the behavior when a duplicate annotation"
                + " file is found. \n\"Merge\" merges the annotations and \"Overwrite\""
                + " replaces the previously existing files. \nIf parsing annotations from"
                + " two Kindles with different annotations for the same books, merging should "
                + " be a good option.");
        dateStampBox.setToolTipText("Saves the date of the current parse, in case the user wants "
                + "to recall when he last updated his annotations backup. \nFor example, the next time"
                + "the user parses the annotations, he/she will only need to store the annotations"
                + "modified after the last modification date. \nAnnotation dates are available on"
                + "the raw kindle annotation file, which is organized in chronological order.");
        
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(mergeButton);
        buttonGroup.add(overwriteButton);
        
        JFrame mainFrame  = new JFrame("Kindle Annotation Parser");
        mainFrame.setLayout(new BorderLayout(10, 10));
        
//////////////////////////        
////////////////        JPanel topPanel = new JPanel(new BorderLayout());
////////////////        JPanel bottomPanel = new JPanel(new BorderLayout());
////////////////        JPanel holderPanel = new JPanel(new BorderLayout());
////////////////        
////////////////        topPanel.add(new JLabel("Input file: ", JLabel.CENTER), BorderLayout.WEST);
////////////////        topPanel.add(inputTextScroll, BorderLayout.CENTER);
////////////////        topPanel.add(inputButton, BorderLayout.EAST);
////////////////        
////////////////        bottomPanel.add(new JLabel("Output folder: ", JLabel.CENTER), BorderLayout.WEST);
////////////////        bottomPanel.add(outputTextScroll, BorderLayout.CENTER);
////////////////        bottomPanel.add(outputButton, BorderLayout.EAST);
////////////////        
////////////////        holderPanel.add(topPanel, BorderLayout.NORTH);
////////////////        holderPanel.add(bottomPanel, BorderLayout.SOUTH);
////////////////        mainFrame.add(holderPanel, BorderLayout.NORTH);
////////////////////////

        JPanel topPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        mainFrame.add(topPanel, BorderLayout.NORTH);
        
        topPanel.add(new JLabel("Input file: ", JLabel.CENTER));
        topPanel.add(inputTextScroll);
        topPanel.add(inputButton);
        
        topPanel.add(new JLabel("Output folder: ", JLabel.CENTER));
        topPanel.add(outputTextScroll);
        topPanel.add(outputButton);
        
        mainFrame.add(runButton, BorderLayout.CENTER);
        
        JPanel emptySpace = new JPanel();
        emptySpace.setBorder(new EmptyBorder(0,0,0,150));
        mainFrame.add(emptySpace, BorderLayout.EAST);
        JPanel emptySpace2 = new JPanel();
        emptySpace2.setBorder(new EmptyBorder(0,0,0,150));
        mainFrame.add(emptySpace2, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JLabel("Duplicate book handling:"));
        buttonPanel.add(mergeButton);
        buttonPanel.add(overwriteButton);
        buttonPanel.add(dateStampBox);
        mainFrame.add(buttonPanel, BorderLayout.SOUTH);
        
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setSize(800, 300);
        mainFrame.setLocationRelativeTo(null);
    }
    
    private static void runParser()
    {
        String errorString = "File not found.";
        try
        {
            System.out.println(outputPath);
            if (!inputPath.equals("") && !outputPath.equals(""))
            {
                //creates and runs the annotation parser
                AnnotationParser parser = new AnnotationParser(inputPath, outputPath, mergeOption);
                parser.parse();
                //informs the user of how many books have been parsed,
                //allows the user to check if any overwriting due to duplicates has happened
                JOptionPane.showMessageDialog(null, "Parsing complete.\n"
                    + "The program should have created " + parser.numberOfBooks() + " annotation entries.");
                if (dateStampBox.isSelected())
                {
                    errorString = "Could not create date stamp file.";
                    saveDateStamp();
                }
            }
            else
                JOptionPane.showMessageDialog(null, "Please select input and output folders.");
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(null, errorString);
        }
    }
    
    /**
     * Saves the date stamp to an external file.
     */
    private static void saveDateStamp() throws IOException
    {
        //write to the file, with append on, so that some previously saved date is not overwriten
        FileWriter output = new FileWriter("LastParseDate.txt", true);
        output.write((new Date()).toString() + System.lineSeparator());
        output.close();
    }
    
    /**
     * Set the look and feel to match the native OS
     */
    private static void nativeLookAndFeel()
    {
        try
        {
            //tries to set the look and feel to match the native OS
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e)
        {
            //é ok não fazer nada?
        } 
    }
    
    private static String currentDir;
    private static String inputPath;
    private static String outputPath;
    private static boolean mergeOption;
    private static JCheckBox dateStampBox;
}
