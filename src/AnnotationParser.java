import java.io.*;
import java.util.HashMap;

/**
 *
 * @author Filipe de Avila Belbute Peres
 */
public class AnnotationParser 
{
    /**
     * Constructs a AnnotationParser object
     * @param inputPath the name of the file to be parsed
     */
    public AnnotationParser(String inputPath, String outputPath, boolean mergeOption)
    {
        //stores the name of the file to be parsed
        this.inputPath = inputPath; 
        this.outputPath = outputPath;
        //creates the dictionary that maps each book to its annotations
        bookContentMap = new HashMap<String,String>();
        //sets merge to false (see fields below for explanation)
        this.mergeOption = mergeOption;
    }
    
    /**
     * Parses the file stored in filename
     * @throws IOException 
     */
    public void parse() 
            throws IOException
    {
        //read information in input file
        read();
        //write information to output file
        write();
    }
    
    /**
     * Returns the number of books parsed.
     * @return the number of books parsed.
     */
    public int numberOfBooks()
    {
        return bookContentMap.size();
    }
    
    /**
     * Reads the input file, storing notes for each book in bookContentMap, a 
     * dictionary that maps books to their annotations.
     * @throws FileNotFoundException 
     * @throws IOException 
     */
    private void read() throws FileNotFoundException, IOException
    {
        //creates input reader from the file path
        BufferedReader input =  new BufferedReader(new FileReader(inputPath));
        try
        {
            String line; 
            //reads the title into line and maps the book title to its annotations
            while ((line = input.readLine()) != null) 
            {
                //removes the first digit, which is invisible and not a part of the title
                if (!line.isEmpty() && !Character.isLetterOrDigit(line.charAt(0)))
                    line = line.substring(1);
                bookContentMap.merge(line, getAnnotation(input), String::concat);
                System.out.println(line + " " + line.hashCode());
            } 
        }
        finally
        {
            //whatever happens, close the input reader
            input.close();
        }
    }
    
    /**
     * Writes the annotations of each book into a separate TXT file. Cleans up
     * the title so that only valid characters are in the file name and adds a 
     * hash code to the file name so that there is no overwriting.
     * @throws IOException 
     */
    private void write() throws IOException
    {
        //creates the output folder
        File outputFolder = new File(outputPath);
        outputFolder.mkdir();
        
        //for each book in the bookContentMap, write a separate file with its annotations
        for (String currentBook : bookContentMap.keySet())
        {
            //gets the title and the author of each book
            String[] titleAuthor = getTitleAuthor(currentBook);
            //creates a clean title, containing alphanumeric and punctuation characters
            String cleanTitle = titleAuthor[TITLE].replaceAll("[^a-zA-Z0-9._ -]+()", "");
            //limits title to a maximum length fo 128 characters 
            //(112 from title + 16 from other chars), to avoid errrors
            cleanTitle = cleanTitle.substring(0, Math.min(cleanTitle.length(), MAX_TITLE_LENGTH));
            //creates full path to output file, adds hash code to avoid duplicates
            String outputName = outputPath + cleanTitle + "_" + currentBook.hashCode() + ".txt";

          
            //creates output writer
            BufferedWriter output = new BufferedWriter(new FileWriter(outputName, mergeOption));
            try
            {
                //writes the output content to the file. first "author, title", then annotations
                output.write(titleAuthor[TITLE] + ", " + titleAuthor[AUTHOR] + NEW_LINE);
                output.write(NEW_LINE);
                output.write(bookContentMap.get(currentBook) + NEW_LINE);
                output.write(NEW_LINE);
            }
            finally
            {
                //whatever happens, close the output writer
                output.close();
            }
        }
    }
    
    /**
     * Reads one annotation entry and returns it. Expects the reader to be 
     * positioned right after the line with the title of the book.
     * @param input the input reader for the annotations file
     * @return a string containing the annotation
     * @throws IOException 
     */
    private String getAnnotation(BufferedReader input) throws IOException
    {
        String annotation = "";
        String currentLine; 
        //while the end of the file is not reached 
        //and the current line is not equal to the annotation break marker
        while((currentLine = input.readLine()) != null && !currentLine.equals("=========="))
            if (currentLine.isEmpty()) //if line is empty
                annotation += currentLine; //do not print new lines for empty lines
            else
                annotation += currentLine + NEW_LINE; //add current line to annotation + new line
        //return annotation
        return annotation + NEW_LINE;
    }
    
    /**
     * Returns the author and the title of the book. Kindle begins each annotation
     * with a line formatted as "title (author)". This method reads this type of 
     * line and returns both fields in a string array.
     * @param unformattedLine the string containing the title and the author in Kindle's annotation formating
     * @return a string array containing the title and the author of the book
     */
    private String[] getTitleAuthor(String unformattedLine)
    {
        String title, author;
        //searches for the last occurrance of "("
        int openParenthesis = unformattedLine.lastIndexOf('(');
        //searches for the last occurrance of ")"
        int closeParenthesis = unformattedLine.lastIndexOf(')');
        //if there is indeed an author (unformattedLine could be just "title", with no author)
        if (openParenthesis > 0 && closeParenthesis > openParenthesis)
        {
            //stores title and author
            title = unformattedLine.substring(0, openParenthesis - 1);
            author = unformattedLine.substring(openParenthesis + 1, closeParenthesis);
        }
        else
        {
            //else, stores just title, with no author
            title = unformattedLine;
            author = "";
        }
        //returns the array with title and author
        return new String[] {title, author};
    }
    
    //the input file's complete path
    private String inputPath;
    //the output directory's complete path
    private String outputPath;
    //a map that maps each book to its annotations
    private HashMap<String,String> bookContentMap;
    //a switch for controling the option of how to handle files that already exist
    //if true, merges annotations to the end of the existing file. if false, replaces
    //file with a new one, only with the current annotations
    private boolean mergeOption;
    
    //variable that stores the system's line separator (new line) character
    private static final String NEW_LINE = System.lineSeparator();
    //the name of the standard output folder
    private static final String STANDARD_OUTPUT_FOLDER = "output";
    //max title length, to avoid errors due to too long filenames
    private static final int MAX_TITLE_LENGTH = 112;
    //the indexes of the title and author elements in the string[] generated by getTitleAuthor()
    private static final int TITLE = 0;
    private static final int AUTHOR = 1;
}