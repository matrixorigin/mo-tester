package io.mo;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        File file = new File("cases/");
        File[] files = file.listFiles();
        List fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                File f1 = (File)o1;
                File f2 = (File)o2;
                if (f1.isDirectory() && f2.isFile())
                    return -1;

                if (f1.isFile() && f2.isDirectory())
                    return 1;

                return f1.getName().compareTo(f2.getName());
            }
        });

        for (File file1 : files) {

            System.out.println(file1.getName());

        }
    }
}
