package xyz.jiel.tools;

import java.io.*;

import xyz.jiel.exceptions.*;
import xyz.jiel.exceptions.IOError;

public class Shutil{
    public static void copyfile(String src, String dst){
        File srcFile = new File(src);
        File dstFile = new File(dst);
        copyfile(srcFile, dstFile);
    }

    /**
    * copy the content of file <code>src</code> to file <code>dst</code>,
    * If <code>src</code> does not exist or is not a file, a unchecked exception
    * is threw. If the path of dst contains at least one unexisted directory, 
    * a unchecked exception is thrown.
    *
    * @param src the file to be copied
    * @param dst the copied file of <code>src</code>
    */
    public static void copyfile(File src, File dst) {
        if(!src.isFile()) {
            throw new FileNotFoundError(
                String.format("File src does not exist or is not a file: %s", src.getPath())
            );
        }

        if(dst.isDirectory()) {
            throw new FileNotFoundError(
                String.format("Except a file, but dst is a directory: %s", dst.getPath())
            );
        }

        try {
            // try to create dst, whether it exists or not.
            dst.createNewFile();
        } catch (IOException e) {
            // the path of dst contains at least one does not existed directory.
            throw new IOError(
                String.format("Cannot create file dst, maybe the path contains unexisted directories: %s", dst.getPath())
            );
        }


        try(FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst)){

            byte[] buffer = new byte[40960];
            int n;
            while((n = fis.read(buffer)) != -1){
                fos.write(buffer, 0, n);
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage());
        }
    }

    public static void move(String src, String dst){
        File srcFile = new File(src);
        File dstFile = new File(dst);
        Shutil.move(srcFile, dstFile);
    }

    /**
     * Move a file or direcotry to another location. It is similar 
     * to the Unix "mv" command.
     * 
     * @param src the source file to be moved
     * @param dst the destination to be moved
     */
    public static void move(File src, File dst){
        copy(src, dst);
        if(src.isFile()) {
            src.delete();
        } else {
            rmtree(src);
        }

    }

    public static void copy(String src, String dst){
        File srcFile = new File(src);
        File dstFile = new File(dst);
        copy(srcFile, dstFile);
    }

    /**
     * Copy a file or a directory to another location. 
     * If <code>dst</code> is a directory, copying <code>src</code>
     * as a child of <code>dst</code>. If <code>dst</code> is an
     * existed file, it will be overwrited.
     * 
     * @param src the source file to be copied.
     * @param dst the destination of a copied file.
     */    
    public static void copy(File src, File dst){
        if (!src.exists()) {
            throw new FileNotFoundError(
                String.format("File src does not exist: %s", src.getPath())
            );
        }

        // Create dst as a file or directory according to src
        if (!dst.exists()){
            if(src.isFile()){
                try {
                    dst.createNewFile();
                } catch (IOException e){
                    throw new IOError(
                        String.format("Cannot create file dst: %s", dst.getPath())
                    );
                }
            } else {
                dst.mkdirs();
            }
        }

        if(src.isDirectory() && dst.isFile()) {
            throw new IOError(
                "cannot copy a directory to a file"
            );
        }

        if(dst.isDirectory()){
            dst = new File(dst, src.getName());
        }
        if(src.isFile()){
            copyfile(src, dst);
        } else {
            copytree(src, dst);
        }
        
    }

    public static void copytree(String olddir, String newdir){
        File oldFile = new File(olddir);
        File newFile = new File(newdir);
        copytree(oldFile, newFile);
    }

    /**
     * Recursively copy a directory to another location. The <code>newdir</code>
     * must not already exist.
     * 
     * @param olddir the source directory
     * @param newdir the targeted directory
     */
    public static void copytree(File olddir, File newdir) {
        if(newdir.exists()) {
            throw new FileNotFoundError(
                String.format("File newdir exists: %s", newdir.getPath())
            );
        }
        if(olddir.isFile() || !olddir.exists()) {
            throw new FileNotFoundError(
                String.format("Except a directory, olddir is a file or does not exist: %s", olddir.getPath())
            );
        }

        _copytree(olddir, newdir);
    }

    private static void _copytree(File olddir, File newdir) {
        newdir.mkdirs();

        for(File file : olddir.listFiles()) {
            if(file.isFile()) {
                copyfile(file, new File(newdir, file.getName()));
            } else {
                _copytree(file, new File(newdir, file.getName()));
            }
        }
    }

    public static void rmtree(String dir) {
        rmtree(new File(dir));
    }

    /**
     * Recursively remove a existed directory.
     * @param dir the directory to be removed.
     */
    public static void rmtree(File dir) {
        if (!dir.exists() || dir.isFile()) {
            throw new FileNotFoundError(
                String.format("File dir does exist or is a file: %s", dir.getPath())
            );
        }

        for(File file : dir.listFiles()) {
            if(file.isFile()) {
                file.delete();
            } else {
                rmtree(file);
                file.delete();
            }
        }
        dir.delete();
    }

    public static void main(String[] args) {
        Shutil.rmtree("C:/Users/jiel/Desktop/org");
    }

}