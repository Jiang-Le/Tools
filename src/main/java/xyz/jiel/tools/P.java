package xyz.jiel.tools;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.*;

public class P{
    public static List<Integer> range(int end){
        return P.range(0, end, 1);
    }

    public static List<Integer> range(int start, int end){
        return P.range(start, end, 1);
    }

    public static List<Integer> range(int start, int end, int step){
        if (step == 0 || (start < end && step < 0))
            return new ArrayList<>();
        List<Integer> rets = new ArrayList<>();
        int sign = 1;
        if (step < 0)
            sign = -1;
        for(int i = start; i * sign < end * sign; i += step){
            rets.add(i);
        }
        return rets;
    }

    public static Printer printer(){
        return new Printer();
    }

    public static final class Printer{
        private String sep = " ";
        private PrintStream stream = System.out;
        private String end = "\n";
        private boolean flush = false;

        public Printer sep(String s){ sep = s; return this; }
        public Printer stream(PrintStream s){ stream = s; return this; }
        public Printer end(String e) { end = e; return this; }
        public Printer flush(boolean b) { flush = b; return this; }

        public void print(Collection<?> c){
            printCollection(c);
            stream.print(end);
        }

        public void print(Map<?, ?> m){
            printMap(m);
            stream.print(end);
        }

        private void intelligentPrint(Object obj) {
            if(obj == null){
                stream.print("null");
            }else if(obj.getClass().isArray()){
                printArray(getArray(obj));
            } else if(obj instanceof Collection){
                printCollection((Collection)obj);
            } else if(obj instanceof Map){
                printMap((Map)obj);
            } else if(obj instanceof Map.Entry){
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)obj;
                stream.print(entry.getKey());
                stream.print(": ");
                stream.print(entry.getValue());
            } else {
                stream.print(obj);
            }
        }

        public void print(Object... values){
            int len = values.length;
            for(int i = 0; i < len; i++){
                Object obj = values[i];
                intelligentPrint(obj);
                if(i != len-1){
                    stream.print(sep);
                }

            }
            stream.print(end);
            if(flush){
                stream.flush();
            }
        }

        public void print(Iterator iterator){
            _print(sep, end, iterator);
        }

        private static Object[] getArray(Object val){
            if(val instanceof Object[]){
                return (Object[]) val;
            }
            int len = Array.getLength(val);
            Object[] obj = new Object[len];
            for(int i = 0; i < len; i++){
                obj[i] = Array.get(val, i);
            }
            return obj;
        }

        private void _print(String s, String e, Iterator iterator){
            while(iterator.hasNext()){
                Object obj = iterator.next();
                intelligentPrint(obj);
                if(iterator.hasNext()){
                    stream.print(s);
                }
            }
            stream.print(e);
            if(flush){
                stream.flush();
            }
        }

        private void printCollection(Collection<?> c){
            if(c instanceof List){
                stream.print('[');
                _print(", ", "", c.iterator());
                stream.print(']');
                
            } else if(c instanceof Set){
                stream.print('{');
                _print(", ", "", c.iterator());
                stream.print('}');
            }
        }

        private void printMap(Map<?, ?> m){
            stream.print('{');
            _print(", ", "", m.entrySet().iterator());
            stream.print('}');
        }

        private void printArray(Object[] array){
            printCollection(Arrays.asList(array));
        }

    }
}