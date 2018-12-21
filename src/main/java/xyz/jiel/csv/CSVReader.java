package xyz.jiel.csv;

import java.io.*;
import java.util.Iterator;
import java.util.*;

import xyz.jiel.exceptions.*;
import xyz.jiel.exceptions.IOError;
import xyz.jiel.tools.P;

/**
 * A slight csv reader, support foreach loop, return an {@code String} array in each loop.
 */
public class CSVReader implements Iterable<String[]> {
    private char delimiter = ',';
    private char quoteStr = '\"';
    private char escapeStr = '\"';

    /** the number of line that be to skipped */
    private int skipRowNum = 0;

    private BufferedReader reader;
    private Iterator<String[]> iterator;

    public CSVReader(InputStream is) {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    public CSVReader(File file) {
        this(file, "UTF8");
    }

    public CSVReader(File file, String charsetName) {
        try {
            InputStream is = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is, charsetName));
        } catch (FileNotFoundException e) {
            throw new FileNotFoundError(String.format(
                    "Cann't open file: %s", file.getPath()));
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedEncodingError(e.getMessage());
        }
    }


    public CSVReader(String path) {
        this(path, "UTF8");
    }

    public CSVReader(String path, String charsetName) {
        this(new File(path), charsetName);
    }


    public char delimiter() {
        return delimiter;
    }

    public CSVReader delimiter(char c) {
        delimiter = c;
        return this;
    }

    public int skip() {
        return skipRowNum;
    }

    /** Set the number of the line that be skipped */
    public CSVReader skip(int i) {
        skipRowNum = i;
        return this;
    }

    @Override
    public Iterator<String[]> iterator() {
        if (iterator == null)
            iterator = new ArrayReader();
        return iterator;
    }

    private class ArrayReader extends Reader<String[]> {
        private String nextLine;

        ArrayReader() {
            super(CSVReader.this.delimiter, CSVReader.this.quoteStr, CSVReader.this.escapeStr);
            try {
                /* skip the first skipRowNum line */
                for (int i = 0; i < skipRowNum; i++) {
                    if (reader.readLine() == null) {
                        break;
                    }
                }
                /* read one line in advance for judging whether has next line*/
                nextLine = reader.readLine();
            } catch (IOException e) {
                throw new IOError(e.getMessage());
            }
        }

        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        @Override
        public String[] next() {
            if (nextLine == null)
                throw new NoSuchElementException();

            String[] ret = split(nextLine);

            try {
                nextLine = reader.readLine();
            } catch (IOException e) {
                throw new IOError(e.getMessage());
            }

            return ret;
        }


        @Override
        public void remove() {

        }
    }

    public static void main(String...args) {
        CSVReader reader = new CSVReader("C:/Users/jiel/Desktop/a.csv");
        for(String[] row : reader) {
            P.printer().print(row.length);
        }
    }

}