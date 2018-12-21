package xyz.jiel.csv;

import xyz.jiel.exceptions.FileNotFoundError;
import xyz.jiel.exceptions.IOError;
import xyz.jiel.exceptions.UnsupportedEncodingError;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A slight csv reader, support foreach loop, return an {@code Object} in each loop.
 * An array of header used as the field of the {@code Map} as default, if does not provided,
 * read the first unskipped line as headers. If the {@code fieldMap} is provided, the fields
 * of the returned {@code Object} are got from the values of the {@code fieldMap}.
 */
public class CSVBeanReader implements Iterable<Object> {
    private char delimiter = ',';
    private char escapeChar = '\"';
    private char quoteChar = '\"';

    private Class<?> beanClass;
    private List<String> headers;

    /** the number of line that be to skipped */
    private int skipRowNum;

    private BufferedReader reader;
    private Iterator<Object> iterator;

    /** An map used to transform headers to field names of bean.
     * The key is header, the value is field name.
     */
    private Map<String, String> fieldMap;

    public CSVBeanReader(InputStream is) {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    public CSVBeanReader(File file) {
        this(file, "UTF8");
    }

    public CSVBeanReader(File file, String charsetName) {
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

    public CSVBeanReader(String path) {
        this(path, "UTF8");
    }

    public CSVBeanReader(String path, String charsetName) {
        this(new File(path), charsetName);
    }

    /** Set the number of the line that be skipped */
    public int skip() {
        return skipRowNum;
    }

    public CSVBeanReader skip(int i) {
        skipRowNum = i;
        return this;
    }

    public List<String> headers() {
        return headers;
    }

    public CSVBeanReader headers(List<String> h) {
        headers = h;
        return this;
    }

    public char delimiter() {
        return delimiter;
    }

    public CSVBeanReader delimiter(char d) {
        delimiter = d;
        return this;
    }

    public char escapeChar() {
        return escapeChar;
    }

    public CSVBeanReader escapeChar(char c) {
        escapeChar = c;
        return this;
    }

    public char quoteChar() {
        return quoteChar;
    }

    public CSVBeanReader quoteChar(char c) {
        quoteChar = c;
        return this;
    }

    public Class<?> beanClass() {
        return beanClass;
    }

    public CSVBeanReader beanClass(Class<?> c) {
        beanClass = c;
        return this;
    }

    public Map<String, String> fieldMap() {
        return fieldMap;
    }

    public CSVBeanReader fieldMap(Map<String, String> m) {
        fieldMap = m;
        return this;
    }

    @Override
    public Iterator<Object> iterator() {
        if (iterator == null)
            iterator = new BeanReader();
        return iterator;
    }

    private class BeanReader extends Reader<Object> {
        /** read one line in advance for judging whether has next line*/
        private String nextLine;

        /** the number of the line that be handled */
        private int lineno;

        BeanReader() {
            super(CSVBeanReader.this.delimiter, CSVBeanReader.this.quoteChar, CSVBeanReader.this.escapeChar);
            if (beanClass == null) {
                throw new CSVException("You should set a bean by beanClass(Class) at first");
            }

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
        public Object next() {
            if (nextLine == null)
                throw new NoSuchElementException();

            String[] items = split(nextLine);
            if (items.length != headers.size()) {
                throw new CSVException(
                        String.format("Invalid csv format in line %d", lineno)
                );
            }

            Object ret;
            try {
                ret = beanClass.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new CSVException(String.format(
                        "The class %s is an abstract class.",
                        beanClass.getName()
                ));
            } catch (InvocationTargetException e) {
                throw new CSVException(String.format(
                        "The default constructor of class %s throws an exception.",
                        beanClass.getName()
                ));
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new CSVException(String.format(
                        "The class %s have not a public default constructor.",
                        beanClass.getName()
                ));
            }

            /* inject values to the ret */
            for (int i = 0; i < items.length; i++) {
                String header = headers.get(i);
                String fieldName;

                /* if fieldMap is not null, transform to get the true field names.
                 * Otherwise, the headers used as the field names.
                 */
                if (fieldMap != null) {
                    fieldName = fieldMap.get(header);
                    if (fieldName == null)
                        fieldName = "";
                } else {
                    fieldName = header;
                }

                // ignore the field name which is blank.
                if (fieldName.equals(""))
                    continue;

                String value = items[i];

                Field field;
                try {
                    field = beanClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new CSVException(String.format(
                            "The class %s have not a field %s",
                            beanClass.getName(), fieldName
                    ));
                }

                try {
                    setProperty(field, ret, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException | InstantiationException e) {
                    throw new CSVException(String.format(
                            "The class of %s.%s cannot be instantiated by %s(String)",
                            beanClass.getName(), field.getName(), field.getType().getName()
                    ));
                } catch (InvocationTargetException e) {
                    throw new CSVException(String.format(
                            "The constructor %s(String) throws an exception, when parsing line %d",
                            field.getType().getName(), lineno
                    ));
                }

            }

            try {
                nextLine = readLine();
            } catch (IOException e) {
                throw new IOError(e.getMessage());
            }

            return ret;
        }

        /**
         * Set the {@code value} to the {@code field} in the {@code obj}. Transform the {@code value}
         * to a proper type, if the type of the {@code field} is not primitive type, try to construct it
         * by the constructor with an {@code String} parameter.
         *
         * @param field the field of the {@code obj}'s class.
         * @param obj the Object need to be injected.
         * @param value the value to be injected into the {@code obj}.
         * @throws IllegalAccessException
         * @throws NoSuchMethodException
         * @throws InvocationTargetException
         * @throws InstantiationException
         */
        private void setProperty(Field field, Object obj, String value) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
            Class type = field.getType();
            field.setAccessible(true);
            if (type == Character.TYPE) {
                field.setChar(obj, value.charAt(0));
            } else if (type == Short.TYPE) {
                field.setShort(obj, Short.parseShort(value));
            } else if (type == Integer.TYPE) {
                field.setInt(obj, Integer.parseInt(value));
            } else if (type == Long.TYPE) {
                field.setLong(obj, Long.parseLong(value));
            } else if (type == Float.TYPE) {
                field.setFloat(obj, Float.parseFloat(value));
            } else if (type == Double.TYPE) {
                field.setDouble(obj, Double.parseDouble(value));
            } else if (type == Byte.TYPE) {
                field.setByte(obj, Byte.parseByte(value));
            } else if (type == Boolean.TYPE) {
                field.setBoolean(obj, Boolean.parseBoolean(value));
            } else {
                Class<?> fieldClass = field.getType();
                Constructor constructor = fieldClass.getDeclaredConstructor(String.class);
                Object fieldInstance = constructor.newInstance(value);
                field.set(obj, fieldInstance);
            }
        }

        @Override
        public void remove() {

        }
    }

}
