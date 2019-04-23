package com.bonc.bdos.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件读写工具
 * 
 * @author Administrator
 * 
 */
public class FileHandler {

    private static Logger logger = LoggerFactory.getLogger(FileHandler.class);

    private static final String UTF_8 = "utf-8";

    private FileHandler() {

    }

    /**
     * 读入流到字符串列表，默认不去除UTF-8的BOM文件头
     * 
     * @param is
     * @return
     * @throws IOException
     */
    public static List<String> readFileToList(InputStream is) throws IOException {
        return readFileToList(is, false);
    }

    /**
     * 读入流到字符串列表
     * 
     * @param is
     * @param doesRemoveBomOfUTF8
     *            是否去除UTF-8的BOM文件头
     * @return
     * @throws IOException
     */
    public static List<String> readFileToList(InputStream is, boolean doesRemoveBomOfUTF8) throws IOException {
        List<String> stringList = new ArrayList<>();
        BufferedReader input = new BufferedReader(new InputStreamReader(is, UTF_8));
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            if (doesRemoveBomOfUTF8) {
                line = removeBOMofUTF8(line); // 去除UTF8文件的BOM头
            }
            stringList.add(line);
        }
        input.close();
        return stringList;
    }

    /**
     * 读入文件到字符串列表，默认不去除UTF-8的BOM文件头
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public static List<String> readFileToList(String fileName) throws IOException {
        return readFileToList(fileName, false);
    }

    /**
     * 读入文件到字符串列表
     * 
     * @param fileName
     * @param doesRemoveBomOfUTF8
     *            是否去除UTF-8的BOM文件头
     * @return
     * @throws IOException
     */
    public static List<String> readFileToList(String fileName, boolean doesRemoveBomOfUTF8) throws IOException {
        try {
            return readFileToList(new FileInputStream(fileName), doesRemoveBomOfUTF8);
        } catch (FileNotFoundException e) {
            logger.error("Read file error. File is not found! " + fileName, e);

            throw e;
        }
    }

    /**
     * 读入流到字符中，默认不去除UTF-8的BOM文件头
     * 
     * @param is
     * @return
     * @throws IOException
     */
    public static String readFileToString(InputStream is) throws IOException {
        return readFileToString(is, false);
    }

    /**
     * 读入流到字符中
     * 
     * @param is
     * @param doesRemoveBomOfUTF8
     *            是否去除UTF-8的BOM文件头
     * @return
     * @throws IOException
     */
    public static String readFileToString(InputStream is, boolean doesRemoveBomOfUTF8) throws IOException {
        List<String> stringList = readFileToList(is, doesRemoveBomOfUTF8);
        StringBuilder sb = new StringBuilder();
        for (String string : stringList) {
            sb.append(string);
        }
        return sb.toString();
    }

    /**
     * 读入文件到字符中，默认不去除UTF-8的BOM文件头
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String readFileToString(String fileName) throws IOException {
        return readFileToString(fileName, false);
    }

    /**
     * 读入文件到字符中
     * 
     * @param fileName
     * @param doesRemoveBomOfUTF8
     *            是否去除UTF-8的BOM文件头
     * @return
     * @throws IOException
     */
    public static String readFileToString(String fileName, boolean doesRemoveBomOfUTF8) throws IOException {
        return readFileToString(new FileInputStream(fileName), doesRemoveBomOfUTF8);
    }

    /**
     * 将字符串列表写入到文件里
     * 
     * @param stringList
     * @param fileName
     * @throws IOException
     */
    public static void writeListToFile(List<String> stringList, String fileName) throws IOException {
        try {
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), UTF_8));
            StringBuilder sb = new StringBuilder();
            for (String string : stringList) {
                sb.append(string + "\n");
            }
            output.write(sb.toString());
            output.close();
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException 错误信息: " + e.getMessage(), e);
            throw e;
        } catch (FileNotFoundException e) {
            logger.error("File Not Found: " + fileName, e);

            throw e;
        } catch (IOException e) {
            logger.error("IOException 错误信息: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 
     * @Title: readFileToList @param @param is @param @param
     *         charsetName @param @param
     *         doesRemoveBomOfUTF8 @param @return @param @throws IOException
     *         设定文件 @return List<String> 返回类型 @throws
     */
    public static List<String> readFileToList(InputStream is, String charsetName, boolean doesRemoveBomOfUTF8)
            throws IOException {
        List<String> stringList = new ArrayList<>();
        BufferedReader input = new BufferedReader(new InputStreamReader(is, charsetName));
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            if (UTF_8.equalsIgnoreCase(charsetName) && doesRemoveBomOfUTF8) {
                line = removeBOMofUTF8(line); // 去除UTF8文件的BOM头
            }
            stringList.add(line);
        }
        input.close();
        return stringList;
    }

    /**
     * 
     * @Title: readFileToString @param @param fileName @param @param
     *         charsetName @param @return @param @throws IOException
     *         设定文件 @return String 返回类型 @throws
     */
    public static String readFileToString(String fileName, String charsetName) throws IOException {
        List<String> stringList = readFileToList(new FileInputStream(fileName), charsetName, false);
        StringBuilder sb = new StringBuilder();
        for (String string : stringList) {
            sb.append(string);
        }
        return sb.toString();
    }

    public static List<String> readFileToList(String fileName, String charsetName) throws IOException {
        return readFileToList(new FileInputStream(fileName), charsetName, false);
    }

    /**
     * 
     * @Title: writeListToFile @param @param stringList @param @param
     *         fileName @param @param charsetName @param @throws IOException
     *         设定文件 @return void 返回类型 @throws
     */
    public static void writeListToFile(List<String> stringList, String fileName, String charsetName)
            throws IOException {
        try {
            BufferedWriter output = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(fileName), charsetName));
            StringBuilder sb = new StringBuilder();
            for (String string : stringList) {
                sb.append(string + "\n");
            }
            output.write(sb.toString());
            output.close();
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException 错误信息: " + e);
            throw e;
        } catch (FileNotFoundException e) {
            logger.error("File Not Found: " + fileName, e);

            throw e;
        } catch (IOException e) {
            logger.error("IOException 错误信息: " + e);
            throw e;
        }
    }

    /**
     * 去除UTF-8的BOM文件头
     * 
     * @param text
     * @return
     */
    public static String removeBOMofUTF8(String text) {
        String bom = String.valueOf((char) 65279); // BOM头
        return text.startsWith(bom) ? text.substring(bom.length(), text.length()) : text;
    }

    public static void convertFile(String filepath, String inputCode, String outputpath, String outputCode) {
        try {
            writeListToFile(readFileToList(filepath, inputCode), outputpath, outputCode);
        } catch (IOException e) {
            logger.error("IO 错误信息: " + e.getMessage(), e);
        }
    }

    public static void convertDir(String inputDir, String inputCode, String outputDir, String outputCode) {
        String inputDirectory = inputDir;
        String outputDirectory = outputDir;

        if (inputDir.endsWith("\\") || inputDir.endsWith(File.separator)) {
            inputDirectory = inputDir.substring(0, inputDir.length() - 1);
        }
        if (outputDir.endsWith("\\") || outputDir.endsWith(File.separator)) {
            outputDirectory = outputDir.substring(0, outputDir.length() - 1);
        }
        File dirFile = new File(inputDirectory);
        File outputFile = new File(outputDirectory);
        if (!outputFile.exists()) {
            outputFile.mkdirs();
        }
        if (dirFile != null) {
            File[] texts = getTexts(dirFile);

            for (int i = 0; i < texts.length; i++) {
                String absPath = texts[i].getAbsolutePath();
                String outputfile = outputDir + File.separator + texts[i].getName();
                try {
                    convertFile(absPath, inputCode, outputfile, outputCode);
                } catch (Exception e) {
                    logger.error("错误信息: " + e.getMessage(), e);
                }
            }

            File[] dirs = getDir(dirFile);
            for (File dir : dirs) {
                String fileNewDir = dir.getAbsolutePath();
                String outputNewDir = fileNewDir.replace(inputDirectory, outputDirectory);
                convertDir(fileNewDir, inputCode, outputNewDir, outputCode);
            }
        }
    }

    private static File[] getTexts(File dirFile) {
        return dirFile.listFiles(new FileFilter() {
            // file 过滤目录文件名
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".txt") && pathname.canRead();
            }
        });

    }

    private static File[] getDir(File dirFile) {
        return dirFile.listFiles(new FileFilter() {
            // file 过滤目录文件名
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

    }

}
