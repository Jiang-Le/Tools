package xyz.jiel.csv;

import xyz.jiel.exceptions.FileNotFoundError;
import xyz.jiel.exceptions.IOError;
import xyz.jiel.exceptions.UnsupportedEncodingError;

import java.io.*;
import java.util.*;

/**
 * A slight csv reader, support foreach loop, return an {@code Map} in each loop.
 * An array of header used as the key of the {@code Map}, if does not provided,
 * read the first unskipped line as headers.
 */
public class CSVMapReader implements Iterable<Map<String, String>> {
    private char delimiter = ',';
    private char quoteChar = '\"';
    private char escapeChar = '\"';

    private List<String> headers = null;

    /** the number of line that be to skipped */
    private int skipRowNum = 0;

    private BufferedReader reader;
    private Iterator<Map<String, String>> iterator;

    public CSVMapReader(InputStream is) {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    public CSVMapReader(File file) {
        this(file, "UTF8");
    }

    public CSVMapReader(File file, String charsetName) {
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

    public CSVMapReader(String path) {
        this(path, "UTF8");
    }

    public CSVMapReader(String path, String charsetName) {
        this(new File(path), charsetName);
    }

    /** Set the number of the line that be skipped */
    public int skip() {
        return skipRowNum;
    }

    public CSVMapReader skip(int i) {
        skipRowNum = i;
        return this;
    }

    public List<String> headers() {
        return headers;
    }

    public CSVMapReader headers(List<String> h) {
        headers = h;
        return this;
    }

    public char delimiter() {
        return delimiter;
    }

    public CSVMapReader delimiter(char d) {
        delimiter = d;
        return this;
    }

    public char escapeChar() {
        return escapeChar;
    }

    public CSVMapReader escapeChar(char c) {
        escapeChar = c;
        return this;
    }

    public char quoteChar() {
        return quoteChar;
    }

    public CSVMapReader quoteChar(char c) {
        quoteChar = c;
        return this;
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        if (iterator == null)
            iterator = new MapReader();
        return iterator;
    }

    private class MapReader extends Reader<Map<String, String>> {
        /** read one line in advance for judging whether has next line*/
        private String nextLine;

        /** the number of the line that be handled */
        private int lineno;

        MapReader() {
            super(CSVMapReader.this.delimiter, CSVMapReader.this.quoteChar, CSVMapReader.this.escapeChar);
            try {
                /* skip the first skipRowNum line */
                for (int i = 0; i < skipRowNum; i++) {
                    if (readLine() == null)
                        break;
                }

                /* if does not provide headers, read one line as headers */
                if (headers == null) {
                    String line = readLine();
                    if (line == null)
                        throw new CSVException("Cannot read headers, file may be empty.");

                    headers = Arrays.asList(split(line));
                }

                nextLine = readLine();
            } catch (IOException e) {
                throw new IOError(e.getMessage());
            }
        }

        private String readLine() throws IOException {
            String ret = reader.readLine();
            if (ret != null)
                lineno++;
            return ret;
        }

        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        @Override
        public Map<String, String> next() {
            if (nextLine == null)
                throw new NoSuchElementException();

            String[] items = split(nextLine);
            if (items.length != headers.size()) {
                throw new CSVException(
                        String.format("Invalid csv format in line %d", lineno)
                );
            }

            Map<String, String> ret = new HashMap<>();
            for (int i = 0; i < items.length; i++) {
                ret.put(headers.get(i), items[i]);
            }

            try {
                nextLine = readLine();
            } catch (IOException e) {
                throw new IOError(e.getMessage());
            }

            return ret;
        }


        @Override
        public void remove() {

        }

    }

}
