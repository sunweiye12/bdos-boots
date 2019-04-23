/*
 * 文件名：ZipUtil.java
 * 版权：Copyright by www.bonc.com.cn
 * 描述：
 * 修改人：ke_wang
 * 修改时间：2016年11月14日
 * 跟踪单号：
 * 修改单号：
 * 修改内容：
 */

package com.bonc.bdos.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;

/**
 * java处理压缩包公共类 目前支持：1.浏览tar类型中的文件列表 镜像和容器快照的tar包
 * 
 * @author ke_wang
 * @version 2016年11月14日
 * @see ZipUtil
 * @since
 */

public class ZipUtil {
    /**
     * 输出日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(ZipUtil.class);

    /**
     *
     * Description: 浏览tar文件,判断是否包含estimate数组中的文件名
     * 
     * @param targzFile
     * @throws IOException
     * @see
     */
    public static boolean visitTAR(File targzFile, String... estimate) throws IOException {
        FileInputStream fileIn = null;
        BufferedInputStream bufIn = null;
        TarArchiveInputStream taris = null;
        boolean flag = false;
        try {
            fileIn = new FileInputStream(targzFile);
            bufIn = new BufferedInputStream(fileIn);
            taris = new TarArchiveInputStream(bufIn);
            TarArchiveEntry entry = null;
            while ((entry = taris.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (estimate.length > 0) {
                    for (String element : estimate) {
                        if (entry.getName().trim().equals(element)) {
                            LOG.info("********find specific file name *********** filename:-" + element);
                            flag = true;
                            break;
                        }
                    }
                }
                if (flag) {
                    break;
                }
            }
            return flag;
        } finally {
            taris.close();
            bufIn.close();
            fileIn.close();
        }
    }

    /**
     * 解压tar包
     * 
     * @param filename
     *            tar文件
     * @param directory
     *            解压目录
     * @return
     */
    public static boolean extTarFileList(File targzFile, String directory, String fileName) {
        boolean flag = false;
        OutputStream out = null;
        try {
            TarInputStream in = new TarInputStream(new FileInputStream(targzFile));
            TarEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (fileName.equals(entry.getName())) {
                    File outfile = new File(directory + "/" + entry.getName());
                    new File(outfile.getParent()).mkdirs();
                    out = new BufferedOutputStream(new FileOutputStream(outfile));
                    int x = 0;
                    while ((x = in.read()) != -1) {
                        out.write(x);
                    }
                    out.close();
                }
            }
            in.close();
            flag = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            flag = false;
        }
        return flag;
    }

    /**
     * getImageId:解析镜像文件，获取镜像id. <br/>
     *
     * @param directory
     * @param fileName
     * @return String
     */
    public static String getImageId(String directory, String fileName) {
        String result = null;
        try {
            File file = new File(directory + "/" + fileName);
            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(file));
            String fileString = null;
            fileString = reader.readLine();
            if (fileString != null) {
                System.out.println("File:" + directory + "/" + fileName + ":" + fileString);
                @SuppressWarnings("rawtypes")
                List<Map> array = JSONObject.parseArray(fileString, Map.class);
                if (array.size() > 0) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = array.get(0);
                    result = map.get("Config").replace(".json", "");
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除指定文件夹下所有文件
     * 
     * @param path
     *            文件夹完整绝对路径 ,"Z:/xuyun/save"
     */
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (String element : tempList) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + element);
            } else {
                temp = new File(path + File.separator + element);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + element);// 先删除文件夹里面的文件
                delFolder(path + "/" + element);// 再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 删除文件夹
     * 
     * @param folderPath
     *            文件夹完整绝对路径 ,"Z:/xxx/save"
     */
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // 删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); // 删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除指定文件
     * 
     * @param abpath
     * @param format格式
     *            void
     */
    public static void deleteFile(String abpath, String format) {
        String[] ss = abpath.split("/");
        String name = ss[ss.length - 1];
        String path = abpath.replace("/" + name, "");

        File file = new File(path);// 里面输入特定目录
        File temp = null;
        File[] filelist = file.listFiles();
        for (File element : filelist) {
            temp = element;
            if (temp.getName().endsWith(format) && !temp.getName().endsWith(name))// 获得文件名，如果后缀为“”，这个你自己写，就删除文件
            {
                temp.delete();// 删除文件}
            }
        }
    }

    /**
     * 解压缩 xxx.zip
     * 
     * @param sourceFile
     *            要解压缩的文件的路径
     * @param destDir
     *            解压缩后的目录路径
     * @throws Exception
     */
    public static void deCompress(String sourceFile, String destDir) throws Exception {
        // 创建需要解压缩的文件对象
        File file = new File(sourceFile);
        if (!file.exists()) {
            throw new RuntimeException(sourceFile + "不存在！");
        }
        // 创建解压缩的文件目录对象
        File destDiretory = new File(destDir);
        if (!destDiretory.exists()) {
            destDiretory.mkdirs();
        }
        /*
         * 保证文件夹路径最后是"/"或者"\" charAt()返回指定索引位置的char值
         */
        char lastChar = destDir.charAt(destDir.length() - 1);
        if (lastChar != '/' && lastChar != '\\') {
            // 在最后加上分隔符
            destDir += File.separator;
        }
        unzip(sourceFile, destDir);
    }

    /**
     * 解压方法 需要xxx.zip
     */
    public static void unzip(String sourceZip, String destDir) throws Exception {
        try {
            Project p = new Project();
            Expand e = new Expand();
            e.setProject(p);
            e.setSrc(new File(sourceZip));
            e.setOverwrite(false);
            e.setDest(new File(destDir));
            e.execute();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 上传方法
     * 
     * @param files
     * @param path
     *            上传路径
     * @return String
     */
    public static String upload(MultipartFile files, String path) {
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;

        // 判断存储的文件夹是否存在
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        try {
            if (!files.isEmpty()) {
                String originalFilename = files.getOriginalFilename();
                // 格式限制，非wav格式的不上传
                /*
                 * if(!suffixList.contains(suffix)) { continue; }
                 */
                // 读取文件
                bis = new BufferedInputStream(files.getInputStream());
                // 指定存储的路径
                bos = new BufferedOutputStream(new FileOutputStream(path + originalFilename));
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                // 刷新此缓冲的输出流，保证数据全部都能写出
                bos.flush();
            }
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.close();
            }
            return "ok";
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "error";
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
    }
}
